import os
import sqlite3
import time
import requests
from flask import Flask, render_template, request, jsonify

app = Flask(__name__)

# Paths
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DB_PATH = os.path.join(BASE_DIR, "app", "src", "main", "assets", "databases", "manchorati_app.db")

# Firebase Config
FIREBASE_PROJECT_ID = "manchorati-4ace8"
FIRESTORE_BASE_URL = f"https://firestore.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/databases/(default)/documents"

def get_sqlite_conn():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def parse_firestore_doc(doc):
    doc_id = doc.get("name", "").split("/")[-1]
    raw_fields = doc.get("fields", {})
    parsed = {"id": doc_id}
    for k, v in raw_fields.items():
        if "stringValue" in v:
            parsed[k] = v["stringValue"]
        elif "integerValue" in v:
            parsed[k] = int(v["integerValue"])
        elif "booleanValue" in v:
            parsed[k] = v["booleanValue"]
        elif "doubleValue" in v:
            parsed[k] = float(v["doubleValue"])
        elif "timestampValue" in v:
            parsed[k] = v["timestampValue"]
        else:
            parsed[k] = str(v)
    return parsed

def make_firestore_fields(data):
    fields = {}
    for k, v in data.items():
        if isinstance(v, bool):
            fields[k] = {"booleanValue": v}
        elif isinstance(v, int):
            fields[k] = {"integerValue": str(v)}
        elif isinstance(v, float):
            fields[k] = {"doubleValue": v}
        elif isinstance(v, str):
            fields[k] = {"stringValue": v}
        elif v is None:
            fields[k] = {"nullValue": None}
    return {"fields": fields}

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/api/stats")
def get_stats():
    try:
        # SQLite Stats
        conn = get_sqlite_conn()
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM posts")
        total_posts_db = cur.fetchone()[0]
        cur.execute("SELECT COUNT(*) FROM categories")
        total_cats_db = cur.fetchone()[0]
        conn.close()

        # Firestore Stats
        posts_r = requests.get(f"{FIRESTORE_BASE_URL}/community_posts?pageSize=100")
        community_posts_count = len(posts_r.json().get("documents", [])) if posts_r.status_code == 200 else 0

        users_r = requests.get(f"{FIRESTORE_BASE_URL}/users?pageSize=100")
        users_count = len(users_r.json().get("documents", [])) if users_r.status_code == 200 else 0

        # Count reported posts
        reported_count = 0
        if posts_r.status_code == 200:
            for doc in posts_r.json().get("documents", []):
                p = parse_firestore_doc(doc)
                if p.get("reportCount", 0) > 0:
                    reported_count += 1

        return jsonify({
            "success": True,
            "total_posts_pc": total_posts_db,
            "total_categories_pc": total_cats_db,
            "community_posts": community_posts_count,
            "users_count": users_count,
            "reported_count": reported_count
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/categories")
def get_categories():
    try:
        conn = get_sqlite_conn()
        cur = conn.cursor()
        cur.execute("SELECT id, name FROM categories ORDER BY id ASC")
        categories = [{"id": row["id"], "name": row["name"].strip()} for row in cur.fetchall()]
        conn.close()
        return jsonify({"success": True, "categories": categories})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/quotes/add", methods=["POST"])
def add_quote():
    data = request.json or {}
    category_id = data.get("category_id")
    category_name = data.get("category_name", "").strip()
    content = data.get("content", "").strip()
    post_to_community = data.get("post_to_community", True)

    if not content or not category_id:
        return jsonify({"success": False, "error": "يرجى كتابة نص الاقتباس واختيار القسم"}), 400

    try:
        # 1. Insert into PC SQLite DB (Permanent pre-build persistence)
        conn = get_sqlite_conn()
        cur = conn.cursor()
        cur.execute("SELECT id FROM posts WHERE content = ?", (content,))
        existing = cur.fetchone()
        inserted_pc = False
        if not existing:
            cur.execute(
                "INSERT INTO posts (category_id, content, is_favorite, is_featured) VALUES (?, ?, 0, 0)",
                (int(category_id), content)
            )
            conn.commit()
            inserted_pc = True
        conn.close()

        # 2. Push to Firestore 'category_updates' (Syncs to all user devices)
        timestamp_ms = int(time.time() * 1000)
        sync_payload = make_firestore_fields({
            "category_name": category_name,
            "content": content,
            "timestamp": timestamp_ms
        })
        requests.post(f"{FIRESTORE_BASE_URL}/category_updates", json=sync_payload)

        # 3. Optionally push to 'community_posts' feed as Official Admin Post
        if post_to_community:
            community_payload = make_firestore_fields({
                "authorId": "admin_official",
                "authorName": "إدارة منشوراتي",
                "authorUsername": "admin",
                "authorBadge": "official",
                "authorPhotoUrl": "",
                "content": content,
                "bgId": "#1E293B",
                "fontId": "tajawal",
                "textColor": "#FFFFFF",
                "tag": category_name,
                "likesCount": 0,
                "commentsCount": 0,
                "reportCount": 0,
                "postType": "TEXT",
                "timestamp": timestamp_ms
            })
            requests.post(f"{FIRESTORE_BASE_URL}/community_posts", json=community_payload)

        return jsonify({
            "success": True,
            "inserted_pc": inserted_pc,
            "message": "تم حفظ المنشور في قاعدة بيانات الحاسوب ومزامنته مع جميع الهواتف بنجاح!"
        })
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/posts")
def get_community_posts():
    try:
        r = requests.get(f"{FIRESTORE_BASE_URL}/community_posts?pageSize=100")
        if r.status_code != 200:
            return jsonify({"success": False, "error": r.text}), r.status_code

        documents = r.json().get("documents", [])
        posts = [parse_firestore_doc(doc) for doc in documents]
        # Sort descending by timestamp
        posts.sort(key=lambda p: p.get("timestamp", 0), reverse=True)
        return jsonify({"success": True, "posts": posts})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/posts/<post_id>", methods=["DELETE"])
def delete_community_post(post_id):
    try:
        r = requests.delete(f"{FIRESTORE_BASE_URL}/community_posts/{post_id}")
        if r.status_code in [200, 204]:
            return jsonify({"success": True, "message": "تم حذف المنشور بنجاح"})
        return jsonify({"success": False, "error": r.text}), r.status_code
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/users")
def get_users():
    try:
        r = requests.get(f"{FIRESTORE_BASE_URL}/users?pageSize=100")
        if r.status_code != 200:
            return jsonify({"success": False, "error": r.text}), r.status_code

        documents = r.json().get("documents", [])
        users = [parse_firestore_doc(doc) for doc in documents]
        return jsonify({"success": True, "users": users})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/users/<user_id>/block", methods=["POST"])
def toggle_block_user(user_id):
    try:
        data = request.json or {}
        is_blocked = data.get("is_blocked", True)
        update_payload = {
            "fields": {
                "isBlocked": {"booleanValue": is_blocked}
            }
        }
        url = f"{FIRESTORE_BASE_URL}/users/{user_id}?updateMask.fieldPaths=isBlocked"
        r = requests.patch(url, json=update_payload)
        if r.status_code in [200, 204]:
            status_txt = "تم حظر المستخدم" if is_blocked else "تم فك الحظر عن المستخدم"
            return jsonify({"success": True, "message": status_txt})
        return jsonify({"success": False, "error": r.text}), r.status_code
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/users/<user_id>/warn", methods=["POST"])
def send_user_warning(user_id):
    try:
        data = request.json or {}
        warning_msg = data.get("warning", "").strip()
        if not warning_msg:
            return jsonify({"success": False, "error": "يرجى كتابة نص التحذير"}), 400

        update_payload = {
            "fields": {
                "adminWarning": {"stringValue": warning_msg}
            }
        }
        url = f"{FIRESTORE_BASE_URL}/users/{user_id}?updateMask.fieldPaths=adminWarning"
        r = requests.patch(url, json=update_payload)
        if r.status_code in [200, 204]:
            return jsonify({"success": True, "message": "تم إرسال التحذير للمستخدم بنجاح"})
        return jsonify({"success": False, "error": r.text}), r.status_code
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/announcements/send", methods=["POST"])
def send_announcement():
    try:
        data = request.json or {}
        title = data.get("title", "").strip()
        message = data.get("message", "").strip()

        if not title or not message:
            return jsonify({"success": False, "error": "يرجى كتابة عنوان ونص الإشعار"}), 400

        payload = make_firestore_fields({
            "title": title,
            "message": message,
            "timestamp": int(time.time() * 1000)
        })
        r = requests.post(f"{FIRESTORE_BASE_URL}/app_announcements", json=payload)
        if r.status_code in [200, 201]:
            return jsonify({"success": True, "message": "تم بث الإشعار العام لجميع المستخدمين بنجاح!"})
        return jsonify({"success": False, "error": r.text}), r.status_code
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/ai_config", methods=["GET", "POST"])
def manage_ai_config():
    try:
        doc_url = f"{FIRESTORE_BASE_URL}/category_updates/ai_settings"
        if request.method == "POST":
            data = request.json or {}
            key = data.get("gemini_api_key", "").strip()
            payload = make_firestore_fields({
                "gemini_api_key": key,
                "updatedAt": int(time.time() * 1000)
            })
            r = requests.patch(f"{doc_url}?updateMask.fieldPaths=gemini_api_key&updateMask.fieldPaths=updatedAt", json=payload)
            if r.status_code in [200, 201]:
                return jsonify({"success": True, "message": "تم حفظ ونشر مفتاح Gemini API بنجاح على جميع أجهزة التطبيق!"})
            return jsonify({"success": False, "error": r.text}), r.status_code
        else:
            r = requests.get(doc_url)
            if r.status_code == 200:
                doc = parse_firestore_doc(r.json())
                return jsonify({"success": True, "gemini_api_key": doc.get("gemini_api_key", "")})
            return jsonify({"success": True, "gemini_api_key": ""})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)}), 500

if __name__ == "__main__":
    print("=" * 60)
    print("  🚀 لوحة تحكم تطبيق منشوراتي (Manchorati Admin Dashboard)")
    print(f"  📂 مسار قاعدة البيانات: {DB_PATH}")
    print(f"  🌐 الرابط المحلي: http://localhost:5000")
    print("=" * 60)
    app.run(host="127.0.0.1", port=5000, debug=True)

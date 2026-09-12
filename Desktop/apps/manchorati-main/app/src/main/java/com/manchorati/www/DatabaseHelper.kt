package com.manchorati.www

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "manchorati_app.db"
        private const val ASSET_DB_PATH = "databases/manchorati_app.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_CATEGORIES = "categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_HAS_NEW = "has_new"

        const val TABLE_POSTS = "posts"
        const val COL_POST_ID = "id"
        const val COL_POST_CAT_ID = "category_id"
        const val COL_POST_CONTENT = "content"
        const val COL_POST_FAVORITE = "is_favorite"
        const val COL_POST_FEATURED = "is_featured"
    }

    private val dbPath: String = context.getDatabasePath(DATABASE_NAME).absolutePath

    init {
        // نسخ قاعدة البيانات من الـ assets عند أول تشغيل فقط
        copyDatabaseFromAssetsIfNeeded()
    }

 

///////////////////////////////////////////
 
private fun copyDatabaseFromAssetsIfNeeded() {
        val prefs = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
        // نتحقق مما إذا تم نسخ هذا الإصدار من قاعدة البيانات من قبل
        val isDbCopied = prefs.getInt("copied_db_version", 0) == DATABASE_VERSION
        val dbFile = File(dbPath)

        if (!isDbCopied || !dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            
            // حذف أي ملف قديم متضارب
            if (dbFile.exists()) {
                dbFile.delete()
            }

            try {
                val input: InputStream = context.assets.open(ASSET_DB_PATH)
                val output: OutputStream = FileOutputStream(dbPath)
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
                output.flush()
                output.close()
                input.close()

                // حفظ علامة تفيد بنجاح النسخ لهذا الإصدار لكي لا تتكرر العملية
                prefs.edit().putInt("copied_db_version", DATABASE_VERSION).apply()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
///////////////////////////////////////////////////

    fun getRandomPostByCategoryId(categoryId: Int): Post? {
            val db = readableDatabase
            // استعلام لاختيار منشور عشوائي من القسم المحدد
            val cursor = db.rawQuery(
                "SELECT * FROM posts WHERE category_id = ? ORDER BY RANDOM() LIMIT 1",
                arrayOf(categoryId.toString())
            )

            var post: Post? = null
            if (cursor.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                val catId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"))
                // تأكد من تمرير باقي الحقول حسب بنية Post عندك
                post = Post(id = id, content = content, categoryId = catId)
            }
            cursor.close()
            return post
    }

    fun searchPosts(keyword: String): List<Post> {
        return queryPosts(
            "SELECT p.*, c.$COL_CAT_NAME FROM $TABLE_POSTS p JOIN $TABLE_CATEGORIES c ON p.$COL_POST_CAT_ID = c.$COL_CAT_ID WHERE p.$COL_POST_CONTENT LIKE ?",
            arrayOf("%$keyword%")
        )
    }
    fun getAllCategories(): List<Category> = getCategoriesWithCounts()
    override fun onCreate(db: SQLiteDatabase) {
        // القاعدة تُنسخ جاهزة من assets، لكن نضع الهيكل كخطة بديلة (Fallback)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL,
                $COL_CAT_HAS_NEW INTEGER DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_POSTS (
                $COL_POST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_POST_CAT_ID INTEGER NOT NULL,
                $COL_POST_CONTENT TEXT NOT NULL,
                $COL_POST_FAVORITE INTEGER DEFAULT 0,
                $COL_POST_FEATURED INTEGER DEFAULT 0,
                FOREIGN KEY ($COL_POST_CAT_ID) REFERENCES $TABLE_CATEGORIES($COL_CAT_ID)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        File(dbPath).delete()
        copyDatabaseFromAssetsIfNeeded()
    }

 // قراءة الأقسام والعداد وحالة NEW بترتيب الـ ID الثابت
    fun getCategoriesWithCounts(): List<Category> {
        val list = mutableListOf<Category>()
        val db = readableDatabase
        
        ensureHasNewColumn(db)

        val query = """
            SELECT c.$COL_CAT_ID, c.$COL_CAT_NAME, c.$COL_CAT_HAS_NEW, COUNT(p.$COL_POST_ID) as count
            FROM $TABLE_CATEGORIES c
            LEFT JOIN $TABLE_POSTS p ON c.$COL_CAT_ID = p.$COL_POST_CAT_ID
            GROUP BY c.$COL_CAT_ID, c.$COL_CAT_NAME
            ORDER BY c.$COL_CAT_ID ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME))
                val hasNew = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_HAS_NEW)) == 1
                val count = cursor.getInt(cursor.getColumnIndexOrThrow("count"))
                list.add(Category(id = id, name = name, postsCount = count, hasNew = hasNew))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getPostsByCategory(categoryId: Int): List<Post> {
        return queryPosts(
            "SELECT p.*, c.$COL_CAT_NAME FROM $TABLE_POSTS p JOIN $TABLE_CATEGORIES c ON p.$COL_POST_CAT_ID = c.$COL_CAT_ID WHERE p.$COL_POST_CAT_ID = ?",
            arrayOf(categoryId.toString())
        )
    }

    fun getFeaturedPosts(): List<Post> {
        return queryPosts(
            "SELECT p.*, c.$COL_CAT_NAME FROM $TABLE_POSTS p JOIN $TABLE_CATEGORIES c ON p.$COL_POST_CAT_ID = c.$COL_CAT_ID WHERE p.$COL_POST_FEATURED = 1",
            null
        )
    }

    fun getFavoritePosts(): List<Post> {
        return queryPosts(
            "SELECT p.*, c.$COL_CAT_NAME FROM $TABLE_POSTS p JOIN $TABLE_CATEGORIES c ON p.$COL_POST_CAT_ID = c.$COL_CAT_ID WHERE p.$COL_POST_FAVORITE = 1",
            null
        )
    }

    private fun queryPosts(sql: String, selectionArgs: Array<String>?): List<Post> {
        val list = mutableListOf<Post>()
        val db = readableDatabase
        val cursor = db.rawQuery(sql, selectionArgs)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Post(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_POST_ID)),
                        categoryId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_POST_CAT_ID)),
                        categoryName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME)),
                        content = cursor.getString(cursor.getColumnIndexOrThrow(COL_POST_CONTENT)),
                        isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_POST_FAVORITE)),
                        isFeatured = cursor.getInt(cursor.getColumnIndexOrThrow(COL_POST_FEATURED))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun toggleFavorite(postId: Int, currentStatus: Int): Int {
        val db = writableDatabase
        val newStatus = if (currentStatus == 1) 0 else 1
        val cv = ContentValues().apply { put(COL_POST_FAVORITE, newStatus) }
        db.update(TABLE_POSTS, cv, "$COL_POST_ID = ?", arrayOf(postId.toString()))
        return newStatus
    }

    // إدخال المنشورات الجديدة القادمة من JSONBin
    fun insertNewPostFromRemote(categoryName: String, content: String): Boolean {
        val db = writableDatabase
        ensureHasNewColumn(db)

        // التحقق من عدم التكرار
        val checkCursor = db.rawQuery("SELECT $COL_POST_ID FROM $TABLE_POSTS WHERE $COL_POST_CONTENT = ?", arrayOf(content))
        val exists = checkCursor.count > 0
        checkCursor.close()
        if (exists) return false

        // جلب معرف القسم أو إنشائه إن لم يكن موجوداً
        var catId = -1
        val catCursor = db.rawQuery("SELECT $COL_CAT_ID FROM $TABLE_CATEGORIES WHERE $COL_CAT_NAME = ?", arrayOf(categoryName))
        if (catCursor.moveToFirst()) {
            catId = catCursor.getInt(catCursor.getColumnIndexOrThrow(COL_CAT_ID))
        }
        catCursor.close()

        if (catId == -1) {
            val cvCat = ContentValues().apply {
                put(COL_CAT_NAME, categoryName)
                put(COL_CAT_HAS_NEW, 1)
            }
            catId = db.insert(TABLE_CATEGORIES, null, cvCat).toInt()
        } else {
            // تفعيل علامة NEW على القسم
            val cvUpdate = ContentValues().apply { put(COL_CAT_HAS_NEW, 1) }
            db.update(TABLE_CATEGORIES, cvUpdate, "$COL_CAT_ID = ?", arrayOf(catId.toString()))
        }

        val cvPost = ContentValues().apply {
            put(COL_POST_CAT_ID, catId)
            put(COL_POST_CONTENT, content)
            put(COL_POST_FAVORITE, 0)
            put(COL_POST_FEATURED, 0)
        }
        db.insert(TABLE_POSTS, null, cvPost)
        return true
    }

    fun clearCategoryNewBadge(categoryId: Int) {
        val db = writableDatabase
        val cv = ContentValues().apply { put(COL_CAT_HAS_NEW, 0) }
        db.update(TABLE_CATEGORIES, cv, "$COL_CAT_ID = ?", arrayOf(categoryId.toString()))
    }

    private fun ensureHasNewColumn(db: SQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE $TABLE_CATEGORIES ADD COLUMN $COL_CAT_HAS_NEW INTEGER DEFAULT 0")
        } catch (_: Exception) {
            // العمود موجود مسبقاً
        }
    }
    // جلب جميع أسماء الأقسام
fun getAllCategoryNames(): List<String> {
    val categories = mutableListOf<String>()
    val db = readableDatabase
    val cursor = db.rawQuery("SELECT name FROM categories ORDER BY id ASC", null)
    if (cursor.moveToFirst()) {
        do {
            categories.add(cursor.getString(0))
        } while (cursor.moveToNext())
    }
    cursor.close()
    return categories
}

// جلب المعرف بناءً على اسم القسم
fun getCategoryIdByName(name: String): Int? {
    val db = readableDatabase
    val cursor = db.rawQuery("SELECT id FROM categories WHERE name = ? LIMIT 1", arrayOf(name))
    var id: Int? = null
    if (cursor.moveToFirst()) {
        id = cursor.getInt(0)
    }
    cursor.close()
    return id
}
}
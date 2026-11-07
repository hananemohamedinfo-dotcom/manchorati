# MANCHORATI LITE (com.manchorati.www) — المخطط الهندسي الشامل والتوثيق المصدري الكامل
> **تاريخ الاعتماد التقني**: 2026-09-06  
> **الحزمة البرمجية (Package Name)**: `com.manchorati.www`  
> **البيئة واللغة**: Android SDK / Kotlin / Gradle / Jetpack / Firebase Cloud Firestore / SQLite  
> **الهدف من هذا الملف**: توثيق بنية التطبيق من الصفر حتى مرحلة الإنتاج بأدق تفاصيل الأكواد، قواعد البيانات، والتفاعلات، ليكون مرجعاً مطلقاً للمطورين ومحركات الذكاء الاصطناعي لفهم وتطوير المشروع دون الحاجة لطرح أي أسئلة.

---

## 1. المعمارية العامة ومحددات النظام (System Architecture)

### 1.1 فلسفة التصميم (Architecture Pattern)
يعتمد التطبيق على معمارية هجينة متعددة الطبقات (Hybrid Layered Architecture):
1. **طبقة التخزين المحلي (Offline Storage Layer)**: تعتمد على محرك SQLite عبر `DatabaseHelper.kt` لتخزين الأقسام الثابتة، المنشورات المضمنة، ونظام المفضلة. تضمن استمرارية عمل التطبيق دون الحاجة لاتصال بالإنترنت.
2. **طبقة المزامنة التلقائية (Sync & Background Worker Layer)**: تدار عبر `SyncManager.kt` ومكتبات `Kotlin Coroutines`، ومهمتها فحص المنشورات الجديدة وتحديث قاعدة البيانات المحلية عند توفر الإنترنت.
3. **طبقة السحابة الحية (Cloud Realtime Layer)**: مبنية على Firebase Cloud Firestore وتدار حصرياً عبر كائن أحادي `FirestoreManager.kt` لإدارة التفاعل المجتمعي الفوري (منشورات، تعليقات، إعجابات، بلاغات).
4. **طبقة واجهة المستخدم والتنقل (UI & ViewPager Layer)**: تتمحور حول نشاط رئيسي واحد `MainActivity.kt` متصل بـ `ViewPager2` يدير ثلاثة تبويبات رئيسية، مع استقلالية الشاشات الفرعية (`CategoryPostsActivity` و `UserProfileActivity`).

---

## 2. مخطط قواعد البيانات بالتفصيل الممل (Database Schemas)

### 2.1 قاعدة البيانات المحلية (SQLite via DatabaseHelper.kt)
* **اسم ملف القاعدة**: `manchorati.db` (أو المعرف الداخلي للتطبيق)
* **جداول النظام**:

#### أ. جدول الأقسام (`categories`)
| الحقل (Column) | النوع (Data Type) | الخصائص والقيود | الوصف الدلالي |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | معرف القسم الفريد |
| `title` | `TEXT` | `NOT NULL` | اسم القسم (مثال: حكم، عبارات حب، دينية، خواطر) |
| `icon_res` | `TEXT` / `INTEGER` | `NULLABLE` | مسار أو معرف الأيقونة الخاصة بالقسم |
| `order_index` | `INTEGER` | `DEFAULT 0` | ترتيب ظهور القسم في واجهة التبويب الأول |

#### ب. جدول المنشورات المحلية (`posts`)
| الحقل (Column) | النوع (Data Type) | الخصائص والقيود | الوصف الدلالي |
| :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | المعرف المحلي للمنشور |
| `category_id` | `INTEGER` | `FOREIGN KEY -> categories(id)` | معرف القسم التابع له المنشور |
| `content` | `TEXT` | `NOT NULL` | نص الاقتباس أو المنشور |
| `is_favorite` | `INTEGER` | `DEFAULT 0` (Boolean: 0 أو 1) | هل المنشور محفوظ في المفضلة |
| `timestamp` | `INTEGER` | `NOT NULL` | وقت إضافة المنشور |

---

### 2.2 قاعدة البيانات السحابية (Firebase Firestore)

#### أ. مجموعة المنشورات (`community_posts`)
المسار: `/community_posts/{postId}`
* **الحقول البرمجية**:
  * `id`: (String) معرّف الوثيقة في Firestore تلقائي التوليد.
  * `authorId`: (String) معرّف جهاز الكاتب `Settings.Secure.ANDROID_ID`.
  * `authorName`: (String) الاسم المستعار للمستخدم المحفوظ في `SharedPreferences` (افتراضياً: "فاعل خير").
  * `authorAvatar`: (String) رابط الصورة الرمزية للمستخدم أو مصفوفة محارف للصورة.
  * `content`: (String) نص المنشور المعروض.
  * `timestamp`: (Long / Timestamp) توقيت النشر بالميلي ثانية بنظام Unix Epoch لفرز النتائج ترتيباً تنازلياً.
  * `likesCount`: (Int) عدد الإعجابات الإجمالي، يُحدث عبر `FieldValue.increment()`.
  * `commentsCount`: (Int) عدد التعليقات التراكمي.
  * `reportCount`: (Int) عدد البلاغات المقدمة ضد المنشور.
  * `likedBy`: (List<String>) قائمة تحتوي على `deviceId` للمستخدمين الذين تفاعلوا مع المنشور، لتحديد حالة زر الإعجاب (أحمر/رمادي) محلياً لكل جهاز.

#### ب. المجموعة الفرعية للتعليقات (`comments`)
المسار: `/community_posts/{postId}/comments/{commentId}`
* **الحقول البرمجية**:
  * `id`: (String) معرف التعليق التلقائي.
  * `authorId`: (String) معرّف جهاز صاحب التعليق.
  * `authorName`: (String) اسم صاحب التعليق.
  * `text`: (String) نص التعليق المفحوص عبر `BadWordsFilter`.
  * `timestamp`: (Long) توقيت إرسال التعليق لترتيب المحادثات.

#### ج. مجموعة البلاغات (`reports`)
المسار: `/reports/{reportId}`
* **الحقول البرمجية**:
  * `postId`: (String) معرّف المنشور المبلّغ عنه.
  * `reporterId`: (String) معرّف الجهاز الذي قدم البلاغ لمنع تكرار الإبلاغ من نفس الجهاز.
  * `timestamp`: (Long) وقت تقديم البلاغ.
  * `reason`: (String) سبب البلاغ ("محتوى غير لائق"، "سب وقذف"، إلخ).

---

## 3. نماذج البيانات (Data Models - Kotlin Data Classes)

### 3.1 كلاس المنشور المجتمعي (`CommunityPost.kt`)
```kotlin
package com.manchorati.www

data class CommunityPost(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val reportCount: Int = 0,
    val likedBy: List<String> = emptyList()
)
```

### 3.2 كلاس التعليق (`Comment.kt`)
```kotlin
package com.manchorati.www

data class Comment(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
```

### 3.3 كلاس القسم والمنشور المحلي (`LocalModels.kt`)
```kotlin
package com.manchorati.www

data class Category(
    val id: Int,
    val title: String,
    val iconRes: Int = 0,
    val count: Int = 0
)

data class LocalPost(
    val id: Int,
    val categoryId: Int,
    val content: String,
    var isFavorite: Boolean = false,
    val timestamp: Long
)
```

---

## 4. مدقق الأمان والمحتوى (BadWordsFilter Engine)
يحتوي التطبيق على نظام صارم لمنع الألفاظ البذيئة والمحتوى غير اللائق قبل وصوله إلى قواعد البيانات السحابية لتوفير استهلاك Firestore والحفاظ على بيئة محترمة.

* **آلية المطابقة (Regex & Word Boundaries)**:
  * لا يتم استخدام الاستبدال البسيط للكلمات لتفادي الـ (False Positives)، بل يتم فحص الكلمات بحدود دقيقة `\b`.
* **نظام القائمة البيضاء (Whitelist)**:
  * تحتوي القائمة البيضاء على العبارات الدينية الشائعة (مثل: "صلى الله عليه وسلم"، "سبحان الله وبحمده"، "أستغفر الله العظيم") لضمان عدم تعرضها للحظر العرضي بسبب تشابه جذور الحروف.
* **مكان التطبيق**:
  1. عند إضافة منشور جديد من زر الـ FloatingActionButton (`fabAddPost`).
  2. عند تعديل منشور موجود (`showEditDialog`).
  3. عند إرسال تعليق في الـ BottomSheetDialog الخاص بالتعليقات.

---

## 5. هيكل الشاشات والواجهات (Activities, Fragments & Adapters)

### 5.1 الشاشة المركزية (MainActivity.kt)
* **المسؤوليات الرئيسية**:
  1. تهيئة `ViewPager2` مع محول الصفحات `MainPagerAdapter`.
  2. مزامنة التبويبات الثلاثة وتلوين المؤشر السفلي باللون الأحمر `#EF4444`.
  3. إدارة شريط البحث العلوي `etSearch` المتواجد داخل بطاقة `CardView` بزوايا دائرية `22dp`.
  4. تمرير نص البحث لحظياً إلى الفراغمنتات النشطة عبر مستمع `TextWatcher`:
     ```kotlin
     private fun setupSearch() {
         etSearch.addTextChangedListener(object : TextWatcher {
             override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
             override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                 val query = s?.toString() ?: ""
                 notifySearchToFragments(query)
             }
             override fun afterTextChanged(s: Editable?) {}
         })
     }

     private fun notifySearchToFragments(query: String) {
         supportFragmentManager.fragments.forEach { fragment ->
             if (fragment is CommunityFragment) {
                 fragment.onSearchQueryChanged(query)
             }
         }
     }
     ```
  5. التحكم في مقاس الخط عبر الزرين `btnTextBigger` و `btnTextSmaller` (الحد الأدنى `12sp`، الحد الأقصى `26sp`).
  6. القائمة المنبثقة `btnOptionsMenu`: تقييم التطبيق على المتجر، مشاركة التطبيق مع الأصدقاء، عرض المزيد من التطبيقات للمطور `hanane`، وزيارة صفحة الفيسبوك.
  7. إدارة زر الرجوع الفيزيائي: إرجاع المستخدم للتبويب 0 إذا كان في التبويب 1 أو 2، وإظهار نافذة إغلاق التطبيق `showExitDialog()` إذا كان في التبويب الأول.

### 5.2 شاشة المجتمع (CommunityFragment.kt)
* **المسؤوليات الرئيسية**:
  1. الاستماع الحي واللحظي لجدول `community_posts` في Firestore عبر `FirestoreManager.listenToCommunityPosts`.
  2. استقبال استعلامات البحث وتصفية قائمة `allPostsList` في الذاكرة ومقارنة النص بـ `post.content` و `post.authorName`.
  3. تفويض الأحداث إلى `CommunityAdapter`:
     * النقر على الإعجاب: استدعاء `FirestoreManager.toggleLike`.
     * النقر على التعليقات: فتح نافذة سفلية منبثقة `BottomSheetDialog`.
     * النقر على صورة الكاتب: فتح `UserProfileActivity` وتمرير معرّف الكاتب واسمه.
     * التمييز بين صاحب المنشور والزائر:
       - إذا كان `post.authorId == currentUserId`: إظهار القائمة ثلاثية النقاط مع خياري (تعديل المنشور، حذف المنشور).
       - إذا كان زائراً آخر: إظهار أيقونة الإبلاغ لإرسال تقرير بمخالفة المنشور إلى جدول `reports`.

### 5.3 محول المنشورات (CommunityAdapter.kt)
* **عناصر البطاقة (`item_community_post.xml`)**:
  * `ivAuthorAvatar`: صورة دائرية للمؤلف.
  * `tvAuthorName`: اسم كاتب المنشور.
  * `tvPostTime`: الوقت بصيغة نسبية (منذ دقيقة، منذ ساعة...).
  * `tvPostContent`: النص مع تطبيق حجم الخط المختار.
  * `btnLike` مع أيقونة تفاعلية وأيقونة قلب تتلون بالأحمر إذا كان معرف الجهاز موجوداً ضمن قائمة `likedBy`.
  * `tvLikesCount`: عدد الإعجابات.
  * `btnComment`: زر التعليق مع `tvCommentsCount`.
  * `btnPostOptions`: يظهر فقط لصاحب المنشور لتعديله أو حذفه.
  * `btnReport`: يظهر فقط للمستخدمين الآخرين للإبلاغ.

---

## 6. مدير الفايربيس (FirestoreManager.kt API Specifications)

يحتوي الكائن الأحادي `FirestoreManager` على كافة العمليات السحابية المنفصلة:

```kotlin
package com.manchorati.www

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

object FirestoreManager {
    private val db = FirebaseFirestore.getInstance()

    // 1. الاستماع المباشر لكافة المنشورات بترتيب زمني عكسي
    fun listenToCommunityPosts(onUpdate: (List<CommunityPost>) -> Unit) {
        db.collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(CommunityPost::class.java)?.copy(id = doc.id)
                    }
                    onUpdate(posts)
                }
            }
    }

    // 2. تحديث نص المنشور
    fun updatePost(postId: String, newContent: String, onComplete: (Boolean) -> Unit) {
        db.collection("community_posts").document(postId)
            .update("content", newContent)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // 3. حذف منشور بالكامل
    fun deletePost(postId: String, onComplete: (Boolean) -> Unit) {
        db.collection("community_posts").document(postId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // 4. تقديم بلاغ عن محتوى مسيء
    fun reportPost(postId: String, reporterId: String, onComplete: (Boolean) -> Unit) {
        val reportData = mapOf(
            "postId" to postId,
            "reporterId" to reporterId,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("reports").add(reportData)
            .addOnSuccessListener {
                db.collection("community_posts").document(postId)
                    .update("reportCount", FieldValue.increment(1))
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    // 5. تفعيل أو إلغاء الإعجاب الذري (Atomic Toggle Like)
    fun toggleLike(postId: String, userId: String, isLiked: Boolean, onComplete: (Boolean) -> Unit) {
        val postRef = db.collection("community_posts").document(postId)
        if (isLiked) {
            postRef.update(
                "likesCount", FieldValue.increment(1),
                "likedBy", FieldValue.arrayUnion(userId)
            ).addOnCompleteListener { onComplete(it.isSuccessful) }
        } else {
            postRef.update(
                "likesCount", FieldValue.increment(-1),
                "likedBy", FieldValue.arrayRemove(userId)
            ).addOnCompleteListener { onComplete(it.isSuccessful) }
        }
    }

    // 6. إضافة تعليق جديد وتحديث العداد
    fun addComment(postId: String, authorId: String, authorName: String, text: String, onComplete: (Boolean) -> Unit) {
        val comment = hashMapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        val postRef = db.collection("community_posts").document(postId)
        postRef.collection("comments").add(comment)
            .addOnSuccessListener {
                postRef.update("commentsCount", FieldValue.increment(1))
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    // 7. حذف تعليق
    fun deleteComment(postId: String, commentId: String, onComplete: (Boolean) -> Unit) {
        val postRef = db.collection("community_posts").document(postId)
        postRef.collection("comments").document(commentId).delete()
            .addOnSuccessListener {
                postRef.update("commentsCount", FieldValue.increment(-1))
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    // 8. الاستماع الحي لتعليقات منشور محدد
    fun listenToComments(postId: String, onUpdate: (List<Comment>) -> Unit) {
        db.collection("community_posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(id = doc.id)
                    }
                    onUpdate(comments)
                }
            }
    }
}
```

---

## 7. ملفات التصميم والتخطيط الشاملة (Layout Definitions)

### 7.1 واجهة الشاشة الرئيسية (`activity_main.xml`)
تتضمن:
* شريط أدوات علوي مخصص يحتوي على عنوان التطبيق، أزرار حجم الخط، وقائمة الخيارات.
* كارد البحث العائم `androidx.cardview.widget.CardView` بحواف `22dp` وظل خفيف وخلفية بيضاء نقية تحوي أيقونة البحث و `etSearch`.
* نظام التبويبات الثلاثي (الأقسام، المجتمع، المفضلة) مع مؤشرات الخطوط السفلية.
* عارض الصفحات `androidx.viewpager2.widget.ViewPager2` لشغل المساحة المتبقية بسلاسة عبر السحب بالأصابع.

### 7.2 واجهة تبويب المجتمع (`fragment_community.xml`)
تتضمن:
* قائمة التمرير الرأسية `androidx.recyclerview.widget.RecyclerView` بمعرف `rvCommunity`.
* زر الإضافة العائم `com.google.android.material.floatingactionbutton.FloatingActionButton` بالمعرف `fabAddPost` وبلون أحمر أساسي `#EF4444`.

### 7.3 واجهة عنصر المنشور (`item_community_post.xml`)
بطاقة `CardView` بزوايا دائرية تضم:
1. الجزء العلوي: صورة المؤلف الدائرية، الاسم، التاريخ النسبي، وزر الخيارات (إما 3 نقاط أو علم الإبلاغ).
2. الجزء الأوسط: النص البرمجي الكامل للمنشور مع إمكانية التحديد والنسخ.
3. الجزء السفلي: شريط التفاعل الأفقي (أزرار الإعجاب، عداد الإعجاب، زر التعليقات، عداد التعليقات، زر مشاركة نص المنشور).

---

## 8. دليل التوسعة البرمجية والصيانة لأي نظام ذكاء اصطناعي (AI & Developer Playbook)

إذا طُلب منك مستقبلاً إضافة أي ميزة، اتبع القواعد الدقيقة التالية الخاصة ببيئة هذا المشروع:

1. **إضافة ميزة الصور للمنشورات**:
   * استخدم `imagePickerLauncher` المسجل بالفعل في `MainActivity`.
   * ارفع الصورة إلى Firebase Storage عبر مسار `post_images/{postId}.jpg`.
   * احصل على رابط التحميل `downloadUrl` وخزنه في حقل `imageUrl` داخل `CommunityPost`.
2. **إضافة نظام الإشعارات (Push Notifications)**:
   * أنشئ خدمة ترث من `FirebaseMessagingService`.
   * في دالة `toggleLike` ودالة `addComment`، أرسل إشعاراً لجهاز المؤلف عبر Cloud Function أو FCM HTTP v1 API باستخدام `authorId` المستهدف.
3. **تطبيق نظام التحميل المجزأ (Pagination)**:
   * لا تسحب جميع الوثائق دفعة واحدة عند تجاوز 1000 منشور؛ استبدل `addSnapshotListener` بدالة تعتمد على `Query.limit(20)` مع `startAfter(lastVisibleDocument)`.
4. **الالتزام بنمط التصميم**:
   * حافظ على اللون الأساسي للتطبيق `#EF4444`.
   * احرص دائماً على تطبيق `direction: rtl` في كافة الواجهات لدعم اللغة العربية بشكل افتراضي وقانوني.

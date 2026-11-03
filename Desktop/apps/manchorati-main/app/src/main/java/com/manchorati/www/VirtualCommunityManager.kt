package com.manchorati.www

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class VirtualPersona(
    val id: String,
    val name: String,
    val username: String,
    val avatarResource: String,
    val badge: String = "",
    val bio: String = ""
)

object VirtualCommunityManager {

    private const val TAG = "VirtualCommunity"
    private const val PREFS_NAME = "virtual_community_prefs"
    private const val KEY_PERSONAS_SEEDED = "personas_seeded_v2"
    private const val KEY_LAST_AUTO_POST_TIME = "last_auto_post_time"

    val PERSONAS = listOf(
        VirtualPersona("persona_sara_b", "سارة بناني", "sara_bennani", "avatar_2", "verified", "شغوفة بالأدب والفكر والكلمة الطيبة 🌸"),
        VirtualPersona("persona_yassine_a", "ياسين العلمي", "yassine_al", "avatar_1", "writer", "كاتب ومهتم بتطوير الذات وبناء العادات ✍️"),
        VirtualPersona("persona_hamza_s", "حمزة الصالحي", "hamza_s", "avatar_3", "", "باحث عن الحكمة والهدوء في تفاصيل الحياة ☕"),
        VirtualPersona("persona_ilham_m", "إلهام المنصوري", "ilham_m", "avatar_4", "writer", "أكتب لأجد نفسي بين السطور والخواطر 🕊️"),
        VirtualPersona("persona_amine_f", "أمين الفاسي", "amine_elfassi", "avatar_5", "", "مهتم بالقراءة ونشر الطاقة الإيجابية ✨"),
        VirtualPersona("persona_maryam_i", "مريم الإدريسي", "maryam_idrissi", "avatar_6", "verified", "محبة للأدب الراقي ولحظات التأمل 🤍"),
        VirtualPersona("persona_omar_t", "عمر التازي", "omar_tazi", "avatar_7", "", "عاشق للهدوء والكتب وفنجان قهوة الصباح 📖"),
        VirtualPersona("persona_kaoutar_m", "كوثر المرابط", "kaoutar_m", "avatar_8", "writer", "الكلمات أثر لا يزول.. أشارككم ما راق لي 🌿"),
        VirtualPersona("persona_nabil_c", "نبيل الشريف", "nabil_cherif", "avatar_9", "", "طالب معرفة ومحب للحكم والأمثال القديمة 🌟"),
        VirtualPersona("persona_fz_zahra", "فاطمة الزهراء", "fz_zahra", "avatar_10", "", "ابتسم فالحياة أجمل بحسن الظن بالله 🌸"),
        VirtualPersona("persona_karim_n", "كريم الناصري", "karim_n", "avatar_11", "writer", "كلمات تصنع الفارق وأفكار تبني العزيمة 🔥"),
        VirtualPersona("persona_houda_a", "هدى العمراني", "houda_amrani", "avatar_12", "", "خواطر وكلمات دافئة من القلب إلى القلب 🕊️"),
        VirtualPersona("persona_tariq_b", "طارق بنسودة", "tariq_b", "avatar_13", "", "النجاح يبدأ بخطوة واستمرار لا يتوقف 💪"),
        VirtualPersona("persona_sanaa_b", "سناء بوزيد", "sanaa_b", "avatar_14", "verified", "مؤسسة نادي قراء ومحبة لنشر الإيجابية 💖"),
        VirtualPersona("persona_youssef_r", "يوسف الرحماني", "youssef_r", "avatar_1", "", "في رحلة مستمرة للتعلم واكتساب الحكمة 🚀"),
        VirtualPersona("persona_zineb_y", "زينب اليعقوبي", "zineb_y", "avatar_2", "", "أحب التفاصيل الصغيرة واللحظات الصادقة ✨"),
        VirtualPersona("persona_adel_m", "عادل المتوكل", "adel_m", "avatar_3", "writer", "حروف تنبض بالحياة ورسائل أمل دائمة ✍️"),
        VirtualPersona("persona_wafaa_s", "وفاء الصنهاجي", "wafaa_s", "avatar_4", "", "اللهم سعة وسلاماً يغمر القلب 🤲"),
        VirtualPersona("persona_mostafa_w", "مصطفى الوديع", "mostafa_w", "avatar_5", "", "الهدوء نعمة، والكلمة الطيبة صدقة 🌿"),
        VirtualPersona("persona_nadia_b", "نادية برادة", "nadia_barrada", "avatar_6", "", "نور وأمل يزهر في كل مكان 💫")
    )

    private val BG_PRESETS = listOf(
        "card_luxury",
        "card_crimson_brand",
        "card_ocean_breeze",
        "card_emerald",
        "card_sunset",
        "card_glass_royal",
        "card_neon_cyber",
        "card_classic_paper"
    )

    private val FONT_PRESETS = listOf("tajawal", "cairoplay", "kufam", "arefruqaa", "marhey")

    private var cachedCuratedPosts: List<CommunityPost> = emptyList()

    fun getPersonaById(id: String): VirtualPersona {
        return PERSONAS.find { it.id == id } ?: PERSONAS[Math.abs(id.hashCode()) % PERSONAS.size]
    }

    fun getPersonaForId(seed: Int): VirtualPersona {
        val idx = Math.abs(seed) % PERSONAS.size
        return PERSONAS[idx]
    }

    fun toVirtualCommunityPost(
        post: Post,
        context: Context,
        targetTag: String? = null,
        staggerMinutesAgo: Long = 0L
    ): CommunityPost {
        val persona = getPersonaForId(post.id)
        val selectedBg = BG_PRESETS[Math.abs(post.id.hashCode()) % BG_PRESETS.size]
        val selectedFont = FONT_PRESETS[Math.abs(post.id.hashCode() / 5) % FONT_PRESETS.size]
        val baseLikes = (28 + (Math.abs(post.id.hashCode()) % 80)).toLong()
        val postId = "vpost_${post.id}"
        val isLiked = LocalLikesManager.isLiked(context, postId)
        val currentUid = AuthManager.currentUser?.uid ?: "local_guest"
        val likedByList = if (isLiked) listOf(currentUid) else emptyList()
        val baseComments = (1 + (Math.abs(post.id.hashCode()) % 4)).toLong()

        val timeOffset = if (staggerMinutesAgo > 0L) {
            staggerMinutesAgo * 60 * 1000L
        } else {
            val hours = (Math.abs(post.id.hashCode()) % 30) + 1
            val mins = (Math.abs(post.id.hashCode() / 3) % 50)
            (hours * 3600L + mins * 60L) * 1000L
        }
        val postTimestamp = System.currentTimeMillis() - timeOffset

        val resolvedTag = targetTag?.takeIf { it.isNotEmpty() && it != "الكل" }
            ?: post.categoryName.takeIf { it.isNotEmpty() }
            ?: "خواطر"

        return CommunityPost(
            id = postId,
            authorId = persona.id,
            authorName = persona.name,
            authorUsername = persona.username,
            authorPhotoUrl = persona.avatarResource,
            content = post.content,
            bgId = selectedBg,
            fontId = selectedFont,
            textColor = "",
            tag = resolvedTag,
            likesCount = if (isLiked) baseLikes + 1 else baseLikes,
            commentsCount = baseComments,
            reportCount = 0L,
            likedBy = likedByList,
            authorBadge = persona.badge,
            timestamp = postTimestamp
        )
    }

    @Synchronized
    fun getCuratedFeedPosts(context: Context, forceRefresh: Boolean = false): List<CommunityPost> {
        if (cachedCuratedPosts.isNotEmpty() && !forceRefresh) {
            return cachedCuratedPosts
        }

        try {
            val dbHelper = DatabaseHelper(context)
            val rawPosts = dbHelper.getRandomCuratedPosts(35)
            if (rawPosts.isNotEmpty()) {
                val list = mutableListOf<CommunityPost>()
                for (i in rawPosts.indices) {
                    val p = rawPosts[i]
                    val staggerMinutes = 15L + (i * 28L) + (Math.abs(p.id.hashCode()) % 15)
                    list.add(toVirtualCommunityPost(p, context, staggerMinutesAgo = staggerMinutes))
                }
                cachedCuratedPosts = list
                return list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating curated feed posts", e)
        }
        return cachedCuratedPosts
    }

    fun getContextualComment(content: String, tag: String): String {
        val lower = (content + " " + tag).lowercase()
        return when {
            lower.contains("تحفيز") || lower.contains("نجاح") || lower.contains("هدف") ||
                    lower.contains("عزيمة") || lower.contains("تطوير") || lower.contains("طموح") -> {
                listOf(
                    "طاقة إيجابية نحتاجها في بداية يومنا، شكراً لك ✨",
                    "كلام محفز وفي الصميم، استمر يا بطل 💪",
                    "بالفعل، لا شيء مستحيل مع الإصرار والعزيمة 🔥",
                    "مشاركاتك دائماً تعطي دفعة أمل كبيرة 👏",
                    "مبدع كالعادة، كلام يبني الهمم 🚀"
                ).random()
            }
            lower.contains("دعاء") || lower.contains("يارب") || lower.contains("الله") ||
                    lower.contains("سكينة") || lower.contains("استغفار") || lower.contains("أذكار") ||
                    lower.contains("جمعة") -> {
                listOf(
                    "اللهم آمين يا رب العالمين، جزاك الله كل خير 🤲🕊️",
                    "ونعم بالله العلي العظيم، كلمات تريح القلب والروح 🤍",
                    "بارك الله فيك ونفع بك، أثابك الله 🌿",
                    "ألا بذكر الله تطمئن القلوب.. تذكير طيب ومبارك 🌸",
                    "ما أجمل هذا الدعاء، ربي يتقبل منا ومنكم 🕊️"
                ).random()
            }
            lower.contains("حزن") || lower.contains("صبر") || lower.contains("فراق") ||
                    lower.contains("ألم") || lower.contains("تعب") || lower.contains("هم") -> {
                listOf(
                    "طبطبة رقيقة على القلب.. كل مرٍّ سيمر بإذن الله 🤍",
                    "فرج الله قريب، ما ضاقت إلا لتفرج، دمت بخير 🕊️",
                    "كلمات صادقة تلامس أعماق الروح، ربي يسعد قلبك 🌸",
                    "إن بعد العسر يسراً.. كلام معبّر جداً 🤍"
                ).random()
            }
            lower.contains("حب") || lower.contains("شعر") || lower.contains("خواطر") ||
                    lower.contains("وفاء") || lower.contains("قلب") -> {
                listOf(
                    "ما أرق هذه الحروف وأجمل معانيها 🌸",
                    "إحساس عالي وذوق رفيع جداً ✨",
                    "حفظت المنشور عندي من جمال ما قرأت 🤍",
                    "كلام يقطر عذوبة وجمالاً، سلمت أناملك ✍️"
                ).random()
            }
            else -> {
                listOf(
                    "كلمات راقية جداً وفي الصميم ✨",
                    "ما شاء الله، ذوق رفيع واختيار موفق 👏",
                    "كلام يلامس القلب.. شكراً على هذه المشاركة 🤍",
                    "أبدعت! استمر في هذا العطاء الجميل 🌿",
                    "صدقت، نحتاج لمثل هذه الكلمات دائماً 👌",
                    "تبارك الله عليك، كلام موزون وجميل 💫",
                    "راقت لي هذه الكلمات كثيراً، سلمت يمينك 🌸"
                ).random()
            }
        }
    }

    /**
     * جدولة التفاعل التلقائي على منشور جديد:
     * - لايكات أولية بعد دقيقة ونصف
     * - تعليق تشجيعي بعد 4 دقائق
     * - دفعة لايكات ثانية بعد 12 دقيقة
     */
    fun scheduleAutoEngagement(
        context: Context,
        postId: String,
        authorId: String,
        authorName: String,
        content: String,
        tag: String
    ) {
        if (postId.isEmpty() || authorId.isEmpty() || authorId.startsWith("persona_")) return

        val workManager = WorkManager.getInstance(context.applicationContext)

        // مرحلة 1: لايكات أولية (بعد 90 ثانية)
        val dataStage1 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 1)
            .build()

        val reqStage1 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage1)
            .setInitialDelay(90, TimeUnit.SECONDS)
            .build()

        // مرحلة 2: تعليق تشجيعي (بعد 4 دقائق)
        val dataStage2 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 2)
            .build()

        val reqStage2 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage2)
            .setInitialDelay(4, TimeUnit.MINUTES)
            .build()

        // مرحلة 3: لايكات إضافية (بعد 12 دقيقة)
        val dataStage3 = Data.Builder()
            .putString("POST_ID", postId)
            .putString("AUTHOR_ID", authorId)
            .putString("AUTHOR_NAME", authorName)
            .putString("CONTENT", content)
            .putString("TAG", tag)
            .putInt("STAGE", 3)
            .build()

        val reqStage3 = OneTimeWorkRequestBuilder<AutoEngagementWorker>()
            .setInputData(dataStage3)
            .setInitialDelay(12, TimeUnit.MINUTES)
            .build()

        workManager.enqueue(reqStage1)
        workManager.enqueue(reqStage2)
        workManager.enqueue(reqStage3)

        Log.d(TAG, "Auto-engagement scheduled successfully for post: $postId")
    }

    /**
     * نشر بروفايلات الأشخاص الافتراضيين في Firestore لضمان فتح صفحاتهم والتفاعل معهم بسلاسة
     */
    fun ensurePersonasInFirestore(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PERSONAS_SEEDED, false)) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val batch = db.batch()
                for (p in PERSONAS) {
                    val docRef = db.collection("users").document(p.id)
                    val followersCount = 18 + (Math.abs(p.id.hashCode()) % 80)
                    val followingCount = 6 + (Math.abs(p.id.hashCode() / 2) % 25)
                    val data = hashMapOf(
                        "name" to p.name,
                        "displayName" to p.name,
                        "username" to p.username,
                        "photoUrl" to p.avatarResource,
                        "bio" to p.bio,
                        "badge" to p.badge,
                        "followersCount" to followersCount,
                        "followingCount" to followingCount,
                        "postsCount" to (5 + (Math.abs(p.id.hashCode()) % 15))
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit().addOnSuccessListener {
                    prefs.edit().putBoolean(KEY_PERSONAS_SEEDED, true).apply()
                    Log.d(TAG, "Virtual personas synced to Firestore successfully.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed syncing personas to Firestore", e)
            }
        }
    }

    /**
     * إذا مرت 8 ساعات دون أي منشور في Firestore، ينشر تلقائياً منشوراً جميلاً من أحد الأشخاص
     */
    fun maybeSeedDailyFirestorePost(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAutoTime = prefs.getLong(KEY_LAST_AUTO_POST_TIME, 0L)
        val now = System.currentTimeMillis()
        if (now - lastAutoTime < 6 * 3600 * 1000L) {
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("community_posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val latestTime = snapshot.documents.firstOrNull()?.getLong("timestamp") ?: 0L
                val hoursSinceLatest = (now - latestTime) / (3600 * 1000L)

                if (snapshot.isEmpty || hoursSinceLatest >= 8) {
                    // حان وقت نشر منشور جديد في المجتمع السحابي!
                    publishOrganicPostToFirestore(context)
                }
            }
            .addOnFailureListener {
                Log.w(TAG, "Error checking latest Firestore post timestamp", it)
            }
    }

    private fun publishOrganicPostToFirestore(context: Context) {
        try {
            val dbHelper = DatabaseHelper(context)
            val posts = dbHelper.getRandomCuratedPosts(3)
            val quote = posts.firstOrNull() ?: return
            val persona = getPersonaForId(quote.id)
            val bgId = BG_PRESETS[Math.abs(quote.id.hashCode()) % BG_PRESETS.size]
            val fontId = FONT_PRESETS[Math.abs(quote.id.hashCode() / 4) % FONT_PRESETS.size]
            val initialLikes = 12 + (Math.abs(quote.id.hashCode()) % 30)
            val sampleLikedBy = PERSONAS.shuffled().take(initialLikes).map { it.id }

            val postMap = hashMapOf<String, Any>(
                "authorId" to persona.id,
                "authorName" to persona.name,
                "authorUsername" to persona.username,
                "authorPhotoUrl" to persona.avatarResource,
                "content" to quote.content,
                "bgId" to bgId,
                "fontId" to fontId,
                "textColor" to "",
                "tag" to (quote.categoryName.takeIf { it.isNotEmpty() } ?: "خواطر"),
                "authorBadge" to persona.badge,
                "likesCount" to initialLikes.toLong(),
                "commentsCount" to 1L,
                "likedBy" to sampleLikedBy,
                "timestamp" to System.currentTimeMillis()
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("community_posts").add(postMap).addOnSuccessListener { docRef ->
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_AUTO_POST_TIME, System.currentTimeMillis()).apply()
                Log.d(TAG, "Auto-seeded new post to Firestore: ${docRef.id}")

                // إضافة تعليق أولي لطيف
                val otherPersona = PERSONAS.filter { it.id != persona.id }.random()
                val commentText = getContextualComment(quote.content, quote.categoryName)
                FirestoreManager.addComment(
                    postId = docRef.id,
                    authorId = otherPersona.id,
                    authorName = otherPersona.name,
                    authorPhotoUrl = otherPersona.avatarResource,
                    content = commentText
                ) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish organic post to Firestore", e)
        }
    }
}

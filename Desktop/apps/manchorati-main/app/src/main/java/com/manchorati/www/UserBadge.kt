package com.manchorati.www

import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ConcurrentHashMap

data class UserBadge(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val bgResId: Int,
    val textColorHex: String,
    val tierLevel: Int = 1,
    val minFollowers: Long = 0L,
    val description: String = ""
)

data class MilestoneBadge(
    val id: String,
    val title: String,
    val description: String,
    val targetFollowers: Long,
    val emoji: String
) {
    fun isUnlocked(followersCount: Long): Boolean = followersCount >= targetFollowers
}

object BadgeHelper {

    private val ADMIN_EMAILS = setOf(
        "hananemohamedinfo@gmail.com",
        "admin@manchorati.com"
    )

    private val userBadgeCache = ConcurrentHashMap<String, UserBadge>()
    private val pendingFetches = ConcurrentHashMap.newKeySet<String>()

    val BADGE_ADMIN = UserBadge(
        id = "admin",
        title = "مشرف التطبيق",
        iconEmoji = "🛡️",
        bgResId = R.drawable.bg_badge_admin,
        textColorHex = "#818CF8",
        tierLevel = 6,
        minFollowers = 0L,
        description = "عضو رسمي في إدارة وتطوير تطبيق منشوراتي."
    )

    val BADGE_CURATED = UserBadge(
        id = "official",
        title = "منشوراتي",
        iconEmoji = "",
        bgResId = 0,
        textColorHex = "#38BDF8",
        tierLevel = 6,
        minFollowers = 0L,
        description = "منشور من مكتبة منشوراتي."
    )

    val BADGE_LEGENDARY = UserBadge(
        id = "legendary",
        title = "كاتب أسطوري",
        iconEmoji = "👑",
        bgResId = R.drawable.bg_badge_legendary,
        textColorHex = "#F59E0B",
        tierLevel = 5,
        minFollowers = 10000L,
        description = "بلغ قمة التأثير والإبداع مع أكثر من 10,000 متابع حقيقي!"
    )

    val BADGE_ELITE = UserBadge(
        id = "elite",
        title = "كاتب نخبة",
        iconEmoji = "💎",
        bgResId = R.drawable.bg_badge_elite,
        textColorHex = "#38BDF8",
        tierLevel = 4,
        minFollowers = 5000L,
        description = "من نخبة الكتّاب الأكثر تميزاً وتفاعلاً مع أكثر من 5,000 متابع."
    )

    val BADGE_DISTINGUISHED = UserBadge(
        id = "distinguished",
        title = "كاتب متميز",
        iconEmoji = "⭐",
        bgResId = R.drawable.bg_badge_writer,
        textColorHex = "#FBBF24",
        tierLevel = 3,
        minFollowers = 1000L,
        description = "تجاوز 1,000 متابع ويحظى بحضور قوي في المجتمع."
    )

    val BADGE_RISING = UserBadge(
        id = "rising",
        title = "كاتب صاعد",
        iconEmoji = "🥈",
        bgResId = R.drawable.bg_badge_rising,
        textColorHex = "#94A3B8",
        tierLevel = 2,
        minFollowers = 100L,
        description = "كاتب موهوب بدأ بالصعود وتجاوز 100 متابع."
    )

    val BADGE_BEGINNER = UserBadge(
        id = "beginner",
        title = "كاتب مبتدئ",
        iconEmoji = "🥉",
        bgResId = R.drawable.bg_badge_beginner,
        textColorHex = "#D97706",
        tierLevel = 1,
        minFollowers = 0L,
        description = "يخطو أولى خطواته في نشر الكلمات والخواطر ومشاركة الإلهام."
    )

    val ALL_TIERS = listOf(
        BADGE_BEGINNER,
        BADGE_RISING,
        BADGE_DISTINGUISHED,
        BADGE_ELITE,
        BADGE_LEGENDARY
    )

    val ALL_MILESTONES = listOf(
        MilestoneBadge(
            id = "milestone_1k",
            title = "نادي الألفية الأولى (1K)",
            description = "تخطي حاجز 1,000 متابع في المجتمع",
            targetFollowers = 1000L,
            emoji = "🎖️"
        ),
        MilestoneBadge(
            id = "milestone_5k",
            title = "وسام النخبة (5K)",
            description = "الوصول إلى 5,000 متابع نشط",
            targetFollowers = 5000L,
            emoji = "💎"
        ),
        MilestoneBadge(
            id = "milestone_7k",
            title = "وسام القمة الذهبي (7K)",
            description = "تخطي 7,000 متابع وتحقيق انتشار واسع",
            targetFollowers = 7000L,
            emoji = "⭐"
        ),
        MilestoneBadge(
            id = "milestone_10k",
            title = "درع الأسطورة الذهبي (10K)",
            description = "بلوغ 10,000 متابع وتربع قمة المجتمع",
            targetFollowers = 10000L,
            emoji = "🏆"
        )
    )

    fun setCachedBadge(userId: String, badge: UserBadge) {
        if (userId.isNotEmpty()) {
            userBadgeCache[userId] = badge
        }
    }

    fun getCachedBadge(userId: String): UserBadge? {
        return if (userId.isNotEmpty()) userBadgeCache[userId] else null
    }

    fun getBadgeForProfile(
        explicitBadge: String?,
        email: String?,
        totalLikes: Long = 0L,
        followersCount: Long = 0L
    ): UserBadge {
        val b = explicitBadge?.trim()?.lowercase() ?: ""
        
        // 1. فحص الشارات الممنوحة يدوياً من لوحة التحكم أو الإيميل الإداري
        if (b == "admin" || b == "crown" || b == "مشرف" || (!email.isNullOrEmpty() && ADMIN_EMAILS.contains(email.lowercase()))) {
            return BADGE_ADMIN
        }
        if (b == "official" || b == "curated" || b.contains("منشوراتي") || b.contains("رسمي")) {
            return BADGE_CURATED
        }
        if (b == "legendary" || b == "legend" || b.contains("أسطور") || b.contains("اسطور")) {
            return BADGE_LEGENDARY
        }
        if (b == "elite" || b.contains("نخبة") || b.contains("نخبه")) {
            return BADGE_ELITE
        }
        if (b == "distinguished" || b == "star" || b == "featured" || b.contains("متميز") || b.contains("مميز")) {
            return BADGE_DISTINGUISHED
        }
        if (b == "rising" || b.contains("صاعد")) {
            return BADGE_RISING
        }
        if (b == "beginner" || b.contains("مبتدئ")) {
            return BADGE_BEGINNER
        }

        // 2. الترقية التلقائية المبنية على عدد المتابعين الفعلي
        return when {
            followersCount >= 10000L -> BADGE_LEGENDARY
            followersCount >= 5000L -> BADGE_ELITE
            followersCount >= 1000L -> BADGE_DISTINGUISHED
            followersCount >= 100L -> BADGE_RISING
            else -> BADGE_BEGINNER
        }
    }

    fun getBadgeForPost(post: CommunityPost): UserBadge {
        if (post.authorId == "manchorati_curated" || post.authorBadge == "official") {
            return BADGE_CURATED
        }

        if (post.authorId.isNotEmpty()) {
            val cached = userBadgeCache[post.authorId]
            if (cached != null) return cached
        }

        val uName = post.authorUsername.lowercase()
        val aName = post.authorName.lowercase()
        if (uName == "admin" || uName.contains("admin") || aName.contains("المشرف") || aName.contains("admin")) {
            return BADGE_ADMIN
        }

        if (post.authorBadge.isNotBlank()) {
            return getBadgeForProfile(post.authorBadge, null, 0L, 0L)
        }

        return when {
            post.likesCount >= 100L -> BADGE_LEGENDARY
            post.likesCount >= 50L -> BADGE_ELITE
            post.likesCount >= 20L -> BADGE_DISTINGUISHED
            post.likesCount >= 5L -> BADGE_RISING
            else -> BADGE_BEGINNER
        }
    }

    fun fetchAndCacheAuthorBadge(authorId: String, onResolved: ((UserBadge) -> Unit)? = null) {
        if (authorId.isEmpty()) return
        val cached = userBadgeCache[authorId]
        if (cached != null) {
            onResolved?.invoke(cached)
            return
        }
        if (!pendingFetches.add(authorId)) return

        try {
            FirebaseFirestore.getInstance().collection("users").document(authorId).get()
                .addOnSuccessListener { doc ->
                    pendingFetches.remove(authorId)
                    if (doc != null && doc.exists()) {
                        val explicitBadge = doc.getString("badge")
                        val email = doc.getString("email")
                        val followersRaw = doc.get("followers")
                        val followersCount = when (followersRaw) {
                            is List<*> -> followersRaw.size.toLong()
                            is Number -> followersRaw.toLong()
                            else -> 0L
                        }
                        val finalBadge = getBadgeForProfile(explicitBadge, email, 0L, followersCount)
                        userBadgeCache[authorId] = finalBadge
                        onResolved?.invoke(finalBadge)
                    }
                }
                .addOnFailureListener {
                    pendingFetches.remove(authorId)
                }
        } catch (e: Exception) {
            pendingFetches.remove(authorId)
        }
    }

    fun getNextTier(current: UserBadge): UserBadge? {
        if (current.id == BADGE_ADMIN.id || current.id == BADGE_LEGENDARY.id) {
            return null
        }
        val idx = ALL_TIERS.indexOfFirst { it.id == current.id }
        return if (idx in 0 until ALL_TIERS.size - 1) ALL_TIERS[idx + 1] else null
    }
}

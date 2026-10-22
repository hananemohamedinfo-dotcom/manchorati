package com.manchorati.www

data class UserBadge(
    val title: String,
    val bgResId: Int,
    val textColorHex: String
)

object BadgeHelper {
    private val ADMIN_EMAILS = setOf(
        "hananemohamedinfo@gmail.com",
        "admin@manchorati.com"
    )

    fun getBadgeForPost(post: CommunityPost): UserBadge? {
        val uName = post.authorUsername.lowercase()
        if (uName == "admin" || uName.contains("admin") || post.authorName.contains("المشرف")) {
            return UserBadge("👑 المشرف", R.drawable.bg_badge_admin, "#818CF8")
        }
        if (post.likesCount >= 20L) {
            return UserBadge("⭐ كاتب متميز", R.drawable.bg_badge_writer, "#FBBF24")
        }
        return null
    }

    fun getBadgeForProfile(email: String?, totalLikes: Long, followersCount: Long): UserBadge? {
        if (!email.isNullOrEmpty() && ADMIN_EMAILS.contains(email.lowercase())) {
            return UserBadge("👑 المشرف", R.drawable.bg_badge_admin, "#818CF8")
        }
        if (totalLikes >= 25L || followersCount >= 10L) {
            return UserBadge("⭐ كاتب متميز", R.drawable.bg_badge_writer, "#FBBF24")
        }
        return null
    }
}

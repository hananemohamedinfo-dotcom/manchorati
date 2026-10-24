package com.manchorati.www

data class UserBadge(
    val titleResId: Int,
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
        if (uName == "admin" || uName.contains("admin") || post.authorName.contains("المشرف") || post.authorName.lowercase().contains("admin")) {
            return UserBadge(R.string.badge_admin, R.drawable.bg_badge_admin, "#818CF8")
        }
        if (post.likesCount >= 20L) {
            return UserBadge(R.string.badge_writer, R.drawable.bg_badge_writer, "#FBBF24")
        }
        return null
    }

    fun getBadgeForProfile(email: String?, totalLikes: Long, followersCount: Long): UserBadge? {
        if (!email.isNullOrEmpty() && ADMIN_EMAILS.contains(email.lowercase())) {
            return UserBadge(R.string.badge_admin, R.drawable.bg_badge_admin, "#818CF8")
        }
        if (totalLikes >= 25L || followersCount >= 10L) {
            return UserBadge(R.string.badge_writer, R.drawable.bg_badge_writer, "#FBBF24")
        }
        return null
    }
}

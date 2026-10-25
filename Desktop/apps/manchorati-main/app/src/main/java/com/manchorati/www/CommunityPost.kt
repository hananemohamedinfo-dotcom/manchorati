package com.manchorati.www

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CommunityPost(
    var id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val bgId: String = "#FFFFFF",
    val fontId: String = "tajawal",
    val textColor: String = "",
    val tag: String = "",
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val reportCount: Long = 0,
    val likedBy: List<String> = emptyList(),
    val authorBadge: String = "",
    val timestamp: Any? = null
)

fun CommunityPost.getTimeMillis(): Long {
    return when (val t = timestamp) {
        is Number -> t.toLong()
        is com.google.firebase.Timestamp -> t.toDate().time
        else -> 0L
    }
}

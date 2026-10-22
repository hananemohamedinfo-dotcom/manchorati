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
    val timestamp: Any? = null
)
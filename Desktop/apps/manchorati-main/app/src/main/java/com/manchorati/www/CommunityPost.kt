package com.manchorati.www

import com.google.firebase.Timestamp

data class CommunityPost(
    var id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "", // الحقل الجديد الثابت
    val authorPhotoUrl: String = "", // أضف هذا السطر هنا
    val content: String = "",
    val bgId: String = "#FFFFFF",
    val fontId: String = "tajawal",
    val likesCount: Long = 0,
    val commentsCount: Long = 0,
    val reportCount: Long = 0,
    val likedBy: List<String> = emptyList(),
    val timestamp: Any? = null
)
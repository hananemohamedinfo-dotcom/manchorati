package com.manchorati.www

import androidx.annotation.Keep

@Keep
data class Comment(
    var id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    val replyToAuthorName: String = "",
    val replyToCommentId: String = "",
    var timestamp: Long = 0L,
    val isEdited: Boolean = false
) {
    constructor() : this("", "", "", "", "", "", "", 0L, false)
}

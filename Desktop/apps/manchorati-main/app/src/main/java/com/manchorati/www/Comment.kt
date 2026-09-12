package com.manchorati.www

import androidx.annotation.Keep

@Keep
data class Comment(
    var id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val content: String = "",
    var timestamp: Long = 0L
) {
    constructor() : this("", "", "", "", "", 0L)
}
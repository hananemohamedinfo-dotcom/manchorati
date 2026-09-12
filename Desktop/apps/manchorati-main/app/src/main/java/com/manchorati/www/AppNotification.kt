package com.manchorati.www

import androidx.annotation.Keep

@Keep
data class AppNotification(
    var id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = "",
    val postId: String = "",
    val message: String = "",
    var isRead: Boolean = false,
    var timestamp: Long = 0L
) {
    constructor() : this("", "", "", "", "", "", "", false, 0L)
}
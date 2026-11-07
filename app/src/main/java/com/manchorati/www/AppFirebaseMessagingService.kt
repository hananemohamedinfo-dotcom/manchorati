package com.manchorati.www

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: data["message"]
            ?: data["body"]
            ?: ""
        val postId = data["postId"] ?: ""
        val type = data["type"] ?: "GENERAL"
        val senderId = data["senderId"] ?: ""
        val senderName = data["senderName"] ?: ""

        if (type == "ANNOUNCEMENT") {
            NotificationHelper.showAnnouncementNotification(this, title, body)
        } else {
            NotificationHelper.showCommunityNotification(
                context = this,
                title = title,
                body = body,
                postId = postId,
                type = type,
                senderId = senderId,
                senderName = senderName
            )
        }
    }
}

package com.manchorati.www

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        try {
            // 1. Check Community Notifications for logged-in user
            if (!uid.isNullOrEmpty()) {
                val lastNotifiedTime = prefs.getLong("last_system_notified_time_$uid", 0L)
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .whereEqualTo("recipientId", uid)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                val newNotifs = snapshot.documents.mapNotNull { doc ->
                    val notif = doc.toObject(AppNotification::class.java)
                    notif?.apply { id = doc.id }
                }.filter { it.timestamp > lastNotifiedTime }

                if (newNotifs.isNotEmpty()) {
                    for (notif in newNotifs) {
                        val sender = if (notif.senderName.isNotEmpty()) notif.senderName else appContext.getString(R.string.app_name)
                        val body = when (notif.type) {
                            "LIKE" -> appContext.getString(R.string.notif_liked_post, sender)
                            "COMMENT" -> appContext.getString(R.string.notif_commented_post, sender)
                            "FOLLOW" -> appContext.getString(R.string.notif_followed_you, sender)
                            "NEW_POST" -> appContext.getString(R.string.notif_new_post, sender)
                            "REPLY" -> appContext.getString(R.string.notif_replied_comment, sender)
                            else -> notif.message.ifEmpty { appContext.getString(R.string.app_name) }
                        }
                        NotificationHelper.showCommunityNotification(
                            context = appContext,
                            title = sender,
                            body = body,
                            postId = notif.postId,
                            type = notif.type,
                            senderId = notif.senderId,
                            senderName = notif.senderName
                        )
                    }
                    val maxTime = newNotifs.maxOf { it.timestamp }
                    prefs.edit().putLong("last_system_notified_time_$uid", maxTime).apply()
                }
            }

            // 2. Check Admin Announcements
            val announcementPrefs = appContext.getSharedPreferences("app_announcements_prefs", Context.MODE_PRIVATE)
            val lastSeenTs = announcementPrefs.getLong("last_seen_announcement_ts", 0L)
            val announcementSnapshot = FirebaseFirestore.getInstance()
                .collection("app_announcements")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!announcementSnapshot.isEmpty) {
                val doc = announcementSnapshot.documents[0]
                val title = doc.getString("title") ?: ""
                val message = doc.getString("message") ?: ""
                val ts = doc.getLong("timestamp") ?: 0L

                if (ts > lastSeenTs && title.isNotEmpty() && message.isNotEmpty()) {
                    announcementPrefs.edit().putLong("last_seen_announcement_ts", ts).apply()
                    NotificationHelper.showAnnouncementNotification(appContext, title, message)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}

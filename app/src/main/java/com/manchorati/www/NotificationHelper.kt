package com.manchorati.www

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_COMMUNITY = "community_activity_channel_v1"
    const val CHANNEL_ANNOUNCEMENTS = "announcements_channel_v1"

    fun showCommunityNotification(
        context: Context,
        title: String,
        body: String,
        postId: String = "",
        type: String = "GENERAL",
        senderId: String = "",
        senderName: String = ""
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_COMMUNITY,
                "إشعارات المجتمع والتفاعل",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الإعجابات والتعليقات والردود والمتابعين"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_POST_ID", postId)
            putExtra("OPEN_POST_TYPE", type)
            putExtra("OPEN_SENDER_ID", senderId)
            putExtra("OPEN_SENDER_NAME", senderName)
        }

        val requestCode = (System.currentTimeMillis() % 100000).toInt() + (postId.hashCode() % 1000)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_COMMUNITY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notifId = ((System.currentTimeMillis() % 100000) + 500).toInt()
        notificationManager.notify(notifId, builder.build())
    }

    fun showAnnouncementNotification(
        context: Context,
        title: String,
        body: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "إعلانات وتنبيهات الإدارة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات وإعلانات رسمية من إدارة التطبيق"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ANNOUNCEMENTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(1001, builder.build())
    }
}

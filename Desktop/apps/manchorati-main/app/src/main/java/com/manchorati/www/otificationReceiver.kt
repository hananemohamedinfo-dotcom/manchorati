package com.manchorati.www

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "daily_alarm_clock_channel_v1"
    private val NOTIFICATION_ID = 2005

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DailyNotificationHelper.rescheduleFromPreferences(context)
            return
        }

        val dbHelper = DatabaseHelper(context)

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val type = intent.getStringExtra("TYPE") ?: "DEFAULT"
        val categoryName = intent.getStringExtra("CATEGORY") ?: ""

        val title = when {
            dayOfWeek == Calendar.FRIDAY && hour < 12 -> "جمعة مباركة 🕌"
            type == "MORNING" || hour < 14 -> "صباح الخير ☀️"
            type == "EVENING" || hour >= 14 -> "مساء الخير 🌙"
            else -> "رسالة لك اليوم 🌿"
        }

        var targetId: Int? = null
        if (categoryName.isNotEmpty() && categoryName != "افتراضي (الكل)") {
            try {
                targetId = dbHelper.getCategoryIdByName(categoryName.trim())
            } catch (_: Exception) {}
        }

        if (targetId == null || targetId <= 0) {
            targetId = if (dayOfWeek == Calendar.FRIDAY && hour < 12) 17 else if (hour < 14) 13 else 14
        }

        val post = dbHelper.getRandomPostByCategoryId(targetId)
            ?: dbHelper.getRandomPostByCategoryId(13)
            ?: dbHelper.getRandomPostByCategoryId(14)

        val content = post?.content ?: "ابتسم وتفاءل، يومك مبارك وسعيد بإذن الله."
        showNotification(context, title, content)

        DailyNotificationHelper.rescheduleFromPreferences(context)
    }

    private fun showNotification(context: Context, title: String, content: String) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "إشعارات يومية", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 350, 200, 350))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
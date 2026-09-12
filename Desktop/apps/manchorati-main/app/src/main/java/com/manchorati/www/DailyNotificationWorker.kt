package com.manchorati.www

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar

class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val CHANNEL_ID = "daily_smart_notification_channel_v4"
    private val NOTIFICATION_ID = 2002

    override fun doWork(): Result {
        val dbHelper = DatabaseHelper(applicationContext)

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val type = inputData.getString("TYPE") ?: "DEFAULT"
        val categoryName = inputData.getString("CATEGORY") ?: ""

        // 1. تحديد العنوان
        val title = when {
            dayOfWeek == Calendar.FRIDAY && hour < 12 -> "جمعة مباركة 🕌"
            type == "MORNING" || hour < 14 -> "صباح الخير ☀️"
            type == "EVENING" || hour >= 14 -> "مساء الخير 🌙"
            else -> "رسالة لك اليوم 🌿"
        }

        // 2. محاولة جلب ID القسم المختار
        var targetId: Int? = null
        if (categoryName.isNotEmpty() && categoryName != "افتراضي (الكل)") {
            try {
                targetId = dbHelper.getCategoryIdByName(categoryName.trim())
            } catch (e: Exception) {
                targetId = null
            }
        }

        if (targetId == null || targetId <= 0) {
            targetId = if (dayOfWeek == Calendar.FRIDAY && hour < 12) 17 else if (hour < 14) 13 else 14
        }

        // 3. جلب المنشور
        val post = dbHelper.getRandomPostByCategoryId(targetId)
            ?: dbHelper.getRandomPostByCategoryId(13)
            ?: dbHelper.getRandomPostByCategoryId(14)

        if (post != null) {
            showNotification(title, post.content)
        } else {
            showNotification(title, "ابتسم وتفاءل، يومك مبارك وسعيد بإذن الله.")
        }

        // إعادة جدولة اليوم التالي للمحافظة على استمرار الإشعارات
        DailyNotificationHelper.rescheduleFromPreferences(applicationContext)

        return Result.success()
    }

    private fun showNotification(title: String, content: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // إعداد القناة مع الصوت والاهتزاز
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "إشعارات منشوراتي اليومية"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "إشعارات يومية صباحية ومسائية مخصصة"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}
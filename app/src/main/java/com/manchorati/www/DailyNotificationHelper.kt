package com.manchorati.www

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object DailyNotificationHelper {

    private const val REQ_MORNING = 4001
    private const val REQ_EVENING = 4002

    fun setupDailyNotifications(context: Context) {
        val prefs = context.getSharedPreferences("app_notification_settings", Context.MODE_PRIVATE)
        if (!prefs.contains("morning_hour")) {
            prefs.edit().apply {
                putBoolean("master_enabled", true)
                putBoolean("morning_enabled", true)
                putInt("morning_hour", 7)
                putInt("morning_minute", 0)
                putBoolean("evening_enabled", true)
                putInt("evening_hour", 19)
                putInt("evening_minute", 0)
                apply()
            }
        }
        rescheduleFromPreferences(context)
    }

    fun rescheduleFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences("app_notification_settings", Context.MODE_PRIVATE)

        val masterEnabled = prefs.getBoolean("master_enabled", true)
        if (!masterEnabled) {
            cancelAlarm(context, REQ_MORNING)
            cancelAlarm(context, REQ_EVENING)
            return
        }

        // إشعار الصباح: 7:00 صباحاً افتراضياً
        if (prefs.getBoolean("morning_enabled", true)) {
            val hour = prefs.getInt("morning_hour", 7)
            val minute = prefs.getInt("morning_minute", 0)
            val category = prefs.getString("morning_category_name", "افتراضي (الكل)") ?: "افتراضي (الكل)"
            scheduleAlarm(context, hour, minute, REQ_MORNING, "MORNING", category)
        } else {
            cancelAlarm(context, REQ_MORNING)
        }

        // إشعار المساء: 7:00 مساءً (19:00) افتراضياً
        if (prefs.getBoolean("evening_enabled", true)) {
            val hour = prefs.getInt("evening_hour", 19)
            val minute = prefs.getInt("evening_minute", 0)
            val category = prefs.getString("evening_category_name", "افتراضي (الكل)") ?: "افتراضي (الكل)"
            scheduleAlarm(context, hour, minute, REQ_EVENING, "EVENING", category)
        } else {
            cancelAlarm(context, REQ_EVENING)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        requestCode: Int,
        type: String,
        category: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= now.timeInMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerTime = calendar.timeInMillis

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TYPE", type)
            putExtra("CATEGORY", category)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
package com.manchorati.www

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class QuoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.manchorati.www.ACTION_REFRESH_WIDGET"
        const val ACTION_COPY_WIDGET = "com.manchorati.www.ACTION_COPY_WIDGET"
        const val EXTRA_QUOTE_TEXT = "EXTRA_QUOTE_TEXT"
        private const val PREFS_NAME = "manchorati_widget_prefs"
        private const val KEY_QUOTE = "cached_quote"
        private const val KEY_TAG = "cached_tag"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuoteWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (allWidgetIds.isNotEmpty()) {
                val intent = Intent(context, QuoteWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val (quote, tag) = fetchNewQuote(context)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, quote, tag)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH_WIDGET -> {
                val (quote, tag) = fetchNewQuote(context)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, QuoteWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                for (appWidgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, quote, tag)
                }
            }

            ACTION_COPY_WIDGET -> {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val textToCopy = intent.getStringExtra(EXTRA_QUOTE_TEXT)
                    ?: prefs.getString(KEY_QUOTE, "") ?: ""

                if (textToCopy.isNotEmpty()) {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(context.getString(R.string.app_name), textToCopy)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.widget_copied_toast), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun fetchNewQuote(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            val dbHelper = DatabaseHelper(context)
            val post = dbHelper.getRandomWidgetQuote()
            if (post != null && post.content.isNotBlank()) {
                val quote = post.content.trim()
                val tag = post.categoryName.ifBlank { "خاطرة اليوم" }
                prefs.edit()
                    .putString(KEY_QUOTE, quote)
                    .putString(KEY_TAG, tag)
                    .apply()
                Pair(quote, tag)
            } else {
                getFallbackQuote(prefs)
            }
        } catch (e: Exception) {
            getFallbackQuote(prefs)
        }
    }

    private fun getFallbackQuote(prefs: android.content.SharedPreferences): Pair<String, String> {
        val cached = prefs.getString(KEY_QUOTE, "")
        val cachedTag = prefs.getString(KEY_TAG, "خاطرة اليوم")
        return if (!cached.isNullOrBlank()) {
            Pair(cached, cachedTag ?: "خاطرة اليوم")
        } else {
            Pair("ابتسم، فالحياة أجمل وأوسع عندما نراها بعين الرضا والبهجة 🌸", "خاطرة اليوم")
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        quote: String,
        tag: String
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_quote)

        views.setTextViewText(R.id.tvWidgetContent, quote)
        views.setTextViewText(R.id.tvWidgetTag, tag)

        // 1. النقر على البطاقة يفتح التطبيق
        val openAppIntent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.tvWidgetContent, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.layoutWidgetRoot, openAppPendingIntent)

        // 2. النقر على زر التحديث يجلب خاطرة جديدة عشوائية
        val refreshIntent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 1,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetRefresh, refreshPendingIntent)

        // 3. النقر على زر النسخ ينسخ الخاطرة الحالية للحافظة
        val copyIntent = Intent(context, QuoteWidgetProvider::class.java).apply {
            action = ACTION_COPY_WIDGET
            putExtra(EXTRA_QUOTE_TEXT, quote)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 2,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetCopy, copyPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

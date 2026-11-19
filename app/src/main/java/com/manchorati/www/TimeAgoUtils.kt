package com.manchorati.www

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeAgoUtils {

    fun formatTimeAgo(context: Context, timeMillis: Long): String {
        if (timeMillis <= 0L) return ""
        val diff = System.currentTimeMillis() - timeMillis
        val lang = LocaleHelper.getSelectedLanguage(context)

        if (diff < 0L) {
            return when (lang) {
                LocaleHelper.LANG_FR -> "À l'instant"
                LocaleHelper.LANG_EN -> "Just now"
                else -> "الآن"
            }
        }

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> when (lang) {
                LocaleHelper.LANG_FR -> "À l'instant"
                LocaleHelper.LANG_EN -> "Just now"
                else -> "الآن"
            }
            minutes < 60 -> when (lang) {
                LocaleHelper.LANG_FR -> "il y a $minutes min"
                LocaleHelper.LANG_EN -> "${minutes}m ago"
                else -> "منذ $minutes د"
            }
            hours < 24 -> when (lang) {
                LocaleHelper.LANG_FR -> "il y a ${hours}h"
                LocaleHelper.LANG_EN -> "${hours}h ago"
                else -> when (hours) {
                    1L -> "منذ ساعة"
                    2L -> "منذ ساعتين"
                    in 3L..10L -> "منذ $hours ساعات"
                    else -> "منذ $hours ساعة"
                }
            }
            days == 1L -> when (lang) {
                LocaleHelper.LANG_FR -> "Hier"
                LocaleHelper.LANG_EN -> "Yesterday"
                else -> "أمس"
            }
            days < 7 -> when (lang) {
                LocaleHelper.LANG_FR -> "il y a $days j"
                LocaleHelper.LANG_EN -> "${days}d ago"
                else -> when (days) {
                    2L -> "منذ يومين"
                    in 3L..6L -> "منذ $days أيام"
                    else -> "منذ $days يوم"
                }
            }
            days < 30 -> {
                val weeks = days / 7
                when (lang) {
                    LocaleHelper.LANG_FR -> "il y a $weeks sem"
                    LocaleHelper.LANG_EN -> "${weeks}w ago"
                    else -> if (weeks == 1L) "منذ أسبوع" else if (weeks == 2L) "منذ أسبوعين" else "منذ $weeks أسابيع"
                }
            }
            else -> {
                val locale = when (lang) {
                    LocaleHelper.LANG_FR -> Locale.FRENCH
                    LocaleHelper.LANG_EN -> Locale.ENGLISH
                    else -> Locale.forLanguageTag("ar")
                }
                val pattern = if (days < 365) "d MMM" else "d MMM yyyy"
                val sdf = SimpleDateFormat(pattern, locale)
                sdf.format(Date(timeMillis))
            }
        }
    }
}

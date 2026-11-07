package com.manchorati.www

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "app_language_pref"
    private const val KEY_LANG = "app_lang"

    const val LANG_AR = "ar"
    const val LANG_FR = "fr"
    const val LANG_EN = "en"

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANG, LANG_AR) ?: LANG_AR
    }

    fun setLocale(context: Context, langCode: String): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, langCode).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        }

        return updateResources(context, langCode)
    }

    fun applyLocale(context: Context): Context {
        val lang = getSelectedLanguage(context)
        return updateResources(context, lang)
    }

    fun applyLayoutDirection(activity: Activity) {
        val lang = getSelectedLanguage(activity)
        val isRtl = lang == LANG_AR
        activity.window.decorView.layoutDirection = if (isRtl) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
    }

    fun isRtl(context: Context): Boolean {
        return getSelectedLanguage(context) == LANG_AR
    }

    private fun updateResources(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
            context
        }
    }
}

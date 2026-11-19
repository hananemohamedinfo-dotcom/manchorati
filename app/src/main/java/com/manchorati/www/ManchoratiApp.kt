package com.manchorati.www

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class ManchoratiApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    override fun onCreate() {
        super.onCreate()
        val lang = LocaleHelper.getSelectedLanguage(this)
        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        } catch (_: Exception) {}
        LocaleHelper.applyLocale(this)
    }
}

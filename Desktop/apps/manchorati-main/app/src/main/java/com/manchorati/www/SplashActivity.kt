package com.manchorati.www

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    // مدة الانتظار بالمللي ثانية (2500 تعني ثانيتين ونصف)
    private val splashDuration: Long = 800

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val themePrefs = getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE)
        val isDarkMode = themePrefs.getBoolean("is_dark_mode", true)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // مؤقت الانتظار قبل فتح MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish() // إغلاق شاشة الـ Splash لكي لا يعود إليها المستخدم عند الضغط على زر الرجوع
        }, splashDuration)
    }
}
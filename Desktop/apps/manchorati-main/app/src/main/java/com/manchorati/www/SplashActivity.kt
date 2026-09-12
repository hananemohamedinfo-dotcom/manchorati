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

    override fun onCreate(savedInstanceState: Bundle?) {
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
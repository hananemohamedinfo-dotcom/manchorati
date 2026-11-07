package com.manchorati.www

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast

class CopyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textToCopy = intent.getStringExtra("TEXT_TO_COPY")
        
        if (!textToCopy.isNullOrEmpty()) {
            try {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("إشعار منشوراتي", textToCopy)
                clipboard.setPrimaryClip(clip)
                
                Toast.makeText(this, "تم النسخ بنجاح!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // إغلاق الشاشة الشفافة فوراً وبدون أي أثر
        finish()
    }
}
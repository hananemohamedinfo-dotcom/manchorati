package com.manchorati.www

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object StudioAssetManager {

    data class OnlineBackground(
        val id: String,
        val title: String,
        val url: String,
        val previewColor: String
    )

    private val okHttpClient = OkHttpClient()

    val onlineBackgrounds = listOf(
        OnlineBackground(
            "bg_luxury_dark",
            "🖤 أسود فخم",
            "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=720&q=80",
            "#0B0E14"
        ),
        OnlineBackground(
            "bg_warm_coffee",
            "☕ قهوة وكتاب",
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=720&q=80",
            "#1E130B"
        ),
        OnlineBackground(
            "bg_rain_window",
            "🌧️ زجاج المطر",
            "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=720&q=80",
            "#0A131F"
        ),
        OnlineBackground(
            "bg_islamic_gold",
            "🕌 ذهبي إسلامي",
            "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=720&q=80",
            "#1A1305"
        ),
        OnlineBackground(
            "bg_nebula",
            "🌌 سديم كوني",
            "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=720&q=80",
            "#0D091A"
        ),
        OnlineBackground(
            "bg_misty_forest",
            "🌿 غابة صنوبر",
            "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=720&q=80",
            "#08140E"
        ),
        OnlineBackground(
            "bg_golden_sunset",
            "🌅 غروب بحري",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=720&q=80",
            "#1A0F0A"
        ),
        OnlineBackground(
            "bg_vintage_paper",
            "📜 ورق عتيق",
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=720&q=80",
            "#1F1A14"
        )
    )

    fun isBackgroundDownloaded(context: Context, id: String): Boolean {
        val file = File(context.filesDir, "studio_bg/${id}.jpg")
        return file.exists() && file.length() > 1024
    }

    data class OnlineAudio(
        val id: String,
        val title: String,
        val url: String,
        val colorHex: String = "#0284C7"
    )

    val onlineAudios = listOf(
        OnlineAudio(
            "audio_calm_meditation",
            "🧘 تأمل وسكون",
            "https://cdn.freesound.org/previews/518/518305_5121236-lq.mp3",
            "#059669"
        ),
        OnlineAudio(
            "audio_deep_strings",
            "🎻 وتريات عميقة",
            "https://cdn.freesound.org/previews/463/463067_7037-lq.mp3",
            "#7C3AED"
        ),
        OnlineAudio(
            "audio_lofi_vibes",
            "☕ لو-فاي هادئ",
            "https://cdn.freesound.org/previews/563/563814_11861866-lq.mp3",
            "#D97706"
        )
    )

    fun isAudioDownloaded(context: Context, id: String): Boolean {
        val file = File(context.filesDir, "studio_audio/${id}.mp3")
        return file.exists() && file.length() > 5000
    }

    fun getDownloadedAudioPath(context: Context, id: String): String? {
        val file = File(context.filesDir, "studio_audio/${id}.mp3")
        return if (file.exists() && file.length() > 5000) file.absolutePath else null
    }

    fun downloadAudio(
        context: Context,
        audio: OnlineAudio,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_audio").apply { if (!exists()) mkdirs() }
        val targetFile = File(dir, "${audio.id}.mp3")

        if (targetFile.exists() && targetFile.length() > 5000) {
            onSuccess(targetFile.absolutePath)
            return
        }

        val request = Request.Builder().url(audio.url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "خطأ في الاتصال بالإنترنت")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("فشل تحميل الصوت: كود ${response.code}")
                    return
                }
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    onError("الملف الصوتي فارغ")
                    return
                }

                try {
                    FileOutputStream(targetFile).use { it.write(bytes) }
                    onSuccess(targetFile.absolutePath)
                } catch (e: Exception) {
                    onError(e.message ?: "فشل حفظ الملف الصوتي")
                }
            }
        })
    }

    fun getDownloadedBackgroundBitmap(context: Context, id: String): Bitmap? {
        val file = File(context.filesDir, "studio_bg/${id}.jpg")
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun downloadBackground(
        context: Context,
        bg: OnlineBackground,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_bg").apply { if (!exists()) mkdirs() }
        val targetFile = File(dir, "${bg.id}.jpg")

        if (targetFile.exists() && targetFile.length() > 1024) {
            val bmp = BitmapFactory.decodeFile(targetFile.absolutePath)
            if (bmp != null) {
                onSuccess(bmp)
                return
            }
        }

        val request = Request.Builder().url(bg.url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "خطأ في الاتصال بالإنترنت")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("فشل التحميل: كود ${response.code}")
                    return
                }
                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    onError("الملف فارغ")
                    return
                }

                try {
                    FileOutputStream(targetFile).use { it.write(bytes) }
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        onSuccess(bmp)
                    } else {
                        onError("فشل فك ترميز الصورة")
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "فشل حفظ الملف")
                }
            }
        })
    }
}

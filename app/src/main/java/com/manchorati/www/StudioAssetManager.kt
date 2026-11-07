package com.manchorati.www

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object StudioAssetManager {

    data class OnlineBackground(
        val id: String,
        val title: String,
        val category: String, // "dark", "nature", "warm", "minimal"
        val url: String,
        val thumbUrl: String,
        val previewColor: String = "#1E222B"
    )

    private val okHttpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    val onlineBackgrounds = listOf(
        // Category: Dark
        OnlineBackground(
            "bg_luxury_dark",
            "Obsidian Dark",
            "dark",
            "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=240&h=300&q=65",
            "#0B0E14"
        ),
        OnlineBackground(
            "bg_nebula",
            "Cosmic Dust",
            "dark",
            "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=240&h=300&q=65",
            "#0D091A"
        ),
        OnlineBackground(
            "bg_midnight_silk",
            "Midnight Silk",
            "dark",
            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=240&h=300&q=65",
            "#0F172A"
        ),
        OnlineBackground(
            "bg_night_stars",
            "Starry Night",
            "dark",
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=240&h=300&q=65",
            "#0B132B"
        ),
        OnlineBackground(
            "bg_black_marble",
            "Black Marble",
            "dark",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=240&h=300&q=65",
            "#0F1117"
        ),

        // Category: Nature
        OnlineBackground(
            "bg_rain_window",
            "Rainy Window",
            "nature",
            "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=240&h=300&q=65",
            "#0A131F"
        ),
        OnlineBackground(
            "bg_misty_forest",
            "Misty Pines",
            "nature",
            "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=240&h=300&q=65",
            "#08140E"
        ),
        OnlineBackground(
            "bg_golden_sunset",
            "Golden Shore",
            "nature",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=240&h=300&q=65",
            "#1A0F0A"
        ),
        OnlineBackground(
            "bg_deep_ocean",
            "Deep Waves",
            "nature",
            "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?auto=format&fit=crop&w=240&h=300&q=65",
            "#021B2B"
        ),
        OnlineBackground(
            "bg_desert_dunes",
            "Sahara Dunes",
            "nature",
            "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=240&h=300&q=65",
            "#1A140E"
        ),

        // Category: Warm & Aesthetic
        OnlineBackground(
            "bg_warm_coffee",
            "Warm Coffee",
            "warm",
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=240&h=300&q=65",
            "#1E130B"
        ),
        OnlineBackground(
            "bg_islamic_gold",
            "Islamic Arch",
            "warm",
            "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=240&h=300&q=65",
            "#1A1305"
        ),
        OnlineBackground(
            "bg_vintage_paper",
            "Parchment Mood",
            "warm",
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=240&h=300&q=65",
            "#1F1A14"
        ),
        OnlineBackground(
            "bg_candle_mood",
            "Quiet Candle",
            "warm",
            "https://images.unsplash.com/photo-1508873696983-2df5293cb395?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1508873696983-2df5293cb395?auto=format&fit=crop&w=240&h=300&q=65",
            "#22140A"
        ),
        OnlineBackground(
            "bg_old_library",
            "Old Library",
            "warm",
            "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=240&h=300&q=65",
            "#1A1108"
        ),
        OnlineBackground(
            "bg_neon_night",
            "Neon Horizon",
            "dark",
            "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?auto=format&fit=crop&w=240&h=300&q=65",
            "#0A0B1A"
        ),
        OnlineBackground(
            "bg_dark_geometry",
            "Dark Minimal",
            "dark",
            "https://images.unsplash.com/photo-1550684376-efcbd6e3f031?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1550684376-efcbd6e3f031?auto=format&fit=crop&w=240&h=300&q=65",
            "#12141A"
        ),
        OnlineBackground(
            "bg_mountain_peak",
            "Mountain Peak",
            "nature",
            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=240&h=300&q=65",
            "#0A1520"
        ),
        OnlineBackground(
            "bg_aurora",
            "Northern Aurora",
            "nature",
            "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?auto=format&fit=crop&w=240&h=300&q=65",
            "#051A18"
        ),
        OnlineBackground(
            "bg_cozy_window",
            "Cozy Rainy Day",
            "warm",
            "https://images.unsplash.com/photo-1518495973542-4542c06a5843?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1518495973542-4542c06a5843?auto=format&fit=crop&w=240&h=300&q=65",
            "#18120B"
        ),
        OnlineBackground(
            "bg_open_book",
            "Open Book Light",
            "warm",
            "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?auto=format&fit=crop&w=720&q=80",
            "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?auto=format&fit=crop&w=240&h=300&q=65",
            "#1E1610"
        )
    )

    fun isBackgroundDownloaded(context: Context, id: String): Boolean {
        val file = File(context.filesDir, "studio_bg/${id}.jpg")
        return file.exists() && file.length() > 1024
    }

    data class OnlineAudio(
        val id: String,
        val title: String,
        val category: String, // "طبيعة", "استرخاء", "مطر", "بحر"
        val url: String,
        val colorHex: String = "#0284C7"
    )

    val onlineAudios = listOf(
        // Melodic Instrumental Music (موسيقى لحنية هادئة: بيانو، غيتار، كمان، قيثارة)
        OnlineAudio(
            "music_piano_dream",
            "بيانو هادئ (Piano Dream)",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/piano.mp3",
            "#38BDF8"
        ),
        OnlineAudio(
            "music_guitar_rural",
            "غيتار ريفي هادئ (Guitar Mood)",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/silent-wood.mp3",
            "#F59E0B"
        ),
        OnlineAudio(
            "music_sorrow_violin",
            "كمان حزين ومؤثر (Violin & Strings)",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/sorrow.mp3",
            "#EC4899"
        ),
        OnlineAudio(
            "music_acoustic_guitar",
            "أوتار دافئة (Acoustic Strings)",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/relaxing.mp3",
            "#10B981"
        ),
        OnlineAudio(
            "music_harp_zen",
            "قيثارة ساحرة (Harp Zen)",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/harp.mp3",
            "#8B5CF6"
        ),
        OnlineAudio(
            "music_zen_mind",
            "صفاء الذهن (Zen Chill)",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/empty-mind-118973.mp3",
            "#06B6D4"
        ),
        OnlineAudio(
            "music_morning_melody",
            "أنغام الصباح (Morning Melody)",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/otjan-bird.mp3",
            "#3B82F6"
        ),
        OnlineAudio(
            "music_lofi_moon",
            "ضوء القمر (Lofi Moon)",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/crescent-moon.mp3",
            "#A855F7"
        ),

        // Gentle Ambient & Peaceful Nature (طبيعة وسكينة - بدون مطر أو عواصف)
        OnlineAudio(
            "audio_campfire",
            "نار دافئة",
            "طبيعة",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/nature/campfire.mp3",
            "#F97316"
        ),
        OnlineAudio(
            "audio_river",
            "خرير النهر",
            "طبيعة",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/nature/river.mp3",
            "#0284C7"
        ),
        OnlineAudio(
            "audio_waterfall",
            "شلال هادئ",
            "طبيعة",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/nature/waterfall.mp3",
            "#059669"
        ),
        OnlineAudio(
            "audio_waves",
            "أمواج البحر",
            "طبيعة",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/nature/waves.mp3",
            "#38BDF8"
        ),
        OnlineAudio(
            "audio_wind_trees",
            "نسيم الأشجار",
            "طبيعة",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/nature/wind-in-trees.mp3",
            "#10B981"
        ),
        OnlineAudio(
            "audio_library",
            "سكينة وهدوء",
            "هدوء",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/places/library.mp3",
            "#F59E0B"
        )
    )

    fun isAudioDownloaded(context: Context, id: String): Boolean {
        val file = File(context.filesDir, "studio_audio/${id}.mp3")
        return file.exists() && file.length() > 1024
    }

    fun getDownloadedAudioPath(context: Context, id: String): String? {
        val file = File(context.filesDir, "studio_audio/${id}.mp3")
        return if (file.exists() && file.length() > 1024) file.absolutePath else null
    }

    fun downloadAudio(
        context: Context,
        audio: OnlineAudio,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_audio").apply { if (!exists()) mkdirs() }
        val targetFile = File(dir, "${audio.id}.mp3")

        if (targetFile.exists() && targetFile.length() > 1024) {
            mainHandler.post { onSuccess(targetFile.absolutePath) }
            return
        }

        val request = Request.Builder().url(audio.url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onError("تعذر تحميل الملف حالياً، يرجى المحاولة لاحقاً") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post { onError("تعذر تحميل الملف حالياً، يرجى المحاولة لاحقاً") }
                    return
                }

                val body = response.body
                if (body == null) {
                    mainHandler.post { onError("تعذر تحميل الملف حالياً، يرجى المحاولة لاحقاً") }
                    return
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val tempFile = File(dir, "${audio.id}.tmp")

                try {
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var lastReportedPercent = -1
                    FileOutputStream(tempFile).use { out ->
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            bytesCopied += read
                            if (totalBytes > 0) {
                                val percent = ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (percent != lastReportedPercent && (percent - lastReportedPercent >= 2 || percent == 100)) {
                                    lastReportedPercent = percent
                                    mainHandler.post { onProgress(percent) }
                                }
                            }
                        }
                    }

                    if (tempFile.renameTo(targetFile) || (tempFile.copyTo(targetFile, overwrite = true).also { tempFile.delete() } != null)) {
                        mainHandler.post {
                            onProgress(100)
                            onSuccess(targetFile.absolutePath)
                        }
                    } else {
                        mainHandler.post { onError("تعذر حفظ الملف الصوتي") }
                    }
                } catch (e: Exception) {
                    mainHandler.post { onError("تعذر تحميل الملف حالياً، يرجى المحاولة لاحقاً") }
                } finally {
                    try { inputStream.close() } catch (_: Exception) {}
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
        onProgress: (Int) -> Unit,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_bg").apply { if (!exists()) mkdirs() }
        val targetFile = File(dir, "${bg.id}.jpg")

        if (targetFile.exists() && targetFile.length() > 1024) {
            val bmp = BitmapFactory.decodeFile(targetFile.absolutePath)
            if (bmp != null) {
                mainHandler.post { onSuccess(bmp) }
                return
            }
        }

        val request = Request.Builder().url(bg.url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onError("تعذر تحميل الصورة حالياً، يرجى المحاولة لاحقاً") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post { onError("تعذر تحميل الصورة حالياً، يرجى المحاولة لاحقاً") }
                    return
                }

                val body = response.body
                if (body == null) {
                    mainHandler.post { onError("تعذر تحميل الصورة حالياً، يرجى المحاولة لاحقاً") }
                    return
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val tempFile = File(dir, "${bg.id}.tmp")

                try {
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    FileOutputStream(tempFile).use { out ->
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            bytesCopied += read
                            if (totalBytes > 0) {
                                val percent = ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 100)
                                mainHandler.post { onProgress(percent) }
                            }
                        }
                    }

                    if (tempFile.renameTo(targetFile) || (tempFile.copyTo(targetFile, overwrite = true).also { tempFile.delete() } != null)) {
                        val bmp = BitmapFactory.decodeFile(targetFile.absolutePath)
                        if (bmp != null) {
                            mainHandler.post {
                                onProgress(100)
                                onSuccess(bmp)
                            }
                        } else {
                            mainHandler.post { onError("تعذر معالجة الصورة") }
                        }
                    } else {
                        mainHandler.post { onError("تعذر حفظ الصورة") }
                    }
                } catch (e: Exception) {
                    mainHandler.post { onError("تعذر تحميل الصورة حالياً، يرجى المحاولة لاحقاً") }
                } finally {
                    try { inputStream.close() } catch (_: Exception) {}
                }
            }
        })
    }
}

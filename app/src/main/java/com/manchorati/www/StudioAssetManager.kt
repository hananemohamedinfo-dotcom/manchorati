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

    data class OnlineSticker(
        val id: String,
        val title: String,
        val category: String, // "islamic", "feelings", "magic", "reactions"
        val url: String,
        val thumbUrl: String = url,
        val isAnimated: Boolean = true
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
            "بيانو هادئ",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/piano.mp3",
            "#38BDF8"
        ),
        OnlineAudio(
            "music_guitar_rural",
            "غيتار ريفي هادئ",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/silent-wood.mp3",
            "#F59E0B"
        ),
        OnlineAudio(
            "music_sorrow_violin",
            "كمان حزين ومؤثر",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/sorrow.mp3",
            "#EC4899"
        ),
        OnlineAudio(
            "music_acoustic_guitar",
            "أوتار دافئة",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/relaxing.mp3",
            "#10B981"
        ),
        OnlineAudio(
            "music_harp_zen",
            "قيثارة ساحرة",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/harp.mp3",
            "#8B5CF6"
        ),
        OnlineAudio(
            "music_zen_mind",
            "صفاء الذهن",
            "موسيقى",
            "https://raw.githubusercontent.com/riccardobertolini/lofi-music/master/public/audio/empty-mind-118973.mp3",
            "#06B6D4"
        ),
        OnlineAudio(
            "music_morning_melody",
            "أنغام الصباح",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/otjan-bird.mp3",
            "#3B82F6"
        ),
        OnlineAudio(
            "music_lofi_moon",
            "ضوء القمر",
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
        ),
        OnlineAudio(
            "audio_rain_window",
            "مطر على النافذة",
            "مطر",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/rain/rain-on-window.mp3",
            "#0284C7"
        ),
        OnlineAudio(
            "audio_thunder_rain",
            "رعد ومطر دافئ",
            "مطر",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/rain/thunder.mp3",
            "#4F46E5"
        ),
        OnlineAudio(
            "audio_rain_leaves",
            "مطر على أوراق الشجر",
            "مطر",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/rain/rain-on-leaves.mp3",
            "#10B981"
        ),
        OnlineAudio(
            "audio_cafe_ambience",
            "أجواء مقهى دافئ",
            "هدوء",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/places/cafe.mp3",
            "#D97706"
        ),
        OnlineAudio(
            "audio_morning_birds",
            "عصافير الصباح",
            "طبيعة",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/animals/birds.mp3",
            "#84CC16"
        ),
        OnlineAudio(
            "audio_wind_chimes",
            "أجراس الرياح الشاعرية",
            "هدوء",
            "https://raw.githubusercontent.com/remvze/moodist/main/public/sounds/things/wind-chimes.mp3",
            "#06B6D4"
        ),
        OnlineAudio(
            "audio_celestia_lofi",
            "أنغام سماوية",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/ghostrifter-official-celestia.mp3",
            "#8B5CF6"
        ),
        OnlineAudio(
            "audio_lost_found_lofi",
            "سكينة ليلية",
            "موسيقى",
            "https://raw.githubusercontent.com/ArvindParekh/Lofi-Music-App/main/public/assets/sound/track/lost-and-found.mp3",
            "#EC4899"
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

    // -------------------------------------------------------------
    // Curated Transparent Animated Stickers & GIFs Catalog
    // -------------------------------------------------------------
    val onlineStickers = listOf(
        // 🕌 إسلاميات وأدعية
        OnlineSticker(
            id = "stk_crescent_moon",
            title = "هلال رمضان",
            category = "islamic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Crescent%20Moon.png"
        ),
        OnlineSticker(
            id = "stk_prayer_hands",
            title = "دعاء وتضرع",
            category = "islamic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Folded%20Hands.png"
        ),
        OnlineSticker(
            id = "stk_glowing_star",
            title = "نجم ساطع",
            category = "islamic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Glowing%20Star.png"
        ),
        OnlineSticker(
            id = "stk_candle_light",
            title = "شمعة مضيئة",
            category = "islamic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Candle.png"
        ),
        OnlineSticker(
            id = "stk_dove",
            title = "حمامة السلام",
            category = "islamic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Dove.png"
        ),
        OnlineSticker(
            id = "stk_sun_gold",
            title = "شمس الإشراق",
            category = "islamic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Sun.png"
        ),

        // ❤️ مشاعر وقلوب
        OnlineSticker(
            id = "stk_red_heart",
            title = "قلب نابض",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Red%20Heart.png"
        ),
        OnlineSticker(
            id = "stk_heart_on_fire",
            title = "قلب مشتعل",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Heart%20on%20Fire.png"
        ),
        OnlineSticker(
            id = "stk_sparkling_heart",
            title = "قلب متوهج",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Sparkling%20Heart.png"
        ),
        OnlineSticker(
            id = "stk_growing_heart",
            title = "قلب مفعم",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Growing%20Heart.png"
        ),
        OnlineSticker(
            id = "stk_two_hearts",
            title = "قلبان ورديان",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Two%20Hearts.png"
        ),
        OnlineSticker(
            id = "stk_heart_arrow",
            title = "سهم الحب",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Heart%20with%20Arrow.png"
        ),
        OnlineSticker(
            id = "stk_rose_flower",
            title = "وردة حمراء",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Rose.png"
        ),
        OnlineSticker(
            id = "stk_cherry_blossom",
            title = "زهر الكرز",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Cherry%20Blossom.png"
        ),
        OnlineSticker(
            id = "stk_heart_eyes",
            title = "وجه بقلوب",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Smiling%20Face%20with%20Heart-Eyes.png"
        ),
        OnlineSticker(
            id = "stk_kissing_heart",
            title = "قبلة حب",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Face%20Blowing%20a%20Kiss.png"
        ),
        OnlineSticker(
            id = "stk_tears_joy",
            title = "دموع الفرح",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Face%20with%20Tears%20of%20Joy.png"
        ),
        OnlineSticker(
            id = "stk_rofl",
            title = "ضحك هستيري",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Rolling%20on%20the%20Floor%20Laughing.png"
        ),
        OnlineSticker(
            id = "stk_star_struck",
            title = "عيون تلمع",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Star-Struck.png"
        ),
        OnlineSticker(
            id = "stk_broken_heart",
            title = "قلب مكسور",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Broken%20Heart.png"
        ),
        OnlineSticker(
            id = "stk_hugging_face",
            title = "عناق دافئ",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Hugging%20Face.png"
        ),
        OnlineSticker(
            id = "stk_thinking_face",
            title = "تفكير عميق",
            category = "feelings",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Thinking%20Face.png"
        ),

        // ✨ سحر وتألق
        OnlineSticker(
            id = "stk_butterfly",
            title = "فراشة مشعة",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Butterfly.png"
        ),
        OnlineSticker(
            id = "stk_sparkles",
            title = "شرارات سحرية",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Sparkles.png"
        ),
        OnlineSticker(
            id = "stk_shooting_star",
            title = "شهاب مضيء",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Shooting%20Star.png"
        ),
        OnlineSticker(
            id = "stk_gem_stone",
            title = "ألماسة نادرة",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Gem%20Stone.png"
        ),
        OnlineSticker(
            id = "stk_rainbow",
            title = "قوس قزح",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Rainbow.png"
        ),
        OnlineSticker(
            id = "stk_maple_leaf",
            title = "ورقة شجر خريفية",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Maple%20Leaf.png"
        ),
        OnlineSticker(
            id = "stk_milky_way",
            title = "درب التبانة",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Milky%20Way.png"
        ),
        OnlineSticker(
            id = "stk_planet",
            title = "كوكب فضائي",
            category = "magic",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Ringed%20Planet.png"
        ),

        // 👏 تفاعلات واحتفال
        OnlineSticker(
            id = "stk_thumbs_up",
            title = "لايك 3D ذهبي",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Thumbs%20Up.png"
        ),
        OnlineSticker(
            id = "stk_party_popper",
            title = "احتفال ومفرقعات",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Party%20Popper.png"
        ),
        OnlineSticker(
            id = "stk_partying_face",
            title = "فرحة واحتفال",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Partying%20Face.png"
        ),
        OnlineSticker(
            id = "stk_clapping_hands",
            title = "تصفيق حار",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Clapping%20Hands.png"
        ),
        OnlineSticker(
            id = "stk_victory_hand",
            title = "علامة النصر",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Victory%20Hand.png"
        ),
        OnlineSticker(
            id = "stk_crown",
            title = "تاج ملكي فخم",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Crown.png"
        ),
        OnlineSticker(
            id = "stk_trophy",
            title = "كأس التميز",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/Trophy.png"
        ),
        OnlineSticker(
            id = "stk_medal",
            title = "ميدالية المركز الأول",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Activities/1st%20Place%20Medal.png"
        ),
        OnlineSticker(
            id = "stk_check_mark",
            title = "توثيق صح",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Check%20Mark%20Button.png"
        ),
        OnlineSticker(
            id = "stk_bell",
            title = "جرس تنبيه",
            category = "reactions",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Bell.png"
        ),

        // 🔥 طاقة وحماس
        OnlineSticker(
            id = "stk_fire_blaze",
            title = "شعلة نار",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Fire.png"
        ),
        OnlineSticker(
            id = "stk_hundred_points",
            title = "100% تميز",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Symbols/Hundred%20Points.png"
        ),
        OnlineSticker(
            id = "stk_flexed_biceps",
            title = "عزيمة وقوة",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Hand%20gestures/Flexed%20Biceps.png"
        ),
        OnlineSticker(
            id = "stk_hot_coffee",
            title = "فنجان قهوة دافئ",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Food/Hot%20Beverage.png"
        ),
        OnlineSticker(
            id = "stk_teacup",
            title = "شاي مغربي",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Food/Teacup%20Without%20Handle.png"
        ),
        OnlineSticker(
            id = "stk_open_book",
            title = "كتاب أدبي",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Open%20Book.png"
        ),
        OnlineSticker(
            id = "stk_fountain_pen",
            title = "ريشة كتابة",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Fountain%20Pen.png"
        ),
        OnlineSticker(
            id = "stk_scroll",
            title = "مخطوطة عتيقة",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Scroll.png"
        ),
        OnlineSticker(
            id = "stk_hourglass",
            title = "ساعة رملية",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Hourglass%20Done.png"
        ),
        OnlineSticker(
            id = "stk_musical_notes",
            title = "نوتات موسيقية",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Musical%20Notes.png"
        ),
        OnlineSticker(
            id = "stk_headphones",
            title = "سماعات رأس",
            category = "vibes",
            url = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Headphone.png"
        )
    )

    fun isStickerDownloaded(context: Context, id: String): Boolean {
        val dir = File(context.filesDir, "studio_stickers")
        val fileGif = File(dir, "${id}.gif")
        val filePng = File(dir, "${id}.png")
        return (fileGif.exists() && fileGif.length() > 512) || (filePng.exists() && filePng.length() > 512)
    }

    fun getDownloadedStickerPath(context: Context, id: String): String? {
        val dir = File(context.filesDir, "studio_stickers")
        val fileGif = File(dir, "${id}.gif")
        if (fileGif.exists() && fileGif.length() > 512) return fileGif.absolutePath
        val filePng = File(dir, "${id}.png")
        if (filePng.exists() && filePng.length() > 512) return filePng.absolutePath
        return null
    }

    fun downloadSticker(
        context: Context,
        sticker: OnlineSticker,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_stickers").apply { if (!exists()) mkdirs() }
        val ext = if (sticker.url.endsWith(".gif", ignoreCase = true)) "gif" else "png"
        val targetFile = File(dir, "${sticker.id}.$ext")

        if (targetFile.exists() && targetFile.length() > 512) {
            mainHandler.post { onSuccess(targetFile.absolutePath) }
            return
        }

        val request = Request.Builder()
            .url(sticker.url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept", "image/png,image/webp,image/gif,image/*,*/*")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onError("تعذر تحميل الملصق حالياً، تأكد من اتصال الإنترنت") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post { onError("تعذر تحميل الملصق حالياً") }
                    return
                }

                val body = response.body
                if (body == null) {
                    mainHandler.post { onError("الملف غير متوفر") }
                    return
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val tempFile = File(dir, "${sticker.id}.tmp")

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
                                if (percent != lastReportedPercent && percent % 5 == 0) {
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
                        mainHandler.post { onError("تعذر حفظ الملصق") }
                    }
                } catch (e: Exception) {
                    mainHandler.post { onError("حدث خطأ أثناء تحميل الملصق") }
                } finally {
                    try { inputStream.close() } catch (_: Exception) {}
                }
            }
        })
    }

    // -------------------------------------------------------------
    // Video Backgrounds (Motion Video Loops MP4)
    // -------------------------------------------------------------
    data class OnlineVideoBg(
        val id: String,
        val title: String,
        val category: String, // "rain", "space", "nature"
        val videoUrl: String,
        val thumbUrl: String,
        val durationSec: Int = 15
    )

    val onlineVideoBackgrounds = listOf(
        OnlineVideoBg(
            id = "vbg_rain_road",
            title = "أمطار الطريق الهادئة",
            category = "rain",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/e/eb/Rain_falling_on_road_-_gnangarra.webm/Rain_falling_on_road_-_gnangarra.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_rain_cyclone",
            title = "أمطار ورياح عاصفة",
            category = "rain",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/f/f3/Rain_due_to_impact_of_Cyclone_Vayu_18_June_2019_01.ogv/Rain_due_to_impact_of_Cyclone_Vayu_18_June_2019_01.ogv.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1534274988757-a28bf1a57c17?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_rain_split",
            title = "زخات المطر على الشرفة",
            category = "rain",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a4/Rain_in_Split_142743.webm/Rain_in_Split_142743.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1519692933481-e162a57d6721?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_waterfall",
            title = "شلال الغابة المتدفق",
            category = "nature",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/a/a6/Boka_Waterfall%2C_Slovenia.ogv/Boka_Waterfall%2C_Slovenia.ogv.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1432405972618-c60b0225b8f9?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_waves_beach",
            title = "أمواج الشاطئ الهادئة",
            category = "nature",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/2/21/Body_surfing_a_wave.webm/Body_surfing_a_wave.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_sunset_sea",
            title = "غروب الشمس على البحر",
            category = "nature",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/3/37/2011_Cayman_Islands_Luyten-De-Hauwere_Man_holding_manta_ray.webm/2011_Cayman_Islands_Luyten-De-Hauwere_Man_holding_manta_ray.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_snow_winter",
            title = "تساقط الثلوج الهادئة",
            category = "nature",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0d/Cardinal_in_the_Snow_%2830761094854%29.webm/Cardinal_in_the_Snow_%2830761094854%29.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1517299321609-52687d1bc55a?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_clouds_timelapse",
            title = "حركة الغيوم والسماء",
            category = "nature",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/1/14/Cloud_timelapse_in_New_Mexico.webm/Cloud_timelapse_in_New_Mexico.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1534088568595-a066f410bcda?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_aurora",
            title = "أضواء الشفق القطبي",
            category = "space",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/f/ff/Aurora-Iceland_Northern_Lights_timelapse_2026-02-06.hd.webm/Aurora-Iceland_Northern_Lights_timelapse_2026-02-06.hd.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_stars_milkyway",
            title = "دوران النجوم والمجرة",
            category = "space",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/0/0f/A_starry_night%2C_in_another_galaxy_%28potw2637a%29.webm/A_starry_night%2C_in_another_galaxy_%28potw2637a%29.webm.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=240&h=300&q=65"
        ),
        OnlineVideoBg(
            id = "vbg_fire_candle",
            title = "شعلة نار هادئة",
            category = "dark",
            videoUrl = "https://upload.wikimedia.org/wikipedia/commons/transcoded/4/46/Russia_Samara_Eternal_flame_Memorial.ogv/Russia_Samara_Eternal_flame_Memorial.ogv.360p.mpeg4.mov",
            thumbUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=240&h=300&q=65"
        )
    )

    fun isVideoBackgroundDownloaded(context: Context, id: String): Boolean {
        val file = File(context.filesDir, "studio_vbg/${id}.mp4")
        return file.exists() && file.length() > 1024
    }

    fun getDownloadedVideoBackgroundPath(context: Context, id: String): String? {
        val file = File(context.filesDir, "studio_vbg/${id}.mp4")
        return if (file.exists() && file.length() > 1024) file.absolutePath else null
    }

    fun downloadVideoBackground(
        context: Context,
        vbg: OnlineVideoBg,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_vbg").apply { if (!exists()) mkdirs() }
        val targetFile = File(dir, "${vbg.id}.mp4")

        if (targetFile.exists() && targetFile.length() > 1024) {
            mainHandler.post { onSuccess(targetFile.absolutePath) }
            return
        }

        val request = Request.Builder()
            .url(vbg.videoUrl)
            .header("User-Agent", "ManchoratiApp/1.0 (https://manchorati.com; contact@manchorati.com)")
            .header("Accept", "*/*")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onError("تعذر تحميل فيديو الخلفية") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post { onError("تعذر تحميل فيديو الخلفية") }
                    return
                }
                val body = response.body
                if (body == null) {
                    mainHandler.post { onError("الملف غير متوفر") }
                    return
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val tempFile = File(dir, "${vbg.id}.tmp")

                try {
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var lastReported = -1
                    FileOutputStream(tempFile).use { out ->
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            bytesCopied += read
                            if (totalBytes > 0) {
                                val percent = ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 100)
                                if (percent != lastReported && percent % 5 == 0) {
                                    lastReported = percent
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
                        mainHandler.post { onError("تعذر حفظ الفيديو") }
                    }
                } catch (e: Exception) {
                    mainHandler.post { onError("حدث خطأ أثناء تحميل فيديو الخلفية") }
                } finally {
                    try { inputStream.close() } catch (_: Exception) {}
                }
            }
        })
    }
}

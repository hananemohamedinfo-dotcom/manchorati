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
        val previewColor: String
    )

    private val okHttpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    val onlineBackgrounds = listOf(
        OnlineBackground(
            "bg_luxury_dark",
            "Obsidian Dark",
            "dark",
            "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?auto=format&fit=crop&w=720&q=80",
            "#0B0E14"
        ),
        OnlineBackground(
            "bg_rain_window",
            "Rainy Window",
            "nature",
            "https://images.unsplash.com/photo-1515694346937-94d85e41e6f0?auto=format&fit=crop&w=720&q=80",
            "#0A131F"
        ),
        OnlineBackground(
            "bg_warm_coffee",
            "Warm Coffee",
            "warm",
            "https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?auto=format&fit=crop&w=720&q=80",
            "#1E130B"
        ),
        OnlineBackground(
            "bg_islamic_gold",
            "Islamic Arch",
            "warm",
            "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=720&q=80",
            "#1A1305"
        ),
        OnlineBackground(
            "bg_nebula",
            "Cosmic Nebula",
            "dark",
            "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?auto=format&fit=crop&w=720&q=80",
            "#0D091A"
        ),
        OnlineBackground(
            "bg_misty_forest",
            "Misty Pines",
            "nature",
            "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=720&q=80",
            "#08140E"
        ),
        OnlineBackground(
            "bg_golden_sunset",
            "Golden Shore",
            "nature",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=720&q=80",
            "#1A0F0A"
        ),
        OnlineBackground(
            "bg_vintage_paper",
            "Parchment Mood",
            "warm",
            "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=720&q=80",
            "#1F1A14"
        ),
        OnlineBackground(
            "bg_midnight_silk",
            "Midnight Silk",
            "dark",
            "https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=720&q=80",
            "#0F172A"
        ),
        OnlineBackground(
            "bg_desert_dunes",
            "Sahara Dunes",
            "minimal",
            "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=720&q=80",
            "#1A140E"
        ),
        OnlineBackground(
            "bg_minimal_marble",
            "Slate Texture",
            "minimal",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=720&q=80",
            "#111827"
        )
    )

    fun isBackgroundDownloaded(context: Context, id: String): Boolean {
        val file = File(context.filesDir, "studio_bg/${id}.jpg")
        return file.exists() && file.length() > 1024
    }

    data class OnlineAudio(
        val id: String,
        val title: String,
        val category: String, // "ambient", "acoustic", "chill", "classical"
        val url: String,
        val colorHex: String = "#0284C7"
    )

    val onlineAudios = listOf(
        OnlineAudio(
            "audio_calm_meditation",
            "Calm Meditation",
            "Ambient",
            "https://cdn.freesound.org/previews/518/518305_5121236-lq.mp3",
            "#059669"
        ),
        OnlineAudio(
            "audio_deep_strings",
            "Deep Strings",
            "Classical",
            "https://cdn.freesound.org/previews/463/463067_7037-lq.mp3",
            "#7C3AED"
        ),
        OnlineAudio(
            "audio_lofi_vibes",
            "Lofi Sunset",
            "Chill",
            "https://cdn.freesound.org/previews/563/563814_11861866-lq.mp3",
            "#D97706"
        ),
        OnlineAudio(
            "audio_zen_garden",
            "Zen Atmosphere",
            "Ambient",
            "https://cdn.freesound.org/previews/416/416529_5121236-lq.mp3",
            "#0284C7"
        ),
        OnlineAudio(
            "audio_acoustic_calm",
            "Acoustic Peace",
            "Acoustic",
            "https://cdn.freesound.org/previews/387/387232_1471780-lq.mp3",
            "#EC4899"
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
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val dir = File(context.filesDir, "studio_audio").apply { if (!exists()) mkdirs() }
        val targetFile = File(dir, "${audio.id}.mp3")

        if (targetFile.exists() && targetFile.length() > 5000) {
            mainHandler.post { onSuccess(targetFile.absolutePath) }
            return
        }

        val request = Request.Builder().url(audio.url).build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post { onError(e.message ?: "Connection error") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post { onError("Download failed (code ${response.code})") }
                    return
                }

                val body = response.body
                if (body == null) {
                    mainHandler.post { onError("Empty audio file") }
                    return
                }

                val totalBytes = body.contentLength()
                val inputStream = body.byteStream()
                val tempFile = File(dir, "${audio.id}.tmp")

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
                        mainHandler.post {
                            onProgress(100)
                            onSuccess(targetFile.absolutePath)
                        }
                    } else {
                        mainHandler.post { onError("Failed to save audio file") }
                    }
                } catch (e: Exception) {
                    mainHandler.post { onError(e.message ?: "Error saving audio") }
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
                mainHandler.post { onError(e.message ?: "Connection error") }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post { onError("Download failed (code ${response.code})") }
                    return
                }

                val body = response.body
                if (body == null) {
                    mainHandler.post { onError("Empty image file") }
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
                            mainHandler.post { onError("Failed to decode image") }
                        }
                    } else {
                        mainHandler.post { onError("Failed to save image") }
                    }
                } catch (e: Exception) {
                    mainHandler.post { onError(e.message ?: "Error saving image") }
                } finally {
                    try { inputStream.close() } catch (_: Exception) {}
                }
            }
        })
    }
}

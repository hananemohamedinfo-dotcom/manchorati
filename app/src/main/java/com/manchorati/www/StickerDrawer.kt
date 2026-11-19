package com.manchorati.www

import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.os.Build
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance animated GIF, APNG, WebP, Video overlays and sticker renderer for Canvas.
 * Supports Movie-based GIF decoding, ImageDecoder on API 28+, MediaMetadataRetriever for videos, and cached Bitmaps.
 */
object StickerDrawer {

    @Suppress("DEPRECATION")
    private val movieCache = ConcurrentHashMap<String, Movie?>()
    private val bitmapCache = ConcurrentHashMap<String, Bitmap?>()
    private val drawableCache = ConcurrentHashMap<String, Drawable?>()
    private val videoRetrieverCache = ConcurrentHashMap<String, MediaMetadataRetriever?>()
    private val videoDurationCache = ConcurrentHashMap<String, Long>()
    private val videoFrameCache = ConcurrentHashMap<String, ConcurrentHashMap<Long, Bitmap>>()

    // Reusable offscreen buffer for hardware-accelerated canvas rendering
    private var tempBitmap: Bitmap? = null
    private var tempCanvas: Canvas? = null

    @Suppress("DEPRECATION")
    fun drawSticker(
        canvas: Canvas,
        filePath: String,
        targetCenterX: Float,
        targetCenterY: Float,
        targetSizePx: Float,
        rotationDeg: Float = 0f,
        timeMs: Long = 0L,
        alpha: Int = 255
    ) {
        val file = File(filePath)
        if (!file.exists() || file.length() < 10) return

        val isVideo = filePath.endsWith(".mp4", ignoreCase = true) ||
                      filePath.endsWith(".mov", ignoreCase = true) ||
                      filePath.endsWith(".mkv", ignoreCase = true) ||
                      filePath.endsWith(".webm", ignoreCase = true)

        // 1. Video Overlays (PiP / Video in Video) via MediaMetadataRetriever with frame cache
        if (isVideo) {
            var retriever = videoRetrieverCache[filePath]
            if (retriever == null && !videoRetrieverCache.containsKey(filePath)) {
                try {
                    retriever = MediaMetadataRetriever().apply { setDataSource(filePath) }
                    videoRetrieverCache[filePath] = retriever
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durMs = durStr?.toLongOrNull()?.coerceAtLeast(500L) ?: 5000L
                    videoDurationCache[filePath] = durMs
                } catch (_: Exception) {
                    videoRetrieverCache[filePath] = null
                }
            }

            if (retriever != null) {
                val durMs = videoDurationCache[filePath] ?: 5000L
                val loopMs = timeMs % durMs
                val frameKey = (loopMs / 100L) * 100L
                val frameMap = videoFrameCache.getOrPut(filePath) { ConcurrentHashMap() }
                var bmp = frameMap[frameKey]
                if (bmp == null || bmp.isRecycled) {
                    try {
                        bmp = retriever.getFrameAtTime(loopMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        if (bmp != null) {
                            if (frameMap.size > 25) {
                                frameMap.clear()
                            }
                            frameMap[frameKey] = bmp
                        }
                    } catch (_: Exception) {}
                }

                if (bmp != null && !bmp.isRecycled) {
                    val bw = bmp.width.toFloat()
                    val bh = bmp.height.toFloat()
                    val maxDim = maxOf(bw, bh).coerceAtLeast(1f)
                    val scale = targetSizePx / maxDim

                    canvas.save()
                    canvas.translate(targetCenterX, targetCenterY)
                    if (rotationDeg != 0f) {
                        canvas.rotate(rotationDeg)
                    }
                    canvas.scale(scale, scale)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        this.alpha = alpha
                    }
                    canvas.drawBitmap(bmp, -bw / 2f, -bh / 2f, paint)
                    canvas.restore()
                    return
                }
            }
        }

        val isGif = filePath.endsWith(".gif", ignoreCase = true)

        // 2. GIF Decoding via Movie
        if (isGif) {
            var movie = movieCache[filePath]
            if (movie == null && !movieCache.containsKey(filePath)) {
                try {
                    movie = Movie.decodeFile(filePath)
                    movieCache[filePath] = movie
                } catch (_: Exception) {
                    movieCache[filePath] = null
                }
            }

            if (movie != null && movie.width() > 0 && movie.height() > 0) {
                val dur = if (movie.duration() > 0) movie.duration() else 1000
                movie.setTime((timeMs % dur).toInt())

                val mw = movie.width().toFloat()
                val mh = movie.height().toFloat()
                val maxDim = maxOf(mw, mh).coerceAtLeast(1f)
                val scale = targetSizePx / maxDim

                canvas.save()
                canvas.translate(targetCenterX, targetCenterY)
                if (rotationDeg != 0f) {
                    canvas.rotate(rotationDeg)
                }
                canvas.scale(scale, scale)

                if (canvas.isHardwareAccelerated) {
                    val wInt = mw.toInt().coerceAtLeast(1)
                    val hInt = mh.toInt().coerceAtLeast(1)
                    var tb = tempBitmap
                    var tc = tempCanvas
                    if (tb == null || tb.width < wInt || tb.height < hInt || tb.isRecycled) {
                        tb = Bitmap.createBitmap(wInt.coerceAtLeast(256), hInt.coerceAtLeast(256), Bitmap.Config.ARGB_8888)
                        tc = Canvas(tb)
                        tempBitmap = tb
                        tempCanvas = tc
                    }
                    tb.eraseColor(Color.TRANSPARENT)
                    movie.draw(tc!!, 0f, 0f)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                        this.alpha = alpha
                    }
                    val srcRect = Rect(0, 0, wInt, hInt)
                    val dstRect = RectF(-mw / 2f, -mh / 2f, mw / 2f, mh / 2f)
                    canvas.drawBitmap(tb, srcRect, dstRect, paint)
                } else {
                    movie.draw(canvas, -mw / 2f, -mh / 2f)
                }

                canvas.restore()
                return
            }
        }

        // 2. Animated APNG or Animated WebP via ImageDecoder on API 28+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            var drawable = drawableCache[filePath]
            if (drawable == null && !drawableCache.containsKey(filePath)) {
                try {
                    val source = ImageDecoder.createSource(file)
                    drawable = ImageDecoder.decodeDrawable(source)
                    if (drawable is AnimatedImageDrawable) {
                        drawable.start()
                    }
                    drawableCache[filePath] = drawable
                } catch (_: Exception) {
                    drawableCache[filePath] = null
                }
            }

            if (drawable != null) {
                val dw = drawable.intrinsicWidth.toFloat().coerceAtLeast(1f)
                val dh = drawable.intrinsicHeight.toFloat().coerceAtLeast(1f)
                val maxDim = maxOf(dw, dh)
                val scale = targetSizePx / maxDim

                canvas.save()
                canvas.translate(targetCenterX, targetCenterY)
                if (rotationDeg != 0f) {
                    canvas.rotate(rotationDeg)
                }
                canvas.scale(scale, scale)

                val left = (-dw / 2f).toInt()
                val top = (-dh / 2f).toInt()
                val right = (dw / 2f).toInt()
                val bottom = (dh / 2f).toInt()
                drawable.setBounds(left, top, right, bottom)
                drawable.alpha = alpha
                drawable.draw(canvas)

                canvas.restore()
                return
            }
        }

        // 3. Static Bitmap (PNG, WebP, JPG)
        var bmp = bitmapCache[filePath]
        if (bmp == null && !bitmapCache.containsKey(filePath)) {
            try {
                bmp = BitmapFactory.decodeFile(filePath)
                bitmapCache[filePath] = bmp
            } catch (_: Exception) {
                bitmapCache[filePath] = null
            }
        }

        if (bmp != null && !bmp.isRecycled) {
            val bw = bmp.width.toFloat()
            val bh = bmp.height.toFloat()
            val maxDim = maxOf(bw, bh).coerceAtLeast(1f)
            val scale = targetSizePx / maxDim

            canvas.save()
            canvas.translate(targetCenterX, targetCenterY)
            if (rotationDeg != 0f) {
                canvas.rotate(rotationDeg)
            }
            canvas.scale(scale, scale)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.alpha = alpha
            }
            canvas.drawBitmap(bmp, -bw / 2f, -bh / 2f, paint)
            canvas.restore()
        }
    }

    fun clearCache() {
        movieCache.clear()
        bitmapCache.clear()
        drawableCache.clear()
        videoFrameCache.forEach { (_, map) ->
            map.forEach { (_, b) -> try { b.recycle() } catch (_: Exception) {} }
            map.clear()
        }
        videoFrameCache.clear()
        videoRetrieverCache.forEach { (_, r) ->
            try { r?.release() } catch (_: Exception) {}
        }
        videoRetrieverCache.clear()
        videoDurationCache.clear()
        try {
            tempBitmap?.recycle()
        } catch (_: Exception) {}
        tempBitmap = null
        tempCanvas = null
    }
}

package com.manchorati.www

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.*

object VideoMotionRenderer {

    data class VideoConfig(
        val content: String,
        val authorName: String,
        val authorUsername: String,
        val authorBadge: String = "",
        val tag: String = "",
        val bgMotionId: String = "STARS", // STARS, RAIN, AURORA, GOLD, OCEAN, CUSTOM_IMAGE
        val audioPreset: String = "piano", // piano, rain, oud, nature, mute, custom
        val animStyle: String = "typewriter", // typewriter, fade, zoom
        val aspectRatio: String = "9:16", // 9:16 or 1:1
        val durationSec: Int = 10,
        val fontId: String = "tajawal",
        val textColor: String = "#FFFFFF",
        var customImageBitmap: Bitmap? = null
    )

    // Pre-allocated particles for deterministic performance (no GC allocations during render loop)
    private const val NUM_STARS = 65
    private val starX = FloatArray(NUM_STARS)
    private val starY = FloatArray(NUM_STARS)
    private val starRadius = FloatArray(NUM_STARS)
    private val starSpeed = FloatArray(NUM_STARS)
    private val starPhase = FloatArray(NUM_STARS)

    private const val NUM_RAIN = 80
    private val rainX = FloatArray(NUM_RAIN)
    private val rainY = FloatArray(NUM_RAIN)
    private val rainLength = FloatArray(NUM_RAIN)
    private val rainSpeed = FloatArray(NUM_RAIN)

    private const val NUM_GOLD = 45
    private val goldX = FloatArray(NUM_GOLD)
    private val goldY = FloatArray(NUM_GOLD)
    private val goldRadius = FloatArray(NUM_GOLD)
    private val goldSpeed = FloatArray(NUM_GOLD)
    private val goldPhase = FloatArray(NUM_GOLD)

    init {
        val rand = java.util.Random(1337)
        for (i in 0 until NUM_STARS) {
            starX[i] = rand.nextFloat()
            starY[i] = rand.nextFloat()
            starRadius[i] = 1.8f + rand.nextFloat() * 3.2f
            starSpeed[i] = 0.5f + rand.nextFloat() * 1.5f
            starPhase[i] = rand.nextFloat() * (2 * Math.PI.toFloat())
        }
        for (i in 0 until NUM_RAIN) {
            rainX[i] = rand.nextFloat()
            rainY[i] = rand.nextFloat()
            rainLength[i] = 25f + rand.nextFloat() * 35f
            rainSpeed[i] = 0.7f + rand.nextFloat() * 0.6f
        }
        for (i in 0 until NUM_GOLD) {
            goldX[i] = rand.nextFloat()
            goldY[i] = rand.nextFloat()
            goldRadius[i] = 4f + rand.nextFloat() * 12f
            goldSpeed[i] = 0.3f + rand.nextFloat() * 0.7f
            goldPhase[i] = rand.nextFloat() * (2 * Math.PI.toFloat())
        }
    }

    fun renderFrame(
        context: Context,
        canvas: Canvas,
        width: Int,
        height: Int,
        timeMs: Long,
        config: VideoConfig
    ) {
        val w = width.toFloat()
        val h = height.toFloat()
        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        val loopTimeMs = timeMs % totalMs
        val progress = loopTimeMs.toFloat() / totalMs.toFloat()

        // 1. Draw Motion Background
        drawMotionBackground(canvas, w, h, loopTimeMs, progress, config)

        // 2. Determine Card Bounds
        val isStory = config.aspectRatio == "9:16"
        val cardMargin = if (isStory) w * 0.07f else w * 0.05f
        val cardLeft = cardMargin
        val cardRight = w - cardMargin
        val cardTop = if (isStory) h * 0.16f else h * 0.08f
        val cardBottom = if (isStory) h * 0.85f else h * 0.90f
        val cardRadius = 36f

        // Draw Frosted Card Background
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B090D16")
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)

        // 3. Card Header: Quotation mark, Author, Username, Badge, Tag
        val quotePulse = 0.35f + 0.15f * sin(progress * 2 * Math.PI.toFloat())
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (quotePulse * 255).toInt().coerceIn(40, 200)
            textSize = 100f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“", w / 2f, cardTop + 105f, quotePaint)

        // Header Author details
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (isStory) 34f else 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val usernamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }

        val headerY = cardTop + 170f
        val headerX = cardRight - 45f
        val authorBadgeText = if (config.authorBadge.isNotEmpty() && config.authorBadge != "beginner") {
            "${config.authorName}  ${config.authorBadge}"
        } else {
            config.authorName
        }
        canvas.drawText(authorBadgeText, headerX, headerY, authorPaint)
        canvas.drawText("@${config.authorUsername}", headerX, headerY + 36f, usernamePaint)

        // Category Tag Chip (Top Left inside card)
        if (config.tag.isNotEmpty()) {
            val cleanTag = config.tag.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
            val tagText = "# ${cleanTag.ifEmpty { config.tag }}"
            val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textWidth = tagPaint.measureText(tagText)
            val chipLeft = cardLeft + 45f
            val chipTop = headerY - 26f
            val chipRight = chipLeft + textWidth + 30f
            val chipBottom = headerY + 16f

            val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#260284C7")
                style = Paint.Style.FILL
            }
            val chipBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4D38BDF8")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val chipRect = RectF(chipLeft, chipTop, chipRight, chipBottom)
            canvas.drawRoundRect(chipRect, 16f, 16f, chipBgPaint)
            canvas.drawRoundRect(chipRect, 16f, 16f, chipBorder)
            canvas.drawText(tagText, chipLeft + 15f, chipBottom - 11f, tagPaint)
        }

        // Header Divider
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#26FFFFFF")
            strokeWidth = 1.5f
        }
        canvas.drawLine(cardLeft + 40f, headerY + 65f, cardRight - 40f, headerY + 65f, dividerPaint)

        // 4. Central Content Typography with Animation
        val textWidth = (cardRight - cardLeft - 80f).toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try {
                if (config.textColor.isNotEmpty()) Color.parseColor(config.textColor) else Color.WHITE
            } catch (_: Exception) {
                Color.WHITE
            }
            textSize = when {
                config.content.length > 250 -> if (isStory) 34f else 30f
                config.content.length > 120 -> if (isStory) 40f else 36f
                else -> if (isStory) 48f else 44f
            }
            typeface = ThemeHelper.getFontTypeface(context, config.fontId)
        }

        // Apply AnimStyle
        var textToDraw = config.content
        var textAlpha = 255
        var scale = 1.0f

        when (config.animStyle.lowercase()) {
            "typewriter" -> {
                val typeRatio = (progress / 0.65f).coerceIn(0f, 1f)
                val charCount = (config.content.length * typeRatio).toInt()
                val isBlinking = ((loopTimeMs / 380) % 2 == 0L) && typeRatio < 1.0f
                textToDraw = config.content.take(charCount) + (if (isBlinking) " |" else "")
            }
            "fade" -> {
                val fadeProgress = (progress / 0.18f).coerceIn(0f, 1f)
                textAlpha = (fadeProgress * 255).toInt()
            }
            "zoom" -> {
                scale = 0.96f + 0.08f * progress
            }
        }
        textPaint.alpha = textAlpha

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(textToDraw, 0, textToDraw.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(14f, 1.25f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                textToDraw,
                textPaint,
                textWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.25f,
                14f,
                true
            )
        }

        val textAvailableHeight = (cardBottom - 100f) - (headerY + 90f)
        val textStartY = headerY + 90f + ((textAvailableHeight - staticLayout.height) / 2f).coerceAtLeast(10f)

        canvas.save()
        if (scale != 1.0f) {
            canvas.scale(scale, scale, w / 2f, textStartY + staticLayout.height / 2f)
        }
        canvas.translate(cardLeft + 40f, textStartY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 5. Sound Visualizer Bars (Bottom-Left inside card)
        drawAudioVisualizerBars(canvas, cardLeft + 45f, cardBottom - 45f, loopTimeMs, config.audioPreset)

        // 6. Watermark Branding
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        val brandY = if (isStory) h * 0.93f else h * 0.95f
        canvas.drawText("من منشوراتي", w / 2f, brandY, brandPaint)
        canvas.drawText("تطبيق منشوراتي للاقتباسات والخواطر", w / 2f, brandY + 32f, subBrandPaint)
    }

    private fun drawMotionBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        timeMs: Long,
        progress: Float,
        config: VideoConfig
    ) {
        val bgPaint = Paint()

        // Check if Custom Image is selected and bitmap available
        if (config.bgMotionId == "CUSTOM_IMAGE" && config.customImageBitmap != null) {
            val bmp = config.customImageBitmap!!
            val kenBurnsScale = 1.0f + 0.08f * progress
            val kenBurnsShift = (progress - 0.5f) * (w * 0.05f)

            canvas.save()
            canvas.translate(w / 2f + kenBurnsShift, h / 2f)
            canvas.scale(kenBurnsScale, kenBurnsScale)
            val srcRect = Rect(0, 0, bmp.width, bmp.height)
            // Center crop fit
            val dstRatio = w / h
            val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
            val drawW: Float
            val drawH: Float
            if (bmpRatio > dstRatio) {
                drawH = h
                drawW = h * bmpRatio
            } else {
                drawW = w
                drawH = w / bmpRatio
            }
            val dstRect = RectF(-drawW / 2f, -drawH / 2f, drawW / 2f, drawH / 2f)
            canvas.drawBitmap(bmp, srcRect, dstRect, bgPaint)
            canvas.restore()

            // Dark vignette overlay so text is 100% readable
            val overlay = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(Color.parseColor("#80000000"), Color.parseColor("#99000000"), Color.parseColor("#B3000000")),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = overlay
            canvas.drawRect(0f, 0f, w, h, bgPaint)
            bgPaint.shader = null
            return
        }

        when (config.bgMotionId.uppercase()) {
            "RAIN" -> {
                // Dark rainy sky gradient
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#090D16"), Color.parseColor("#131C2E")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Slanted rain drops
                val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#6694A3B8")
                    strokeWidth = 2.2f
                    strokeCap = Paint.Cap.ROUND
                }
                val slantX = -8f
                for (i in 0 until NUM_RAIN) {
                    val curY = ((rainY[i] * h) + (timeMs * rainSpeed[i] * 1.5f)) % h
                    val curX = (rainX[i] * w)
                    canvas.drawLine(curX, curY, curX + slantX, curY + rainLength[i], rainPaint)
                }

                // Soft ripple rings at bottom
                val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2638BDF8")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                for (r in 0..4) {
                    val rPhase = (timeMs + r * 600) % 3000 / 3000f
                    val rx = w * (0.2f + r * 0.15f)
                    val ry = h * 0.94f
                    val rRadius = 15f + rPhase * 75f
                    ripplePaint.alpha = ((1f - rPhase) * 60).toInt()
                    canvas.drawOval(RectF(rx - rRadius, ry - rRadius * 0.35f, rx + rRadius, ry + rRadius * 0.35f), ripplePaint)
                }
            }

            "AURORA" -> {
                // Northern Sky deep gradient
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#021816"), Color.parseColor("#0A1226")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Aurora waving harmonic glow curves
                val t = timeMs * 0.0015f
                drawAuroraRibbon(canvas, w, h, t, Color.parseColor("#2610B981"), 0.30f, 70f, 0.004f)
                drawAuroraRibbon(canvas, w, h, t * 1.2f + 1.0f, Color.parseColor("#2614B8A6"), 0.38f, 90f, 0.003f)
                drawAuroraRibbon(canvas, w, h, t * 0.8f + 2.5f, Color.parseColor("#228B5CF6"), 0.46f, 80f, 0.005f)
            }

            "GOLD" -> {
                // Luxury amber dark gradient
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#15110E"), Color.parseColor("#281F18")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Rising golden embers & bokeh
                val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until NUM_GOLD) {
                    val curY = h - (((goldY[i] * h) + (timeMs * goldSpeed[i] * 0.4f)) % h)
                    val sway = sin(timeMs * 0.002f + goldPhase[i]) * 20f
                    val curX = (goldX[i] * w + sway).coerceIn(0f, w)
                    val alphaPulse = 0.4f + 0.6f * sin(timeMs * 0.003f + goldPhase[i])
                    goldPaint.color = Color.parseColor("#F59E0B")
                    goldPaint.alpha = (alphaPulse * 160).toInt().coerceIn(20, 220)
                    canvas.drawCircle(curX, curY, goldRadius[i], goldPaint)
                }
            }

            "OCEAN" -> {
                // Deep ocean gradient
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#020D1A"), Color.parseColor("#0A2540")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Calming wave horizons
                val t = timeMs * 0.0012f
                drawOceanWave(canvas, w, h, t, Color.parseColor("#1F0284C7"), 0.75f, 40f)
                drawOceanWave(canvas, w, h, t * 1.3f + 1.5f, Color.parseColor("#2B06B6D4"), 0.82f, 30f)
            }

            else -> { // "STARS" is default
                // Midnight cosmic gradient
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#040711"), Color.parseColor("#0D162B")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                // Cosmic nebula highlight drifting slowly
                val nebPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F38BDF8")
                    maskFilter = BlurMaskFilter(180f, BlurMaskFilter.Blur.NORMAL)
                }
                val nebX = w * 0.7f + sin(timeMs * 0.0006f) * 60f
                val nebY = h * 0.28f + cos(timeMs * 0.0005f) * 40f
                canvas.drawCircle(nebX, nebY, 260f, nebPaint)

                // Twinkling stars
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                }
                for (i in 0 until NUM_STARS) {
                    val curX = starX[i] * w
                    val curY = ((starY[i] * h) - (timeMs * 0.015f)) % h
                    val yPos = if (curY < 0) curY + h else curY
                    val twinkle = 0.35f + 0.65f * sin(timeMs * 0.003f * starSpeed[i] + starPhase[i])
                    starPaint.alpha = (twinkle * 240).toInt().coerceIn(30, 255)
                    canvas.drawCircle(curX, yPos, starRadius[i], starPaint)
                }

                // Periodic shooting star
                val shootPeriod = 4500L
                val shootProgress = (timeMs % shootPeriod) / 1200f
                if (shootProgress in 0f..1f) {
                    val shootX1 = w * 0.2f + shootProgress * (w * 0.6f)
                    val shootY1 = h * 0.10f + shootProgress * (h * 0.15f)
                    val shootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        alpha = ((1f - shootProgress) * 200).toInt()
                        strokeWidth = 2.5f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(shootX1, shootY1, shootX1 - 50f, shootY1 - 25f, shootPaint)
                }
            }
        }
    }

    private fun drawAuroraRibbon(canvas: Canvas, w: Float, h: Float, t: Float, color: Int, baseYRatio: Float, amp: Float, freq: Float) {
        val path = Path()
        val baseY = h * baseYRatio
        path.moveTo(0f, baseY)
        var x = 0f
        while (x <= w) {
            val y = baseY + sin(x * freq + t) * amp + cos(x * freq * 1.5f + t * 0.8f) * (amp * 0.4f)
            path.lineTo(x, y)
            x += 20f
        }
        path.lineTo(w, baseY + 180f)
        path.lineTo(0f, baseY + 180f)
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawOceanWave(canvas: Canvas, w: Float, h: Float, t: Float, color: Int, baseYRatio: Float, amp: Float) {
        val path = Path()
        val baseY = h * baseYRatio
        path.moveTo(0f, h)
        path.lineTo(0f, baseY)
        var x = 0f
        while (x <= w) {
            val y = baseY + sin(x * 0.005f + t) * amp
            path.lineTo(x, y)
            x += 20f
        }
        path.lineTo(w, h)
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    private fun drawAudioVisualizerBars(canvas: Canvas, startX: Float, bottomY: Float, timeMs: Long, audioPreset: String) {
        if (audioPreset == "mute") return

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4.5f
        }
        val barCount = 5
        val barSpacing = 10f
        val maxH = 26f
        val minH = 6f

        for (i in 0 until barCount) {
            val freq = 0.007f + i * 0.003f
            val phase = i * 1.3f
            val hRatio = 0.5f + 0.5f * sin(timeMs * freq + phase)
            val currentH = minH + hRatio * (maxH - minH)
            val bx = startX + i * barSpacing
            canvas.drawLine(bx, bottomY, bx, bottomY - currentH, barPaint)
        }
    }
}

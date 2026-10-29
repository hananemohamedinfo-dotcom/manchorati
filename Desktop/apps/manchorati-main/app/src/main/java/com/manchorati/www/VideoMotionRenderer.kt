package com.manchorati.www

import android.content.Context
import android.graphics.*
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
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
        val animStyle: String = "typewriter", // typewriter, fade, zoom, glow, slide_up, word_by_word, blur_in
        val aspectRatio: String = "9:16", // 9:16 or 1:1
        val durationSec: Int = 15,
        val fontId: String = "tajawal",
        val textColor: String = "#FFFFFF",
        var customImageBitmap: Bitmap? = null,
        val showTag: Boolean = false,
        val showAuthor: Boolean = true,
        val downloadedAudioPath: String? = null,
        val animSpeed: Float = 1.0f, // 0.5f, 1.0f, 1.5f, 2.0f, 3.0f
        val imageFramingMode: String = "cover", // "cover", "fit_blur", "zoom"
        val imageFocusY: Float = 0.5f, // 0.0f = top, 0.5f = center, 1.0f = bottom
        val imagePanX: Float = 0f, // Relative horizontal pan (-1f to 1f)
        val imagePanY: Float = 0f, // Relative vertical pan (-1f to 1f)
        val imageZoomScale: Float = 1.0f // Touch pinch zoom multiplier (0.5f to 5.0f)
    )

    // Pre-allocated deterministic particles
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
            starRadius[i] = 1.8f + rand.nextFloat() * 3.0f
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
            goldRadius[i] = 4f + rand.nextFloat() * 10f
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
        if (w <= 0f || h <= 0f) return

        val isStory = config.aspectRatio == "9:16"
        val refW = 720f
        val refH = if (isStory) 1280f else 720f
        // Dynamic scale factor for perfect responsive rendering on ANY screen size
        val S = minOf(w / refW, h / refH)

        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        val loopTimeMs = timeMs % totalMs
        val progress = loopTimeMs.toFloat() / totalMs.toFloat()

        // 1. Draw Motion Background
        drawMotionBackground(canvas, w, h, loopTimeMs, progress, config, S)

        // 2. Card Dimensions
        val cardMarginX = if (isStory) 48f * S else 36f * S
        val cardLeft = cardMarginX
        val cardRight = w - cardMarginX
        val cardTop = if (isStory) h * 0.14f else h * 0.08f
        val cardBottom = if (isStory) h * 0.86f else h * 0.92f
        val cardRadius = 28f * S

        // Frosted Card Shadow / Glow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40000000")
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(20f * S, BlurMaskFilter.Blur.NORMAL)
        }
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, shadowPaint)

        // Frosted Glass Card Fill
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#380B1220")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)

        // Card Border with subtle gradient
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardLeft, cardTop, cardRight, cardBottom,
                intArrayOf(Color.parseColor("#55FFFFFF"), Color.parseColor("#15FFFFFF"), Color.parseColor("#4038BDF8")),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.STROKE
            strokeWidth = 2f * S
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

        // 3. Header: Author info on the RIGHT, Tag chip on the LEFT (conditionally)
        val padX = 32f * S
        val headerY = cardTop + 44f * S
        val hasAuthor = config.showAuthor && config.authorName.isNotEmpty()
        val hasTag = config.showTag && config.tag.isNotEmpty()

        if (hasAuthor) {
            // 1. Sleek Profile Avatar Circle on the Right
            val avatarRadius = 18f * S
            val avatarCenterX = cardRight - padX - avatarRadius
            val avatarCenterY = headerY + 10f * S

            val avatarBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    avatarCenterX - avatarRadius, avatarCenterY - avatarRadius,
                    avatarCenterX + avatarRadius, avatarCenterY + avatarRadius,
                    intArrayOf(Color.parseColor("#38BDF8"), Color.parseColor("#0284C7")),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBg)

            val avatarBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4DFFFFFF")
                style = Paint.Style.STROKE
                strokeWidth = 1.2f * S
            }
            canvas.drawCircle(avatarCenterX, avatarCenterY, avatarRadius, avatarBorder)

            val initialPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 15f * S
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val initial = config.authorName.trim().take(1).ifEmpty { "م" }
            canvas.drawText(initial, avatarCenterX, avatarCenterY + 5.5f * S, initialPaint)

            // 2. Author Name & Username (Left of Avatar, NO 'verified' word)
            val textRight = avatarCenterX - avatarRadius - 10f * S
            val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (if (isStory) 23f else 21f) * S
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val usernamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#94A3B8")
                textSize = 15f * S
                textAlign = Paint.Align.RIGHT
            }

            val maxAuthorW = (textRight - (if (hasTag) (cardLeft + 140f * S) else (cardLeft + padX))).coerceAtLeast(100f)
            val ellipAuthor = TextUtils.ellipsize(config.authorName, authorPaint, maxAuthorW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(ellipAuthor, textRight, headerY + 4f * S, authorPaint)

            if (config.authorUsername.isNotEmpty()) {
                val cleanUser = "@${config.authorUsername.removePrefix("@")}"
                val ellipUser = TextUtils.ellipsize(cleanUser, usernamePaint, maxAuthorW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(ellipUser, textRight, headerY + 28f * S, usernamePaint)
            }
        }

        // Tag Chip (Top Left inside card)
        if (hasTag) {
            val cleanTag = config.tag.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
            val tagText = "# ${cleanTag.ifEmpty { config.tag }}"
            val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                textSize = 17f * S
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tagW = tagPaint.measureText(tagText)
            val chipLeft = cardLeft + padX
            val chipTop = headerY - 18f * S
            val chipRight = chipLeft + tagW + 22f * S
            val chipBottom = headerY + 14f * S

            val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2B0284C7")
                style = Paint.Style.FILL
            }
            val chipBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5538BDF8")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f * S
            }
            val chipRect = RectF(chipLeft, chipTop, chipRight, chipBottom)
            val chipRadius = 12f * S
            canvas.drawRoundRect(chipRect, chipRadius, chipRadius, chipBgPaint)
            canvas.drawRoundRect(chipRect, chipRadius, chipRadius, chipBorder)
            canvas.drawText(tagText, chipLeft + 11f * S, chipBottom - 8f * S, tagPaint)
        }

        // Header Divider (only if header elements exist)
        val dividerY: Float
        if (hasAuthor || hasTag) {
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#20FFFFFF")
                strokeWidth = 1.2f * S
            }
            dividerY = headerY + 44f * S
            canvas.drawLine(cardLeft + 24f * S, dividerY, cardRight - 24f * S, dividerY, dividerPaint)
        } else {
            dividerY = cardTop + 16f * S
        }

        // Subtle Watermark Quotation Mark behind quote
        val quotePulse = 0.20f + 0.08f * sin(progress * 2 * Math.PI.toFloat())
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (quotePulse * 255).toInt().coerceIn(25, 75)
            textSize = 76f * S
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val quoteMarkY = if (hasAuthor || hasTag) dividerY + 54f * S else cardTop + 68f * S
        canvas.drawText("“", w / 2f, quoteMarkY, quotePaint)

        // 5. Central Typography with Selected Animation
        val textPaddingX = 36f * S
        val textWidth = (cardRight - cardLeft - (textPaddingX * 2)).toInt().coerceAtLeast(100)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try {
                if (config.textColor.isNotEmpty()) Color.parseColor(config.textColor) else Color.WHITE
            } catch (_: Exception) {
                Color.WHITE
            }
            textSize = when {
                config.content.length > 250 -> (if (isStory) 24f else 22f) * S
                config.content.length > 120 -> (if (isStory) 30f else 26f) * S
                else -> (if (isStory) 38f else 32f) * S
            }
            typeface = ThemeHelper.getFontTypeface(context, config.fontId)
        }

        // Apply AnimStyle
        var textToDraw = config.content
        var textAlpha = 255
        var scale = 1.0f
        var translateY = 0f

        val speed = config.animSpeed.coerceIn(0.2f, 5.0f)
        val animProgress = (progress * speed).coerceIn(0f, 1f)

        when (config.animStyle.lowercase()) {
            "typewriter" -> {
                val typeRatio = (animProgress / 0.70f).coerceIn(0f, 1f)
                val charCount = (config.content.length * typeRatio).toInt()
                val isBlinking = ((loopTimeMs / 380) % 2 == 0L) && typeRatio < 1.0f
                textToDraw = config.content.take(charCount) + (if (isBlinking) " |" else "")
            }
            "word_by_word" -> {
                val words = config.content.split(Regex("\\s+")).filter { it.isNotEmpty() }
                if (words.isNotEmpty()) {
                    val wordRatio = (animProgress / 0.75f).coerceIn(0f, 1f)
                    val visibleCount = maxOf(1, (words.size * wordRatio).toInt().coerceAtMost(words.size))
                    textToDraw = words.take(visibleCount).joinToString(" ")
                }
            }
            "fade" -> {
                val fadeProgress = (animProgress / 0.25f).coerceIn(0f, 1f)
                textAlpha = (fadeProgress * 255).toInt()
            }
            "zoom" -> {
                val popProgress = (animProgress / 0.35f).coerceIn(0f, 1f)
                val ease = sin(popProgress * Math.PI.toFloat() / 2f)
                scale = 0.80f + (0.20f * ease) + (0.02f * sin(progress * 2 * Math.PI.toFloat()))
                val fade = (popProgress / 0.6f).coerceIn(0f, 1f)
                textAlpha = (fade * 255).toInt()
            }
            "glow" -> {
                val pulse = 0.5f + 0.5f * sin(progress * 4 * Math.PI.toFloat() * speed)
                val glowRadius = (12f + pulse * 14f) * S
                textPaint.setShadowLayer(glowRadius, 0f, 0f, Color.parseColor("#38BDF8"))
            }
            "slide_up" -> {
                val slideProgress = (animProgress / 0.30f).coerceIn(0f, 1f)
                val ease = 1f - (1f - slideProgress).pow(2)
                translateY = -(1f - ease) * 45f * S
                textAlpha = (ease * 255).toInt()
            }
            "scroll_up", "rise", "scroll" -> {
                // Smooth cinematic scroll: text starts at bottom, scrolls up smoothly through the center, and floats out off the top until it completely disappears
                val startDownShift = (cardBottom - cardTop) * 0.45f + 40f * S
                val endUpShift = (cardBottom - cardTop) * 0.50f + 50f * S
                val totalDistance = startDownShift + endUpShift
                translateY = -startDownShift + (animProgress * totalDistance)
                textAlpha = when {
                    animProgress < 0.12f -> ((animProgress / 0.12f) * 255).toInt().coerceIn(0, 255)
                    animProgress > 0.86f -> (((1f - animProgress) / 0.14f) * 255).toInt().coerceIn(0, 255)
                    else -> 255
                }
            }
            "blur_in" -> {
                val blurTime = (animProgress / 0.35f).coerceIn(0f, 1f)
                if (blurTime < 1.0f) {
                    val blurRemaining = 1f - blurTime
                    textAlpha = (blurTime * 255).toInt()
                    scale = 0.94f + (0.06f * blurTime)
                    val glow = blurRemaining * 14f * S
                    if (glow > 0.5f) {
                        textPaint.setShadowLayer(glow, 0f, 0f, textPaint.color)
                    }
                }
            }
        }
        textPaint.alpha = textAlpha

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(textToDraw, 0, textToDraw.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(10f * S, 1.25f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                textToDraw,
                textPaint,
                textWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.25f,
                10f * S,
                true
            )
        }

        val textTopLimit = if (hasAuthor || hasTag) (dividerY + 28f * S) else (cardTop + 40f * S)
        val textBottomLimit = cardBottom - 48f * S
        val textAvailableHeight = (textBottomLimit - textTopLimit).coerceAtLeast(50f)
        val textStartY = textTopLimit + ((textAvailableHeight - staticLayout.height) / 2f).coerceAtLeast(0f)

        canvas.save()
        // Clip text strictly inside card content bounds so scroll-up flows seamlessly
        canvas.clipRect(cardLeft + 12f * S, textTopLimit - 10f * S, cardRight - 12f * S, textBottomLimit + 10f * S)
        if (scale != 1.0f) {
            canvas.scale(scale, scale, w / 2f, textStartY + staticLayout.height / 2f)
        }
        canvas.translate(cardLeft + textPaddingX, textStartY - translateY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. Sound Visualizer Bars (Bottom Left inside card)
        drawAudioVisualizerBars(canvas, cardLeft + 36f * S, cardBottom - 32f * S, loopTimeMs, config.audioPreset, S)

        // 7. Watermark Branding (Below card)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * S
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 14f * S
            textAlign = Paint.Align.CENTER
        }
        val brandY = cardBottom + (if (isStory) 44f * S else 30f * S)
        if (brandY + 24f * S < h) {
            canvas.drawText("من منشوراتي", w / 2f, brandY, brandPaint)
            canvas.drawText("تطبيق منشوراتي للاقتباسات والخواطر", w / 2f, brandY + 22f * S, subBrandPaint)
        }
    }

    private fun drawMotionBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        timeMs: Long,
        progress: Float,
        config: VideoConfig,
        S: Float
    ) {
        val bgPaint = Paint()

        if (config.customImageBitmap != null) {
            val bmp = config.customImageBitmap!!
            val kenBurnsScale = 1.0f + 0.03f * sin(progress * Math.PI.toFloat())
            val touchZoom = config.imageZoomScale.coerceIn(0.5f, 5.0f)
            val focusY = config.imageFocusY.coerceIn(0f, 1f)

            canvas.save()
            when (config.imageFramingMode) {
                "fit_blur" -> {
                    // Stretched blurred background
                    val bgDst = RectF(0f, 0f, w, h)
                    canvas.drawBitmap(bmp, null, bgDst, bgPaint)
                    canvas.drawColor(Color.parseColor("#99000000"))

                    // Aspect-fit centered with touch zoom & pan
                    val fitScale = minOf(w / bmp.width.toFloat(), h / bmp.height.toFloat()) * touchZoom
                    val fw = bmp.width.toFloat() * fitScale
                    val fh = bmp.height.toFloat() * fitScale
                    val panX = config.imagePanX * w
                    val panY = config.imagePanY * h
                    val left = (w - fw) / 2f + panX
                    val top = (h - fh) / 2f + panY
                    val fitDst = RectF(left, top, left + fw, top + fh)
                    canvas.drawBitmap(bmp, null, fitDst, bgPaint)
                }
                else -> {
                    // Free touch gesture mode: Zoom + Pan horizontally & vertically anywhere!
                    val baseScale = maxOf(w / bmp.width.toFloat(), h / bmp.height.toFloat()) * touchZoom * kenBurnsScale
                    val drawW = bmp.width.toFloat() * baseScale
                    val drawH = bmp.height.toFloat() * baseScale
                    val panX = config.imagePanX * w
                    val panY = (config.imagePanY + (0.5f - focusY)) * h
                    val left = (w - drawW) / 2f + panX
                    val top = (h - drawH) / 2f + panY
                    val dstRect = RectF(left, top, left + drawW, top + drawH)
                    canvas.drawBitmap(bmp, null, dstRect, bgPaint)
                }
            }
            canvas.restore()

            // Dark vignette overlay
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
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#090D16"), Color.parseColor("#131C2E")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#6694A3B8")
                    strokeWidth = 2.0f * S
                    strokeCap = Paint.Cap.ROUND
                }
                val slantX = -6f * S
                for (i in 0 until NUM_RAIN) {
                    val curY = ((rainY[i] * h) + (timeMs * rainSpeed[i] * 1.5f * S)) % h
                    val curX = (rainX[i] * w)
                    canvas.drawLine(curX, curY, curX + slantX, curY + rainLength[i] * S, rainPaint)
                }

                val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2638BDF8")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f * S
                }
                for (r in 0..4) {
                    val rPhase = (timeMs + r * 600) % 3000 / 3000f
                    val rx = w * (0.2f + r * 0.15f)
                    val ry = h * 0.94f
                    val rRadius = (15f + rPhase * 70f) * S
                    ripplePaint.alpha = ((1f - rPhase) * 60).toInt()
                    canvas.drawOval(RectF(rx - rRadius, ry - rRadius * 0.35f, rx + rRadius, ry + rRadius * 0.35f), ripplePaint)
                }
            }

            "AURORA" -> {
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#021816"), Color.parseColor("#0A1226")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                val t = timeMs * 0.0015f
                drawAuroraRibbon(canvas, w, h, t, Color.parseColor("#2610B981"), 0.30f, 60f * S, 0.004f)
                drawAuroraRibbon(canvas, w, h, t * 1.2f + 1.0f, Color.parseColor("#2614B8A6"), 0.38f, 75f * S, 0.003f)
                drawAuroraRibbon(canvas, w, h, t * 0.8f + 2.5f, Color.parseColor("#228B5CF6"), 0.46f, 65f * S, 0.005f)
            }

            "GOLD" -> {
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#15110E"), Color.parseColor("#281F18")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                for (i in 0 until NUM_GOLD) {
                    val curY = h - (((goldY[i] * h) + (timeMs * goldSpeed[i] * 0.4f * S)) % h)
                    val sway = sin(timeMs * 0.002f + goldPhase[i]) * 15f * S
                    val curX = (goldX[i] * w + sway).coerceIn(0f, w)
                    val alphaPulse = 0.4f + 0.6f * sin(timeMs * 0.003f + goldPhase[i])
                    goldPaint.color = Color.parseColor("#F59E0B")
                    goldPaint.alpha = (alphaPulse * 160).toInt().coerceIn(20, 220)
                    canvas.drawCircle(curX, curY, goldRadius[i] * S, goldPaint)
                }
            }

            "OCEAN" -> {
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#020D1A"), Color.parseColor("#0A2540")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                val t = timeMs * 0.0012f
                drawOceanWave(canvas, w, h, t, Color.parseColor("#1F0284C7"), 0.75f, 35f * S)
                drawOceanWave(canvas, w, h, t * 1.3f + 1.5f, Color.parseColor("#2B06B6D4"), 0.82f, 25f * S)
            }

            else -> { // STARS
                val gradient = LinearGradient(
                    0f, 0f, 0f, h,
                    intArrayOf(Color.parseColor("#040711"), Color.parseColor("#0D162B")),
                    null, Shader.TileMode.CLAMP
                )
                bgPaint.shader = gradient
                canvas.drawRect(0f, 0f, w, h, bgPaint)
                bgPaint.shader = null

                val nebPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F38BDF8")
                    maskFilter = BlurMaskFilter(150f * S, BlurMaskFilter.Blur.NORMAL)
                }
                val nebX = w * 0.7f + sin(timeMs * 0.0006f) * 45f * S
                val nebY = h * 0.28f + cos(timeMs * 0.0005f) * 30f * S
                canvas.drawCircle(nebX, nebY, 200f * S, nebPaint)

                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                }
                for (i in 0 until NUM_STARS) {
                    val curX = starX[i] * w
                    val curY = ((starY[i] * h) - (timeMs * 0.012f * S)) % h
                    val yPos = if (curY < 0) curY + h else curY
                    val twinkle = 0.35f + 0.65f * sin(timeMs * 0.003f * starSpeed[i] + starPhase[i])
                    starPaint.alpha = (twinkle * 240).toInt().coerceIn(30, 255)
                    canvas.drawCircle(curX, yPos, starRadius[i] * S, starPaint)
                }

                // Shooting star
                val shootPeriod = 4500L
                val shootProgress = (timeMs % shootPeriod) / 1200f
                if (shootProgress in 0f..1f) {
                    val shootX1 = w * 0.2f + shootProgress * (w * 0.6f)
                    val shootY1 = h * 0.10f + shootProgress * (h * 0.15f)
                    val shootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        alpha = ((1f - shootProgress) * 200).toInt()
                        strokeWidth = 2.2f * S
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(shootX1, shootY1, shootX1 - 40f * S, shootY1 - 20f * S, shootPaint)
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
        path.lineTo(w, baseY + 140f)
        path.lineTo(0f, baseY + 140f)
        path.close()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            maskFilter = BlurMaskFilter(45f, BlurMaskFilter.Blur.NORMAL)
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

    private fun drawAudioVisualizerBars(canvas: Canvas, startX: Float, bottomY: Float, timeMs: Long, audioPreset: String, S: Float) {
        if (audioPreset == "mute") return

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 3.5f * S
        }
        val barCount = 5
        val barSpacing = 8f * S
        val maxH = 22f * S
        val minH = 5f * S

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

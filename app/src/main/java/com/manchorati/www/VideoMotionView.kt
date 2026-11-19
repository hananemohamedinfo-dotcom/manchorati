package com.manchorati.www

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.io.File

class VideoMotionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    private var isInternalAudioUpdate = false

    var config: VideoMotionRenderer.VideoConfig = VideoMotionRenderer.VideoConfig(
        content = "",
        authorName = "",
        authorUsername = ""
    )
        set(value) {
            val oldAudio = field.audioPreset
            val oldPath = field.downloadedAudioPath
            val oldDur = field.durationSec
            field = value

            if (!isInternalAudioUpdate && (oldAudio != value.audioPreset || oldPath != value.downloadedAudioPath)) {
                if (isAttached) {
                    playAudioPreset(value.audioPreset, value.downloadedAudioPath, streamAudioUrl)
                }
            }

            if (oldDur != value.durationSec) {
                pausedTimeOffsetMs = pausedTimeOffsetMs % (value.durationSec * 1000L).coerceAtLeast(1000L)
            }
            requestLayout()
            invalidate()
        }

    var customAudioUri: Uri? = null
        set(value) {
            field = value
            if (isAttached && value != null) {
                playAudioPreset("custom", null, null)
            }
        }

    var audioStartOffsetMs: Long = 0L

    var streamAudioUrl: String? = null

    var isMuted: Boolean = true
        set(value) {
            field = value
            val vol = if (value) 0f else 0.9f
            try {
                mediaPlayer?.setVolume(vol, vol)
            } catch (_: Exception) {}
        }

    var isPlaying: Boolean = true
        private set

    private var startTimeNs: Long = 0L
    private var pausedTimeOffsetMs: Long = 0L
    private var isAttached = false

    private var mediaPlayer: MediaPlayer? = null
    var onPlayStateChanged: ((Boolean) -> Unit)? = null
    var onProgressUpdate: ((elapsedMs: Long, totalMs: Long, ratio: Float) -> Unit)? = null

    // Touch gesture callbacks for direct manipulation of custom background & stickers
    var onImageFramingChanged: ((panX: Float, panY: Float, zoom: Float) -> Unit)? = null
    var onStickerPositionChanged: ((x: Float, y: Float, scale: Float) -> Unit)? = null
    var onStickerDeleted: (() -> Unit)? = null
    var onStickerSelected: ((stk: VideoMotionRenderer.StickerItem, index: Int) -> Unit)? = null
    var onStickerTimingRequested: ((stk: VideoMotionRenderer.StickerItem, index: Int) -> Unit)? = null
    var onStickersListChanged: ((stickers: List<VideoMotionRenderer.StickerItem>) -> Unit)? = null
    var activeStickerIndex: Int = -1
    var onCanvasTap: (() -> Unit)? = null
    var onLoopCompleted: (() -> Unit)? = null

    // Touch gesture state
    private var panX = 0f
    private var panY = 0f
    private var zoomScale = 1.0f

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    private var isDraggingSticker = false
    private var isStickerSelected = false
    private var isHoveringTrash = false
    private var lastStickerTapTime: Long = 0L

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (isDraggingSticker) {
                val list = config.stickers.toMutableList()
                if (activeStickerIndex in list.indices) {
                    val target = list[activeStickerIndex]
                    val newScale = (target.scale * detector.scaleFactor).coerceIn(0.4f, 3.5f)
                    target.scale = newScale
                    config = config.copy(stickers = list)
                    onStickersListChanged?.invoke(list)
                    invalidate()
                    return true
                } else if (!config.stickerPath.isNullOrEmpty()) {
                    val newScale = (config.stickerScale * detector.scaleFactor).coerceIn(0.4f, 3.5f)
                    config = config.copy(stickerScale = newScale)
                    onStickerPositionChanged?.invoke(config.stickerX, config.stickerY, newScale)
                    invalidate()
                    return true
                }
            }
            if (config.customImageBitmap != null) {
                zoomScale = (zoomScale * detector.scaleFactor).coerceIn(0.5f, 4.0f)
                config = config.copy(imageZoomScale = zoomScale)
                onImageFramingChanged?.invoke(panX, panY, zoomScale)
                invalidate()
                return true
            }
            return false
        }
    })

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        startTimeNs = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(this)
        if (mediaPlayer == null) {
            playAudioPreset(config.audioPreset, config.downloadedAudioPath, streamAudioUrl)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttached = false
        Choreographer.getInstance().removeFrameCallback(this)
        stopAudio()
    }

    private var lastLoopCount: Long = 0L

    private fun getCurrentElapsedMs(): Long {
        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        val rawMs = if (isPlaying) {
            val elapsedNs = System.nanoTime() - startTimeNs
            (elapsedNs / 1_000_000L) + pausedTimeOffsetMs
        } else {
            pausedTimeOffsetMs
        }
        val currentLoop = rawMs / totalMs
        if (isPlaying && currentLoop > lastLoopCount) {
            lastLoopCount = currentLoop
            try {
                val target = if (customAudioUri != null) audioStartOffsetMs.toInt() else 0
                mediaPlayer?.seekTo(target)
            } catch (_: Exception) {}
            onLoopCompleted?.invoke()
        }
        return rawMs % totalMs
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAttached) return
        if (isPlaying) {
            val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
            val curMs = getCurrentElapsedMs()
            val ratio = curMs.toFloat() / totalMs.toFloat()
            onProgressUpdate?.invoke(curMs, totalMs, ratio)
            invalidate()
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentW = MeasureSpec.getSize(widthMeasureSpec)
        val parentH = MeasureSpec.getSize(heightMeasureSpec)
        val isStory = config.aspectRatio == "9:16"
        val ratio = if (isStory) 9f / 16f else 1f

        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)

        var targetH = if (hMode != MeasureSpec.UNSPECIFIED && parentH > 0) parentH else (parentW / ratio).toInt()
        var targetW = (targetH * ratio).toInt()

        if (wMode != MeasureSpec.UNSPECIFIED && parentW > 0 && targetW > parentW) {
            targetW = parentW
            targetH = (targetW / ratio).toInt()
        }

        setMeasuredDimension(targetW, targetH)
    }

    fun getEffectiveStickers(): List<VideoMotionRenderer.StickerItem> {
        return if (config.stickers.isNotEmpty()) {
            config.stickers
        } else if (!config.stickerPath.isNullOrEmpty()) {
            listOf(VideoMotionRenderer.StickerItem(path = config.stickerPath!!, x = config.stickerX, y = config.stickerY, scale = config.stickerScale, rotation = config.stickerRotation))
        } else {
            emptyList()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val currentElapsedMs = getCurrentElapsedMs()
        VideoMotionRenderer.renderFrame(context, canvas, width, height, currentElapsedMs, config)

        val allStickers = getEffectiveStickers()
        if (allStickers.isNotEmpty()) {
            val S = resources.displayMetrics.density

            // 1. WhatsApp-Style Top Trash Bin (appears at the top while dragging any sticker)
            if (isDraggingSticker) {
                val trashCenterX = width / 2f
                val trashCenterY = 46f * S
                val trashRadius = if (isHoveringTrash) 34f * S else 26f * S

                val trashBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isHoveringTrash) Color.parseColor("#EEF43F5E") else Color.parseColor("#D91E293B")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(trashCenterX, trashCenterY, trashRadius, trashBgPaint)

                val trashBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isHoveringTrash) Color.WHITE else Color.parseColor("#66F43F5E")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * S
                }
                canvas.drawCircle(trashCenterX, trashCenterY, trashRadius, trashBorderPaint)

                val trashIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = (if (isHoveringTrash) 22f else 17f) * S
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🗑️", trashCenterX, trashCenterY + (if (isHoveringTrash) 8f else 6f) * S, trashIconPaint)

                if (isHoveringTrash) {
                    val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#F43F5E")
                        textSize = 12f * S
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText("إفلات للحذف", trashCenterX, trashCenterY + trashRadius + 16f * S, hintPaint)
                }
            }

            // 2. Selection Box and Quick Delete (✕) Badge on the active sticker
            val targetSticker = if (activeStickerIndex in allStickers.indices) allStickers[activeStickerIndex] else null
            if (targetSticker != null && (isDraggingSticker || isStickerSelected)) {
                val stickerPixelX = targetSticker.x * width
                val stickerPixelY = targetSticker.y * height
                val baseSize = minOf(width, height) * 0.28f * targetSticker.scale
                val half = baseSize / 2f

                val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isHoveringTrash) Color.parseColor("#CCF43F5E") else Color.parseColor("#B338BDF8")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.8f * S
                    pathEffect = DashPathEffect(floatArrayOf(10f * S, 6f * S), 0f)
                }
                val rect = RectF(stickerPixelX - half - 6f * S, stickerPixelY - half - 6f * S, stickerPixelX + half + 6f * S, stickerPixelY + half + 6f * S)
                canvas.drawRoundRect(rect, 10f * S, 10f * S, boxPaint)

                // Top-Right Quick Delete (✕) circular button
                val badgeX = stickerPixelX + half + 6f * S
                val badgeY = stickerPixelY - half - 6f * S
                val badgeRadius = 13f * S

                val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#F43F5E")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBgPaint)

                val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * S
                }
                canvas.drawCircle(badgeX, badgeY, badgeRadius, badgeBorderPaint)

                val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 11f * S
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("✕", badgeX, badgeY + 4f * S, badgeTextPaint)

                // Top-Left Quick Timing (⏱️) circular button
                val timeBadgeX = stickerPixelX - half - 6f * S
                val timeBadgeY = stickerPixelY - half - 6f * S

                val timeBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0284C7")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(timeBadgeX, timeBadgeY, badgeRadius, timeBadgeBgPaint)
                canvas.drawCircle(timeBadgeX, timeBadgeY, badgeRadius, badgeBorderPaint)

                val timeIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 10f * S
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("⏱️", timeBadgeX, timeBadgeY + 3.5f * S, timeIconPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 1. Sticker Hit-testing & Drag / Pinch Gesture
        val allStickers = getEffectiveStickers().toMutableList()
        if (allStickers.isNotEmpty()) {
            val S = resources.displayMetrics.density

            // Active sticker reference
            val targetSticker = if (activeStickerIndex in allStickers.indices) allStickers[activeStickerIndex] else null
            val badgeX = if (targetSticker != null) (targetSticker.x * width) + (minOf(width, height) * 0.28f * targetSticker.scale / 2f) + 6f * S else -999f
            val badgeY = if (targetSticker != null) (targetSticker.y * height) - (minOf(width, height) * 0.28f * targetSticker.scale / 2f) - 6f * S else -999f
            val timeBadgeX = if (targetSticker != null) (targetSticker.x * width) - (minOf(width, height) * 0.28f * targetSticker.scale / 2f) - 6f * S else -999f
            val timeBadgeY = if (targetSticker != null) (targetSticker.y * height) - (minOf(width, height) * 0.28f * targetSticker.scale / 2f) - 6f * S else -999f
            val badgeHitRadius = 26f * S

            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 1. Check if user tapped directly on the Quick Delete (✕) button of active sticker
                    val distToBadge = Math.hypot((event.x - badgeX).toDouble(), (event.y - badgeY).toDouble()).toFloat()
                    if ((isStickerSelected || isDraggingSticker) && distToBadge <= badgeHitRadius && activeStickerIndex in allStickers.indices) {
                        allStickers.removeAt(activeStickerIndex)
                        activeStickerIndex = allStickers.lastIndex
                        config = config.copy(stickers = allStickers, stickerPath = allStickers.lastOrNull()?.path)
                        isStickerSelected = false
                        isDraggingSticker = false
                        isHoveringTrash = false
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onStickersListChanged?.invoke(allStickers)
                        onStickerDeleted?.invoke()
                        invalidate()
                        return true
                    }

                    // 2. Check if user tapped directly on the Quick Timing (⏱️) button of active sticker
                    val distToTimeBadge = Math.hypot((event.x - timeBadgeX).toDouble(), (event.y - timeBadgeY).toDouble()).toFloat()
                    if ((isStickerSelected || isDraggingSticker) && distToTimeBadge <= badgeHitRadius && activeStickerIndex in allStickers.indices) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onStickerTimingRequested?.invoke(allStickers[activeStickerIndex], activeStickerIndex)
                        return true
                    }

                    // 3. Hit-test stickers in reverse order (top-most z-index first)
                    var tappedIndex = -1
                    for (i in allStickers.indices.reversed()) {
                        val stk = allStickers[i]
                        val sx = stk.x * width
                        val sy = stk.y * height
                        val sSize = minOf(width, height) * 0.28f * stk.scale
                        val sRadius = (sSize / 2f) + 36f
                        val dist = Math.hypot((event.x - sx).toDouble(), (event.y - sy).toDouble()).toFloat()
                        if (dist <= sRadius) {
                            tappedIndex = i
                            break
                        }
                    }

                    if (tappedIndex != -1) {
                        val now = System.currentTimeMillis()
                        // Double tap detection (< 380ms on the same active sticker) -> Opens timing!
                        if (tappedIndex == activeStickerIndex && (now - lastStickerTapTime) < 380L) {
                            lastStickerTapTime = 0L
                            isDraggingSticker = false
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onStickerTimingRequested?.invoke(allStickers[tappedIndex], tappedIndex)
                            return true
                        }
                        lastStickerTapTime = now

                        activeStickerIndex = tappedIndex
                        isDraggingSticker = true
                        isStickerSelected = true
                        isHoveringTrash = false
                        downX = event.x
                        downY = event.y
                        lastX = event.x
                        lastY = event.y
                        parent?.requestDisallowInterceptTouchEvent(true)
                        onStickerSelected?.invoke(allStickers[tappedIndex], tappedIndex)
                        invalidate()
                        return true
                    } else {
                        // Tapped outside stickers: dismiss selection
                        if (isStickerSelected) {
                            isStickerSelected = false
                            activeStickerIndex = -1
                            invalidate()
                        }
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingSticker && !scaleDetector.isInProgress && activeStickerIndex in allStickers.indices) {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        lastX = event.x
                        lastY = event.y

                        val stk = allStickers[activeStickerIndex]
                        val baseSize = minOf(width, height) * 0.28f * stk.scale
                        val halfW = ((baseSize / 2f) + 8f) / width.toFloat()
                        val halfH = ((baseSize / 2f) + 8f) / height.toFloat()
                        val newX = (stk.x + (dx / width.toFloat())).coerceIn(halfW, (1f - halfW).coerceAtLeast(halfW))
                        val newY = (stk.y + (dy / height.toFloat())).coerceIn(halfH, (1f - halfH).coerceAtLeast(halfH))

                        stk.x = newX
                        stk.y = newY

                        // WhatsApp-Style Trash Detection
                        val distToTopTrash = Math.hypot((event.x - width / 2f).toDouble(), (event.y - 46f * S).toDouble()).toFloat()
                        val wasHovering = isHoveringTrash
                        isHoveringTrash = (distToTopTrash <= 75f * S) || (event.y < 85f * S && (event.x < 120f * S || event.x > width - 120f * S))

                        if (!wasHovering && isHoveringTrash) {
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }

                        config = config.copy(stickers = allStickers, stickerPath = allStickers.lastOrNull()?.path)
                        onStickersListChanged?.invoke(allStickers)
                        invalidate()
                        return true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingSticker) {
                        if (isHoveringTrash && activeStickerIndex in allStickers.indices) {
                            // User dropped sticker in trash (WhatsApp style)
                            allStickers.removeAt(activeStickerIndex)
                            activeStickerIndex = allStickers.lastIndex
                            config = config.copy(stickers = allStickers, stickerPath = allStickers.lastOrNull()?.path)
                            isStickerSelected = false
                            isDraggingSticker = false
                            isHoveringTrash = false
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onStickersListChanged?.invoke(allStickers)
                            onStickerDeleted?.invoke()
                            parent?.requestDisallowInterceptTouchEvent(false)
                            invalidate()
                            return true
                        }

                        isDraggingSticker = false
                        isHoveringTrash = false
                        parent?.requestDisallowInterceptTouchEvent(false)
                        invalidate()
                        return true
                    }
                }
            }
        }

        // 2. Custom Image Framing Gestures or Tap to Play/Pause
        if (config.customImageBitmap != null) {
            scaleDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    lastX = event.x
                    lastY = event.y
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!scaleDetector.isInProgress && event.pointerCount == 1 && isDragging) {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        lastX = event.x
                        lastY = event.y
                        val viewW = width.toFloat().coerceAtLeast(1f)
                        val viewH = height.toFloat().coerceAtLeast(1f)
                        panX = (panX + (dx / viewW)).coerceIn(-1.5f, 1.5f)
                        panY = (panY + (dy / viewH)).coerceIn(-1.5f, 1.5f)
                        config = config.copy(imagePanX = panX, imagePanY = panY)
                        onImageFramingChanged?.invoke(panX, panY, zoomScale)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    val totalDistance = Math.hypot((event.x - downX).toDouble(), (event.y - downY).toDouble())
                    if (totalDistance < 25 && event.eventTime - event.downTime < 250) {
                        togglePlay()
                        onCanvasTap?.invoke()
                    }
                    return true
                }
            }
        } else {
            if (event.action == MotionEvent.ACTION_UP) {
                togglePlay()
                onCanvasTap?.invoke()
                return true
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    fun togglePlay() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (isPlaying) return
        isPlaying = true
        startTimeNs = System.nanoTime()
        if (mediaPlayer == null) {
            playAudioPreset(config.audioPreset, config.downloadedAudioPath, streamAudioUrl)
        } else {
            try {
                mediaPlayer?.start()
            } catch (_: Exception) {}
        }
        onPlayStateChanged?.invoke(true)
        invalidate()
    }

    fun pause() {
        if (!isPlaying) return
        isPlaying = false
        val elapsedNs = System.nanoTime() - startTimeNs
        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        pausedTimeOffsetMs = (pausedTimeOffsetMs + (elapsedNs / 1_000_000L)) % totalMs
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
        onPlayStateChanged?.invoke(false)
        invalidate()
    }

    fun restart() {
        startTimeNs = System.nanoTime()
        pausedTimeOffsetMs = 0L
        lastLoopCount = 0L
        try {
            val target = if (customAudioUri != null) audioStartOffsetMs.toInt() else 0
            mediaPlayer?.seekTo(target)
            if (isPlaying && !isMuted) mediaPlayer?.start()
        } catch (_: Exception) {}
        invalidate()
    }

    fun seekTo(ratio: Float) {
        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        val targetMs = (ratio.coerceIn(0f, 1f) * totalMs).toLong()
        pausedTimeOffsetMs = targetMs
        startTimeNs = System.nanoTime()
        lastLoopCount = 0L
        try {
            mediaPlayer?.let { mp ->
                val mpDur = mp.duration
                if (mpDur > 0) {
                    val baseOffset = if (customAudioUri != null) audioStartOffsetMs else 0L
                    val audioTarget = ((baseOffset + targetMs) % mpDur).toInt()
                    mp.seekTo(audioTarget)
                }
            }
        } catch (_: Exception) {}
        onProgressUpdate?.invoke(targetMs, totalMs, ratio)
        invalidate()
    }

    fun playAudioImmediately(presetId: String, localPath: String? = null, streamUrl: String? = null) {
        playAudioPreset(presetId, localPath, streamUrl)
    }

    @Synchronized
    fun playAudioPreset(presetId: String, localPath: String? = null, streamUrl: String? = null) {
        stopAudio()

        isMuted = false
        isPlaying = true
        this.streamAudioUrl = streamUrl

        isInternalAudioUpdate = true
        config = config.copy(
            audioPreset = presetId,
            downloadedAudioPath = localPath
        )
        isInternalAudioUpdate = false

        if (presetId.lowercase() in listOf("none", "mute", "off", "silent")) {
            onPlayStateChanged?.invoke(true)
            invalidate()
            return
        }

        try {
            val vol = 0.9f

            // Case 1: Downloaded Audio local file
            if (localPath != null && File(localPath).exists()) {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(localPath)
                    isLooping = true
                    setVolume(vol, vol)
                    setOnErrorListener { p, _, _ ->
                        try { p.reset(); p.release() } catch (_: Exception) {}
                        true
                    }
                    prepare()
                    start()
                }
                mediaPlayer = mp
                onPlayStateChanged?.invoke(true)
                invalidate()
                return
            }

            // Case 2: Custom Picked Audio URI
            if (customAudioUri != null) {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, customAudioUri!!)
                    isLooping = true
                    setVolume(vol, vol)
                    setOnErrorListener { p, _, _ ->
                        try { p.reset(); p.release() } catch (_: Exception) {}
                        true
                    }
                    prepare()
                    if (audioStartOffsetMs > 0L) {
                        try { seekTo(audioStartOffsetMs.toInt()) } catch (_: Exception) {}
                    }
                    start()
                }
                mediaPlayer = mp
                onPlayStateChanged?.invoke(true)
                invalidate()
                return
            }

            // Case 3: Online Streaming MP3
            if (!streamUrl.isNullOrEmpty()) {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(streamUrl))
                    isLooping = true
                    setVolume(vol, vol)
                    setOnErrorListener { p, _, _ ->
                        try { p.reset(); p.release() } catch (_: Exception) {}
                        true
                    }
                    setOnPreparedListener { player ->
                        try {
                            if (!isMuted) player.start()
                        } catch (_: Exception) {}
                    }
                    prepareAsync()
                }
                mediaPlayer = mp
                onPlayStateChanged?.invoke(true)
                invalidate()
                return
            }

            // Case 4: Rock-solid Built-in Raw Resources
            val rawRes = when (presetId.lowercase()) {
                "rain" -> R.raw.ambient_rain
                "oud" -> R.raw.ambient_oud
                "nature" -> R.raw.ambient_nature
                else -> R.raw.ambient_piano
            }

            val mp = MediaPlayer.create(context, rawRes)?.apply {
                isLooping = true
                setVolume(vol, vol)
                setOnErrorListener { p, _, _ ->
                    try { p.reset(); p.release() } catch (_: Exception) {}
                    true
                }
                start()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onPlayStateChanged?.invoke(true)
        invalidate()
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}

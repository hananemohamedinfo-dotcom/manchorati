package com.manchorati.www

import android.content.Context
import android.graphics.Canvas
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Choreographer
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

    var streamAudioUrl: String? = null

    var isMuted: Boolean = false
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

    // Touch gesture callbacks for direct manipulation of custom background
    var onImageFramingChanged: ((panX: Float, panY: Float, zoom: Float) -> Unit)? = null
    var onCanvasTap: (() -> Unit)? = null

    // Touch gesture state
    private var panX = 0f
    private var panY = 0f
    private var zoomScale = 1.0f

    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
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

    private fun getCurrentElapsedMs(): Long {
        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        val rawMs = if (isPlaying) {
            val elapsedNs = System.nanoTime() - startTimeNs
            (elapsedNs / 1_000_000L) + pausedTimeOffsetMs
        } else {
            pausedTimeOffsetMs
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val currentElapsedMs = getCurrentElapsedMs()
        VideoMotionRenderer.renderFrame(context, canvas, width, height, currentElapsedMs, config)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
        try {
            mediaPlayer?.seekTo(0)
            if (isPlaying && !isMuted) mediaPlayer?.start()
        } catch (_: Exception) {}
        invalidate()
    }

    fun seekTo(ratio: Float) {
        val totalMs = (config.durationSec * 1000L).coerceAtLeast(1000L)
        val targetMs = (ratio.coerceIn(0f, 1f) * totalMs).toLong()
        pausedTimeOffsetMs = targetMs
        startTimeNs = System.nanoTime()
        try {
            mediaPlayer?.let { mp ->
                val mpDur = mp.duration
                if (mpDur > 0) {
                    val audioTarget = (targetMs % mpDur).toInt()
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

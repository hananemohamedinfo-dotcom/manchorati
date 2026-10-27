package com.manchorati.www

import android.content.Context
import android.graphics.Canvas
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View

class VideoMotionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Choreographer.FrameCallback {

    var config: VideoMotionRenderer.VideoConfig = VideoMotionRenderer.VideoConfig(
        content = "",
        authorName = "",
        authorUsername = ""
    )
        set(value) {
            val oldAudio = field.audioPreset
            field = value
            if (oldAudio != value.audioPreset) {
                updateAudioPlayback()
            }
            requestLayout()
            invalidate()
        }

    var customAudioUri: Uri? = null
        set(value) {
            field = value
            updateAudioPlayback()
        }

    var isPlaying: Boolean = true
        private set

    private var startTimeNs: Long = 0L
    private var pausedTimeOffsetMs: Long = 0L
    private var isAttached = false

    private var mediaPlayer: MediaPlayer? = null
    var onPlayStateChanged: ((Boolean) -> Unit)? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        startTimeNs = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(this)
        startAudio()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttached = false
        Choreographer.getInstance().removeFrameCallback(this)
        stopAudio()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isAttached) return
        if (isPlaying) {
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

        val currentElapsedMs = if (isPlaying) {
            val elapsedNs = System.nanoTime() - startTimeNs
            (elapsedNs / 1_000_000L) + pausedTimeOffsetMs
        } else {
            pausedTimeOffsetMs
        }

        VideoMotionRenderer.renderFrame(context, canvas, width, height, currentElapsedMs, config)
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
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}
        onPlayStateChanged?.invoke(true)
        invalidate()
    }

    fun pause() {
        if (!isPlaying) return
        isPlaying = false
        val elapsedNs = System.nanoTime() - startTimeNs
        pausedTimeOffsetMs += (elapsedNs / 1_000_000L)
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
            if (isPlaying) mediaPlayer?.start()
        } catch (_: Exception) {}
        invalidate()
    }

    private fun updateAudioPlayback() {
        stopAudio()
        if (isAttached && isPlaying) {
            startAudio()
        }
    }

    private fun startAudio() {
        if (config.audioPreset == "mute") {
            stopAudio()
            return
        }

        try {
            if (customAudioUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, customAudioUri!!)
                    isLooping = true
                    prepare()
                    if (isPlaying) start()
                }
                return
            }

            val rawRes = when (config.audioPreset.lowercase()) {
                "rain" -> R.raw.ambient_rain
                "oud" -> R.raw.ambient_oud
                "nature" -> R.raw.ambient_nature
                else -> R.raw.ambient_piano
            }

            mediaPlayer = MediaPlayer.create(context, rawRes)?.apply {
                isLooping = true
                if (isPlaying) start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}

package com.manchorati.www

import android.content.Context
import android.graphics.Canvas
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import java.io.File

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
            val oldPath = field.downloadedAudioPath
            val oldDur = field.durationSec
            field = value
            if (oldAudio != value.audioPreset || oldPath != value.downloadedAudioPath) {
                updateAudioPlayback()
            }
            if (oldDur != value.durationSec) {
                // Keep progress within bounds
                pausedTimeOffsetMs = pausedTimeOffsetMs % (value.durationSec * 1000L).coerceAtLeast(1000L)
            }
            requestLayout()
            invalidate()
        }

    var customAudioUri: Uri? = null
        set(value) {
            field = value
            updateAudioPlayback()
        }

    var isMuted: Boolean = false
        set(value) {
            field = value
            val vol = if (value) 0f else 1f
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
            if (isPlaying) mediaPlayer?.start()
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

    private fun updateAudioPlayback() {
        stopAudio()
        if (isAttached && isPlaying) {
            startAudio()
        }
    }

    private fun startAudio() {
        stopAudio()
        if (config.audioPreset.lowercase() in listOf("mute", "none", "off", "silent")) return

        try {
            // 1. Downloaded Audio file path
            val downloadedPath = config.downloadedAudioPath
            if (downloadedPath != null && File(downloadedPath).exists()) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(downloadedPath)
                    isLooping = true
                    val vol = if (isMuted) 0f else 1f
                    setVolume(vol, vol)
                    prepare()
                    if (isPlaying) start()
                }
                return
            }

            // 2. Custom User Selected Audio URI
            if (customAudioUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, customAudioUri!!)
                    isLooping = true
                    val vol = if (isMuted) 0f else 1f
                    setVolume(vol, vol)
                    prepare()
                    if (isPlaying) start()
                }
                return
            }

            // 3. Built-in Raw Resources
            val rawRes = when (config.audioPreset.lowercase()) {
                "rain" -> R.raw.ambient_rain
                "oud" -> R.raw.ambient_oud
                "nature" -> R.raw.ambient_nature
                else -> R.raw.ambient_piano
            }

            val afd = context.resources.openRawResourceFd(rawRes) ?: return
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                val vol = if (isMuted) 0f else 1f
                setVolume(vol, vol)
                prepare()
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

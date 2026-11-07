package com.manchorati.www

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AudioTimelineTrimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var totalDurationMs: Long = 30000L
        set(value) {
            field = max(1000L, value)
            generateWaveform()
            invalidate()
        }

    var clipDurationMs: Long = 15000L
        set(value) {
            field = max(1000L, min(value, totalDurationMs))
            currentStartMs = currentStartMs.coerceIn(0L, max(0L, totalDurationMs - field))
            invalidate()
        }

    var currentStartMs: Long = 0L
        set(value) {
            val maxStart = max(0L, totalDurationMs - clipDurationMs)
            field = value.coerceIn(0L, maxStart)
            invalidate()
        }

    var currentPlayheadMs: Long = 0L
        set(value) {
            field = value.coerceIn(0L, totalDurationMs)
            invalidate()
        }

    var trackTitle: String = "audio_track.mp3"
        set(value) {
            field = value
            invalidate()
        }

    var onTrimChanged: ((startMs: Long, endMs: Long) -> Unit)? = null
    var onSeek: ((positionMs: Long) -> Unit)? = null

    private val density = resources.displayMetrics.density

    // Dimensions
    private val rulerHeight = 26f * density
    private val trackHeight = 56f * density
    private val cornerRadius = 10f * density
    private val handleWidth = 16f * density

    // Paints
    private val rulerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B1120")
        style = Paint.Style.FILL
    }

    private val rulerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        strokeWidth = 1f * density
    }

    private val rulerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 10f * density
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        strokeWidth = 1.5f * density
    }

    private val minorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        strokeWidth = 1f * density
    }

    // Audio Track Paints
    private val trackBaseBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        style = Paint.Style.FILL
    }

    private val trackActiveBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2563EB") // CapCut Vibrant Royal Blue
        style = Paint.Style.FILL
    }

    private val trackDimOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B30F172A") // 70% dim on unselected parts
        style = Paint.Style.FILL
    }

    private val selectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val handleArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        strokeWidth = 1.8f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val titleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11.5f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val musicIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 13f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD") // Light blue wave bars
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8") // Cyan Needle Line
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(4f * density, 0f, 0f, Color.parseColor("#80000000"))
    }

    private val playheadCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    private val playheadInnerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Interaction State
    private var isDraggingTrim = false
    private var isDraggingPlayhead = false
    private var lastTouchX = 0f

    // Simulated waveform heights (0.1f .. 0.9f)
    private val waveformPoints = FloatArray(120)

    init {
        generateWaveform()
    }

    private fun generateWaveform() {
        val seed = abs(trackTitle.hashCode() + totalDurationMs.toInt())
        for (i in waveformPoints.indices) {
            val v = (Math.sin(i * 0.45 + seed) * 0.35 + Math.cos(i * 0.22) * 0.25 + 0.5).toFloat()
            waveformPoints[i] = v.coerceIn(0.18f, 0.95f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredH = ((rulerHeight + trackHeight + 16f * density)).toInt()
        val h = resolveSize(desiredH, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val paddingHorizontal = 16f * density
        val availableTrackW = w - (paddingHorizontal * 2f)
        if (availableTrackW <= 0f) return

        // 1. Draw Time Ruler (y=0 to y=rulerHeight)
        canvas.drawRect(0f, 0f, w, rulerHeight, rulerBgPaint)
        canvas.drawLine(0f, rulerHeight, w, rulerHeight, rulerBorderPaint)

        val totalSec = max(1, (totalDurationMs / 1000L).toInt())
        val tickStepSec = when {
            totalSec <= 30 -> 5
            totalSec <= 90 -> 10
            totalSec <= 180 -> 15
            else -> 30
        }

        for (s in 0..totalSec) {
            val ratio = s.toFloat() / totalSec.toFloat()
            val x = paddingHorizontal + (ratio * availableTrackW)

            if (s % tickStepSec == 0 || s == 0 || s == totalSec) {
                // Major tick with label
                canvas.drawLine(x, rulerHeight - 9f * density, x, rulerHeight, majorTickPaint)
                val timeStr = String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
                canvas.drawText(timeStr, x, rulerHeight - 12f * density, rulerTextPaint)
            } else {
                // Minor tick
                canvas.drawLine(x, rulerHeight - 5f * density, x, rulerHeight, minorTickPaint)
            }
        }

        // 2. Audio Track Pill
        val trackTop = rulerHeight + 8f * density
        val trackBottom = trackTop + trackHeight
        val trackRect = RectF(paddingHorizontal, trackTop, paddingHorizontal + availableTrackW, trackBottom)

        // Background of track
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackBaseBgPaint)

        // Waveform Bars inside track
        val barCount = waveformPoints.size
        val barSpacing = availableTrackW / barCount.toFloat()
        val trackCenterY = (trackTop + trackBottom) / 2f
        val maxBarH = trackHeight * 0.72f

        for (i in 0 until barCount) {
            val bx = paddingHorizontal + (i * barSpacing) + (barSpacing / 2f)
            val barH = waveformPoints[i] * maxBarH
            val by1 = trackCenterY - (barH / 2f)
            val by2 = trackCenterY + (barH / 2f)
            canvas.drawLine(bx, by1, bx, by2, wavePaint)
        }

        // 3. Selection Trim Window
        val startRatio = currentStartMs.toFloat() / totalDurationMs.toFloat()
        val endMs = min(totalDurationMs, currentStartMs + clipDurationMs)
        val endRatio = endMs.toFloat() / totalDurationMs.toFloat()

        val winLeft = paddingHorizontal + (startRatio * availableTrackW)
        val winRight = paddingHorizontal + (endRatio * availableTrackW)
        val winRect = RectF(winLeft, trackTop, winRight, trackBottom)

        // Highlight active blue segment
        canvas.save()
        canvas.clipRect(winRect)
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackActiveBgPaint)
        // Redraw active waveform inside blue area with white tint
        wavePaint.color = Color.parseColor("#BFDBFE")
        for (i in 0 until barCount) {
            val bx = paddingHorizontal + (i * barSpacing) + (barSpacing / 2f)
            if (bx >= winLeft - 4f && bx <= winRight + 4f) {
                val barH = waveformPoints[i] * maxBarH
                val by1 = trackCenterY - (barH / 2f)
                val by2 = trackCenterY + (barH / 2f)
                canvas.drawLine(bx, by1, bx, by2, wavePaint)
            }
        }
        canvas.restore()
        wavePaint.color = Color.parseColor("#64748B")

        // Draw Inactive Dim Regions (left and right of selection)
        if (winLeft > paddingHorizontal) {
            canvas.save()
            canvas.clipRect(paddingHorizontal, trackTop, winLeft, trackBottom)
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackDimOverlayPaint)
            canvas.restore()
        }
        if (winRight < paddingHorizontal + availableTrackW) {
            canvas.save()
            canvas.clipRect(winRight, trackTop, paddingHorizontal + availableTrackW, trackBottom)
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackDimOverlayPaint)
            canvas.restore()
        }

        // White Selection Box Border
        canvas.drawRoundRect(winRect, cornerRadius, cornerRadius, selectionBorderPaint)

        // Left Handle (<)
        val leftHandleRect = RectF(winLeft - handleWidth / 2f, trackTop, winLeft + handleWidth / 2f, trackBottom)
        canvas.drawRoundRect(leftHandleRect, 6f * density, 6f * density, handlePaint)
        val lhcx = winLeft
        val lhcy = (trackTop + trackBottom) / 2f
        canvas.drawLine(lhcx + 2f * density, lhcy - 5f * density, lhcx - 2f * density, lhcy, handleArrowPaint)
        canvas.drawLine(lhcx - 2f * density, lhcy, lhcx + 2f * density, lhcy + 5f * density, handleArrowPaint)

        // Right Handle (>)
        val rightHandleRect = RectF(winRight - handleWidth / 2f, trackTop, winRight + handleWidth / 2f, trackBottom)
        canvas.drawRoundRect(rightHandleRect, 6f * density, 6f * density, handlePaint)
        val rhcx = winRight
        val rhcy = (trackTop + trackBottom) / 2f
        canvas.drawLine(rhcx - 2f * density, rhcy - 5f * density, rhcx + 2f * density, rhcy, handleArrowPaint)
        canvas.drawLine(rhcx + 2f * density, rhcy, rhcx - 2f * density, rhcy + 5f * density, handleArrowPaint)

        // 4. Music Title & Icon inside the track (if space permits)
        canvas.save()
        canvas.clipRect(winLeft + handleWidth, trackTop, winRight - handleWidth, trackBottom)
        val iconX = winLeft + handleWidth + 4f * density
        val textY = trackTop + 20f * density
        canvas.drawText("♫", iconX, textY, musicIconPaint)

        val cleanTitle = if (trackTitle.length > 28) trackTitle.take(26) + "…" else trackTitle
        canvas.drawText(cleanTitle, iconX + 16f * density, textY - 1f * density, titleTextPaint)
        canvas.restore()

        // 5. Cyan Playhead Needle Line & Cursor
        val playheadRatio = currentPlayheadMs.toFloat() / totalDurationMs.toFloat()
        val playheadX = paddingHorizontal + (playheadRatio * availableTrackW)

        // Vertical cyan line traversing ruler down to bottom of track
        canvas.drawLine(playheadX, 0f, playheadX, trackBottom + 4f * density, playheadPaint)
        // Playhead Top Cap
        canvas.drawCircle(playheadX, 4f * density, 4.5f * density, playheadCapPaint)
        canvas.drawCircle(playheadX, 4f * density, 2f * density, playheadInnerDotPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val paddingHorizontal = 16f * density
        val availableTrackW = w - (paddingHorizontal * 2f)
        if (availableTrackW <= 0f) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                val touchY = event.y

                // If touched near ruler, seek playhead
                if (touchY <= rulerHeight + 4f * density) {
                    isDraggingPlayhead = true
                    seekToTouch(event.x, availableTrackW, paddingHorizontal)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }

                // If touched track, drag the trim selection window
                isDraggingTrim = true
                parent?.requestDisallowInterceptTouchEvent(true)
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                lastTouchX = event.x

                if (isDraggingPlayhead) {
                    seekToTouch(event.x, availableTrackW, paddingHorizontal)
                    return true
                }

                if (isDraggingTrim) {
                    val deltaRatio = dx / availableTrackW
                    val deltaMs = (deltaRatio * totalDurationMs).toLong()
                    val maxStart = max(0L, totalDurationMs - clipDurationMs)
                    val newStart = (currentStartMs + deltaMs).coerceIn(0L, maxStart)

                    if (newStart != currentStartMs) {
                        currentStartMs = newStart
                        // Keep playhead within selection
                        currentPlayheadMs = currentStartMs
                        invalidate()
                        onTrimChanged?.invoke(currentStartMs, min(totalDurationMs, currentStartMs + clipDurationMs))
                    }
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingTrim || isDraggingPlayhead) {
                    isDraggingTrim = false
                    isDraggingPlayhead = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun seekToTouch(touchX: Float, availableTrackW: Float, paddingHorizontal: Float) {
        val clampedX = touchX.coerceIn(paddingHorizontal, paddingHorizontal + availableTrackW)
        val ratio = (clampedX - paddingHorizontal) / availableTrackW
        val targetMs = (ratio * totalDurationMs).toLong().coerceIn(0L, totalDurationMs)
        currentPlayheadMs = targetMs
        invalidate()
        onSeek?.invoke(targetMs)
    }

    fun seekRelative(deltaMs: Long) {
        val targetMs = (currentPlayheadMs + deltaMs).coerceIn(0L, totalDurationMs)
        currentPlayheadMs = targetMs
        invalidate()
        onSeek?.invoke(targetMs)
    }
}

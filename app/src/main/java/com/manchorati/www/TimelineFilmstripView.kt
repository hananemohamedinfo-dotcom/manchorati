package com.manchorati.www

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Locale

class TimelineFilmstripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var durationSec: Int = 15
        set(value) {
            field = value.coerceAtLeast(1)
            invalidate()
        }

    var currentRatio: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var thumbnailBitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    var onSeek: ((Float) -> Unit)? = null
    var onPlusClick: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val pps = 48f * density // pixels per second
    private var isDragging = false
    private var lastTouchX = 0f

    // Paints
    private val tickTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF")
        textSize = 9.5f * density
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val tickDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FFFFFF")
        style = Paint.Style.FILL
    }

    private val trackBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1E24")
        style = Paint.Style.FILL
    }

    private val trackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#384252")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    private val playheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2.2f * density
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(4f * density, 0f, 0f, Color.parseColor("#80000000"))
    }

    private val plusBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2DD4BF") // CapCut turquoise / cyan
        style = Paint.Style.FILL
    }

    private val plusIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        strokeWidth = 2f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val frameDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#25FFFFFF")
        strokeWidth = 1f * density
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredH = (72 * density).toInt()
        val h = resolveSize(desiredH, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val totalTrackW = durationSec * pps
        val headOffset = cx - (currentRatio * totalTrackW)

        // 1. Time Ruler (Top: y=0 to y=18dp)
        val rulerY = 13f * density
        for (sec in 0..durationSec) {
            val tickX = headOffset + (sec * pps)
            if (tickX < -100f || tickX > w + 100f) continue

            if (sec % 5 == 0 || sec == 0 || sec == durationSec) {
                val timeStr = String.format(Locale.US, "00:%02d", sec)
                canvas.drawText(timeStr, tickX, rulerY, tickTextPaint)
            } else {
                canvas.drawCircle(tickX, rulerY - 3f * density, 1.8f * density, tickDotPaint)
            }
        }

        // 2. Filmstrip Track (y=18dp to y=h - 8dp)
        val trackTop = 18f * density
        val trackBottom = h - 6f * density
        val trackLeft = headOffset
        val trackRight = headOffset + totalTrackW
        val cornerR = 8f * density

        val filmRect = RectF(trackLeft, trackTop, trackRight, trackBottom)
        canvas.drawRoundRect(filmRect, cornerR, cornerR, trackBgPaint)

        // Draw Filmstrip Video Thumbnail Frames inside track
        val frameW = (trackBottom - trackTop) * 0.65f // portrait frame aspect
        var fx = trackLeft
        val srcRect = Rect()
        val dstRect = RectF()

        while (fx < trackRight) {
            val rightBound = minOf(fx + frameW, trackRight)
            if (rightBound > 0 && fx < w) {
                if (thumbnailBitmap != null && !thumbnailBitmap!!.isRecycled) {
                    srcRect.set(0, 0, thumbnailBitmap!!.width, thumbnailBitmap!!.height)
                    dstRect.set(fx, trackTop, rightBound, trackBottom)
                    canvas.drawBitmap(thumbnailBitmap!!, srcRect, dstRect, null)
                }
                // Divider between frame cells
                if (rightBound < trackRight) {
                    canvas.drawLine(rightBound, trackTop, rightBound, trackBottom, frameDividerPaint)
                }
            }
            fx += frameW
        }

        // Filmstrip border
        canvas.drawRoundRect(filmRect, cornerR, cornerR, trackBorderPaint)

        // 3. Plus Button on the right of the filmstrip
        val plusRadius = 14f * density
        val plusCx = trackRight + plusRadius + 10f * density
        val plusCy = (trackTop + trackBottom) / 2f
        if (plusCx + plusRadius > 0 && plusCx - plusRadius < w) {
            canvas.drawCircle(plusCx, plusCy, plusRadius, plusBgPaint)
            // Draw '+'
            val arm = 6f * density
            canvas.drawLine(plusCx - arm, plusCy, plusCx + arm, plusCy, plusIconPaint)
            canvas.drawLine(plusCx, plusCy - arm, plusCx, plusCy + arm, plusIconPaint)
        }

        // 4. Fixed White Playhead Line in Center
        canvas.drawLine(cx, 0f, cx, h, playheadPaint)
        // Small white circular cursor on top
        canvas.drawCircle(cx, 3f * density, 3.5f * density, playheadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val totalTrackW = durationSec * pps
        if (totalTrackW <= 0f) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                lastTouchX = event.x
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastTouchX
                    lastTouchX = event.x
                    // Dragging left moves forward in time; dragging right moves backward
                    val deltaRatio = -dx / totalTrackW
                    val newRatio = (currentRatio + deltaRatio).coerceIn(0f, 1f)
                    currentRatio = newRatio
                    onSeek?.invoke(newRatio)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    // Check if clicked the '+' button area
                    val cx = width / 2f
                    val headOffset = cx - (currentRatio * totalTrackW)
                    val trackRight = headOffset + totalTrackW
                    val plusCx = trackRight + (24f * density)
                    if (Math.abs(event.x - plusCx) < 30f * density) {
                        onPlusClick?.invoke()
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}

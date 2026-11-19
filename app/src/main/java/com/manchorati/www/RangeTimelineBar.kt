package com.manchorati.www

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/**
 * RangeTimelineBar: شريط زمني أفقي احترافي قابل للتمرير لتحديد وقص مقطع الفيديو أو ظهور الملصق.
 * يدعم الفيديوهات الطويلة جداً (ساعات ودقائق)، السحب من الوسط لنقل المدى، والتحكم في الحواف لتكبير/تصغير المدة.
 */
class RangeTimelineBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var minRangeSec: Float = 1.0f // الحد الأدنى للمقطع (ثانية واحدة)
    var maxRangeSec: Float = Float.MAX_VALUE // الحد الأقصى للمدى المحدد (مدة الاستوديو مثلاً 15ث أو 60ث)

    var customWidthDp: Float = 0f
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var totalDurationSec: Float = 15f
        set(value) {
            field = value.coerceAtLeast(1f)
            setRange(_startSec, _endSec)
            invalidate()
        }

    private var _startSec: Float = 0f
    private var _endSec: Float = 15f

    var startSec: Float
        get() = _startSec
        set(value) {
            val minVal = (_endSec - maxRangeSec).coerceAtLeast(0f)
            val maxVal = (_endSec - minRangeSec).coerceAtLeast(0f)
            _startSec = value.coerceIn(minVal, maxVal)
            invalidate()
        }

    var endSec: Float
        get() = _endSec
        set(value) {
            val minVal = (_startSec + minRangeSec).coerceAtMost(totalDurationSec)
            val maxVal = (_startSec + maxRangeSec).coerceAtMost(totalDurationSec)
            _endSec = value.coerceIn(minVal, maxVal)
            invalidate()
        }

    fun setRange(newStart: Float, newEnd: Float) {
        val maxDur = maxRangeSec.coerceAtLeast(minRangeSec)
        val clampedStart = newStart.coerceIn(0f, (totalDurationSec - minRangeSec).coerceAtLeast(0f))
        val maxAllowedEnd = minOf(totalDurationSec, clampedStart + maxDur)
        val clampedEnd = newEnd.coerceIn((clampedStart + minRangeSec).coerceAtMost(totalDurationSec), maxAllowedEnd)
        _startSec = clampedStart
        _endSec = clampedEnd
        invalidate()
    }

    var onRangeChanged: ((startSec: Float, endSec: Float) -> Unit)? = null
    var onRangeChangeFinished: ((startSec: Float, endSec: Float) -> Unit)? = null

    // Drawing paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141A26")
        style = Paint.Style.FILL
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        strokeWidth = 2f
    }
    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        strokeWidth = 3f
    }
    private val tickTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val activeRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val startHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0284C7")
        style = Paint.Style.FILL
    }
    private val endHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0EA5E9")
        style = Paint.Style.FILL
    }
    private val handleGripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B0F19")
        style = Paint.Style.FILL
    }
    private val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Touch interaction mode
    private enum class TouchMode { NONE, START_HANDLE, END_HANDLE, MIDDLE_RANGE }
    private var touchMode = TouchMode.NONE
    private var lastTouchX = 0f
    private var initialDragStart = 0f
    private var initialDragEnd = 0f

    private val dp get() = resources.displayMetrics.density
    val handleWidth get() = 26f * dp
    val barPaddingX get() = 24f * dp
    val barTop get() = 34f * dp
    val barHeight get() = 44f * dp
    val barBottom get() = barTop + barHeight

    init {
        tickTextPaint.textSize = 9.5f * dp
        badgeTextPaint.textSize = 10.5f * dp
    }

    fun secToX(sec: Float): Float {
        val usableW = width - (2 * barPaddingX)
        if (usableW <= 0f || totalDurationSec <= 0f) return barPaddingX
        return barPaddingX + (sec / totalDurationSec) * usableW
    }

    fun xToSec(x: Float): Float {
        val usableW = width - (2 * barPaddingX)
        if (usableW <= 0f) return 0f
        val ratio = ((x - barPaddingX) / usableW).coerceIn(0f, 1f)
        return ratio * totalDurationSec
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val usableW = width - (2 * barPaddingX)
        val trackRect = RectF(barPaddingX, barTop, width - barPaddingX, barBottom)
        val trackRadius = 8f * dp

        // 1. Background Rail (شريط الخلفية للتايم لاين)
        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)

        // 2. Dynamic Ticks & Time Labels
        val maxSeconds = totalDurationSec.toInt()
        val step = when {
            maxSeconds > 7200 -> 600 // every 10 min
            maxSeconds > 3600 -> 300 // every 5 min
            maxSeconds > 1800 -> 120 // every 2 min
            maxSeconds > 600 -> 60   // every 1 min
            maxSeconds > 300 -> 30   // every 30s
            maxSeconds > 120 -> 15   // every 15s
            maxSeconds > 60 -> 10    // every 10s
            maxSeconds > 30 -> 5     // every 5s
            maxSeconds > 15 -> 2     // every 2s
            else -> 1                // every 1s
        }

        fun formatSecLabel(s: Int): String {
            val h = s / 3600
            val m = (s % 3600) / 60
            val remS = s % 60
            return when {
                h > 0 -> if (m == 0 && remS == 0) "${h}س" else "${h}:${String.format("%02d", m)}"
                m > 0 -> if (remS == 0) "${m}د" else "${m}:${String.format("%02d", remS)}"
                else -> "${s}ث"
            }
        }

        for (s in 0..maxSeconds step step) {
            val tx = secToX(s.toFloat())
            val isMajor = (s % (step * 2) == 0)
            if (isMajor) {
                canvas.drawLine(tx, barBottom - (10f * dp), tx, barBottom, majorTickPaint)
                canvas.drawText(formatSecLabel(s), tx, barBottom - (13f * dp), tickTextPaint)
            } else {
                canvas.drawLine(tx, barBottom - (6f * dp), tx, barBottom, tickPaint)
            }
        }

        // 3. Highlighted Active Range
        val startX = secToX(startSec)
        val endX = secToX(endSec)

        // Ensure handles never overlap into an un-touchable mess even on 2h videos
        val minVisualWidth = handleWidth * 1.6f
        val visualStartX: Float
        val visualEndX: Float
        if (endX - startX < minVisualWidth) {
            val midX = (startX + endX) / 2f
            visualStartX = midX - (minVisualWidth / 2f)
            visualEndX = midX + (minVisualWidth / 2f)
        } else {
            visualStartX = startX
            visualEndX = endX
        }

        val activeRect = RectF(visualStartX, barTop, visualEndX, barBottom)

        activeRangePaint.shader = LinearGradient(
            visualStartX, barTop, visualEndX, barBottom,
            Color.parseColor("#440284C7"),
            Color.parseColor("#4438BDF8"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(activeRect, 6f * dp, 6f * dp, activeRangePaint)
        canvas.drawRoundRect(activeRect, 6f * dp, 6f * dp, borderPaint)

        // 4. Start Handle (Left / شرطة البداية)
        val startHandleRect = RectF(
            visualStartX - (handleWidth / 2f),
            barTop - (3f * dp),
            visualStartX + (handleWidth / 2f),
            barBottom + (3f * dp)
        )
        canvas.drawRoundRect(startHandleRect, 7f * dp, 7f * dp, startHandlePaint)
        // Handle grip lines
        canvas.drawLine(visualStartX - 3f * dp, barTop + 14f * dp, visualStartX - 3f * dp, barBottom - 14f * dp, handleGripPaint)
        canvas.drawLine(visualStartX + 3f * dp, barTop + 14f * dp, visualStartX + 3f * dp, barBottom - 14f * dp, handleGripPaint)

        // 5. End Handle (Right / شرطة النهاية)
        val endHandleRect = RectF(
            visualEndX - (handleWidth / 2f),
            barTop - (3f * dp),
            visualEndX + (handleWidth / 2f),
            barBottom + (3f * dp)
        )
        canvas.drawRoundRect(endHandleRect, 7f * dp, 7f * dp, endHandlePaint)
        // Handle grip lines
        canvas.drawLine(visualEndX - 3f * dp, barTop + 14f * dp, visualEndX - 3f * dp, barBottom - 14f * dp, handleGripPaint)
        canvas.drawLine(visualEndX + 3f * dp, barTop + 14f * dp, visualEndX + 3f * dp, barBottom - 14f * dp, handleGripPaint)

        // 6. Time Badges above handles (or single duration badge if handles are close)
        val handleDist = visualEndX - visualStartX
        if (handleDist < 75f * dp) {
            val dur = Math.round((endSec - startSec) * 10f) / 10f
            val durText = String.format(java.util.Locale.US, "%.1f ث", dur)
            drawTimeBadge(canvas, (visualStartX + visualEndX) / 2f, barTop - (6f * dp), durText)
        } else {
            drawTimeBadge(canvas, visualStartX, barTop - (6f * dp), formatBadgeTime(startSec))
            drawTimeBadge(canvas, visualEndX, barTop - (6f * dp), formatBadgeTime(endSec))
        }
    }

    private fun formatBadgeTime(sec: Float): String {
        val totalS = sec.toInt()
        val h = totalS / 3600
        val m = (totalS % 3600) / 60
        val s = totalS % 60
        val frac = ((sec - totalS) * 10).toInt()
        return when {
            h > 0 -> String.format(java.util.Locale.US, "%02d:%02d:%02d.%d", h, m, s, frac)
            m > 0 -> String.format(java.util.Locale.US, "%02d:%02d.%d", m, s, frac)
            else -> String.format(java.util.Locale.US, "%.1f ث", sec)
        }
    }

    private fun drawTimeBadge(canvas: Canvas, centerX: Float, bottomY: Float, text: String) {
        val padX = 8f * dp
        val textW = badgeTextPaint.measureText(text)
        val badgeW = textW + (padX * 2)
        val badgeH = 20f * dp
        val clampedX = centerX.coerceIn(barPaddingX + (badgeW / 2f), width - barPaddingX - (badgeW / 2f))
        val rect = RectF(clampedX - (badgeW / 2f), bottomY - badgeH, clampedX + (badgeW / 2f), bottomY)
        canvas.drawRoundRect(rect, 5f * dp, 5f * dp, badgeBgPaint)
        canvas.drawRoundRect(rect, 5f * dp, 5f * dp, badgeBorderPaint)
        canvas.drawText(text, clampedX, bottomY - (5f * dp), badgeTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val startX = secToX(startSec)
        val endX = secToX(endSec)
        val touchTolerance = 32f * dp

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                initialDragStart = startSec
                initialDragEnd = endSec

                val distToStart = Math.abs(x - startX)
                val distToEnd = Math.abs(x - endX)

                touchMode = when {
                    distToStart <= touchTolerance && distToStart <= distToEnd -> {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        TouchMode.START_HANDLE
                    }
                    distToEnd <= touchTolerance -> {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        TouchMode.END_HANDLE
                    }
                    x in (startX - (handleWidth / 2f))..(endX + (handleWidth / 2f)) -> {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        TouchMode.MIDDLE_RANGE
                    }
                    else -> TouchMode.NONE
                }
                parent?.requestDisallowInterceptTouchEvent(touchMode != TouchMode.NONE)
                return touchMode != TouchMode.NONE
            }

            MotionEvent.ACTION_MOVE -> {
                if (touchMode == TouchMode.NONE) return false
                val deltaX = x - lastTouchX
                val usableW = width - (2 * barPaddingX)
                val deltaSec = if (usableW > 0) (deltaX / usableW) * totalDurationSec else 0f

                when (touchMode) {
                    TouchMode.START_HANDLE -> {
                        val minAllowed = (endSec - maxRangeSec).coerceAtLeast(0f)
                        val maxAllowed = (endSec - minRangeSec).coerceAtLeast(0f)
                        val newStart = (xToSec(x)).coerceIn(minAllowed, maxAllowed)
                        startSec = Math.round(newStart * 10f) / 10f
                    }
                    TouchMode.END_HANDLE -> {
                        val minAllowed = (startSec + minRangeSec).coerceAtMost(totalDurationSec)
                        val maxAllowed = (startSec + maxRangeSec).coerceAtMost(totalDurationSec)
                        val newEnd = (xToSec(x)).coerceIn(minAllowed, maxAllowed)
                        endSec = Math.round(newEnd * 10f) / 10f
                    }
                    TouchMode.MIDDLE_RANGE -> {
                        val rangeDuration = initialDragEnd - initialDragStart
                        var newStart = initialDragStart + deltaSec
                        var newEnd = newStart + rangeDuration

                        if (newStart < 0f) {
                            newStart = 0f
                            newEnd = rangeDuration
                        }
                        if (newEnd > totalDurationSec) {
                            newEnd = totalDurationSec
                            newStart = totalDurationSec - rangeDuration
                        }
                        setRange(Math.round(newStart * 10f) / 10f, Math.round(newEnd * 10f) / 10f)
                    }
                    else -> {}
                }

                invalidate()
                onRangeChanged?.invoke(startSec, endSec)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = TouchMode.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                onRangeChangeFinished?.invoke(startSec, endSec)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (94f * dp).toInt()
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)
        val finalH = if (hMode == MeasureSpec.EXACTLY) hSize else desiredHeight

        val finalW = if (customWidthDp > 0f) {
            (customWidthDp * dp).toInt()
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }
        setMeasuredDimension(finalW, finalH)
    }
}

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
 * SpeedRulerBar: شريط تقدم ومسطرة سرعة (Progress bar with notches / ticks)
 * يتراوح من 0.1x إلى 5.0x مع علامات وشرطات دقيقة للمسافات (ticks)
 * وبدون نصوص مطولة لتوفير واجهة مونتاج احترافية وسريعة.
 */
class SpeedRulerBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density

    var onSpeedChanged: ((Float) -> Unit)? = null

    var speed: Float = 1.0f
        set(value) {
            val clamped = value.coerceIn(0.1f, 5.0f)
            if (Math.abs(field - clamped) > 0.01f) {
                field = clamped
                invalidate()
            }
        }

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1E293B")
    }

    private val trackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * dp
        color = Color.parseColor("#334155")
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val majorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * dp
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#94A3B8")
    }

    private val centerTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * dp
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#38BDF8")
    }

    private val minorTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * dp
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#475569")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        textSize = 10f * dp
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        textSize = 10.5f * dp
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val thumbGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#38BDF8")
        alpha = 60
    }

    private val thumbOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0EA5E9")
    }

    private val thumbInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val trackRect = RectF()
    private val progressRect = RectF()

    // Dimensions
    private val horizontalPadding get() = 24f * dp
    private val trackHeight get() = 8f * dp
    private val thumbRadius get() = 9f * dp
    private val thumbGlowRadius get() = 15f * dp

    // Milestones: (Speed, Label, isMajor)
    private val milestones = listOf(
        // Slow motion area (0.1x to 1.0x)
        Milestone(0.1f, "0.1x", isMajor = true),
        Milestone(0.2f, "", isMajor = false),
        Milestone(0.3f, "", isMajor = false),
        Milestone(0.4f, "", isMajor = false),
        Milestone(0.5f, "0.5x", isMajor = true),
        Milestone(0.6f, "", isMajor = false),
        Milestone(0.7f, "", isMajor = false),
        Milestone(0.75f, "", isMajor = false),
        Milestone(0.8f, "", isMajor = false),
        Milestone(0.9f, "", isMajor = false),
        // Normal (1.0x)
        Milestone(1.0f, "1x", isMajor = true, isCenter = true),
        // Fast motion area (1.0x to 5.0x)
        Milestone(1.25f, "", isMajor = false),
        Milestone(1.5f, "", isMajor = false),
        Milestone(1.75f, "", isMajor = false),
        Milestone(2.0f, "2x", isMajor = true),
        Milestone(2.5f, "", isMajor = false),
        Milestone(3.0f, "3x", isMajor = true),
        Milestone(3.5f, "", isMajor = false),
        Milestone(4.0f, "", isMajor = false),
        Milestone(4.5f, "", isMajor = false),
        Milestone(5.0f, "5x", isMajor = true)
    )

    data class Milestone(
        val speedVal: Float,
        val label: String,
        val isMajor: Boolean,
        val isCenter: Boolean = false
    )

    private fun speedToRatio(sp: Float): Float {
        val s = sp.coerceIn(0.1f, 5.0f)
        return when {
            s <= 0.5f -> ((s - 0.1f) / 0.4f) * 0.25f
            s <= 1.0f -> 0.25f + ((s - 0.5f) / 0.5f) * 0.25f
            s <= 2.0f -> 0.50f + ((s - 1.0f) / 1.0f) * 0.25f
            else -> 0.75f + ((s - 2.0f) / 3.0f) * 0.25f
        }.coerceIn(0f, 1f)
    }

    private fun ratioToSpeed(r: Float): Float {
        val ratio = r.coerceIn(0f, 1f)
        val raw = when {
            ratio <= 0.25f -> 0.1f + (ratio / 0.25f) * 0.4f
            ratio <= 0.50f -> 0.5f + ((ratio - 0.25f) / 0.25f) * 0.5f
            ratio <= 0.75f -> 1.0f + ((ratio - 0.50f) / 0.25f) * 1.0f
            else -> 2.0f + ((ratio - 0.75f) / 0.25f) * 3.0f
        }
        // Snap close to major landmarks
        if (Math.abs(raw - 1.0f) < 0.06f) return 1.0f
        if (Math.abs(raw - 0.5f) < 0.04f) return 0.5f
        if (Math.abs(raw - 0.25f) < 0.03f) return 0.25f
        if (Math.abs(raw - 0.75f) < 0.03f) return 0.75f
        if (Math.abs(raw - 2.0f) < 0.08f) return 2.0f
        if (Math.abs(raw - 3.0f) < 0.1f) return 3.0f
        if (Math.abs(raw - 5.0f) < 0.1f) return 5.0f
        if (Math.abs(raw - 0.1f) < 0.03f) return 0.1f

        return Math.round(raw * 10f) / 10f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredH = (56 * dp).toInt()
        val h = resolveSize(desiredH, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= horizontalPadding * 2) return

        val usableWidth = w - horizontalPadding * 2
        val trackY = 22f * dp
        val trackRadius = trackHeight / 2f

        // 1. Draw Background Track
        trackRect.set(horizontalPadding, trackY - trackRadius, w - horizontalPadding, trackY + trackRadius)
        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)
        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackBorderPaint)

        // 2. Draw Progress Fill from Start to Current Thumb
        val currentRatio = speedToRatio(speed)
        val thumbX = horizontalPadding + usableWidth * currentRatio

        if (thumbX > horizontalPadding) {
            progressRect.set(horizontalPadding, trackY - trackRadius, thumbX, trackY + trackRadius)
            progressPaint.shader = LinearGradient(
                horizontalPadding, trackY, thumbX, trackY,
                Color.parseColor("#0EA5E9"), Color.parseColor("#38BDF8"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(progressRect, trackRadius, trackRadius, progressPaint)
        }

        // 3. Draw Ticks ("الشرطات") and Labels
        val tickTopMajor = trackY - (12f * dp)
        val tickTopMinor = trackY - (7f * dp)
        val tickBottom = trackY - (2f * dp)
        val labelY = trackY + (20f * dp)

        for (m in milestones) {
            val mRatio = speedToRatio(m.speedVal)
            val tickX = horizontalPadding + usableWidth * mRatio
            val isCurrent = Math.abs(speed - m.speedVal) < 0.04f

            if (m.isMajor) {
                val p = if (m.isCenter) centerTickPaint else (if (isCurrent) centerTickPaint else majorTickPaint)
                canvas.drawLine(tickX, tickTopMajor, tickX, tickBottom, p)

                if (m.label.isNotEmpty()) {
                    val tp = if (isCurrent) activeTextPaint else textPaint
                    canvas.drawText(m.label, tickX, labelY, tp)
                }
            } else {
                canvas.drawLine(tickX, tickTopMinor, tickX, tickBottom, minorTickPaint)
            }
        }

        // 4. Draw Thumb
        canvas.drawCircle(thumbX, trackY, thumbGlowRadius, thumbGlowPaint)
        canvas.drawCircle(thumbX, trackY, thumbRadius, thumbOuterPaint)
        canvas.drawCircle(thumbX, trackY, thumbRadius * 0.45f, thumbInnerPaint)
    }

    private var isDragging = false
    private var lastHapticSpeed = -1f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val usableWidth = w - horizontalPadding * 2
        if (usableWidth <= 0) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                isDragging = true
                updateTouch(event.x, usableWidth)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateTouch(event.x, usableWidth)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    updateTouch(event.x, usableWidth)
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateTouch(rawX: Float, usableWidth: Float) {
        val relX = (rawX - horizontalPadding).coerceIn(0f, usableWidth)
        val ratio = relX / usableWidth
        val newSpeed = ratioToSpeed(ratio)

        if (Math.abs(speed - newSpeed) > 0.01f) {
            speed = newSpeed
            // Haptic on major landmarks
            if (newSpeed in listOf(0.1f, 0.5f, 1.0f, 2.0f, 3.0f, 5.0f) && newSpeed != lastHapticSpeed) {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                lastHapticSpeed = newSpeed
            }
            onSpeedChanged?.invoke(speed)
            invalidate()
        }
    }
}

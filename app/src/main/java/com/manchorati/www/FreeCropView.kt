package com.manchorati.www

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * FreeCropView: أداة قص حر متطورة (Freeform Image Cropper)
 * تتيح للمستخدم قص الصورة من أي جانب (الجوانب، الأعلى، الأسفل) بكل حرية وبدون تقييد لنسب الأبعاد.
 */
class FreeCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sourceBitmap: Bitmap? = null
    private val bitmapRect = RectF()
    val cropRect = RectF()

    private val dp get() = resources.displayMetrics.density
    private val handleTouchRadius get() = 30f * dp
    private val cornerBracketLength get() = 18f * dp
    private val cornerBracketThickness get() = 3.5f * dp

    // Paints
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3000000") // 70% dimming outside crop area
        style = Paint.Style.FILL
    }
    private val cropBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * dp
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FFFFFF") // 50% white rule-of-thirds grid
        style = Paint.Style.STROKE
        strokeWidth = 1f * dp
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8") // Vibrant cyan handles
        style = Paint.Style.STROKE
        strokeWidth = cornerBracketThickness
        strokeCap = Paint.Cap.ROUND
    }
    private val edgePillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.FILL
    }

    // Touch handle enum
    private enum class TouchHandle {
        NONE, CENTER,
        CORNER_TL, CORNER_TR, CORNER_BL, CORNER_BR,
        EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT
    }
    private var activeHandle = TouchHandle.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    fun setSourceBitmap(bitmap: Bitmap) {
        this.sourceBitmap = bitmap
        calculateBounds()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        calculateBounds()
    }

    private fun calculateBounds() {
        val bmp = sourceBitmap ?: return
        if (width <= 0 || height <= 0) return

        val pad = 24f * dp
        val availW = width - (2 * pad)
        val availH = height - (2 * pad)
        if (availW <= 0 || availH <= 0) return

        val scale = min(availW / bmp.width.toFloat(), availH / bmp.height.toFloat())
        val scaledW = bmp.width * scale
        val scaledH = bmp.height * scale

        val left = (width - scaledW) / 2f
        val top = (height - scaledH) / 2f
        bitmapRect.set(left, top, left + scaledW, top + scaledH)

        // Default crop rect: 90% of image centered
        val insetX = scaledW * 0.05f
        val insetY = scaledH * 0.05f
        cropRect.set(
            bitmapRect.left + insetX,
            bitmapRect.top + insetY,
            bitmapRect.right - insetX,
            bitmapRect.bottom - insetY
        )
    }

    fun resetCrop() {
        cropRect.set(bitmapRect)
        invalidate()
    }

    fun rotate90() {
        val bmp = sourceBitmap ?: return
        val matrix = Matrix().apply { postRotate(90f) }
        sourceBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        calculateBounds()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = sourceBitmap ?: return
        if (width <= 0 || height <= 0) return

        // 1. Draw scaled source bitmap
        canvas.drawBitmap(bmp, null, bitmapRect, bitmapPaint)

        // 2. Draw darkened scrim outside crop rect
        // Top
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, scrimPaint)
        // Bottom
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), scrimPaint)
        // Left
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, scrimPaint)
        // Right
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, scrimPaint)

        // 3. Draw Crop Border
        canvas.drawRect(cropRect, cropBorderPaint)

        // 4. Draw Rule of Thirds Grid
        val thirdW = cropRect.width() / 3f
        val thirdH = cropRect.height() / 3f
        // Vertical grid lines
        canvas.drawLine(cropRect.left + thirdW, cropRect.top, cropRect.left + thirdW, cropRect.bottom, gridPaint)
        canvas.drawLine(cropRect.left + (2 * thirdW), cropRect.top, cropRect.left + (2 * thirdW), cropRect.bottom, gridPaint)
        // Horizontal grid lines
        canvas.drawLine(cropRect.left, cropRect.top + thirdH, cropRect.right, cropRect.top + thirdH, gridPaint)
        canvas.drawLine(cropRect.left, cropRect.top + (2 * thirdH), cropRect.right, cropRect.top + (2 * thirdH), gridPaint)

        // 5. Draw 4 Corner Brackets (Cyan)
        val bl = cornerBracketLength
        // Top-Left
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left + bl, cropRect.top, handlePaint)
        canvas.drawLine(cropRect.left, cropRect.top, cropRect.left, cropRect.top + bl, handlePaint)
        // Top-Right
        canvas.drawLine(cropRect.right - bl, cropRect.top, cropRect.right, cropRect.top, handlePaint)
        canvas.drawLine(cropRect.right, cropRect.top, cropRect.right, cropRect.top + bl, handlePaint)
        // Bottom-Left
        canvas.drawLine(cropRect.left, cropRect.bottom - bl, cropRect.left, cropRect.bottom, handlePaint)
        canvas.drawLine(cropRect.left, cropRect.bottom, cropRect.left + bl, cropRect.bottom, handlePaint)
        // Bottom-Right
        canvas.drawLine(cropRect.right - bl, cropRect.bottom, cropRect.right, cropRect.bottom, handlePaint)
        canvas.drawLine(cropRect.right, cropRect.bottom - bl, cropRect.right, cropRect.bottom, handlePaint)

        // 6. Draw 4 Edge Midpoint Handles (Pills)
        val pillLen = 14f * dp
        val pillThick = 3f * dp
        // Top Edge
        canvas.drawRoundRect(cropRect.centerX() - pillLen, cropRect.top - (pillThick / 2f), cropRect.centerX() + pillLen, cropRect.top + (pillThick / 2f), pillThick, pillThick, edgePillPaint)
        // Bottom Edge
        canvas.drawRoundRect(cropRect.centerX() - pillLen, cropRect.bottom - (pillThick / 2f), cropRect.centerX() + pillLen, cropRect.bottom + (pillThick / 2f), pillThick, pillThick, edgePillPaint)
        // Left Edge
        canvas.drawRoundRect(cropRect.left - (pillThick / 2f), cropRect.centerY() - pillLen, cropRect.left + (pillThick / 2f), cropRect.centerY() + pillLen, pillThick, pillThick, edgePillPaint)
        // Right Edge
        canvas.drawRoundRect(cropRect.right - (pillThick / 2f), cropRect.centerY() - pillLen, cropRect.right + (pillThick / 2f), cropRect.centerY() + pillLen, pillThick, pillThick, edgePillPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y
                activeHandle = findTouchHandle(x, y)
                if (activeHandle != TouchHandle.NONE) {
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeHandle == TouchHandle.NONE) return false
                val dx = x - lastTouchX
                val dy = y - lastTouchY
                lastTouchX = x
                lastTouchY = y

                val minSize = 40f * dp

                when (activeHandle) {
                    TouchHandle.CENTER -> {
                        val w = cropRect.width()
                        val h = cropRect.height()
                        var newL = (cropRect.left + dx).coerceIn(bitmapRect.left, bitmapRect.right - w)
                        var newT = (cropRect.top + dy).coerceIn(bitmapRect.top, bitmapRect.bottom - h)
                        cropRect.set(newL, newT, newL + w, newT + h)
                    }
                    TouchHandle.CORNER_TL -> {
                        cropRect.left = (cropRect.left + dx).coerceIn(bitmapRect.left, cropRect.right - minSize)
                        cropRect.top = (cropRect.top + dy).coerceIn(bitmapRect.top, cropRect.bottom - minSize)
                    }
                    TouchHandle.CORNER_TR -> {
                        cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, bitmapRect.right)
                        cropRect.top = (cropRect.top + dy).coerceIn(bitmapRect.top, cropRect.bottom - minSize)
                    }
                    TouchHandle.CORNER_BL -> {
                        cropRect.left = (cropRect.left + dx).coerceIn(bitmapRect.left, cropRect.right - minSize)
                        cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, bitmapRect.bottom)
                    }
                    TouchHandle.CORNER_BR -> {
                        cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, bitmapRect.right)
                        cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, bitmapRect.bottom)
                    }
                    TouchHandle.EDGE_TOP -> {
                        cropRect.top = (cropRect.top + dy).coerceIn(bitmapRect.top, cropRect.bottom - minSize)
                    }
                    TouchHandle.EDGE_BOTTOM -> {
                        cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, bitmapRect.bottom)
                    }
                    TouchHandle.EDGE_LEFT -> {
                        cropRect.left = (cropRect.left + dx).coerceIn(bitmapRect.left, cropRect.right - minSize)
                    }
                    TouchHandle.EDGE_RIGHT -> {
                        cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, bitmapRect.right)
                    }
                    TouchHandle.NONE -> {}
                }
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = TouchHandle.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchHandle(x: Float, y: Float): TouchHandle {
        val r = handleTouchRadius
        // Corners first
        if (Math.hypot((x - cropRect.left).toDouble(), (y - cropRect.top).toDouble()) <= r) return TouchHandle.CORNER_TL
        if (Math.hypot((x - cropRect.right).toDouble(), (y - cropRect.top).toDouble()) <= r) return TouchHandle.CORNER_TR
        if (Math.hypot((x - cropRect.left).toDouble(), (y - cropRect.bottom).toDouble()) <= r) return TouchHandle.CORNER_BL
        if (Math.hypot((x - cropRect.right).toDouble(), (y - cropRect.bottom).toDouble()) <= r) return TouchHandle.CORNER_BR

        // Edges
        if (Math.abs(y - cropRect.top) <= r && x in (cropRect.left - r)..(cropRect.right + r)) return TouchHandle.EDGE_TOP
        if (Math.abs(y - cropRect.bottom) <= r && x in (cropRect.left - r)..(cropRect.right + r)) return TouchHandle.EDGE_BOTTOM
        if (Math.abs(x - cropRect.left) <= r && y in (cropRect.top - r)..(cropRect.bottom + r)) return TouchHandle.EDGE_LEFT
        if (Math.abs(x - cropRect.right) <= r && y in (cropRect.top - r)..(cropRect.bottom + r)) return TouchHandle.EDGE_RIGHT

        // Center / Inside
        if (cropRect.contains(x, y)) return TouchHandle.CENTER

        return TouchHandle.NONE
    }

    /**
     * getCroppedBitmap: يستخرج الجزء المقصوص من الصورة الأصلية بدقة عالية
     */
    fun getCroppedBitmap(): Bitmap? {
        val bmp = sourceBitmap ?: return null
        if (bitmapRect.width() <= 0f || bitmapRect.height() <= 0f) return bmp

        val scaleX = bmp.width.toFloat() / bitmapRect.width()
        val scaleY = bmp.height.toFloat() / bitmapRect.height()

        val srcLeft = ((cropRect.left - bitmapRect.left) * scaleX).toInt().coerceIn(0, bmp.width - 1)
        val srcTop = ((cropRect.top - bitmapRect.top) * scaleY).toInt().coerceIn(0, bmp.height - 1)
        val srcWidth = (cropRect.width() * scaleX).toInt().coerceIn(1, bmp.width - srcLeft)
        val srcHeight = (cropRect.height() * scaleY).toInt().coerceIn(1, bmp.height - srcTop)

        return try {
            Bitmap.createBitmap(bmp, srcLeft, srcTop, srcWidth, srcHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            bmp
        }
    }
}

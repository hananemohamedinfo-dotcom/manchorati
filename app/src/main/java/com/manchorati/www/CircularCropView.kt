package com.manchorati.www

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

class CircularCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sourceBitmap: Bitmap? = null
    private val displayMatrix = Matrix()
    private val inverseMatrix = Matrix()

    private var circleCenterX = 0f
    private var circleCenterY = 0f
    private var circleRadius = 0f

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3000000.toInt() // 70% black dimming
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF38BDF8.toInt() // sky blue modern ring
        strokeWidth = 2.5f * resources.displayMetrics.density
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            displayMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            invalidate()
            return true
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            circleCenterX = w / 2f
            circleCenterY = h / 2f
            circleRadius = min(w, h) * 0.42f
            initMatrixForBitmap()
        }
    }

    fun setImageBitmap(bitmap: Bitmap) {
        sourceBitmap = bitmap
        initMatrixForBitmap()
        invalidate()
    }

    private fun initMatrixForBitmap() {
        val bmp = sourceBitmap ?: return
        if (width <= 0 || height <= 0 || circleRadius <= 0) return

        displayMatrix.reset()
        // مقياس ملء الدائرة بالكامل في البداية
        val scale = max(
            (circleRadius * 2.2f) / bmp.width.toFloat(),
            (circleRadius * 2.2f) / bmp.height.toFloat()
        )
        displayMatrix.postScale(scale, scale)

        // توسط الصورة في منتصف الدائرة
        val scaledW = bmp.width * scale
        val scaledH = bmp.height * scale
        val dx = circleCenterX - (scaledW / 2f)
        val dy = circleCenterY - (scaledH / 2f)
        displayMatrix.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    displayMatrix.postTranslate(dx, dy)
                    invalidate()
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. رسم الصورة بالتحويلات الحالية (تكبير/تصغير/سحب)
        sourceBitmap?.let { bmp ->
            canvas.drawBitmap(bmp, displayMatrix, bitmapPaint)
        }

        // 2. رسم القناع الدائري الاحترافي
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawColor(maskPaint.color)
        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, clearPaint)
        canvas.restoreToCount(saveCount)

        // 3. رسم الإطار الدائري الخارجي
        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, strokePaint)
    }

    /**
     * قص الصورة داخل الدائرة وتصغيرها إلى أبعاد صغيرة جداً (مثل 180x180)
     */
    fun getCroppedBitmap(targetSize: Int = 180): Bitmap? {
        val bmp = sourceBitmap ?: return null
        if (!displayMatrix.invert(inverseMatrix)) return null

        val pts = floatArrayOf(
            circleCenterX - circleRadius,
            circleCenterY - circleRadius,
            circleCenterX + circleRadius,
            circleCenterY + circleRadius
        )
        inverseMatrix.mapPoints(pts)

        val cropLeft = max(0, pts[0].toInt())
        val cropTop = max(0, pts[1].toInt())
        val cropRight = min(bmp.width, pts[2].toInt())
        val cropBottom = min(bmp.height, pts[3].toInt())

        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop

        if (cropWidth <= 0 || cropHeight <= 0) return null

        val cropped = Bitmap.createBitmap(bmp, cropLeft, cropTop, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (cropped != scaled && cropped != bmp) {
            cropped.recycle()
        }
        return scaled
    }

    /**
     * تحويل الصورة المقصوصة إلى صيغة WebP المضغوطة للغاية (حجم بين 10KB إلى 15KB)
     */
    fun getCroppedWebpBytes(targetSize: Int = 180, quality: Int = 75): ByteArray? {
        val scaled = getCroppedBitmap(targetSize) ?: return null
        val out = ByteArrayOutputStream()
        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, out)
        } else {
            @Suppress("DEPRECATION")
            scaled.compress(Bitmap.CompressFormat.WEBP, quality, out)
        }
        scaled.recycle()
        return if (success) out.toByteArray() else null
    }

    companion object {
        /**
         * تحميل الصورة من URI مع تصحيح اتجاه الكاميرا (EXIF) وتفادي نفاذ الذاكرة
         */
        fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1200): Bitmap? {
            try {
                // 1. حساب أبعاد الصورة لتفادي استهلاك الرام
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                var inSampleSize = 1
                while ((options.outWidth / inSampleSize) > maxDimension || (options.outHeight / inSampleSize) > maxDimension) {
                    inSampleSize *= 2
                }

                // 2. فك التشفير الحقيقي
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                var decoded: Bitmap? = null
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    decoded = BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
                val bitmap = decoded ?: return null

                // 3. تصحيح اتجاه الصورة من الـ EXIF (خاص بهواتف سامسونج وشاومي)
                var rotation = 0
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    rotation = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }

                return if (rotation != 0) {
                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if (rotated != bitmap) bitmap.recycle()
                    rotated
                } else {
                    bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}

package com.manchorati.www

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

object StoryShareHelper {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun generatePostBitmap(context: Context, post: CommunityPost): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background
        drawStoryBackground(context, canvas, width, height, post.bgId)

        // 2. Central Card Area
        val cardLeft = 70f
        val cardRight = (width - 70).toFloat()
        val cardTop = 320f
        val cardBottom = 1620f
        val cardRadius = 48f

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33000000")
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#44FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardPaint)
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, cardBorderPaint)

        // 3. Decorative Quotation Marks
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4DFFFFFF")
            textSize = 140f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“", width / 2f, cardTop + 140f, quotePaint)

        // 4. Header: Author Name, Username & Tag
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val usernamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 28f
            textAlign = Paint.Align.RIGHT
        }

        val headerY = cardTop + 230f
        val headerX = cardRight - 60f
        canvas.drawText(post.authorName, headerX, headerY, authorPaint)
        canvas.drawText("@${post.authorUsername}", headerX, headerY + 44f, usernamePaint)

        // Badge / Tag chip
        if (!post.tag.isNullOrEmpty()) {
            val cleanTag = post.tag.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
            val tagText = "# ${cleanTag.ifEmpty { post.tag }}"
            val tagTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textWidth = tagTextPaint.measureText(tagText)
            val chipLeft = cardLeft + 60f
            val chipTop = headerY - 34f
            val chipRight = chipLeft + textWidth + 40f
            val chipBottom = headerY + 18f

            val chipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#260284C7")
                style = Paint.Style.FILL
            }
            val chipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#6638BDF8")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            val chipRect = RectF(chipLeft, chipTop, chipRight, chipBottom)
            canvas.drawRoundRect(chipRect, 20f, 20f, chipBgPaint)
            canvas.drawRoundRect(chipRect, 20f, 20f, chipBorderPaint)
            canvas.drawText(tagText, chipLeft + 20f, chipBottom - 14f, tagTextPaint)
        }

        // Divider Line
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#26FFFFFF")
            strokeWidth = 1.5f
        }
        canvas.drawLine(cardLeft + 50f, headerY + 80f, cardRight - 50f, headerY + 80f, dividerPaint)

        // 5. Post Content
        val contentPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            val postColor = try {
                if (!post.textColor.isNullOrEmpty()) Color.parseColor(post.textColor) else Color.WHITE
            } catch (_: Exception) {
                Color.WHITE
            }
            color = postColor
            textSize = if (post.content.length > 200) 40f else if (post.content.length > 100) 48f else 56f
            typeface = ThemeHelper.getFontTypeface(context, post.fontId)
        }

        val contentWidth = (cardRight - cardLeft - 120f).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(post.content, 0, post.content.length, contentPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(16f, 1.25f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                post.content,
                contentPaint,
                contentWidth,
                Layout.Alignment.ALIGN_CENTER,
                1.25f,
                16f,
                true
            )
        }

        val textStartY = (cardTop + 330f + (cardBottom - (cardTop + 330f) - staticLayout.height) / 2f).coerceAtLeast(cardTop + 330f)
        canvas.save()
        canvas.translate(cardLeft + 60f, textStartY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. Bottom Branding / Watermark
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("من منشوراتي", width / 2f, 1750f, brandPaint)
        canvas.drawText("تطبيق منشوراتي للاقتباسات والخواطر", width / 2f, 1795f, subBrandPaint)

        return bitmap
    }

    /**
     * مشاركة الصورة فقط عبر التطبيقات (واتساب، انستغرام، فيسبوك) لحفظ الخصوصية وبدون نصوص مشوشة
     */
    fun sharePostAsStory(context: Context, post: CommunityPost) {
        try {
            Toast.makeText(context, context.getString(R.string.preparing_story), Toast.LENGTH_SHORT).show()

            executor.execute {
                try {
                    val bitmap = generatePostBitmap(context, post)

                    val imagesFolder = File(context.cacheDir, "images")
                    if (!imagesFolder.exists()) imagesFolder.mkdirs()

                    val file = File(imagesFolder, "story_${post.id}_${System.currentTimeMillis()}.png")
                    val fos = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                    // مشاركة الصورة فقط بدون نصوص إضافية
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    mainHandler.post {
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_story_chooser)))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    mainHandler.post {
                        Toast.makeText(context, "حدث خطأ أثناء تجهيز الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * مشاركة صورة المنشور مباشرة في حالة واتساب بنقرة واحدة
     */
    fun sharePostToWhatsApp(context: Context, post: CommunityPost) {
        Toast.makeText(context, "جاري تحضير حالة الواتساب...", Toast.LENGTH_SHORT).show()

        executor.execute {
            try {
                val bitmap = generatePostBitmap(context, post)
                val imagesFolder = File(context.cacheDir, "shared_stories")
                imagesFolder.mkdirs()
                val file = File(imagesFolder, "story_wa_${post.id}_${System.currentTimeMillis()}.png")
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

                val waIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    `package` = "com.whatsapp"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                mainHandler.post {
                    try {
                        context.startActivity(waIntent)
                    } catch (e: Exception) {
                        val fallback = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(fallback, context.getString(R.string.share_story_chooser)))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    Toast.makeText(context, "حدث خطأ أثناء تجهيز الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * حفظ صورة المنشور مباشرة في معرض صور الهاتف (Gallery)
     */
    fun savePostImageToGallery(context: Context, post: CommunityPost) {
        Toast.makeText(context, context.getString(R.string.saving_post_image), Toast.LENGTH_SHORT).show()

        executor.execute {
            try {
                val bitmap = generatePostBitmap(context, post)
                val fileName = "manchorati_${post.id}_${System.currentTimeMillis()}.png"
                var savedSuccess = false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Manchorati")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            savedSuccess = bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val manchoratiDir = File(picturesDir, "Manchorati")
                    if (!manchoratiDir.exists()) manchoratiDir.mkdirs()
                    val file = File(manchoratiDir, fileName)
                    FileOutputStream(file).use { os ->
                        savedSuccess = bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                }

                mainHandler.post {
                    if (savedSuccess) {
                        Toast.makeText(context, context.getString(R.string.post_image_saved_success), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.post_image_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    Toast.makeText(context, context.getString(R.string.post_image_save_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun drawStoryBackground(context: Context, canvas: Canvas, width: Int, height: Int, bgId: String) {
        val tmpl = ThemeHelper.getCardTemplate(bgId)
        val (topColor, bottomColor) = if (tmpl != null) {
            tmpl.startColor to tmpl.endColor
        } else if (bgId.contains(",")) {
            val parts = bgId.split(",")
            try {
                Color.parseColor(parts[0].trim()) to Color.parseColor(parts[1].trim())
            } catch (_: Exception) {
                Color.parseColor("#0F172A") to Color.parseColor("#1E293B")
            }
        } else {
            when (bgId) {
                "grad_dark" -> Color.parseColor("#0F172A") to Color.parseColor("#1E293B")
                "grad_ocean" -> Color.parseColor("#0369A1") to Color.parseColor("#0F172A")
                "grad_sunset" -> Color.parseColor("#C2410C") to Color.parseColor("#831843")
                "grad_emerald" -> Color.parseColor("#047857") to Color.parseColor("#064E3B")
                "grad_royal" -> Color.parseColor("#4C1D95") to Color.parseColor("#1E1B4B")
                "preset_aurora" -> Color.parseColor("#052E16") to Color.parseColor("#065F46")
                "preset_cyber" -> Color.parseColor("#1E1B4B") to Color.parseColor("#4C1D95")
                "preset_luxury" -> Color.parseColor("#1C1917") to Color.parseColor("#44403C")
                "preset_sunset" -> Color.parseColor("#831843") to Color.parseColor("#9A3412")
                else -> {
                    if (bgId.startsWith("#")) {
                        try {
                            val solid = Color.parseColor(bgId)
                            solid to solid
                        } catch (_: Exception) {
                            Color.parseColor("#0F172A") to Color.parseColor("#1E293B")
                        }
                    } else {
                        Color.parseColor("#0F172A") to Color.parseColor("#1E293B")
                    }
                }
            }
        }

        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(topColor, bottomColor),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply {
            shader = gradient
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle Ambient Mesh Overlays
        val circlePaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1AFFFFFF")
            maskFilter = BlurMaskFilter(160f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(width * 0.85f, height * 0.18f, 320f, circlePaint1)

        val circlePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#14000000")
            maskFilter = BlurMaskFilter(200f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(width * 0.15f, height * 0.82f, 400f, circlePaint2)
    }
}

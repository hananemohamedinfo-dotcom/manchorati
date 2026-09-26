package com.manchorati.www

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.ContentValues
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {

    private var activeCustomImageView: ImageView? = null
    private var activeOverlayView: View? = null

    fun registerImagePicker(activity: ComponentActivity): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream: InputStream? = activity.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    activeCustomImageView?.setImageBitmap(bitmap)
                    activeCustomImageView?.visibility = View.VISIBLE
                    activeOverlayView?.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private data class FontItem(val displayName: String, val fontResId: Int)

    fun showImageEditorDialog(
        context: Context,
        categoryName: String,
        content: String,
        onPickImageRequest: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
        dialog.setContentView(view)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cardPreview = view.findViewById<CardView>(R.id.cardPreviewContainer)
        val ivBackground = view.findViewById<ImageView>(R.id.ivCustomBackground)
        val viewOverlay = view.findViewById<View>(R.id.viewOverlay)
        val tvCategory = view.findViewById<TextView>(R.id.tvPreviewCategory)
        val tvContent = view.findViewById<TextView>(R.id.tvPreviewContent)
        
        activeCustomImageView = ivBackground
        activeOverlayView = viewOverlay

        tvCategory.text = "• $categoryName •"
        tvContent.text = content

        val toolColors = view.findViewById<HorizontalScrollView>(R.id.toolColors)
        val toolFonts = view.findViewById<LinearLayout>(R.id.toolFonts) // tbdel l LinearLayout
        val layoutColorsContainer = view.findViewById<LinearLayout>(R.id.layoutColorsContainer)
        val layoutFontsContainer = view.findViewById<LinearLayout>(R.id.layoutFontsContainer)
        
        val tabBackground = view.findViewById<LinearLayout>(R.id.tabBackground)
        val tabFont = view.findViewById<LinearLayout>(R.id.tabFont)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnSaveImage = view.findViewById<Button>(R.id.btnSaveImage)
        val btnConfirmShare = view.findViewById<Button>(R.id.btnConfirmShare)

        // Tbadal bin Tabs
        tabBackground.setOnClickListener {
            toolColors.visibility = View.VISIBLE
            toolFonts.visibility = View.GONE
            tabBackground.alpha = 1.0f
            tabFont.alpha = 0.5f
        }
        tabFont.setOnClickListener {
            toolColors.visibility = View.GONE
            toolFonts.visibility = View.VISIBLE
            tabBackground.alpha = 0.5f
            tabFont.alpha = 1.0f
        }
        tabFont.alpha = 0.5f

        // =====================================
        // ADWAT TANSSIQ JDAD (Alignment & Effects)
        // =====================================
        val btnAlignRight = view.findViewById<ImageView>(R.id.btnAlignRight)
        val btnAlignCenter = view.findViewById<ImageView>(R.id.btnAlignCenter)
        val btnAlignLeft = view.findViewById<ImageView>(R.id.btnAlignLeft)
        val btnTextShadow = view.findViewById<ImageView>(R.id.btnTextShadow)
        val btnTextHighlight = view.findViewById<ImageView>(R.id.btnTextHighlight)

        // 1. Mo7adat
        btnAlignRight?.setOnClickListener { 
            tvContent.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            btnAlignRight.alpha = 1.0f
            btnAlignCenter?.alpha = 0.5f
            btnAlignLeft?.alpha = 0.5f
        }
        btnAlignCenter?.setOnClickListener { 
            tvContent.gravity = Gravity.CENTER
            btnAlignCenter.alpha = 1.0f
            btnAlignRight?.alpha = 0.5f
            btnAlignLeft?.alpha = 0.5f
        }
        btnAlignLeft?.setOnClickListener { 
            tvContent.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            btnAlignLeft.alpha = 1.0f
            btnAlignCenter?.alpha = 0.5f
            btnAlignRight?.alpha = 0.5f
        }
        // Iftiradyan (Center)
        btnAlignCenter?.alpha = 1.0f
        btnAlignRight?.alpha = 0.5f
        btnAlignLeft?.alpha = 0.5f

        // 2. Dal (Shadow)
        var isShadowEnabled = true // Iftiradyan f XML fih dal
        btnTextShadow?.alpha = 1.0f
        btnTextShadow?.setOnClickListener {
            isShadowEnabled = !isShadowEnabled
            if (isShadowEnabled) {
                tvContent.setShadowLayer(3f, 1.5f, 1.5f, Color.parseColor("#80000000"))
                btnTextShadow.alpha = 1.0f
            } else {
                tvContent.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                btnTextShadow.alpha = 0.5f
            }
        }

        // 3. Khalfiya (Highlight)
        var isHighlightEnabled = false
        btnTextHighlight?.alpha = 0.5f
        btnTextHighlight?.setOnClickListener {
            isHighlightEnabled = !isHighlightEnabled
            if (isHighlightEnabled) {
                tvContent.setBackgroundColor(Color.parseColor("#66000000")) // k7el chfaf
                tvContent.setPadding(20, 20, 20, 20)
                btnTextHighlight.alpha = 1.0f
            } else {
                tvContent.background = null
                tvContent.setPadding(0, 0, 0, 0)
                btnTextHighlight.alpha = 0.5f
            }
        }
        // =====================================

        
        val btnPickImage = TextView(context).apply {
            val padH = (16 * context.resources.displayMetrics.density).toInt()
            val padV = (8 * context.resources.displayMetrics.density).toInt()
            val margin = (4 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, margin, margin, margin)
            }
            setPadding(padH, padV, padH, padV)
            text = "صورة 📷"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#0284C7"))
            }
            setOnClickListener { onPickImageRequest?.invoke() }
        }
        layoutColorsContainer.addView(btnPickImage)

        val customBgColorBtn = ImageView(context).apply {
            val size = (46 * context.resources.displayMetrics.density).toInt()
            val margin = (4 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            setImageResource(R.drawable.ic_color_picker) 
            setPadding(12, 12, 12, 12)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1F2937"))
            }
            setOnClickListener {
                val colorPickerDialog = yuku.ambilwarna.AmbilWarnaDialog(context, cardPreview.cardBackgroundColor.defaultColor, object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                    override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                        ivBackground.visibility = View.GONE
                        viewOverlay.visibility = View.GONE
                        cardPreview.setCardBackgroundColor(color)
                    }
                })
                colorPickerDialog.show()
            }
        }
        layoutColorsContainer.addView(customBgColorBtn)

        val colors = arrayOf("#0F172A", "#1E3A8A", "#064E3B", "#581C87", "#7F1D1D", "#F59E0B", "#10B981", "#F1F5F9")
        for (colorHex in colors) {
            val colorCircle = View(context).apply {
                val size = (46 * context.resources.displayMetrics.density).toInt()
                val margin = (4 * context.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(colorHex))
                    if (colorHex == "#F1F5F9") setStroke(3, Color.parseColor("#94A3B8"))
                }
                setOnClickListener {
                    ivBackground.visibility = View.GONE
                    viewOverlay.visibility = View.GONE
                    cardPreview.setCardBackgroundColor(Color.parseColor(colorHex))
                    
                    if (colorHex == "#F1F5F9") {
                        tvContent.setTextColor(Color.parseColor("#0F172A"))
                    } else {
                        tvContent.setTextColor(Color.WHITE)
                    }
                }
            }
            layoutColorsContainer.addView(colorCircle)
        }

        val customTextColorBtn = TextView(context).apply {
            val padH = (16 * context.resources.displayMetrics.density).toInt()
            val padV = (8 * context.resources.displayMetrics.density).toInt()
            val margin = (4 * context.resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, margin, margin, margin)
            }
            setPadding(padH, padV, padH, padV)
            text = "لون النص 🎨"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                cornerRadius = 50f
                setColor(Color.parseColor("#1F2937"))
            }
            setOnClickListener {
                val colorPickerDialog = yuku.ambilwarna.AmbilWarnaDialog(context, tvContent.currentTextColor, object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                    override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                        tvContent.setTextColor(color)
                    }
                })
                colorPickerDialog.show()
            }
        }
        layoutFontsContainer.addView(customTextColorBtn)

        val fontList = listOf(
            FontItem("الرقعة", R.font.arefruqaa),
            FontItem("كايرو", R.font.cairoplay),
            FontItem("كوفام", R.font.kufam),
            FontItem("مرحي", R.font.marhey),
            FontItem("تجوّل", R.font.tajawal),
            FontItem("نستعليق", R.font.notonastaliqurdu),
            FontItem("بلاكا", R.font.blakahollow),
            FontItem("عريض", R.font.oi)
        )

        for (item in fontList) {
            val tvFont = TextView(context).apply {
                val padH = (20 * context.resources.displayMetrics.density).toInt()
                val padV = (8 * context.resources.displayMetrics.density).toInt()
                val margin = (4 * context.resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setPadding(padH, padV, padH, padV)
                text = item.displayName
                textSize = 15f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = 50f 
                    setColor(Color.parseColor("#1F2937"))
                }
                
                try {
                    typeface = ResourcesCompat.getFont(context, item.fontResId)
                } catch (e: Exception) {}
                
                setOnClickListener {
                    try {
                        tvContent.typeface = ResourcesCompat.getFont(context, item.fontResId)
                    } catch (e: Exception) {}
                }
            }
            layoutFontsContainer.addView(tvFont)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        
        btnSaveImage.setOnClickListener {
            val bitmap = createBitmapFromView(cardPreview)
            saveBitmapToGallery(context, bitmap)
        }

        btnConfirmShare.setOnClickListener {
            val bitmap = createBitmapFromView(cardPreview)
            dialog.dismiss()
            shareBitmap(context, bitmap)
        }

        dialog.show()
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        val filename = "Manchorati_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Manchorati")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(context, "تم حفظ البطاقة في المعرض", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "post_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة البطاقة عبر"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
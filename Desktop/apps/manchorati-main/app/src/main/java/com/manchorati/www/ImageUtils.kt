package com.manchorati.www

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast

object ImageUtils {

    private var activeCustomImageView: ImageView? = null
    private var activeOverlayView: View? = null
    private var activeRemoveBtn: Button? = null

    // مسجل اختيار الصورة من المعرض
    fun registerImagePicker(activity: ComponentActivity): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream: InputStream? = activity.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    activeCustomImageView?.setImageBitmap(bitmap)
                    activeCustomImageView?.visibility = View.VISIBLE
                    activeOverlayView?.visibility = View.VISIBLE
                    activeRemoveBtn?.visibility = View.VISIBLE
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
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

                Toast.makeText(context, "تم حفظ البطاقة في المعرض بنجاح بنقاء عالي", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "حدث خطأ أثناء حفظ الصورة", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "تعذر إنشاء ملف الصورة", Toast.LENGTH_SHORT).show()
        }
    }
    // نافذة محرر الصورة ومعاينتها
    fun showImageEditorDialog(
        context: Context,
        categoryName: String,
        content: String,
        onPickImageRequest: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
        dialog.setContentView(view)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val cardPreview = view.findViewById<CardView>(R.id.cardPreviewContainer)
        val ivBackground = view.findViewById<ImageView>(R.id.ivCustomBackground)
        val viewOverlay = view.findViewById<View>(R.id.viewOverlay)
        val tvCategory = view.findViewById<TextView>(R.id.tvPreviewCategory)
        val tvContent = view.findViewById<TextView>(R.id.tvPreviewContent)

        val btnPickImage = view.findViewById<Button>(R.id.btnPickImage)
        val btnRemoveImage = view.findViewById<Button>(R.id.btnRemoveImage)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelPreview)
        val btnShare = view.findViewById<Button>(R.id.btnConfirmShare)
        val btnOpenFontPicker = view.findViewById<Button>(R.id.btnOpenFontPicker)

        activeCustomImageView = ivBackground
        activeOverlayView = viewOverlay
        activeRemoveBtn = btnRemoveImage

        tvCategory.text = "• $categoryName •"
        tvContent.text = content

        var currentTextSize = 18f

        // فتح معرض الصور
        btnPickImage.setOnClickListener {
            onPickImageRequest?.invoke()
        }

        // إزالة الصورة المختارة
        btnRemoveImage.setOnClickListener {
            ivBackground.setImageDrawable(null)
            ivBackground.visibility = View.GONE
            viewOverlay.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
        }

        // فتح نافذة اختيار الخطوط المنبثقة
        btnOpenFontPicker.setOnClickListener {
            showFontSelectionDialog(context) { selectedTypeface ->
                tvContent.typeface = selectedTypeface
            }
        }
        val btnSaveImage = view.findViewById<Button>(R.id.btnSaveImage)

        // حفظ الصورة في المعرض
        btnSaveImage.setOnClickListener {
            val bitmap = createBitmapFromView(cardPreview)
            saveBitmapToGallery(context, bitmap)
        }

        // ألوان النص
        view.findViewById<View>(R.id.btnTextColorWhite).setOnClickListener {
            tvContent.setTextColor(Color.WHITE)
        }
        view.findViewById<View>(R.id.btnTextColorYellow).setOnClickListener {
            tvContent.setTextColor(Color.parseColor("#FACC15"))
        }
        view.findViewById<View>(R.id.btnTextColorBlack).setOnClickListener {
            tvContent.setTextColor(Color.parseColor("#0F172A"))
        }

        // تكبير وتصغير الخط
        view.findViewById<TextView>(R.id.btnPreviewBigger).setOnClickListener {
            if (currentTextSize < 30f) {
                currentTextSize += 2f
                tvContent.textSize = currentTextSize
            }
        }
        view.findViewById<TextView>(R.id.btnPreviewSmaller).setOnClickListener {
            if (currentTextSize > 12f) {
                currentTextSize -= 2f
                tvContent.textSize = currentTextSize
            }
        }

        // ألوان الخلفيات السادة
        fun setBgColor(hexBg: String) {
            ivBackground.setImageDrawable(null)
            ivBackground.visibility = View.GONE
            viewOverlay.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
            cardPreview.setCardBackgroundColor(Color.parseColor(hexBg))
        }

        view.findViewById<View>(R.id.btnColorDark).setOnClickListener { setBgColor("#0F172A") }
        view.findViewById<View>(R.id.btnColorBlue).setOnClickListener { setBgColor("#1E3A8A") }
        view.findViewById<View>(R.id.btnColorGreen).setOnClickListener { setBgColor("#064E3B") }
        view.findViewById<View>(R.id.btnColorPurple).setOnClickListener { setBgColor("#581C87") }
        view.findViewById<View>(R.id.btnColorDarkRed).setOnClickListener { setBgColor("#7F1D1D") }
        view.findViewById<View>(R.id.btnColorLight).setOnClickListener { setBgColor("#F1F5F9") }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnShare.setOnClickListener {
            val bitmap = createBitmapFromView(cardPreview)
            dialog.dismiss()
            shareBitmap(context, bitmap)
        }

        dialog.show()
    }

    // دالة عرض قائمة الخطوط المنبثقة
    private data class FontItem(val displayName: String, val fontResId: Int)

    private fun showFontSelectionDialog(context: Context, onFontSelected: (Typeface?) -> Unit) {
        val fontList = listOf(
            FontItem("خط الرقعة (Aref Ruqaa)", R.font.arefruqaa),
            FontItem("خط كايرو (Cairo Play)", R.font.cairoplay),
            FontItem("خط كوفام (Kufam)", R.font.kufam),
            FontItem("خط مرحي (Marhey)", R.font.marhey),
            FontItem("خط تجوّل (Tajawal)", R.font.tajawal),
            FontItem("خط نستعليق (Urdu)", R.font.notonastaliqurdu),
            FontItem("خط بلاكا (Blaka Hollow)", R.font.blakahollow),
            FontItem("خط عريض (Oi)", R.font.oi)
        )

        val dialog = Dialog(context)
        val recyclerView = RecyclerView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(context)
            setPadding(20, 24, 20, 24)
        }

        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val rowView = LayoutInflater.from(parent.context).inflate(R.layout.item_font_choice, parent, false)
                return object : RecyclerView.ViewHolder(rowView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val item = fontList[position]
                val tvName = holder.itemView.findViewById<TextView>(R.id.tvFontPreviewName)
                tvName.text = item.displayName

                try {
                    val tf = ResourcesCompat.getFont(context, item.fontResId)
                    tvName.typeface = tf
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                holder.itemView.setOnClickListener {
                    try {
                        val tf = ResourcesCompat.getFont(context, item.fontResId)
                        onFontSelected(tf)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    dialog.dismiss()
                }
            }

            override fun getItemCount(): Int = fontList.size
        }

        dialog.setContentView(recyclerView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)
        dialog.show()
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
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
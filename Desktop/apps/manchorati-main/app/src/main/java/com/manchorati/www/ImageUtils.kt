package com.manchorati.www

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs

object ImageUtils {

    var onImagePickedCallback: ((Bitmap) -> Unit)? = null

    fun handleImageUri(context: Context, uri: Uri) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                onImagePickedCallback?.invoke(bitmap)
            } else {
                Toast.makeText(context, "تعذر قراءة ملف الصورة", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء تحميل الصورة", Toast.LENGTH_SHORT).show()
        }
    }

    fun registerImagePicker(activity: ComponentActivity): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleImageUri(activity, it) }
        }
    }

    private data class FontItem(val displayName: String, val fontResId: Int)

    fun showImageEditorDialog(
        context: Context,
        categoryName: String,
        content: String,
        onPickImageRequest: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
        dialog.setContentView(view)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val density = context.resources.displayMetrics.density

        // --- Core UI Views ---
        val layoutEditorRoot = view.findViewById<LinearLayout>(R.id.layoutEditorRoot)
        val tvEditorTitle = view.findViewById<TextView>(R.id.tvEditorTitle)
        val tvEditorSubtitle = view.findViewById<TextView>(R.id.tvEditorSubtitle)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseDialog)
        val btnThemeToggle = view.findViewById<ImageView>(R.id.btnThemeToggle)
        val btnResetAll = view.findViewById<ImageView>(R.id.btnResetAll)
        val btnSaveImage = view.findViewById<View>(R.id.btnSaveImage)
        val btnConfirmShare = view.findViewById<View>(R.id.btnConfirmShare)

        val cardPreview = view.findViewById<CardView>(R.id.cardPreviewContainer)
        val ivBackground = view.findViewById<ImageView>(R.id.ivCustomBackground)
        val viewOverlay = view.findViewById<View>(R.id.viewOverlay)
        val viewFilterTint = view.findViewById<View>(R.id.viewFilterTint)
        val viewCardBorder = view.findViewById<View>(R.id.viewCardBorder)
        val tvCategory = view.findViewById<TextView>(R.id.tvPreviewCategory)
        val tvQuoteTop = view.findViewById<TextView>(R.id.tvQuoteTop)
        val tvContent = view.findViewById<TextView>(R.id.tvPreviewContent)
        val tvQuoteBottom = view.findViewById<TextView>(R.id.tvQuoteBottom)
        val tvPreviewAuthor = view.findViewById<TextView>(R.id.tvPreviewAuthor)
        val tvWatermark = view.findViewById<TextView>(R.id.tvPreviewWatermark)

        val layoutToolSubPanel = view.findViewById<FrameLayout>(R.id.layoutToolSubPanel)
        val dividerMiddle = view.findViewById<View>(R.id.dividerMiddle)
        val dividerBottom = view.findViewById<View>(R.id.dividerBottom)
        val layoutBottomDockContainer = view.findViewById<LinearLayout>(R.id.layoutBottomDockContainer)
        val viewBottomInsetSpacer = view.findViewById<View>(R.id.viewBottomInsetSpacer)

        // Sub Panels
        val panelBackground = view.findViewById<LinearLayout>(R.id.panelBackground)
        val panelFont = view.findViewById<LinearLayout>(R.id.panelFont)
        val panelTextColor = view.findViewById<LinearLayout>(R.id.panelTextColor)
        val panelRatio = view.findViewById<LinearLayout>(R.id.panelRatio)
        val panelFrame = view.findViewById<LinearLayout>(R.id.panelFrame)
        val panelPresets = view.findViewById<LinearLayout>(R.id.panelPresets)

        // Bottom Dock Tabs & Frames
        val tabToolBackground = view.findViewById<LinearLayout>(R.id.tabToolBackground)
        val tabToolFont = view.findViewById<LinearLayout>(R.id.tabToolFont)
        val tabToolColor = view.findViewById<LinearLayout>(R.id.tabToolColor)
        val tabToolRatio = view.findViewById<LinearLayout>(R.id.tabToolRatio)
        val tabToolFrame = view.findViewById<LinearLayout>(R.id.tabToolFrame)
        val tabToolPresets = view.findViewById<LinearLayout>(R.id.tabToolPresets)

        val frameIconBackground = view.findViewById<FrameLayout>(R.id.frameIconBackground)
        val frameIconFont = view.findViewById<FrameLayout>(R.id.frameIconFont)
        val frameIconColor = view.findViewById<FrameLayout>(R.id.frameIconColor)
        val frameIconRatio = view.findViewById<FrameLayout>(R.id.frameIconRatio)
        val frameIconFrame = view.findViewById<FrameLayout>(R.id.frameIconFrame)
        val frameIconPresets = view.findViewById<FrameLayout>(R.id.frameIconPresets)

        val ivIconBackground = view.findViewById<ImageView>(R.id.ivIconBackground)
        val ivIconFont = view.findViewById<ImageView>(R.id.ivIconFont)
        val ivIconColor = view.findViewById<ImageView>(R.id.ivIconColor)
        val ivIconRatio = view.findViewById<ImageView>(R.id.ivIconRatio)
        val ivIconFrame = view.findViewById<ImageView>(R.id.ivIconFrame)
        val ivIconPresets = view.findViewById<ImageView>(R.id.ivIconPresets)

        val tvLabelBackground = view.findViewById<TextView>(R.id.tvLabelBackground)
        val tvLabelFont = view.findViewById<TextView>(R.id.tvLabelFont)
        val tvLabelColor = view.findViewById<TextView>(R.id.tvLabelColor)
        val tvLabelRatio = view.findViewById<TextView>(R.id.tvLabelRatio)
        val tvLabelFrame = view.findViewById<TextView>(R.id.tvLabelFrame)
        val tvLabelPresets = view.findViewById<TextView>(R.id.tvLabelPresets)

        // Insets handling to raise the bottom dock safely above phone navigation buttons
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val navBarHeight = insets.bottom
            val safeHeight = maxOf(navBarHeight, (28 * density).toInt())
            viewBottomInsetSpacer.layoutParams = viewBottomInsetSpacer.layoutParams.apply {
                height = safeHeight
            }
            viewBottomInsetSpacer.requestLayout()
            windowInsets
        }

        // Labels in panels for dynamic theming
        val tvDimTitle = view.findViewById<TextView>(R.id.tvDimTitle)
        val tvBlurTitle = view.findViewById<TextView>(R.id.tvBlurTitle)
        val tvAlignTitle = view.findViewById<TextView>(R.id.tvAlignTitle)
        val tvRadiusTitle = view.findViewById<TextView>(R.id.tvRadiusTitle)
        val tvPresetsSubtitle = view.findViewById<TextView>(R.id.tvPresetsSubtitle)

        // Setup Initial Texts
        tvCategory.text = if (categoryName.isNotBlank()) "• $categoryName •" else "• منشوراتي •"
        tvContent.text = content

        // ==============================================================
        // 1. STATE & DARK/LIGHT THEME SYSTEM ("dir b7sab dark mod w like them")
        // ==============================================================
        val themePrefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        var isEditorDarkMode = themePrefs.getBoolean("is_dark_mode", isSystemDark)

        var activeTabIndex = 0

        fun updateBottomDockUI() {
            val frames = listOf(frameIconBackground, frameIconFont, frameIconColor, frameIconRatio, frameIconFrame, frameIconPresets)
            val icons = listOf(ivIconBackground, ivIconFont, ivIconColor, ivIconRatio, ivIconFrame, ivIconPresets)
            val labels = listOf(tvLabelBackground, tvLabelFont, tvLabelColor, tvLabelRatio, tvLabelFrame, tvLabelPresets)

            val inactiveColor = if (isEditorDarkMode) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            val activeColor = Color.parseColor("#0284C7")
            val activeLabelColor = if (isEditorDarkMode) Color.parseColor("#38BDF8") else Color.parseColor("#0284C7")

            for (i in frames.indices) {
                if (i == activeTabIndex) {
                    frames[i].background = GradientDrawable().apply {
                        cornerRadius = 10f * density
                        setColor(activeColor)
                    }
                    icons[i].setColorFilter(Color.WHITE)
                    labels[i].setTextColor(activeLabelColor)
                    labels[i].setTypeface(null, Typeface.BOLD)
                } else {
                    frames[i].background = null
                    icons[i].setColorFilter(inactiveColor)
                    labels[i].setTextColor(inactiveColor)
                    labels[i].setTypeface(null, Typeface.NORMAL)
                }
            }
        }

        fun applyEditorTheme(isDark: Boolean) {
            isEditorDarkMode = isDark
            if (isDark) {
                layoutEditorRoot.setBackgroundColor(Color.parseColor("#0B0F19"))
                layoutToolSubPanel.setBackgroundColor(Color.parseColor("#131C2E"))
                layoutBottomDockContainer.setBackgroundColor(Color.parseColor("#0B0F19"))
                viewBottomInsetSpacer.setBackgroundColor(Color.parseColor("#0B0F19"))
                dividerMiddle.setBackgroundColor(Color.parseColor("#1E293B"))
                dividerBottom.setBackgroundColor(Color.parseColor("#1E293B"))

                tvEditorTitle.setTextColor(Color.parseColor("#F8FAFC"))
                tvEditorSubtitle.setTextColor(Color.parseColor("#94A3B8"))
                btnThemeToggle.setColorFilter(Color.parseColor("#F59E0B"))
                btnResetAll.setColorFilter(Color.parseColor("#94A3B8"))

                tvDimTitle.setTextColor(Color.parseColor("#CBD5E1"))
                tvBlurTitle.setTextColor(Color.parseColor("#CBD5E1"))
                tvAlignTitle.setTextColor(Color.parseColor("#CBD5E1"))
                tvRadiusTitle.setTextColor(Color.parseColor("#CBD5E1"))
                tvPresetsSubtitle.setTextColor(Color.parseColor("#CBD5E1"))
            } else {
                layoutEditorRoot.setBackgroundColor(Color.parseColor("#F1F5F9"))
                layoutToolSubPanel.setBackgroundColor(Color.WHITE)
                layoutBottomDockContainer.setBackgroundColor(Color.parseColor("#F8FAFC"))
                viewBottomInsetSpacer.setBackgroundColor(Color.parseColor("#F8FAFC"))
                dividerMiddle.setBackgroundColor(Color.parseColor("#E2E8F0"))
                dividerBottom.setBackgroundColor(Color.parseColor("#E2E8F0"))

                tvEditorTitle.setTextColor(Color.parseColor("#0F172A"))
                tvEditorSubtitle.setTextColor(Color.parseColor("#64748B"))
                btnThemeToggle.setColorFilter(Color.parseColor("#0284C7"))
                btnResetAll.setColorFilter(Color.parseColor("#64748B"))

                tvDimTitle.setTextColor(Color.parseColor("#334155"))
                tvBlurTitle.setTextColor(Color.parseColor("#334155"))
                tvAlignTitle.setTextColor(Color.parseColor("#334155"))
                tvRadiusTitle.setTextColor(Color.parseColor("#334155"))
                tvPresetsSubtitle.setTextColor(Color.parseColor("#334155"))
            }
            updateBottomDockUI()
        }

        applyEditorTheme(isEditorDarkMode)

        btnThemeToggle.setOnClickListener {
            applyEditorTheme(!isEditorDarkMode)
        }

        fun selectTab(index: Int) {
            activeTabIndex = index
            panelBackground.visibility = if (index == 0) View.VISIBLE else View.GONE
            panelFont.visibility = if (index == 1) View.VISIBLE else View.GONE
            panelTextColor.visibility = if (index == 2) View.VISIBLE else View.GONE
            panelRatio.visibility = if (index == 3) View.VISIBLE else View.GONE
            panelFrame.visibility = if (index == 4) View.VISIBLE else View.GONE
            panelPresets.visibility = if (index == 5) View.VISIBLE else View.GONE
            updateBottomDockUI()
        }

        tabToolBackground.setOnClickListener { selectTab(0) }
        tabToolFont.setOnClickListener { selectTab(1) }
        tabToolColor.setOnClickListener { selectTab(2) }
        tabToolRatio.setOnClickListener { selectTab(3) }
        tabToolFrame.setOnClickListener { selectTab(4) }
        tabToolPresets.setOnClickListener { selectTab(5) }

        // ==============================================================
        // 2. BACKGROUND IMAGE & PHOTO SYSTEM ("zid image bax y9dar ydir image fi background")
        // ==============================================================
        var originalCustomBitmap: Bitmap? = null
        var currentBlurLevel = 0 // 0: None, 1: Soft (8), 2: Deep (16)
        var currentDimHex = "#80000000"

        fun applyBackgroundEffects() {
            val base = originalCustomBitmap
            if (base != null) {
                val processedBitmap = when (currentBlurLevel) {
                    1 -> blurBitmap(base, 8)
                    2 -> blurBitmap(base, 16)
                    else -> base
                }
                ivBackground.setImageBitmap(processedBitmap)
                ivBackground.visibility = View.VISIBLE
                viewOverlay.visibility = View.VISIBLE
                viewOverlay.setBackgroundColor(Color.parseColor(currentDimHex))
                cardPreview.background = null
            } else {
                ivBackground.visibility = View.GONE
                viewOverlay.visibility = View.GONE
            }
        }

        // Register callback for image picker
        onImagePickedCallback = { bitmap ->
            originalCustomBitmap = bitmap
            applyBackgroundEffects()
            tvContent.setTextColor(Color.WHITE)
            tvCategory.setTextColor(Color.parseColor("#38BDF8"))
            tvWatermark.setTextColor(Color.parseColor("#CBD5E1"))
            Toast.makeText(context, "تم تطبيق صورة الخلفية بنجاح", Toast.LENGTH_SHORT).show()
        }

        dialog.setOnDismissListener {
            onImagePickedCallback = null
        }

        val layoutColorsContainer = view.findViewById<LinearLayout>(R.id.layoutColorsContainer)

        // 1. Pick Photo button (Gallery)
        val btnPickImage = TextView(context).apply {
            val padH = (14 * density).toInt()
            val padV = (8 * density).toInt()
            val margin = (4 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, margin, margin, margin)
            }
            setPadding(padH, padV, padH, padV)
            text = context.getString(R.string.my_photo)
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundResource(R.drawable.bg_tool_chip_active)

            setOnClickListener {
                if (onPickImageRequest != null) {
                    onPickImageRequest.invoke()
                } else {
                    Toast.makeText(context, context.getString(R.string.pick_photo_not_supported), Toast.LENGTH_SHORT).show()
                }
            }
        }
        layoutColorsContainer.addView(btnPickImage)

        // 2. Remove Image button
        val btnRemoveImage = TextView(context).apply {
            val padH = (12 * density).toInt()
            val padV = (8 * density).toInt()
            val margin = (4 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, margin, margin, margin)
            }
            setPadding(padH, padV, padH, padV)
            text = context.getString(R.string.remove_photo)
            textSize = 11f
            setTextColor(Color.parseColor("#CBD5E1"))
            setBackgroundResource(R.drawable.bg_tool_chip_idle)

            setOnClickListener {
                originalCustomBitmap = null
                ivBackground.setImageDrawable(null)
                ivBackground.visibility = View.GONE
                viewOverlay.visibility = View.GONE
                cardPreview.background = null
                cardPreview.setCardBackgroundColor(Color.parseColor("#1E293B"))
                tvContent.setTextColor(Color.WHITE)
                Toast.makeText(context, context.getString(R.string.bg_removed), Toast.LENGTH_SHORT).show()
            }
        }
        layoutColorsContainer.addView(btnRemoveImage)

        // 3. Custom Color Picker button
        val customBgColorBtn = ImageView(context).apply {
            val size = (40 * density).toInt()
            val margin = (4 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            setImageResource(R.drawable.ic_color_picker)
            setPadding(8, 8, 8, 8)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            setOnClickListener {
                val colorPickerDialog = yuku.ambilwarna.AmbilWarnaDialog(
                    context,
                    cardPreview.cardBackgroundColor.defaultColor,
                    object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                        override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                            originalCustomBitmap = null
                            ivBackground.visibility = View.GONE
                            viewOverlay.visibility = View.GONE
                            cardPreview.background = null
                            cardPreview.setCardBackgroundColor(color)
                        }
                    }
                )
                colorPickerDialog.show()
            }
        }
        layoutColorsContainer.addView(customBgColorBtn)

        // 4. Curated Wallpapers & Gradients Presets
        data class GradientPreset(val name: String, val colors: IntArray, val isLight: Boolean = false)
        val gradients = listOf(
            GradientPreset("Luxury Dark", intArrayOf(Color.parseColor("#111827"), Color.parseColor("#030712"))),
            GradientPreset("Parchment Paper", intArrayOf(Color.parseColor("#F6F0EA"), Color.parseColor("#DFD2C0")), isLight = true),
            GradientPreset("Cyber Neon", intArrayOf(Color.parseColor("#091A28"), Color.parseColor("#022C43"))),
            GradientPreset("Aurora Violet", intArrayOf(Color.parseColor("#2D1154"), Color.parseColor("#0F3854"))),
            GradientPreset("Sunset Chill", intArrayOf(Color.parseColor("#4A0E2E"), Color.parseColor("#9A3412"))),
            GradientPreset("Emerald Forest", intArrayOf(Color.parseColor("#064E3B"), Color.parseColor("#047857"))),
            GradientPreset("Ocean Wave", intArrayOf(Color.parseColor("#0C4A6E"), Color.parseColor("#0284C7"))),
            GradientPreset("Clean Cream", intArrayOf(Color.parseColor("#FEF3C7"), Color.parseColor("#FDE68A")), isLight = true),
            GradientPreset("Velvet Noir", intArrayOf(Color.parseColor("#18181B"), Color.parseColor("#27272A"))),
            GradientPreset("Minimal White", intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#F1F5F9")), isLight = true)
        )

        for (preset in gradients) {
            val circleView = View(context).apply {
                val size = (38 * density).toInt()
                val margin = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, preset.colors).apply {
                    shape = GradientDrawable.OVAL
                    if (preset.isLight) setStroke(2, Color.parseColor("#94A3B8"))
                }
                setOnClickListener {
                    originalCustomBitmap = null
                    ivBackground.visibility = View.GONE
                    viewOverlay.visibility = View.GONE

                    cardPreview.setCardBackgroundColor(Color.TRANSPARENT)
                    cardPreview.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, preset.colors).apply {
                        cornerRadius = cardPreview.radius
                    }

                    if (preset.isLight) {
                        tvContent.setTextColor(Color.parseColor("#0F172A"))
                        tvCategory.setTextColor(Color.parseColor("#0284C7"))
                        tvWatermark.setTextColor(Color.parseColor("#64748B"))
                    } else {
                        tvContent.setTextColor(Color.WHITE)
                        tvCategory.setTextColor(Color.parseColor("#38BDF8"))
                        tvWatermark.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
            }
            layoutColorsContainer.addView(circleView)
        }

        // Dim Buttons
        val btnDim0 = view.findViewById<TextView>(R.id.btnDim0)
        val btnDim30 = view.findViewById<TextView>(R.id.btnDim30)
        val btnDim50 = view.findViewById<TextView>(R.id.btnDim50)
        val btnDim75 = view.findViewById<TextView>(R.id.btnDim75)
        val dimButtons = listOf(btnDim0, btnDim30, btnDim50, btnDim75)

        fun updateDim(selected: TextView, hex: String) {
            dimButtons.forEach {
                it.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                it.setTextColor(Color.parseColor("#CBD5E1"))
                it.setTypeface(null, Typeface.NORMAL)
            }
            selected.setBackgroundResource(R.drawable.bg_tool_chip_active)
            selected.setTextColor(Color.WHITE)
            selected.setTypeface(null, Typeface.BOLD)
            currentDimHex = hex
            viewOverlay.setBackgroundColor(Color.parseColor(hex))
            viewOverlay.visibility = if (ivBackground.visibility == View.VISIBLE) View.VISIBLE else View.GONE
        }

        btnDim0.setOnClickListener { updateDim(btnDim0, "#00000000") }
        btnDim30.setOnClickListener { updateDim(btnDim30, "#4D000000") }
        btnDim50.setOnClickListener { updateDim(btnDim50, "#80000000") }
        btnDim75.setOnClickListener { updateDim(btnDim75, "#BF000000") }

        // Blur Buttons
        val btnBlurNone = view.findViewById<TextView>(R.id.btnBlurNone)
        val btnBlurSoft = view.findViewById<TextView>(R.id.btnBlurSoft)
        val btnBlurDeep = view.findViewById<TextView>(R.id.btnBlurDeep)
        val blurButtons = listOf(btnBlurNone, btnBlurSoft, btnBlurDeep)

        fun updateBlur(selected: TextView, level: Int) {
            blurButtons.forEach {
                it.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                it.setTextColor(Color.parseColor("#CBD5E1"))
                it.setTypeface(null, Typeface.NORMAL)
            }
            selected.setBackgroundResource(R.drawable.bg_tool_chip_active)
            selected.setTextColor(Color.WHITE)
            selected.setTypeface(null, Typeface.BOLD)
            currentBlurLevel = level
            applyBackgroundEffects()
        }

        btnBlurNone.setOnClickListener { updateBlur(btnBlurNone, 0) }
        btnBlurSoft.setOnClickListener { updateBlur(btnBlurSoft, 1) }
        btnBlurDeep.setOnClickListener { updateBlur(btnBlurDeep, 2) }

        // Photo Filters / Color Tint
        val btnTintNone = view.findViewById<TextView>(R.id.btnTintNone)
        val btnTintWarm = view.findViewById<TextView>(R.id.btnTintWarm)
        val btnTintCold = view.findViewById<TextView>(R.id.btnTintCold)
        val btnTintEmerald = view.findViewById<TextView>(R.id.btnTintEmerald)
        val btnTintRose = view.findViewById<TextView>(R.id.btnTintRose)
        val tintButtons = listOf(btnTintNone, btnTintWarm, btnTintCold, btnTintEmerald, btnTintRose)

        fun updateTint(selected: TextView, colorHex: String) {
            tintButtons.forEach {
                it.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                it.setTextColor(Color.parseColor("#CBD5E1"))
                it.setTypeface(null, Typeface.NORMAL)
            }
            selected.setBackgroundResource(R.drawable.bg_tool_chip_active)
            selected.setTextColor(Color.WHITE)
            selected.setTypeface(null, Typeface.BOLD)

            if (colorHex == "#00000000") {
                viewFilterTint.visibility = View.GONE
            } else {
                viewFilterTint.visibility = View.VISIBLE
                viewFilterTint.setBackgroundColor(Color.parseColor(colorHex))
            }
        }

        btnTintNone.setOnClickListener { updateTint(btnTintNone, "#00000000") }
        btnTintWarm.setOnClickListener { updateTint(btnTintWarm, "#40F59E0B") }
        btnTintCold.setOnClickListener { updateTint(btnTintCold, "#400284C7") }
        btnTintEmerald.setOnClickListener { updateTint(btnTintEmerald, "#40047857") }
        btnTintRose.setOnClickListener { updateTint(btnTintRose, "#409F1239") }

        // ==============================================================
        // 3. FONTS, TEXT SIZE, BOLD & ITALIC, LINE & LETTER SPACING
        // ==============================================================
        val layoutFontsContainer = view.findViewById<LinearLayout>(R.id.layoutFontsContainer)
        val fontList = listOf(
            FontItem(context.getString(R.string.font_tajawal), R.font.tajawal),
            FontItem(context.getString(R.string.font_cairo), R.font.cairoplay),
            FontItem(context.getString(R.string.font_ruqaa), R.font.arefruqaa),
            FontItem(context.getString(R.string.font_kufam), R.font.kufam),
            FontItem(context.getString(R.string.font_marhey), R.font.marhey),
            FontItem(context.getString(R.string.font_nastaliq), R.font.notonastaliqurdu),
            FontItem(context.getString(R.string.font_blaka), R.font.blakahollow),
            FontItem(context.getString(R.string.font_bold), R.font.oi)
        )

        var currentSelectedTypeface: Typeface? = null
        var isTextBold = false
        var isTextItalic = false

        fun applyFontStyle() {
            var style = Typeface.NORMAL
            if (isTextBold && isTextItalic) style = Typeface.BOLD_ITALIC
            else if (isTextBold) style = Typeface.BOLD
            else if (isTextItalic) style = Typeface.ITALIC

            if (currentSelectedTypeface != null) {
                tvContent.setTypeface(currentSelectedTypeface, style)
            } else {
                tvContent.setTypeface(Typeface.DEFAULT, style)
            }
        }

        val fontViewList = mutableListOf<TextView>()
        for (item in fontList) {
            val tvFont = TextView(context).apply {
                val padH = (14 * density).toInt()
                val padV = (6 * density).toInt()
                val margin = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setPadding(padH, padV, padH, padV)
                text = item.displayName
                textSize = 12f
                setTextColor(if (item.fontResId == R.font.tajawal) Color.WHITE else Color.parseColor("#CBD5E1"))
                setBackgroundResource(if (item.fontResId == R.font.tajawal) R.drawable.bg_tool_chip_active else R.drawable.bg_tool_chip_idle)

                try {
                    typeface = ResourcesCompat.getFont(context, item.fontResId)
                } catch (_: Exception) {}

                setOnClickListener {
                    fontViewList.forEach {
                        it.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                        it.setTextColor(Color.parseColor("#CBD5E1"))
                    }
                    setBackgroundResource(R.drawable.bg_tool_chip_active)
                    setTextColor(Color.WHITE)
                    try {
                        currentSelectedTypeface = ResourcesCompat.getFont(context, item.fontResId)
                        applyFontStyle()
                    } catch (_: Exception) {}
                }
            }
            fontViewList.add(tvFont)
            layoutFontsContainer.addView(tvFont)
        }

        // Font Size Adjustments
        var currentFontSize = 18f
        val tvCurrentFontSize = view.findViewById<TextView>(R.id.tvCurrentFontSize)
        val btnFontSizeDown = view.findViewById<TextView>(R.id.btnFontSizeDown)
        val btnFontSizeUp = view.findViewById<TextView>(R.id.btnFontSizeUp)

        btnFontSizeDown.setOnClickListener {
            if (currentFontSize > 12f) {
                currentFontSize -= 2f
                tvContent.textSize = currentFontSize
                tvCurrentFontSize.text = currentFontSize.toInt().toString()
            }
        }

        btnFontSizeUp.setOnClickListener {
            if (currentFontSize < 34f) {
                currentFontSize += 2f
                tvContent.textSize = currentFontSize
                tvCurrentFontSize.text = currentFontSize.toInt().toString()
            }
        }

        // Bold & Italic
        val btnToggleBold = view.findViewById<TextView>(R.id.btnToggleBold)
        val btnToggleItalic = view.findViewById<TextView>(R.id.btnToggleItalic)

        btnToggleBold.setOnClickListener {
            isTextBold = !isTextBold
            btnToggleBold.setBackgroundResource(if (isTextBold) R.drawable.bg_tool_chip_active else R.drawable.bg_tool_chip_idle)
            btnToggleBold.setTextColor(if (isTextBold) Color.WHITE else Color.parseColor("#CBD5E1"))
            applyFontStyle()
        }

        btnToggleItalic.setOnClickListener {
            isTextItalic = !isTextItalic
            btnToggleItalic.setBackgroundResource(if (isTextItalic) R.drawable.bg_tool_chip_active else R.drawable.bg_tool_chip_idle)
            btnToggleItalic.setTextColor(if (isTextItalic) Color.WHITE else Color.parseColor("#CBD5E1"))
            applyFontStyle()
        }

        // Line Spacing
        val btnLineSpacingNormal = view.findViewById<TextView>(R.id.btnLineSpacingNormal)
        val btnLineSpacingWide = view.findViewById<TextView>(R.id.btnLineSpacingWide)

        btnLineSpacingNormal.setOnClickListener {
            tvContent.setLineSpacing(5f * density, 1.0f)
            btnLineSpacingNormal.setBackgroundResource(R.drawable.bg_tool_chip_active)
            btnLineSpacingNormal.setTextColor(Color.WHITE)
            btnLineSpacingNormal.setTypeface(null, Typeface.BOLD)

            btnLineSpacingWide.setBackgroundResource(R.drawable.bg_tool_chip_idle)
            btnLineSpacingWide.setTextColor(Color.parseColor("#CBD5E1"))
            btnLineSpacingWide.setTypeface(null, Typeface.NORMAL)
        }

        btnLineSpacingWide.setOnClickListener {
            tvContent.setLineSpacing(12f * density, 1.0f)
            btnLineSpacingWide.setBackgroundResource(R.drawable.bg_tool_chip_active)
            btnLineSpacingWide.setTextColor(Color.WHITE)
            btnLineSpacingWide.setTypeface(null, Typeface.BOLD)

            btnLineSpacingNormal.setBackgroundResource(R.drawable.bg_tool_chip_idle)
            btnLineSpacingNormal.setTextColor(Color.parseColor("#CBD5E1"))
            btnLineSpacingNormal.setTypeface(null, Typeface.NORMAL)
        }

        // Letter Spacing Toggle (New Feature!)
        var letterSpacingMode = 0
        val btnLetterSpacingToggle = view.findViewById<TextView>(R.id.btnLetterSpacingToggle)
        btnLetterSpacingToggle.setOnClickListener {
            letterSpacingMode = (letterSpacingMode + 1) % 3
            when (letterSpacingMode) {
                0 -> {
                    tvContent.letterSpacing = 0f
                    btnLetterSpacingToggle.text = context.getString(R.string.letter_spacing)
                    btnLetterSpacingToggle.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnLetterSpacingToggle.setTextColor(Color.parseColor("#CBD5E1"))
                }
                1 -> {
                    tvContent.letterSpacing = 0.04f
                    btnLetterSpacingToggle.text = context.getString(R.string.letter_spacing_medium)
                    btnLetterSpacingToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnLetterSpacingToggle.setTextColor(Color.WHITE)
                }
                2 -> {
                    tvContent.letterSpacing = 0.08f
                    btnLetterSpacingToggle.text = context.getString(R.string.letter_spacing_wide)
                    btnLetterSpacingToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnLetterSpacingToggle.setTextColor(Color.WHITE)
                }
            }
        }

        // ==============================================================
        // 4. TEXT COLOR, SHADOW, HIGHLIGHT & GRADIENT SHADER
        // ==============================================================
        val layoutTextColorsContainer = view.findViewById<LinearLayout>(R.id.layoutTextColorsContainer)

        val customTextColorBtn = TextView(context).apply {
            val padH = (12 * density).toInt()
            val padV = (6 * density).toInt()
            val margin = (3 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, margin, margin, margin)
            }
            setPadding(padH, padV, padH, padV)
            text = context.getString(R.string.color_custom)
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.bg_tool_chip_idle)
            setOnClickListener {
                val colorPickerDialog = yuku.ambilwarna.AmbilWarnaDialog(
                    context,
                    tvContent.currentTextColor,
                    object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                        override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                            tvContent.paint.shader = null
                            tvContent.setTextColor(color)
                        }
                    }
                )
                colorPickerDialog.show()
            }
        }
        layoutTextColorsContainer.addView(customTextColorBtn)

        val textColors = listOf("#FFFFFF", "#FDE68A", "#F59E0B", "#38BDF8", "#34D399", "#FB7185", "#E2E8F0", "#0F172A")
        for (colorHex in textColors) {
            val circle = View(context).apply {
                val size = (30 * density).toInt()
                val margin = (3 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(margin, margin, margin, margin)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(colorHex))
                    if (colorHex == "#FFFFFF" || colorHex == "#FDE68A") {
                        setStroke(1, Color.parseColor("#94A3B8"))
                    }
                }
                setOnClickListener {
                    tvContent.paint.shader = null
                    tvContent.setTextColor(Color.parseColor(colorHex))
                }
            }
            layoutTextColorsContainer.addView(circle)
        }

        // Gradient Text Shader (New Feature!)
        var textGradientMode = 0
        val btnTextGradientToggle = view.findViewById<TextView>(R.id.btnTextGradientToggle)
        btnTextGradientToggle.setOnClickListener {
            textGradientMode = (textGradientMode + 1) % 5
            when (textGradientMode) {
                0 -> {
                    tvContent.paint.shader = null
                    tvContent.invalidate()
                    btnTextGradientToggle.text = context.getString(R.string.text_gradient_normal)
                    btnTextGradientToggle.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnTextGradientToggle.setTextColor(Color.parseColor("#CBD5E1"))
                }
                1 -> {
                    val shader = android.graphics.LinearGradient(
                        0f, 0f, 0f, 50f * density,
                        intArrayOf(Color.parseColor("#FDE68A"), Color.parseColor("#D97706")),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                    tvContent.paint.shader = shader
                    tvContent.invalidate()
                    btnTextGradientToggle.text = context.getString(R.string.text_gradient_gold)
                    btnTextGradientToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextGradientToggle.setTextColor(Color.WHITE)
                }
                2 -> {
                    val shader = android.graphics.LinearGradient(
                        0f, 0f, 250f * density, 0f,
                        intArrayOf(Color.parseColor("#38BDF8"), Color.parseColor("#A855F7")),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                    tvContent.paint.shader = shader
                    tvContent.invalidate()
                    btnTextGradientToggle.text = context.getString(R.string.text_gradient_neon)
                    btnTextGradientToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextGradientToggle.setTextColor(Color.WHITE)
                }
                3 -> {
                    val shader = android.graphics.LinearGradient(
                        0f, 0f, 0f, 50f * density,
                        intArrayOf(Color.parseColor("#FB7185"), Color.parseColor("#F59E0B")),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                    tvContent.paint.shader = shader
                    tvContent.invalidate()
                    btnTextGradientToggle.text = context.getString(R.string.text_gradient_sunset)
                    btnTextGradientToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextGradientToggle.setTextColor(Color.WHITE)
                }
                4 -> {
                    val shader = android.graphics.LinearGradient(
                        0f, 0f, 250f * density, 0f,
                        intArrayOf(Color.parseColor("#34D399"), Color.parseColor("#0284C7")),
                        null, android.graphics.Shader.TileMode.CLAMP
                    )
                    tvContent.paint.shader = shader
                    tvContent.invalidate()
                    btnTextGradientToggle.text = context.getString(R.string.text_gradient_emerald)
                    btnTextGradientToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextGradientToggle.setTextColor(Color.WHITE)
                }
            }
        }

        // Highlight Badge
        var isHighlightEnabled = false
        val btnTextHighlight = view.findViewById<TextView>(R.id.btnTextHighlight)
        btnTextHighlight.setOnClickListener {
            isHighlightEnabled = !isHighlightEnabled
            if (isHighlightEnabled) {
                tvContent.background = GradientDrawable().apply {
                    cornerRadius = 10f * density
                    setColor(Color.parseColor("#80000000"))
                }
                tvContent.setPadding((16 * density).toInt(), (10 * density).toInt(), (16 * density).toInt(), (10 * density).toInt())
                btnTextHighlight.setBackgroundResource(R.drawable.bg_tool_chip_active)
                btnTextHighlight.setTextColor(Color.WHITE)
                btnTextHighlight.setTypeface(null, Typeface.BOLD)
            } else {
                tvContent.background = null
                tvContent.setPadding(0, 0, 0, 0)
                btnTextHighlight.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                btnTextHighlight.setTextColor(Color.parseColor("#CBD5E1"))
                btnTextHighlight.setTypeface(null, Typeface.NORMAL)
            }
        }

        // Text Shadow
        var shadowMode = 1 // 0: None, 1: Soft, 2: Deep, 3: Neon Glow
        val btnTextShadow = view.findViewById<TextView>(R.id.btnTextShadow)
        btnTextShadow.setOnClickListener {
            shadowMode = (shadowMode + 1) % 4
            when (shadowMode) {
                0 -> {
                    tvContent.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                    btnTextShadow.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnTextShadow.setTextColor(Color.parseColor("#CBD5E1"))
                    btnTextShadow.text = context.getString(R.string.text_shadow_none)
                }
                1 -> {
                    tvContent.setShadowLayer(4f, 1.5f, 1.5f, Color.parseColor("#99000000"))
                    btnTextShadow.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextShadow.setTextColor(Color.WHITE)
                    btnTextShadow.text = context.getString(R.string.text_shadow_soft)
                }
                2 -> {
                    tvContent.setShadowLayer(8f, 2.5f, 2.5f, Color.parseColor("#E6000000"))
                    btnTextShadow.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextShadow.setTextColor(Color.WHITE)
                    btnTextShadow.text = context.getString(R.string.text_shadow_deep)
                }
                3 -> {
                    tvContent.setShadowLayer(10f, 0f, 0f, Color.parseColor("#800284C7"))
                    btnTextShadow.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnTextShadow.setTextColor(Color.WHITE)
                    btnTextShadow.text = context.getString(R.string.text_shadow_glow)
                }
            }
        }

        // ==============================================================
        // 5. RATIOS & ALIGNMENT
        // ==============================================================
        val btnRatioSquare = view.findViewById<TextView>(R.id.btnRatioSquare)
        val btnRatioPortrait = view.findViewById<TextView>(R.id.btnRatioPortrait)
        val btnRatioStory = view.findViewById<TextView>(R.id.btnRatioStory)
        val btnRatioLandscape = view.findViewById<TextView>(R.id.btnRatioLandscape)
        val ratioButtons = listOf(btnRatioSquare, btnRatioPortrait, btnRatioStory, btnRatioLandscape)

        fun updateRatio(selected: TextView, heightDp: Int) {
            ratioButtons.forEach {
                it.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                it.setTextColor(Color.parseColor("#CBD5E1"))
                it.setTypeface(null, Typeface.NORMAL)
            }
            selected.setBackgroundResource(R.drawable.bg_tool_chip_active)
            selected.setTextColor(Color.WHITE)
            selected.setTypeface(null, Typeface.BOLD)
            cardPreview.layoutParams = cardPreview.layoutParams.apply {
                height = (heightDp * density).toInt()
            }
            cardPreview.requestLayout()
        }

        btnRatioSquare.setOnClickListener { updateRatio(btnRatioSquare, 270) }
        btnRatioPortrait.setOnClickListener { updateRatio(btnRatioPortrait, 340) }
        btnRatioStory.setOnClickListener { updateRatio(btnRatioStory, 400) }
        btnRatioLandscape.setOnClickListener { updateRatio(btnRatioLandscape, 220) }

        // Alignment
        val btnAlignRight = view.findViewById<ImageView>(R.id.btnAlignRight)
        val btnAlignCenter = view.findViewById<ImageView>(R.id.btnAlignCenter)
        val btnAlignLeft = view.findViewById<ImageView>(R.id.btnAlignLeft)
        val alignButtons = listOf(btnAlignRight, btnAlignCenter, btnAlignLeft)

        fun updateAlignment(selected: ImageView, gravityVal: Int) {
            tvContent.gravity = gravityVal
            alignButtons.forEach {
                it.setBackgroundResource(0)
                it.setColorFilter(Color.parseColor("#CBD5E1"))
            }
            selected.setBackgroundResource(R.drawable.bg_tool_chip_active)
            selected.setColorFilter(Color.WHITE)
        }

        btnAlignRight.setOnClickListener { updateAlignment(btnAlignRight, Gravity.END or Gravity.CENTER_VERTICAL) }
        btnAlignCenter.setOnClickListener { updateAlignment(btnAlignCenter, Gravity.CENTER) }
        btnAlignLeft.setOnClickListener { updateAlignment(btnAlignLeft, Gravity.START or Gravity.CENTER_VERTICAL) }

        // ==============================================================
        // 6. FRAME, CORNERS, BORDERS, QUOTES, AUTHOR & WATERMARK
        // ==============================================================
        val btnRadiusSharp = view.findViewById<TextView>(R.id.btnRadiusSharp)
        val btnRadiusMedium = view.findViewById<TextView>(R.id.btnRadiusMedium)
        val btnRadiusPill = view.findViewById<TextView>(R.id.btnRadiusPill)
        val radiusButtons = listOf(btnRadiusSharp, btnRadiusMedium, btnRadiusPill)

        fun updateRadius(selected: TextView, radiusDp: Float) {
            radiusButtons.forEach {
                it.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                it.setTextColor(Color.parseColor("#CBD5E1"))
                it.setTypeface(null, Typeface.NORMAL)
            }
            selected.setBackgroundResource(R.drawable.bg_tool_chip_active)
            selected.setTextColor(Color.WHITE)
            selected.setTypeface(null, Typeface.BOLD)
            cardPreview.radius = radiusDp * density
        }

        btnRadiusSharp.setOnClickListener { updateRadius(btnRadiusSharp, 0f) }
        btnRadiusMedium.setOnClickListener { updateRadius(btnRadiusMedium, 18f) }
        btnRadiusPill.setOnClickListener { updateRadius(btnRadiusPill, 30f) }

        // Card Border
        var borderIndex = 0 // 0: None, 1: Subtle White, 2: Royal Gold, 3: Glowing Cyan
        val btnCardBorderToggle = view.findViewById<TextView>(R.id.btnCardBorderToggle)

        fun applyCardBorder() {
            when (borderIndex) {
                0 -> {
                    viewCardBorder.visibility = View.GONE
                    btnCardBorderToggle.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnCardBorderToggle.setTextColor(Color.parseColor("#CBD5E1"))
                    btnCardBorderToggle.text = context.getString(R.string.card_border_none)
                }
                1 -> {
                    viewCardBorder.visibility = View.VISIBLE
                    viewCardBorder.background = GradientDrawable().apply {
                        cornerRadius = cardPreview.radius
                        setStroke((1.5f * density).toInt(), Color.parseColor("#4DFFFFFF"))
                    }
                    btnCardBorderToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnCardBorderToggle.setTextColor(Color.WHITE)
                    btnCardBorderToggle.text = context.getString(R.string.card_border_soft)
                }
                2 -> {
                    viewCardBorder.visibility = View.VISIBLE
                    viewCardBorder.background = GradientDrawable().apply {
                        cornerRadius = cardPreview.radius
                        setStroke((2f * density).toInt(), Color.parseColor("#F59E0B"))
                    }
                    btnCardBorderToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnCardBorderToggle.setTextColor(Color.WHITE)
                    btnCardBorderToggle.text = context.getString(R.string.card_border_gold)
                }
                3 -> {
                    viewCardBorder.visibility = View.VISIBLE
                    viewCardBorder.background = GradientDrawable().apply {
                        cornerRadius = cardPreview.radius
                        setStroke((2f * density).toInt(), Color.parseColor("#00F2FE"))
                    }
                    btnCardBorderToggle.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnCardBorderToggle.setTextColor(Color.WHITE)
                    btnCardBorderToggle.text = context.getString(R.string.card_border_neon)
                }
            }
        }

        btnCardBorderToggle.setOnClickListener {
            borderIndex = (borderIndex + 1) % 4
            applyCardBorder()
        }

        // Quotes Decoration
        var quoteMode = 0 // 0: None, 1: “ ”, 2: « », 3: ❝ ❞
        val btnToggleQuotes = view.findViewById<TextView>(R.id.btnToggleQuotes)

        btnToggleQuotes.setOnClickListener {
            quoteMode = (quoteMode + 1) % 4
            when (quoteMode) {
                0 -> {
                    tvQuoteTop.visibility = View.GONE
                    tvQuoteBottom.visibility = View.GONE
                    btnToggleQuotes.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnToggleQuotes.setTextColor(Color.parseColor("#CBD5E1"))
                    btnToggleQuotes.text = context.getString(R.string.quote_none)
                }
                1 -> {
                    tvQuoteTop.text = "“"
                    tvQuoteBottom.text = "”"
                    tvQuoteTop.visibility = View.VISIBLE
                    tvQuoteBottom.visibility = View.VISIBLE
                    btnToggleQuotes.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnToggleQuotes.setTextColor(Color.WHITE)
                    btnToggleQuotes.text = context.getString(R.string.quote_quotes)
                }
                2 -> {
                    tvQuoteTop.text = "«"
                    tvQuoteBottom.text = "»"
                    tvQuoteTop.visibility = View.VISIBLE
                    tvQuoteBottom.visibility = View.VISIBLE
                    btnToggleQuotes.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnToggleQuotes.setTextColor(Color.WHITE)
                    btnToggleQuotes.text = context.getString(R.string.quote_guillemets)
                }
                3 -> {
                    tvQuoteTop.text = "❝"
                    tvQuoteBottom.text = "❞"
                    tvQuoteTop.visibility = View.VISIBLE
                    tvQuoteBottom.visibility = View.VISIBLE
                    btnToggleQuotes.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnToggleQuotes.setTextColor(Color.WHITE)
                    btnToggleQuotes.text = context.getString(R.string.quote_brackets)
                }
            }
        }

        // Author Signature Toggle (New Feature!)
        var authorMode = 0
        val btnToggleAuthor = view.findViewById<TextView>(R.id.btnToggleAuthor)
        btnToggleAuthor.setOnClickListener {
            authorMode = (authorMode + 1) % 4
            when (authorMode) {
                0 -> {
                    tvPreviewAuthor.visibility = View.GONE
                    btnToggleAuthor.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnToggleAuthor.setTextColor(Color.parseColor("#CBD5E1"))
                    btnToggleAuthor.text = context.getString(R.string.author_signature)
                }
                1 -> {
                    tvPreviewAuthor.visibility = View.VISIBLE
                    tvPreviewAuthor.text = "— Manchorati"
                    btnToggleAuthor.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnToggleAuthor.setTextColor(Color.WHITE)
                    btnToggleAuthor.text = "${context.getString(R.string.author_signature)}: Manchorati"
                }
                2 -> {
                    tvPreviewAuthor.visibility = View.VISIBLE
                    tvPreviewAuthor.text = "— ${context.getString(R.string.tag_wisdom)}"
                    btnToggleAuthor.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnToggleAuthor.setTextColor(Color.WHITE)
                    btnToggleAuthor.text = "${context.getString(R.string.author_signature)}: ${context.getString(R.string.tag_wisdom)}"
                }
                3 -> {
                    tvPreviewAuthor.visibility = View.VISIBLE
                    tvPreviewAuthor.text = "— ${context.getString(R.string.featured_writer)}"
                    btnToggleAuthor.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnToggleAuthor.setTextColor(Color.WHITE)
                    btnToggleAuthor.text = "${context.getString(R.string.author_signature)}: ${context.getString(R.string.featured_writer)}"
                }
            }
        }

        // Category Badge Toggle (يمكن للمستخدم إزالته أو تركه حسب طلبه)
        var isCategoryVisible = true
        val btnToggleCategory = view.findViewById<TextView>(R.id.btnToggleCategory)
        btnToggleCategory?.setOnClickListener {
            isCategoryVisible = !isCategoryVisible
            tvCategory.visibility = if (isCategoryVisible) View.VISIBLE else View.GONE
            btnToggleCategory.setBackgroundResource(if (isCategoryVisible) R.drawable.bg_tool_chip_active else R.drawable.bg_tool_chip_idle)
            btnToggleCategory.setTextColor(if (isCategoryVisible) Color.WHITE else Color.parseColor("#CBD5E1"))
        }

        // اسم التطبيق منشوراتي يجب أن يبقى ثابتاً في كل الصور
        tvWatermark.visibility = View.VISIBLE

        // ==============================================================
        // 7. PRO PRESETS (1-TAP STYLES)
        // ==============================================================
        val layoutPresetsContainer = view.findViewById<LinearLayout>(R.id.layoutPresetsContainer)
        data class ProTemplate(
            val title: String,
            val bgColors: IntArray,
            val textColor: String,
            val fontResId: Int,
            val isLight: Boolean = false,
            val hasBorder: Int = 0
        )

        val templates = listOf(
            ProTemplate(context.getString(R.string.template_dark_luxury), intArrayOf(Color.parseColor("#0B0F19"), Color.parseColor("#1E293B")), "#F8FAFC", R.font.tajawal, hasBorder = 2),
            ProTemplate(context.getString(R.string.template_classic_paper), intArrayOf(Color.parseColor("#F6F0EA"), Color.parseColor("#DFD2C0")), "#1E293B", R.font.arefruqaa, isLight = true, hasBorder = 1),
            ProTemplate(context.getString(R.string.template_neon_cyber), intArrayOf(Color.parseColor("#091A28"), Color.parseColor("#022C43")), "#38BDF8", R.font.blakahollow, hasBorder = 3),
            ProTemplate(context.getString(R.string.template_calm_sunset), intArrayOf(Color.parseColor("#4A0E2E"), Color.parseColor("#9A3412")), "#FFFFFF", R.font.cairoplay),
            ProTemplate(context.getString(R.string.template_glass), intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#312E81")), "#FFFFFF", R.font.kufam, hasBorder = 1),
            ProTemplate(context.getString(R.string.template_pure_minimal), intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#F8FAFC")), "#0F172A", R.font.tajawal, isLight = true, hasBorder = 1)
        )

        for (tmpl in templates) {
            val btnTmpl = TextView(context).apply {
                val padH = (14 * density).toInt()
                val padV = (8 * density).toInt()
                val margin = (4 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(margin, margin, margin, margin)
                }
                setPadding(padH, padV, padH, padV)
                text = tmpl.title
                textSize = 12f
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_tool_chip_idle)

                setOnClickListener {
                    originalCustomBitmap = null
                    ivBackground.visibility = View.GONE
                    viewOverlay.visibility = View.GONE
                    viewFilterTint.visibility = View.GONE
                    tvContent.paint.shader = null

                    cardPreview.setCardBackgroundColor(Color.TRANSPARENT)
                    cardPreview.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, tmpl.bgColors).apply {
                        cornerRadius = cardPreview.radius
                    }

                    tvContent.setTextColor(Color.parseColor(tmpl.textColor))
                    try {
                        currentSelectedTypeface = ResourcesCompat.getFont(context, tmpl.fontResId)
                        applyFontStyle()
                    } catch (_: Exception) {}

                    if (tmpl.isLight) {
                        tvCategory.setTextColor(Color.parseColor("#0284C7"))
                        tvWatermark.setTextColor(Color.parseColor("#64748B"))
                    } else {
                        tvCategory.setTextColor(Color.parseColor("#38BDF8"))
                        tvWatermark.setTextColor(Color.parseColor("#94A3B8"))
                    }

                    borderIndex = tmpl.hasBorder
                    applyCardBorder()

                    Toast.makeText(context, context.getString(R.string.template_applied, tmpl.title), Toast.LENGTH_SHORT).show()
                }
            }
            layoutPresetsContainer.addView(btnTmpl)
        }

        // ==============================================================
        // 8. RESET & ACTIONS
        // ==============================================================
        btnResetAll.setOnClickListener {
            originalCustomBitmap = null
            ivBackground.setImageDrawable(null)
            ivBackground.visibility = View.GONE
            viewOverlay.visibility = View.GONE
            viewFilterTint.visibility = View.GONE
            cardPreview.background = null
            cardPreview.setCardBackgroundColor(Color.parseColor("#1E293B"))
            cardPreview.radius = 18f * density
            viewCardBorder.visibility = View.GONE
            borderIndex = 0

            tvContent.paint.shader = null
            tvContent.setTextColor(Color.WHITE)
            tvContent.textSize = 17f
            tvCurrentFontSize.text = "18"
            tvContent.gravity = Gravity.CENTER
            tvContent.letterSpacing = 0f
            tvContent.setShadowLayer(4f, 1.5f, 1.5f, Color.parseColor("#99000000"))
            tvContent.background = null
            tvContent.setPadding(0, 0, 0, 0)
            isHighlightEnabled = false

            tvQuoteTop.visibility = View.GONE
            tvQuoteBottom.visibility = View.GONE
            tvPreviewAuthor.visibility = View.GONE
            quoteMode = 0
            authorMode = 0

            isCategoryVisible = true
            tvCategory.visibility = View.VISIBLE
            btnToggleCategory?.setBackgroundResource(R.drawable.bg_tool_chip_active)
            btnToggleCategory?.setTextColor(Color.WHITE)

            tvWatermark.visibility = View.VISIBLE

            updateRatio(btnRatioSquare, 270)
            Toast.makeText(context, context.getString(R.string.reset_design_success), Toast.LENGTH_SHORT).show()
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

    private fun blurBitmap(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source
        return try {
            val scale = 0.4f
            val width = (source.width * scale).toInt().coerceAtLeast(1)
            val height = (source.height * scale).toInt().coerceAtLeast(1)
            val smallBitmap = Bitmap.createScaledBitmap(source, width, height, false)
            val blurredSmall = fastBoxBlur(smallBitmap, radius)
            Bitmap.createScaledBitmap(blurredSmall, source.width, source.height, true)
        } catch (e: Exception) {
            e.printStackTrace()
            source
        }
    }

    private fun fastBoxBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var p: Int
        val vmin = IntArray(maxOf(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in 0 until 256 * divsum) {
            dv[i] = (i / divsum)
        }

        var yw = 0
        var yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (y in 0 until h) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            for (i in -radius..radius) {
                p = pix[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = minOf(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            rinsum = 0; ginsum = 0; binsum = 0
            routsum = 0; goutsum = 0; boutsum = 0
            rsum = 0; gsum = 0; bsum = 0
            val yp = -radius * w
            for (i in -radius..radius) {
                yi = maxOf(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            yi = x
            stackpointer = radius
            for (y in 0 until h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = minOf(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
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
                Toast.makeText(context, context.getString(R.string.image_saved_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.image_save_error), Toast.LENGTH_SHORT).show()
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
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_card_via)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

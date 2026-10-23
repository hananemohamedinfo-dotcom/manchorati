package com.manchorati.www

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import yuku.ambilwarna.AmbilWarnaDialog

object CommunityPostDialogHelper {

    fun show(
        context: Context,
        authorId: String,
        authorName: String,
        authorUsername: String,
        editingPost: CommunityPost? = null,
        onAuthRequired: (() -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        val dialog = BottomSheetDialog(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_post, null)
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val density = context.resources.displayMetrics.density

        // Header and preview views
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val btnCloseDialog = dialogView.findViewById<ImageView>(R.id.btnCloseDialog)
        val previewContainer = dialogView.findViewById<View>(R.id.previewContainer)
        val ivPreviewAvatar = dialogView.findViewById<ImageView>(R.id.ivPreviewAvatar)
        val tvPreviewAuthorName = dialogView.findViewById<TextView>(R.id.tvPreviewAuthorName)
        val tvPreviewTagBadge = dialogView.findViewById<TextView>(R.id.tvPreviewTagBadge)
        val etContent = dialogView.findViewById<EditText>(R.id.etPostContent)
        val tvCharCounter = dialogView.findViewById<TextView>(R.id.tvCharCounter)

        // Menu Tabs
        val tabBg = dialogView.findViewById<TextView>(R.id.tabBg)
        val tabTag = dialogView.findViewById<TextView>(R.id.tabTag)
        val tabTextColor = dialogView.findViewById<TextView>(R.id.tabTextColor)
        val tabFont = dialogView.findViewById<TextView>(R.id.tabFont)

        // ScrollViews for each section
        val scrollBgColors = dialogView.findViewById<HorizontalScrollView>(R.id.scrollBgColors)
        val scrollTags = dialogView.findViewById<HorizontalScrollView>(R.id.scrollTags)
        val scrollTextColors = dialogView.findViewById<HorizontalScrollView>(R.id.scrollTextColors)
        val scrollFonts = dialogView.findViewById<HorizontalScrollView>(R.id.scrollFonts)

        // Containers
        val layoutColors = dialogView.findViewById<LinearLayout>(R.id.layoutColorsContainer)
        val layoutTags = dialogView.findViewById<LinearLayout>(R.id.layoutTagsContainer)
        val layoutTextColors = dialogView.findViewById<LinearLayout>(R.id.layoutTextColorsContainer)
        val layoutFonts = dialogView.findViewById<LinearLayout>(R.id.layoutFontsContainer)

        // Publish Button
        val btnPublish = dialogView.findViewById<Button>(R.id.btnPublish)
        val pbPublishing = dialogView.findViewById<ProgressBar>(R.id.pbPublishing)

        // State
        var selectedBg = editingPost?.bgId ?: "#FFFFFF"
        var selectedFont = editingPost?.fontId ?: "tajawal"
        var selectedTextColor = editingPost?.textColor ?: ""
        var selectedTag = editingPost?.tag ?: ""

        // Load Author Profile
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val user = AuthManager.currentUser ?: AuthManager.auth.currentUser
        val authorPhoto = prefs.getString("user_photo", "")?.takeIf { it.isNotEmpty() }
            ?: user?.photoUrl?.toString()
            ?: editingPost?.authorPhotoUrl
            ?: ""
        ivPreviewAvatar.loadUserAvatar(authorPhoto)
        tvPreviewAuthorName.text = if (authorName.isNotEmpty()) authorName else "أنت"

        btnCloseDialog.setOnClickListener { dialog.dismiss() }

        if (editingPost != null) {
            tvDialogTitle.text = "تعديل المنشور"
            etContent.setText(editingPost.content)
            etContent.setSelection(etContent.text.length)
            btnPublish.text = "حفظ التعديل 💾"
        }

        // Live Preview Updater
        fun updatePreview() {
            ThemeHelper.applyBackground(previewContainer, selectedBg, 16f)
            etContent.typeface = ThemeHelper.getFontTypeface(context, selectedFont)

            val autoTextColor = ThemeHelper.getTextColor(selectedBg)
            val effectiveTextColor = if (selectedTextColor.isNotEmpty()) {
                try {
                    Color.parseColor(selectedTextColor)
                } catch (e: Exception) {
                    autoTextColor
                }
            } else {
                autoTextColor
            }

            etContent.setTextColor(effectiveTextColor)
            val hintColor = ColorUtils.setAlphaComponent(effectiveTextColor, 130)
            etContent.setHintTextColor(hintColor)

            tvPreviewAuthorName.setTextColor(autoTextColor)
            val counterColor = ColorUtils.setAlphaComponent(autoTextColor, 170)
            tvCharCounter.setTextColor(counterColor)

            if (selectedTag.isNotEmpty()) {
                tvPreviewTagBadge.visibility = View.VISIBLE
                tvPreviewTagBadge.text = selectedTag
            } else {
                tvPreviewTagBadge.visibility = View.GONE
            }
        }
        updatePreview()

        // Character counter watcher
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCharCounter.text = "${s?.length ?: 0} / 280"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        tvCharCounter.text = "${etContent.text.length} / 280"

        // 1. Tab Menu Navigation
        fun switchTab(activeTab: TextView, activeScroll: View) {
            val allTabs = listOf(tabBg, tabTag, tabTextColor, tabFont)
            val allScrolls = listOf(scrollBgColors, scrollTags, scrollTextColors, scrollFonts)

            for (t in allTabs) {
                if (t == activeTab) {
                    t.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    t.setTextColor(Color.WHITE)
                    t.typeface = android.graphics.Typeface.DEFAULT_BOLD
                } else {
                    t.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    t.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    t.typeface = android.graphics.Typeface.DEFAULT
                }
            }

            for (s in allScrolls) {
                s.visibility = if (s == activeScroll) View.VISIBLE else View.GONE
            }
        }

        tabBg.setOnClickListener { switchTab(tabBg, scrollBgColors) }
        tabTag.setOnClickListener { switchTab(tabTag, scrollTags) }
        tabTextColor.setOnClickListener { switchTab(tabTextColor, scrollTextColors) }
        tabFont.setOnClickListener { switchTab(tabFont, scrollFonts) }

        // 2. Background Colors (Clean Circular Swatches + Custom Picker)
        val defaultBgColors = listOf(
            "#FFFFFF",
            "#FF512F,#DD2476",
            "#2193B0,#6DD5ED",
            "#8A2387,#E94057",
            "#11998E,#38EF7D",
            "#232526,#414345",
            "#00c996,#003d4d",
            "#9400d3,#4b0082",
            "#29eac4,#4284db",
            "#009ffc,#1da1f2",
            "#0F766E",
            "#4338CA",
            "#D97706",
            "#7C3AED",
            "#BE123C"
        )

        fun createCircularColorDrawable(colorCode: String): GradientDrawable {
            return if (colorCode.contains(",")) {
                val parts = colorCode.split(",")
                val start = Color.parseColor(parts[0].trim())
                val end = Color.parseColor(parts[1].trim())
                GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
                    shape = GradientDrawable.OVAL
                }
            } else {
                val solid = try { Color.parseColor(colorCode) } catch (e: Exception) { Color.WHITE }
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(solid)
                    if (solid == Color.WHITE || ColorUtils.calculateLuminance(solid) > 0.85) {
                        setStroke((1 * density).toInt(), Color.parseColor("#33000000"))
                    }
                }
            }
        }

        val boxSizePx = (42 * density).toInt()
        val circleSizePx = (34 * density).toInt()
        val marginPx = (6 * density).toInt()

        fun renderBgColors() {
            layoutColors.removeAllViews()
            val isCustomBg = !defaultBgColors.contains(selectedBg)

            for (colorCode in defaultBgColors) {
                val isSelected = selectedBg == colorCode

                val outerBox = FrameLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(boxSizePx, boxSizePx).apply {
                        marginEnd = marginPx
                    }
                    if (isSelected) {
                        val ring = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.TRANSPARENT)
                            setStroke((2.5f * density).toInt(), Color.parseColor("#0284C7"))
                        }
                        background = ring
                    } else {
                        background = null
                    }
                }

                val innerCircle = View(context).apply {
                    val innerSize = if (isSelected) (28 * density).toInt() else circleSizePx
                    layoutParams = FrameLayout.LayoutParams(innerSize, innerSize).apply {
                        gravity = Gravity.CENTER
                    }
                    background = createCircularColorDrawable(colorCode)
                }
                outerBox.addView(innerCircle)

                if (isSelected) {
                    val checkIcon = ImageView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            (16 * density).toInt(),
                            (16 * density).toInt()
                        ).apply {
                            gravity = Gravity.CENTER
                        }
                        setImageResource(R.drawable.ic_check)
                        val iconColor = ThemeHelper.getTextColor(colorCode)
                        setColorFilter(iconColor)
                    }
                    outerBox.addView(checkIcon)
                }

                outerBox.setOnClickListener {
                    selectedBg = colorCode
                    renderBgColors()
                    updatePreview()
                }

                layoutColors.addView(outerBox)
            }

            // Custom Background Picker Circle
            val customBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(boxSizePx, boxSizePx).apply {
                    marginEnd = marginPx
                }
                if (isCustomBg) {
                    val ring = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke((2.5f * density).toInt(), Color.parseColor("#0284C7"))
                    }
                    background = ring
                } else {
                    background = null
                }
            }

            val customCircle = View(context).apply {
                val innerSize = if (isCustomBg) (28 * density).toInt() else circleSizePx
                layoutParams = FrameLayout.LayoutParams(innerSize, innerSize).apply {
                    gravity = Gravity.CENTER
                }
                if (isCustomBg) {
                    background = createCircularColorDrawable(selectedBg)
                } else {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(context, R.color.bg_card))
                        setStroke((1.5f * density).toInt(), ContextCompat.getColor(context, R.color.divider_color))
                    }
                }
            }
            customBox.addView(customCircle)

            val customIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (16 * density).toInt(),
                    (16 * density).toInt()
                ).apply {
                    gravity = Gravity.CENTER
                }
                if (isCustomBg) {
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(ThemeHelper.getTextColor(selectedBg))
                } else {
                    setImageResource(R.drawable.ic_tool_color)
                    setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
                }
            }
            customBox.addView(customIcon)

            customBox.setOnClickListener {
                val initialColor = try {
                    if (selectedBg.contains(",")) Color.parseColor(selectedBg.split(",")[0].trim())
                    else Color.parseColor(selectedBg)
                } catch (e: Exception) {
                    Color.parseColor("#0284C7")
                }

                val dialogPicker = AmbilWarnaDialog(
                    context,
                    initialColor,
                    object : AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onCancel(dialog: AmbilWarnaDialog?) {}
                        override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                            selectedBg = String.format("#%06X", (0xFFFFFF and color))
                            renderBgColors()
                            updatePreview()
                        }
                    }
                )
                dialogPicker.show()
            }
            layoutColors.addView(customBox)
        }
        renderBgColors()

        // 3. Tags Section
        val tagsList = listOf(
            Pair("", "بدون وسم"),
            Pair("حكم وأقوال", "حكم وأقوال"),
            Pair("خواطر", "خواطر"),
            Pair("أدعية وأذكار", "أدعية وأذكار"),
            Pair("تحفيز وتطوير", "تحفيز وتطوير"),
            Pair("فكاهة وضحك", "فكاهة وضحك"),
            Pair("شعر وأدب", "شعر وأدب"),
            Pair("قهوة ومساء", "قهوة ومساء"),
            Pair("عام", "عام")
        )

        fun renderTags() {
            layoutTags.removeAllViews()
            val chipPadH = (12 * density).toInt()
            val chipPadV = (5 * density).toInt()
            val chipMargin = (6 * density).toInt()

            for ((tagValue, tagLabel) in tagsList) {
                val isSelected = selectedTag == tagValue
                val chip = TextView(context).apply {
                    text = tagLabel
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setPadding(chipPadH, chipPadV, chipPadH, chipPadV)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (32 * density).toInt()
                    ).apply {
                        marginEnd = chipMargin
                    }
                    if (isSelected) {
                        setBackgroundResource(R.drawable.bg_tool_chip_active)
                        setTextColor(Color.WHITE)
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    } else {
                        setBackgroundResource(R.drawable.bg_tool_chip_idle)
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        typeface = android.graphics.Typeface.DEFAULT
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        selectedTag = if (tagValue.isEmpty() || selectedTag == tagValue) "" else tagValue
                        renderTags()
                        updatePreview()
                    }
                }
                layoutTags.addView(chip)
            }
        }
        renderTags()

        // 4. Text Colors Section
        val curatedTextColors = listOf(
            Pair("", "تلقائي"),
            Pair("#FFFFFF", "أبيض"),
            Pair("#0F172A", "داكن"),
            Pair("#FBBF24", "ذهبي"),
            Pair("#38BDF8", "سماوي"),
            Pair("#34D399", "أخضر"),
            Pair("#FB7185", "وردي"),
            Pair("#A855F7", "بنفسجي")
        )

        fun renderTextColors() {
            layoutTextColors.removeAllViews()
            val isCustomTextColor = selectedTextColor.isNotEmpty() && curatedTextColors.none { it.first.equals(selectedTextColor, ignoreCase = true) }

            for ((code, label) in curatedTextColors) {
                if (code.isEmpty()) {
                    // Auto Chip
                    val isSelected = selectedTextColor.isEmpty()
                    val autoChip = TextView(context).apply {
                        text = "تلقائي A"
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            (32 * density).toInt()
                        ).apply {
                            marginEnd = marginPx
                        }
                        if (isSelected) {
                            setBackgroundResource(R.drawable.bg_tool_chip_active)
                            setTextColor(Color.WHITE)
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        } else {
                            setBackgroundResource(R.drawable.bg_tool_chip_idle)
                            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                            typeface = android.graphics.Typeface.DEFAULT
                        }
                        setOnClickListener {
                            selectedTextColor = ""
                            renderTextColors()
                            updatePreview()
                        }
                    }
                    layoutTextColors.addView(autoChip)
                } else {
                    // Color Swatch
                    val isSelected = selectedTextColor.equals(code, ignoreCase = true)
                    val outerBox = FrameLayout(context).apply {
                        layoutParams = LinearLayout.LayoutParams(boxSizePx, boxSizePx).apply {
                            marginEnd = marginPx
                        }
                        if (isSelected) {
                            val ring = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(Color.TRANSPARENT)
                                setStroke((2.5f * density).toInt(), Color.parseColor("#0284C7"))
                            }
                            background = ring
                        } else {
                            background = null
                        }
                    }

                    val innerCircle = View(context).apply {
                        val innerSize = if (isSelected) (28 * density).toInt() else circleSizePx
                        layoutParams = FrameLayout.LayoutParams(innerSize, innerSize).apply {
                            gravity = Gravity.CENTER
                        }
                        background = createCircularColorDrawable(code)
                    }
                    outerBox.addView(innerCircle)

                    if (isSelected) {
                        val checkIcon = ImageView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                (16 * density).toInt(),
                                (16 * density).toInt()
                            ).apply {
                                gravity = Gravity.CENTER
                            }
                            setImageResource(R.drawable.ic_check)
                            val iconColor = ThemeHelper.getTextColor(code)
                            setColorFilter(iconColor)
                        }
                        outerBox.addView(checkIcon)
                    }

                    outerBox.setOnClickListener {
                        selectedTextColor = code
                        renderTextColors()
                        updatePreview()
                    }
                    layoutTextColors.addView(outerBox)
                }
            }

            // Custom Text Color Picker Box
            val customTextBox = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(boxSizePx, boxSizePx).apply {
                    marginEnd = marginPx
                }
                if (isCustomTextColor) {
                    val ring = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.TRANSPARENT)
                        setStroke((2.5f * density).toInt(), Color.parseColor("#0284C7"))
                    }
                    background = ring
                } else {
                    background = null
                }
            }

            val customTextCircle = View(context).apply {
                val innerSize = if (isCustomTextColor) (28 * density).toInt() else circleSizePx
                layoutParams = FrameLayout.LayoutParams(innerSize, innerSize).apply {
                    gravity = Gravity.CENTER
                }
                if (isCustomTextColor) {
                    background = createCircularColorDrawable(selectedTextColor)
                } else {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ContextCompat.getColor(context, R.color.bg_card))
                        setStroke((1.5f * density).toInt(), ContextCompat.getColor(context, R.color.divider_color))
                    }
                }
            }
            customTextBox.addView(customTextCircle)

            val customTextIcon = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (16 * density).toInt(),
                    (16 * density).toInt()
                ).apply {
                    gravity = Gravity.CENTER
                }
                if (isCustomTextColor) {
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(ThemeHelper.getTextColor(selectedTextColor))
                } else {
                    setImageResource(R.drawable.ic_tool_color)
                    setColorFilter(ContextCompat.getColor(context, R.color.text_primary))
                }
            }
            customTextBox.addView(customTextIcon)

            customTextBox.setOnClickListener {
                val initialColor = try {
                    if (selectedTextColor.isNotEmpty()) Color.parseColor(selectedTextColor)
                    else Color.parseColor("#38BDF8")
                } catch (e: Exception) {
                    Color.parseColor("#38BDF8")
                }

                val dialogPicker = AmbilWarnaDialog(
                    context,
                    initialColor,
                    object : AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onCancel(dialog: AmbilWarnaDialog?) {}
                        override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                            selectedTextColor = String.format("#%06X", (0xFFFFFF and color))
                            renderTextColors()
                            updatePreview()
                        }
                    }
                )
                dialogPicker.show()
            }
            layoutTextColors.addView(customTextBox)
        }
        renderTextColors()

        // 5. Fonts Section
        val fontsList = listOf(
            Pair("tajawal", "تجوال"),
            Pair("cairoplay", "كايرو"),
            Pair("arefruqaa", "رقعة"),
            Pair("kufam", "كوفام"),
            Pair("marhey", "مرحي"),
            Pair("notonastaliqurdu", "نستعليق"),
            Pair("blakahollow", "بلاك"),
            Pair("oi", "عريض")
        )

        fun renderFonts() {
            layoutFonts.removeAllViews()
            val fontPadH = (12 * density).toInt()
            val fontPadV = (5 * density).toInt()
            val fontMargin = (6 * density).toInt()

            for ((fontId, fontTitle) in fontsList) {
                val isSelected = selectedFont == fontId
                val tvFont = TextView(context).apply {
                    text = fontTitle
                    textSize = 13f
                    gravity = Gravity.CENTER
                    typeface = ThemeHelper.getFontTypeface(context, fontId)
                    setPadding(fontPadH, fontPadV, fontPadH, fontPadV)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (32 * density).toInt()
                    ).apply {
                        marginEnd = fontMargin
                    }
                    if (isSelected) {
                        setBackgroundResource(R.drawable.bg_tool_chip_active)
                        setTextColor(Color.WHITE)
                        typeface = ThemeHelper.getFontTypeface(context, fontId)
                    } else {
                        setBackgroundResource(R.drawable.bg_tool_chip_idle)
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    }
                    setOnClickListener {
                        selectedFont = fontId
                        renderFonts()
                        updatePreview()
                    }
                }
                layoutFonts.addView(tvFont)
            }
        }
        renderFonts()

        // 6. Publish Button Logic
        btnPublish.setOnClickListener {
            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                etContent.error = "اكتب كلمات المنشور"
                etContent.requestFocus()
                return@setOnClickListener
            }

            if (BadWordsFilter.containsBadWords(content)) {
                Toast.makeText(context, "عذراً، يحتوي النص على كلمات مخالفة لسياسة النشر", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val currentUser = AuthManager.currentUser ?: AuthManager.auth.currentUser
            if (currentUser == null) {
                Toast.makeText(context, "تعذر التحقق من الحساب، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
                onAuthRequired?.invoke()
                return@setOnClickListener
            }

            btnPublish.isEnabled = false
            btnPublish.text = ""
            pbPublishing.visibility = View.VISIBLE

            if (editingPost == null) {
                FirestoreManager.publishCommunityPost(
                    authorId = currentUser.uid,
                    authorName = authorName,
                    authorUsername = authorUsername,
                    authorPhotoUrl = authorPhoto,
                    content = content,
                    bgId = selectedBg,
                    fontId = selectedFont,
                    textColor = selectedTextColor,
                    tag = selectedTag,
                    onSuccess = {
                        dialog.dismiss()
                        Toast.makeText(context, "تم النشر في المجتمع بنجاح! 🚀", Toast.LENGTH_SHORT).show()
                        onSuccess?.invoke()
                    },
                    onFailure = {
                        btnPublish.isEnabled = true
                        btnPublish.text = "نشر في المجتمع 🚀"
                        pbPublishing.visibility = View.GONE
                        Toast.makeText(context, "فشل النشر، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                FirestoreManager.updateCommunityPost(
                    postId = editingPost.id,
                    content = content,
                    bgId = selectedBg,
                    fontId = selectedFont,
                    textColor = selectedTextColor,
                    tag = selectedTag
                ) { ok ->
                    if (ok) {
                        dialog.dismiss()
                        Toast.makeText(context, "تم حفظ التعديل بنجاح! 💾", Toast.LENGTH_SHORT).show()
                        onSuccess?.invoke()
                    } else {
                        btnPublish.isEnabled = true
                        btnPublish.text = "حفظ التعديل 💾"
                        pbPublishing.visibility = View.GONE
                        Toast.makeText(context, "فشل التعديل، حاول مجدداً", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }
}

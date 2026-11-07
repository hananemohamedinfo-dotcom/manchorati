package com.manchorati.www

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

object AiWriterBottomSheetDialog {

    @JvmOverloads
    fun show(
        context: Context,
        initialText: String = "",
        initialTopic: String = "",
        onQuoteSelectedWithTopic: ((quote: String, topic: String) -> Unit)? = null
    ) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_ai_writer, null)
        dialog.setContentView(view)

        val tvQuotaBadge = view.findViewById<TextView>(R.id.tvQuotaBadge)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseAiDialog)
        val btnKeyConfig = view.findViewById<ImageView>(R.id.btnAiKeyConfig)
        val etCustomPrompt = view.findViewById<EditText>(R.id.etCustomPrompt)
        val btnGenerate = view.findViewById<MaterialButton>(R.id.btnGenerateAi)
        val cardResult = view.findViewById<CardView>(R.id.cardAiResult)
        val tvGeneratedQuote = view.findViewById<TextView>(R.id.tvGeneratedQuote)

        val btnCopy = view.findViewById<MaterialButton>(R.id.btnAiActionCopy)
        val btnImage = view.findViewById<MaterialButton>(R.id.btnAiActionImage)
        val btnVideo = view.findViewById<MaterialButton>(R.id.btnAiActionVideo)
        val btnPublish = view.findViewById<MaterialButton>(R.id.btnAiActionPublish)

        // Topic Chips
        val chipWisdom = view.findViewById<TextView>(R.id.chipTopicWisdom)
        val chipMotivation = view.findViewById<TextView>(R.id.chipTopicMotivation)
        val chipLove = view.findViewById<TextView>(R.id.chipTopicLove)
        val chipDua = view.findViewById<TextView>(R.id.chipTopicDua)
        val chipCalm = view.findViewById<TextView>(R.id.chipTopicCalm)
        val chipFarewell = view.findViewById<TextView>(R.id.chipTopicFarewell)

        val allTopicChips = listOf(chipWisdom, chipMotivation, chipLove, chipDua, chipCalm, chipFarewell)
        var selectedTopic = "حكم وأمثال"

        fun selectTopic(selected: TextView, topicName: String) {
            selectedTopic = topicName
            allTopicChips.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    chip.setTextColor(android.graphics.Color.WHITE)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
            }
        }

        chipWisdom.setOnClickListener { selectTopic(chipWisdom, "حكم وأمثال") }
        chipMotivation.setOnClickListener { selectTopic(chipMotivation, "تحفيز ونجاح") }
        chipLove.setOnClickListener { selectTopic(chipLove, "حب وغرام") }
        chipDua.setOnClickListener { selectTopic(chipDua, "أدعية وأمل") }
        chipCalm.setOnClickListener { selectTopic(chipCalm, "خواطر هادئة") }
        chipFarewell.setOnClickListener { selectTopic(chipFarewell, "وداع وفراق") }

        // Pre-select topic if provided
        if (initialTopic.isNotBlank()) {
            when {
                initialTopic.contains("حكم") -> selectTopic(chipWisdom, "حكم وأمثال")
                initialTopic.contains("تحفيز") || initialTopic.contains("تطوير") -> selectTopic(chipMotivation, "تحفيز ونجاح")
                initialTopic.contains("حب") -> selectTopic(chipLove, "حب وغرام")
                initialTopic.contains("أدعية") || initialTopic.contains("دعاء") -> selectTopic(chipDua, "أدعية وأمل")
                initialTopic.contains("قهوة") || initialTopic.contains("مساء") -> selectTopic(chipCalm, "خواطر هادئة")
                initialTopic.contains("شعر") -> selectTopic(chipWisdom, "شعر وأدب")
            }
        }

        // Style Chips
        val chipStyleWisdom = view.findViewById<TextView>(R.id.chipStyleWisdom)
        val chipStyleShort = view.findViewById<TextView>(R.id.chipStyleShort)
        val chipStylePoem = view.findViewById<TextView>(R.id.chipStylePoem)
        val chipStylePolish = view.findViewById<TextView>(R.id.chipStylePolish)

        val allStyleChips = listOfNotNull(chipStyleWisdom, chipStyleShort, chipStylePoem, chipStylePolish)
        var selectedStyle = if (initialText.isNotBlank()) "تحسين وبلاغة" else "حكمة بليغة"

        fun selectStyle(selected: TextView, styleName: String) {
            selectedStyle = styleName
            allStyleChips.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    chip.setTextColor(android.graphics.Color.WHITE)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }
            }
        }

        chipStyleWisdom?.setOnClickListener { selectStyle(chipStyleWisdom, "حكمة بليغة") }
        chipStyleShort?.setOnClickListener { selectStyle(chipStyleShort, "خاطرة قصيرة") }
        chipStylePoem?.setOnClickListener { selectStyle(chipStylePoem, "شعر فخم") }
        chipStylePolish?.setOnClickListener { selectStyle(chipStylePolish, "تحسين وبلاغة") }

        // Pre-fill initial text if coming from post editor
        if (initialText.isNotBlank()) {
            etCustomPrompt.setText(initialText)
            etCustomPrompt.setSelection(initialText.length)
            chipStylePolish?.let { selectStyle(it, "تحسين وبلاغة") }
        }

        // Quota display
        fun updateQuotaDisplay() {
            val remaining = AiWriterManager.getRemainingQuota(context)
            tvQuotaBadge.text = context.getString(R.string.ai_quota_badge, remaining, AiWriterManager.MAX_DAILY_QUOTA)
        }
        updateQuotaDisplay()

        // Close button
        btnClose.setOnClickListener { dialog.dismiss() }

        // API Key Configuration Dialog (Key icon)
        btnKeyConfig?.setOnClickListener {
            showKeyConfigDialog(context)
        }

        // Quota badge long-click to open Key config
        tvQuotaBadge.setOnLongClickListener {
            showKeyConfigDialog(context)
            true
        }

        // If used from Add Post, adapt UI
        val isForPost = onQuoteSelectedWithTopic != null
        if (isForPost) {
            btnPublish.text = context.getString(R.string.ai_use_in_post_btn)
            btnVideo.visibility = View.GONE
            btnImage.visibility = View.GONE
        }

        var currentGeneratedQuote = ""

        btnGenerate.setOnClickListener {
            val (canGen, message) = AiWriterManager.canGenerate(context)
            if (!canGen) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            AiWriterManager.recordGeneration(context)
            updateQuotaDisplay()

            val customPrompt = etCustomPrompt.text.toString().trim()
            btnGenerate.isEnabled = false
            btnGenerate.text = context.getString(R.string.ai_generating)

            AiWriterManager.generateQuoteSmartAdvanced(context, selectedTopic, customPrompt, selectedStyle) { quote, isRealAi, statusMsg ->
                currentGeneratedQuote = quote
                tvGeneratedQuote.text = quote
                cardResult.visibility = View.VISIBLE

                btnGenerate.isEnabled = true
                btnGenerate.text = context.getString(R.string.ai_generate_again_btn)

                if (isRealAi) {
                    Toast.makeText(context, "تمت الصياغة بنجاح بواسطة الذكاء الاصطناعي ✨", Toast.LENGTH_SHORT).show()
                } else if (!statusMsg.isNullOrEmpty()) {
                    Toast.makeText(context, statusMsg, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Action: Copy
        btnCopy.setOnClickListener {
            if (currentGeneratedQuote.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Quote", currentGeneratedQuote)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.ai_copied_toast), Toast.LENGTH_SHORT).show()
            }
        }

        // Action: Image Editor (Only if not in post mode)
        btnAiActionImage(btnImage, context, dialog) { currentGeneratedQuote to selectedTopic }

        // Action: Video Studio (Only if not in post mode)
        btnVideo.setOnClickListener {
            if (currentGeneratedQuote.isNotEmpty()) {
                dialog.dismiss()
                val intent = Intent(context, VideoStudioActivity::class.java).apply {
                    putExtra("EXTRA_CONTENT", currentGeneratedQuote)
                    putExtra("EXTRA_TAG", selectedTopic)
                }
                context.startActivity(intent)
            }
        }

        // Action: Publish / Insert
        btnPublish.setOnClickListener {
            if (currentGeneratedQuote.isNotEmpty()) {
                dialog.dismiss()
                if (onQuoteSelectedWithTopic != null) {
                    onQuoteSelectedWithTopic(currentGeneratedQuote, selectedTopic)
                } else if (context is MainActivity) {
                    context.openAddPostWithQuote(currentGeneratedQuote, selectedTopic)
                }
            }
        }

        dialog.show()
    }

    /**
     * Backward compatible show method
     */
    fun show(context: Context, onQuoteSelected: ((String) -> Unit)?) {
        show(
            context = context,
            initialText = "",
            initialTopic = "",
            onQuoteSelectedWithTopic = if (onQuoteSelected != null) { quote, _ -> onQuoteSelected(quote) } else null
        )
    }

    private fun showKeyConfigDialog(context: Context) {
        val currentKey = AiWriterManager.getApiKey(context)
        val input = EditText(context).apply {
            hint = context.getString(R.string.ai_key_settings_hint)
            setText(currentKey)
            setSelection(currentKey.length)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.ai_key_settings_title))
            .setMessage(context.getString(R.string.ai_key_settings_desc))
            .setView(input)
            .setPositiveButton(context.getString(R.string.ai_key_save)) { _, _ ->
                val newKey = input.text.toString().trim()
                AiWriterManager.setCustomApiKey(context, newKey)

                val currentUser = AuthManager.currentUser ?: AuthManager.auth.currentUser
                val email = currentUser?.email ?: ""
                val isAdmin = email.equals("hananemohamedinfo@gmail.com", ignoreCase = true) ||
                        email.equals("admin@manchorati.com", ignoreCase = true)

                if (isAdmin && newKey.isNotEmpty()) {
                    AiWriterManager.saveApiKeyToFirestore(newKey, onSuccess = {
                        Toast.makeText(context, "تم حفظ المفتاح وتعميمه في السحابة بنجاح ✨", Toast.LENGTH_SHORT).show()
                    }, onFailure = {
                        Toast.makeText(context, context.getString(R.string.ai_key_saved_toast), Toast.LENGTH_SHORT).show()
                    })
                } else {
                    Toast.makeText(context, context.getString(R.string.ai_key_saved_toast), Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(context.getString(R.string.ai_key_get_free)) { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun btnAiActionImage(
        btnImage: MaterialButton,
        context: Context,
        dialog: BottomSheetDialog,
        quoteProvider: () -> Pair<String, String>
    ) {
        btnImage.setOnClickListener {
            val (quote, topic) = quoteProvider()
            if (quote.isNotEmpty() && context is AppCompatActivity) {
                dialog.dismiss()
                ImageUtils.showImageEditorDialog(context, topic, quote)
            }
        }
    }
}

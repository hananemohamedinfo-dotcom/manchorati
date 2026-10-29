package com.manchorati.www

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

object AiWriterBottomSheetDialog {

    fun show(context: Context, onQuoteSelected: ((String) -> Unit)? = null) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_ai_writer, null)
        dialog.setContentView(view)

        val tvQuotaBadge = view.findViewById<TextView>(R.id.tvQuotaBadge)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseAiDialog)
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
                    chip.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                }
            }
        }

        chipWisdom.setOnClickListener { selectTopic(chipWisdom, "حكم وأمثال") }
        chipMotivation.setOnClickListener { selectTopic(chipMotivation, "تحفيز ونجاح") }
        chipLove.setOnClickListener { selectTopic(chipLove, "حب وغرام") }
        chipDua.setOnClickListener { selectTopic(chipDua, "أدعية وأمل") }
        chipCalm.setOnClickListener { selectTopic(chipCalm, "خواطر هادئة") }
        chipFarewell.setOnClickListener { selectTopic(chipFarewell, "وداع وفراق") }

        // Style Chips
        val chipStyleWisdom = view.findViewById<TextView>(R.id.chipStyleWisdom)
        val chipStyleShort = view.findViewById<TextView>(R.id.chipStyleShort)
        val chipStylePoem = view.findViewById<TextView>(R.id.chipStylePoem)
        val allStyleChips = listOf(chipStyleWisdom, chipStyleShort, chipStylePoem)
        var selectedStyle = "حكمة بليغة"

        fun selectStyle(selected: TextView, styleName: String) {
            selectedStyle = styleName
            allStyleChips.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    chip.setTextColor(android.graphics.Color.WHITE)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    chip.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_primary))
                }
            }
        }

        chipStyleWisdom.setOnClickListener { selectStyle(chipStyleWisdom, "حكمة بليغة") }
        chipStyleShort.setOnClickListener { selectStyle(chipStyleShort, "خاطرة قصيرة") }
        chipStylePoem.setOnClickListener { selectStyle(chipStylePoem, "شعر فخم") }

        fun updateQuotaDisplay() {
            val remaining = AiWriterManager.getRemainingQuota(context)
            tvQuotaBadge.text = context.getString(R.string.ai_quota_badge, remaining, AiWriterManager.MAX_DAILY_QUOTA)
        }
        updateQuotaDisplay()

        btnClose.setOnClickListener { dialog.dismiss() }

        var currentGeneratedQuote = ""

        btnGenerate.setOnClickListener {
            val (canGen, message) = AiWriterManager.canGenerate(context)
            if (!canGen) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            AiWriterManager.recordGeneration(context)
            updateQuotaDisplay()

            val customPrompt = etCustomPrompt.text.toString()
            btnGenerate.isEnabled = false
            btnGenerate.text = context.getString(R.string.ai_generating)

            view.postDelayed({
                val quote = AiWriterManager.generateQuote(selectedTopic, customPrompt, selectedStyle)
                currentGeneratedQuote = quote
                tvGeneratedQuote.text = quote
                cardResult.visibility = View.VISIBLE

                btnGenerate.isEnabled = true
                btnGenerate.text = context.getString(R.string.ai_generate_again_btn)
            }, 500)
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

        // Action: Image Editor
        btnAiActionImage(btnImage, context, dialog) { currentGeneratedQuote to selectedTopic }

        // Action: Video Studio
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

        // Action: Publish / Use
        btnPublish.setOnClickListener {
            if (currentGeneratedQuote.isNotEmpty()) {
                dialog.dismiss()
                if (onQuoteSelected != null) {
                    onQuoteSelected(currentGeneratedQuote)
                } else if (context is MainActivity) {
                    context.openAddPostWithQuote(currentGeneratedQuote, selectedTopic)
                }
            }
        }

        dialog.show()
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

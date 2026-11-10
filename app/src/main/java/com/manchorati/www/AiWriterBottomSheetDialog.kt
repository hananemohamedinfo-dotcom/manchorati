package com.manchorati.www

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

        // تمديد الـ BottomSheet ليأخذ حجماً مريحاً للمحادثة
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        // عناصر الرأس
        val tvQuotaBadge = view.findViewById<TextView>(R.id.tvQuotaBadge)
        val viewStatusDot = view.findViewById<View>(R.id.viewStatusDot)
        val layoutKeyStatusBadge = view.findViewById<View>(R.id.layoutKeyStatusBadge)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseAiDialog)
        val btnAiKeyConfig = view.findViewById<ImageView>(R.id.btnAiKeyConfig)

        // بطاقة الترويج للمفتاح الشخصي
        val cardCustomKeyPromo = view.findViewById<CardView>(R.id.cardCustomKeyPromo)
        val btnEnterCustomKey = view.findViewById<MaterialButton>(R.id.btnEnterCustomKey)
        val btnGetFreeKey = view.findViewById<MaterialButton>(R.id.btnGetFreeKey)

        // منطقة المحادثة
        val scrollChatArea = view.findViewById<NestedScrollView>(R.id.scrollChatArea)
        val layoutUserBubble = view.findViewById<LinearLayout>(R.id.layoutUserBubble)
        val tvUserPromptText = view.findViewById<TextView>(R.id.tvUserPromptText)
        val layoutAiLoading = view.findViewById<LinearLayout>(R.id.layoutAiLoading)
        val tvAiThinkingText = view.findViewById<TextView>(R.id.tvAiThinkingText)
        val cardAiResult = view.findViewById<CardView>(R.id.cardAiResult)
        val tvAiModelBadge = view.findViewById<TextView>(R.id.tvAiModelBadge)
        val tvGeneratedQuote = view.findViewById<TextView>(R.id.tvGeneratedQuote)

        // إجراءات الاقتباس
        val btnCopy = view.findViewById<MaterialButton>(R.id.btnAiActionCopy)
        val btnImage = view.findViewById<MaterialButton>(R.id.btnAiActionImage)
        val btnVideo = view.findViewById<MaterialButton>(R.id.btnAiActionVideo)
        val btnPublish = view.findViewById<MaterialButton>(R.id.btnAiActionPublish)

        // مقترحات المحادثة السريعة
        val suggestPatience = view.findViewById<TextView>(R.id.suggestPatience)
        val suggestCoffee = view.findViewById<TextView>(R.id.suggestCoffee)
        val suggestMotivation = view.findViewById<TextView>(R.id.suggestMotivation)
        val suggestFriendship = view.findViewById<TextView>(R.id.suggestFriendship)
        val suggestDua = view.findViewById<TextView>(R.id.suggestDua)

        // رقائق الأسلوب
        val chipStyleWisdom = view.findViewById<TextView>(R.id.chipStyleWisdom)
        val chipStyleShort = view.findViewById<TextView>(R.id.chipStyleShort)
        val chipStylePoem = view.findViewById<TextView>(R.id.chipStylePoem)
        val chipStylePolish = view.findViewById<TextView>(R.id.chipStylePolish)
        val allStyleChips = listOfNotNull(chipStyleWisdom, chipStyleShort, chipStylePoem, chipStylePolish)
        var selectedStyle = if (initialText.isNotBlank()) "تحسين وبلاغة" else "حكمة بليغة"

        // شريط الإدخال السفلي
        val etCustomPrompt = view.findViewById<EditText>(R.id.etCustomPrompt)
        val btnGenerateAi = view.findViewById<FrameLayout>(R.id.btnGenerateAi)

        var selectedTopic = if (initialTopic.isNotBlank()) initialTopic else "حكم وأمثال"
        var currentGeneratedQuote = ""

        fun updateKeyAndQuotaUI() {
            if (AiWriterManager.hasUserApiKey(context)) {
                viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_green)
                tvQuotaBadge.text = "مفتاحك الشخصي مفعّل (توليد لا محدود)"
                tvQuotaBadge.setTextColor(Color.parseColor("#10B981"))
                cardCustomKeyPromo.visibility = View.GONE
            } else {
                val remaining = AiWriterManager.getRemainingQuota(context)
                viewStatusDot.setBackgroundResource(R.drawable.bg_status_dot_amber)
                tvQuotaBadge.text = "المفتاح التجريبي ($remaining / ${AiWriterManager.MAX_DAILY_QUOTA} محاولات)"
                tvQuotaBadge.setTextColor(Color.parseColor("#38BDF8"))
                cardCustomKeyPromo.visibility = View.VISIBLE
            }
        }
        updateKeyAndQuotaUI()

        fun selectStyle(selected: TextView, styleName: String) {
            selectedStyle = styleName
            allStyleChips.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    chip.setTextColor(Color.WHITE)
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

        // إذا كان هناك نص مبدئي قادم من محرر المنشورات
        if (initialText.isNotBlank()) {
            etCustomPrompt.setText(initialText)
            etCustomPrompt.setSelection(initialText.length)
            chipStylePolish?.let { selectStyle(it, "تحسين وبلاغة") }
        }

        // أزرار الرأس وإغلاق النافذة
        btnClose.setOnClickListener { dialog.dismiss() }
        btnAiKeyConfig.setOnClickListener { showGeminiKeyConfigDialog(context) { updateKeyAndQuotaUI() } }
        layoutKeyStatusBadge.setOnClickListener { showGeminiKeyConfigDialog(context) { updateKeyAndQuotaUI() } }

        // بطاقة الترويج لمفتاح المستخدم الشخصي
        btnEnterCustomKey.setOnClickListener { showGeminiKeyConfigDialog(context) { updateKeyAndQuotaUI() } }
        btnGetFreeKey.setOnClickListener {
            openAiStudioWeb(context)
        }

        // إذا كان الاستخدام من داخل إنشاء المنشور
        val isForPost = onQuoteSelectedWithTopic != null
        if (isForPost) {
            btnPublish.text = context.getString(R.string.ai_use_in_post_btn)
            btnVideo.visibility = View.GONE
            btnImage.visibility = View.GONE
        }

        // دالة تنفيذ التوليد
        fun doGenerate(customPromptOverride: String? = null) {
            val (canGen, message) = AiWriterManager.canGenerate(context)
            if (!canGen) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                return
            }

            AiWriterManager.recordGeneration(context)
            updateKeyAndQuotaUI()

            val prompt = (customPromptOverride ?: etCustomPrompt.text.toString()).trim()

            // إظهار فقاعة المستخدم
            layoutUserBubble.visibility = View.VISIBLE
            tvUserPromptText.text = if (prompt.isNotEmpty()) prompt else "صياغة $selectedStyle في $selectedTopic"

            // إظهار حالة التحميل وإخفاء النتيجة السابقة
            layoutAiLoading.visibility = View.VISIBLE
            tvAiThinkingText.text = "جاري استحضار الفكرة وصياغتها بأناقة..."
            cardAiResult.visibility = View.GONE
            btnGenerateAi.isEnabled = false

            // التمرير للأسفل لمتابعة المحادثة
            scrollChatArea.post { scrollChatArea.fullScroll(View.FOCUS_DOWN) }

            AiWriterManager.generateQuoteSmartAdvanced(context, selectedTopic, prompt, selectedStyle) { quote, isRealAi, statusMsg ->
                currentGeneratedQuote = quote
                layoutAiLoading.visibility = View.GONE
                btnGenerateAi.isEnabled = true

                cardAiResult.visibility = View.VISIBLE
                tvGeneratedQuote.text = quote
                tvAiModelBadge.text = if (isRealAi) "Google Gemini" else "مكتبة أدبية"

                scrollChatArea.post { scrollChatArea.fullScroll(View.FOCUS_DOWN) }

                if (isRealAi) {
                    Toast.makeText(context, "تمت الصياغة بنجاح بواسطة الذكاء الاصطناعي", Toast.LENGTH_SHORT).show()
                } else if (!statusMsg.isNullOrEmpty()) {
                    Toast.makeText(context, statusMsg, Toast.LENGTH_LONG).show()
                }
            }
        }

        // إرسال عبر الزر
        btnGenerateAi.setOnClickListener {
            doGenerate()
        }

        // إرسال عبر لوحة المفاتيح
        etCustomPrompt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                doGenerate()
                true
            } else {
                false
            }
        }

        // ربط المقترحات السريعة
        suggestPatience?.setOnClickListener {
            chipStyleWisdom?.let { selectStyle(it, "حكمة بليغة") }
            selectedTopic = "حكم وأمثال"
            val text = "حكمة بليغة ومؤثرة عن الصبر واليقين وحسن الظن بالله"
            etCustomPrompt.setText(text)
            doGenerate(text)
        }

        suggestCoffee?.setOnClickListener {
            chipStyleShort?.let { selectStyle(it, "خاطرة قصيرة") }
            selectedTopic = "خواطر هادئة"
            val text = "خاطرة مسائية دافئة وجميلة عن القهوة والسكينة وراحة البال"
            etCustomPrompt.setText(text)
            doGenerate(text)
        }

        suggestMotivation?.setOnClickListener {
            chipStyleWisdom?.let { selectStyle(it, "حكمة بليغة") }
            selectedTopic = "تحفيز ونجاح"
            val text = "كلمات تحفيزية قوية عن الإصرار ومواجهة الصعاب وتحقيق القمة"
            etCustomPrompt.setText(text)
            doGenerate(text)
        }

        suggestFriendship?.setOnClickListener {
            chipStyleWisdom?.let { selectStyle(it, "حكمة بليغة") }
            selectedTopic = "حكم وأمثال"
            val text = "اقتباس راقٍ وعميق عن الصداقة والوفاء والقلوب النقية"
            etCustomPrompt.setText(text)
            doGenerate(text)
        }

        suggestDua?.setOnClickListener {
            chipStyleShort?.let { selectStyle(it, "خاطرة قصيرة") }
            selectedTopic = "أدعية وأمل"
            val text = "دعاء يبعث الطمأنينة وراحة البال وتيسير الأمور"
            etCustomPrompt.setText(text)
            doGenerate(text)
        }

        // الإجراءات على الاقتباس
        btnCopy.setOnClickListener {
            if (currentGeneratedQuote.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Quote", currentGeneratedQuote)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.ai_copied_toast), Toast.LENGTH_SHORT).show()
            }
        }

        btnImage.setOnClickListener {
            if (currentGeneratedQuote.isNotEmpty() && context is AppCompatActivity) {
                dialog.dismiss()
                ImageUtils.showImageEditorDialog(context, selectedTopic, currentGeneratedQuote)
            }
        }

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
     * فتح صفحة الحصول على مفتاح مجاني من Google AI Studio
     */
    private fun openAiStudioWeb(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * نافذة متطورة لإدخال مفتاح Gemini الشخصي مع التحقق اللحظي التلقائي
     */
    private fun showGeminiKeyConfigDialog(context: Context, onKeyChanged: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_gemini_key_config, null)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvKeyDialogCurrentStatus)
        val btnOpenStudio = dialogView.findViewById<MaterialButton>(R.id.btnOpenAiStudio)
        val etKeyInput = dialogView.findViewById<EditText>(R.id.etGeminiApiKeyInput)
        val tvVerifying = dialogView.findViewById<TextView>(R.id.tvKeyVerifyingStatus)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSaveAndVerifyKey)
        val btnReset = dialogView.findViewById<MaterialButton>(R.id.btnResetDefaultKey)

        val userKey = AiWriterManager.getUserApiKey(context)
        if (userKey.isNotEmpty()) {
            tvStatus.text = "مفتاحك الشخصي مفعّل حالياً (توليد لا محدود)"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            etKeyInput.setText(userKey)
            etKeyInput.setSelection(userKey.length)
        } else {
            tvStatus.text = "جاري استخدام المفتاح التجريبي الافتراضي"
            tvStatus.setTextColor(Color.parseColor("#F59E0B"))
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnOpenStudio.setOnClickListener {
            openAiStudioWeb(context)
        }

        btnSave.setOnClickListener {
            val newKey = etKeyInput.text.toString().trim()
            if (newKey.isEmpty()) {
                Toast.makeText(context, "يرجى لصق مفتاح Gemini أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "جاري التحقق..."
            tvVerifying.visibility = View.VISIBLE
            tvVerifying.text = "جاري فحص المفتاح مع خوادم Google..."

            AiWriterManager.verifyKeyWithGemini(newKey) { isValid, errorMsg ->
                btnSave.isEnabled = true
                btnSave.text = "حفظ وتفعيل المفتاح"
                tvVerifying.visibility = View.GONE

                if (isValid) {
                    AiWriterManager.setUserApiKey(context, newKey)
                    Toast.makeText(context, "تم التحقق من مفتاحك وتفعيله بنجاح! التوليد الآن غير محدود وبسرعة فائقة.", Toast.LENGTH_LONG).show()
                    onKeyChanged()
                    alertDialog.dismiss()
                } else {
                    val alert = errorMsg ?: "المفتاح غير صالح أو لم يتم تفعيله بعد."
                    Toast.makeText(context, alert, Toast.LENGTH_LONG).show()
                }
            }
        }

        btnReset.setOnClickListener {
            AiWriterManager.clearUserApiKey(context)
            Toast.makeText(context, "تمت العودة للمفتاح الافتراضي.", Toast.LENGTH_SHORT).show()
            onKeyChanged()
            alertDialog.dismiss()
        }

        alertDialog.show()
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
}

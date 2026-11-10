package com.manchorati.www

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AiChatActivity : AppCompatActivity() {

    private lateinit var btnAiBack: ImageView
    private lateinit var dotAiStatus: View
    private lateinit var tvAiStatusSubtitle: TextView
    private lateinit var btnAiKeyTop: FrameLayout
    private lateinit var dotKeyBadge: View
    private lateinit var btnClearChat: ImageView

    private lateinit var layoutHeroEmpty: View
    private lateinit var rvAiChatMessages: RecyclerView
    private lateinit var chatAdapter: AiChatAdapter
    private val messages = mutableListOf<AiChatMessage>()

    private lateinit var etChatInput: EditText
    private lateinit var btnSendChat: FrameLayout

    // الأساليب
    private lateinit var pillStyleWisdom: TextView
    private lateinit var pillStyleShort: TextView
    private lateinit var pillStylePoem: TextView
    private lateinit var pillStylePolish: TextView
    private var selectedStyle = "حكمة بليغة"
    private var selectedTopic = "حكم وأمثال"

    private var isForPostResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        isForPostResult = intent.getBooleanExtra("EXTRA_FOR_POST", false)
        val initialPrompt = intent.getStringExtra("EXTRA_INITIAL_PROMPT") ?: ""
        val initialTopic = intent.getStringExtra("EXTRA_INITIAL_TOPIC") ?: ""

        if (initialTopic.isNotBlank()) {
            selectedTopic = initialTopic
        }

        initViews()
        setupWindowInsets()
        setupStyles()
        setupHeroCards()
        setupChatRecyclerView()
        updateHeaderKeyStatus()

        // إذا كان هناك نص مبدئي قادم من محرر المنشورات
        if (initialPrompt.isNotBlank()) {
            etChatInput.setText(initialPrompt)
            selectStyle(pillStylePolish, "تحسين وبلاغة")
            sendMessage(initialPrompt)
        }
    }

    private fun setupWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootAiChat)) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            findViewById<View>(R.id.toolbarAiChat).setPadding(
                (14 * resources.displayMetrics.density).toInt(),
                systemBars.top + (8 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt(),
                (8 * resources.displayMetrics.density).toInt()
            )
            findViewById<View>(R.id.layoutBottomBar).setPadding(
                (14 * resources.displayMetrics.density).toInt(),
                (10 * resources.displayMetrics.density).toInt(),
                (14 * resources.displayMetrics.density).toInt(),
                systemBars.bottom + (10 * resources.displayMetrics.density).toInt()
            )
            insets
        }
    }

    private fun initViews() {
        btnAiBack = findViewById(R.id.btnAiBack)
        dotAiStatus = findViewById(R.id.dotAiStatus)
        tvAiStatusSubtitle = findViewById(R.id.tvAiStatusSubtitle)
        btnAiKeyTop = findViewById(R.id.btnAiKeyTop)
        dotKeyBadge = findViewById(R.id.dotKeyBadge)
        btnClearChat = findViewById(R.id.btnClearChat)

        layoutHeroEmpty = findViewById(R.id.layoutHeroEmpty)
        rvAiChatMessages = findViewById(R.id.rvAiChatMessages)
        etChatInput = findViewById(R.id.etChatInput)
        btnSendChat = findViewById(R.id.btnSendChat)

        pillStyleWisdom = findViewById(R.id.pillStyleWisdom)
        pillStyleShort = findViewById(R.id.pillStyleShort)
        pillStylePoem = findViewById(R.id.pillStylePoem)
        pillStylePolish = findViewById(R.id.pillStylePolish)

        btnAiBack.setOnClickListener { finish() }

        btnClearChat.setOnClickListener {
            if (messages.isNotEmpty()) {
                messages.clear()
                chatAdapter.notifyDataSetChanged()
                updateListVisibility()
                Toast.makeText(this, "تم بدء محادثة جديدة", Toast.LENGTH_SHORT).show()
            }
        }

        btnAiKeyTop.setOnClickListener {
            showGeminiKeyConfigDialog()
        }

        btnSendChat.setOnClickListener {
            val text = etChatInput.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }

        etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = etChatInput.text.toString().trim()
                if (text.isNotEmpty()) {
                    sendMessage(text)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupStyles() {
        val allPills = listOf(pillStyleWisdom, pillStyleShort, pillStylePoem, pillStylePolish)

        fun select(selected: TextView, styleName: String) {
            selectedStyle = styleName
            allPills.forEach { pill ->
                if (pill == selected) {
                    pill.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    pill.setTextColor(Color.WHITE)
                } else {
                    pill.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    pill.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
        }

        pillStyleWisdom.setOnClickListener { select(pillStyleWisdom, "حكمة بليغة") }
        pillStyleShort.setOnClickListener { select(pillStyleShort, "خاطرة قصيرة") }
        pillStylePoem.setOnClickListener { select(pillStylePoem, "شعر فخم") }
        pillStylePolish.setOnClickListener { select(pillStylePolish, "تحسين وبلاغة") }
    }

    private fun selectStyle(selected: TextView, styleName: String) {
        selectedStyle = styleName
        val allPills = listOf(pillStyleWisdom, pillStyleShort, pillStylePoem, pillStylePolish)
        allPills.forEach { pill ->
            if (pill == selected) {
                pill.setBackgroundResource(R.drawable.bg_tool_chip_active)
                pill.setTextColor(Color.WHITE)
            } else {
                pill.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                pill.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }
    }

    private fun setupHeroCards() {
        findViewById<View>(R.id.cardPromptPatience).setOnClickListener {
            selectStyle(pillStyleWisdom, "حكمة بليغة")
            selectedTopic = "حكم وأمثال"
            sendMessage("اكتب لي حكمة بليغة ومؤثرة عن الصبر واليقين وحسن الظن بالله")
        }

        findViewById<View>(R.id.cardPromptCoffee).setOnClickListener {
            selectStyle(pillStyleShort, "خاطرة قصيرة")
            selectedTopic = "خواطر هادئة"
            sendMessage("خاطرة مسائية دافئة وجميلة عن فنجان القهوة والسكينة وراحة البال")
        }

        findViewById<View>(R.id.cardPromptMotivation).setOnClickListener {
            selectStyle(pillStyleWisdom, "حكمة بليغة")
            selectedTopic = "تحفيز ونجاح"
            sendMessage("كلمات تحفيزية قوية تلهم العزيمة ومواصلة الكفاح نحو القمة")
        }

        findViewById<View>(R.id.cardPromptPolish).setOnClickListener {
            selectStyle(pillStylePolish, "تحسين وبلاغة")
            etChatInput.hint = "ألصق أو اكتب النص الذي ترغب في تحسين بلاغته..."
            etChatInput.requestFocus()
            Toast.makeText(this, "ألصق كتابتك في الحقل أدناه لإعادة صياغتها ببلاغة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = AiChatAdapter(messages) { quote, topic ->
            handleQuoteActionPublish(quote, topic)
        }
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvAiChatMessages.layoutManager = layoutManager
        rvAiChatMessages.adapter = chatAdapter
        updateListVisibility()
    }

    private fun updateListVisibility() {
        if (messages.isEmpty()) {
            layoutHeroEmpty.visibility = View.VISIBLE
            rvAiChatMessages.visibility = View.GONE
        } else {
            layoutHeroEmpty.visibility = View.GONE
            rvAiChatMessages.visibility = View.VISIBLE
        }
    }

    private fun updateHeaderKeyStatus() {
        if (AiWriterManager.hasUserApiKey(this)) {
            dotAiStatus.setBackgroundResource(R.drawable.bg_status_dot_green)
            dotKeyBadge.setBackgroundResource(R.drawable.bg_status_dot_green)
            tvAiStatusSubtitle.text = "مفتاحك الشخصي مفعّل (غير محدود)"
            tvAiStatusSubtitle.setTextColor(Color.parseColor("#10B981"))
        } else {
            val remaining = AiWriterManager.getRemainingQuota(this)
            dotAiStatus.setBackgroundResource(R.drawable.bg_status_dot_amber)
            dotKeyBadge.setBackgroundResource(R.drawable.bg_status_dot_amber)
            tvAiStatusSubtitle.text = "المفتاح التجريبي ($remaining / ${AiWriterManager.MAX_DAILY_QUOTA} محاولات)"
            tvAiStatusSubtitle.setTextColor(Color.parseColor("#38BDF8"))
        }
    }

    private fun sendMessage(promptText: String) {
        val (canGen, message) = AiWriterManager.canGenerate(this)
        if (!canGen) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }

        AiWriterManager.recordGeneration(this)
        updateHeaderKeyStatus()

        // مسح الإدخال
        etChatInput.setText("")

        // 1. إضافة رسالة المستخدم
        messages.add(AiChatMessage.User(promptText))
        // 2. إضافة حالة التحميل
        messages.add(AiChatMessage.Loading)
        updateListVisibility()
        chatAdapter.notifyItemRangeInserted(messages.size - 2, 2)
        rvAiChatMessages.smoothScrollToPosition(messages.size - 1)

        btnSendChat.isEnabled = false

        AiWriterManager.generateQuoteSmartAdvanced(this, selectedTopic, promptText, selectedStyle) { quote, isRealAi, statusMsg ->
            btnSendChat.isEnabled = true

            // إزالة مؤشر التحميل
            val loadingIdx = messages.indexOfLast { it is AiChatMessage.Loading }
            if (loadingIdx != -1) {
                messages.removeAt(loadingIdx)
                chatAdapter.notifyItemRemoved(loadingIdx)
            }

            // إضافة رسالة المساعد
            val modelTag = if (isRealAi) "Google Gemini" else "مكتبة أدبية"
            messages.add(AiChatMessage.Bot(
                quote = quote,
                topic = selectedTopic,
                isRealAi = isRealAi,
                modelTag = modelTag
            ))
            val newIdx = messages.size - 1
            chatAdapter.notifyItemInserted(newIdx)
            rvAiChatMessages.smoothScrollToPosition(newIdx)

            if (!statusMsg.isNullOrEmpty() && !isRealAi) {
                Toast.makeText(this, statusMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleQuoteActionPublish(quote: String, topic: String) {
        if (isForPostResult) {
            val resultIntent = Intent().apply {
                putExtra("EXTRA_GENERATED_QUOTE", quote)
                putExtra("EXTRA_GENERATED_TOPIC", topic)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            // فتح إضافة منشور جديد مباشرة في MainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_OPEN_ADD_POST_QUOTE", quote)
                putExtra("EXTRA_OPEN_ADD_POST_TAG", topic)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun showGeminiKeyConfigDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gemini_key_config, null)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvKeyDialogCurrentStatus)
        val btnOpenStudio = dialogView.findViewById<MaterialButton>(R.id.btnOpenAiStudio)
        val etKeyInput = dialogView.findViewById<EditText>(R.id.etGeminiApiKeyInput)
        val tvVerifying = dialogView.findViewById<TextView>(R.id.tvKeyVerifyingStatus)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSaveAndVerifyKey)
        val btnReset = dialogView.findViewById<MaterialButton>(R.id.btnResetDefaultKey)

        val userKey = AiWriterManager.getUserApiKey(this)
        if (userKey.isNotEmpty()) {
            tvStatus.text = "مفتاحك الشخصي مفعّل حالياً (توليد لا محدود)"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            etKeyInput.setText(userKey)
            etKeyInput.setSelection(userKey.length)
        } else {
            tvStatus.text = "جاري استخدام المفتاح التجريبي الافتراضي"
            tvStatus.setTextColor(Color.parseColor("#F59E0B"))
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnOpenStudio.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "https://aistudio.google.com/app/apikey", Toast.LENGTH_LONG).show()
            }
        }

        btnSave.setOnClickListener {
            val newKey = etKeyInput.text.toString().trim()
            if (newKey.isEmpty()) {
                Toast.makeText(this, "يرجى لصق مفتاح Gemini أولاً", Toast.LENGTH_SHORT).show()
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
                    AiWriterManager.setUserApiKey(this, newKey)
                    Toast.makeText(this, "تم التحقق من مفتاحك وتفعيله بنجاح! التوليد الآن غير محدود.", Toast.LENGTH_LONG).show()
                    updateHeaderKeyStatus()
                    alertDialog.dismiss()
                } else {
                    val alert = errorMsg ?: "المفتاح غير صالح أو لم يتم تفعيله بعد."
                    Toast.makeText(this, alert, Toast.LENGTH_LONG).show()
                }
            }
        }

        btnReset.setOnClickListener {
            AiWriterManager.clearUserApiKey(this)
            Toast.makeText(this, "تمت العودة للمفتاح الافتراضي.", Toast.LENGTH_SHORT).show()
            updateHeaderKeyStatus()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}

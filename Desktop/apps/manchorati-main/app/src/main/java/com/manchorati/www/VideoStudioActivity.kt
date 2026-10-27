package com.manchorati.www

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.InputStream

class VideoStudioActivity : AppCompatActivity() {

    private lateinit var videoMotionView: VideoMotionView
    private lateinit var ivPlayPauseIndicator: ImageView

    // Ratio & Duration
    private lateinit var btnRatioStory: TextView
    private lateinit var btnRatioSquare: TextView
    private lateinit var btnDur5s: TextView
    private lateinit var btnDur10s: TextView
    private lateinit var btnDur15s: TextView

    // Tabs
    private lateinit var tabMotion: TextView
    private lateinit var tabAudio: TextView
    private lateinit var tabAnim: TextView
    private lateinit var tabFont: TextView
    private lateinit var scrollMotion: HorizontalScrollView
    private lateinit var scrollAudio: HorizontalScrollView
    private lateinit var scrollAnim: HorizontalScrollView
    private lateinit var scrollFont: HorizontalScrollView

    // Containers
    private lateinit var layoutMotionPresets: LinearLayout
    private lateinit var layoutAudioPresets: LinearLayout
    private lateinit var layoutAnimStyles: LinearLayout
    private lateinit var layoutFontPresets: LinearLayout

    // Actions
    private lateinit var btnSaveGallery: LinearLayout
    private lateinit var btnShareWhatsApp: LinearLayout
    private lateinit var btnShareGeneral: FrameLayout

    // State
    private var selectedMotion = "STARS"
    private var selectedAudio = "piano"
    private var selectedAnim = "typewriter"
    private var selectedRatio = "9:16"
    private var selectedDuration = 10
    private var selectedFont = "tajawal"
    private var selectedTextColor = "#FFFFFF"

    private var customImageBitmap: Bitmap? = null
    private var customAudioUri: Uri? = null

    private var postContent = ""
    private var postAuthorName = ""
    private var postAuthorUsername = ""
    private var postAuthorBadge = ""
    private var postTag = ""

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            loadCustomImage(uri)
        }
    }

    // Audio Picker
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            loadCustomAudio(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_studio)

        initIntentData()
        initViews()
        applyWindowInsets()
        setupListeners()
        populateMotionPresets()
        populateAudioPresets()
        populateAnimPresets()
        populateFontPresets()
        updateConfig()
    }

    private fun initIntentData() {
        postContent = intent.getStringExtra("EXTRA_CONTENT") ?: "كل لحظة هي بداية جديدة، فاصنع منها ما يستحق أن يُذكر."
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val user = AuthManager.currentUser

        postAuthorName = intent.getStringExtra("EXTRA_AUTHOR_NAME")
            ?: prefs.getString("user_name", "")?.takeIf { it.isNotEmpty() }
            ?: user?.displayName
            ?: "من منشوراتي"

        postAuthorUsername = intent.getStringExtra("EXTRA_AUTHOR_USERNAME")
            ?: prefs.getString("user_username", "")?.takeIf { it.isNotEmpty() }
            ?: "manchorati"

        postAuthorBadge = intent.getStringExtra("EXTRA_AUTHOR_BADGE")
            ?: BadgeHelper.getCachedBadge(user?.uid ?: "")?.iconEmoji
            ?: ""

        postTag = intent.getStringExtra("EXTRA_TAG") ?: "حكمة"
        selectedFont = intent.getStringExtra("EXTRA_FONT_ID") ?: "tajawal"
        selectedTextColor = intent.getStringExtra("EXTRA_TEXT_COLOR") ?: "#FFFFFF"
        selectedMotion = intent.getStringExtra("EXTRA_BG_MOTION_ID") ?: "STARS"
        selectedAudio = intent.getStringExtra("EXTRA_AUDIO_PRESET") ?: "piano"
        selectedAnim = intent.getStringExtra("EXTRA_ANIM_STYLE") ?: "typewriter"
        selectedRatio = intent.getStringExtra("EXTRA_VIDEO_RATIO") ?: "9:16"
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBackContainer).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        videoMotionView = findViewById(R.id.videoMotionView)
        ivPlayPauseIndicator = findViewById(R.id.ivPlayPauseIndicator)

        btnRatioStory = findViewById(R.id.btnRatioStory)
        btnRatioSquare = findViewById(R.id.btnRatioSquare)
        btnDur5s = findViewById(R.id.btnDur5s)
        btnDur10s = findViewById(R.id.btnDur10s)
        btnDur15s = findViewById(R.id.btnDur15s)

        tabMotion = findViewById(R.id.tabMotion)
        tabAudio = findViewById(R.id.tabAudio)
        tabAnim = findViewById(R.id.tabAnim)
        tabFont = findViewById(R.id.tabFont)

        scrollMotion = findViewById(R.id.scrollMotion)
        scrollAudio = findViewById(R.id.scrollAudio)
        scrollAnim = findViewById(R.id.scrollAnim)
        scrollFont = findViewById(R.id.scrollFont)

        layoutMotionPresets = findViewById(R.id.layoutMotionPresets)
        layoutAudioPresets = findViewById(R.id.layoutAudioPresets)
        layoutAnimStyles = findViewById(R.id.layoutAnimStyles)
        layoutFontPresets = findViewById(R.id.layoutFontPresets)

        btnSaveGallery = findViewById(R.id.btnSaveGallery)
        btnShareWhatsApp = findViewById(R.id.btnShareWhatsApp)
        btnShareGeneral = findViewById(R.id.btnShareGeneral)
    }

    private fun applyWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#080C14")

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val spacer = findViewById<View>(R.id.statusBarSpacer)
            if (spacer != null && spacer.layoutParams.height != statusBar.top) {
                spacer.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    statusBar.top
                )
            }

            findViewById<View>(R.id.layoutBottomDock)?.setPadding(
                (16 * density).toInt(),
                (10 * density).toInt(),
                (16 * density).toInt(),
                navBar.bottom + (10 * density).toInt()
            )

            insets
        }
    }

    private fun setupListeners() {
        // Tap canvas toggles play/pause with sleek feedback
        videoMotionView.setOnClickListener {
            videoMotionView.togglePlay()
            showPlayPauseFeedback(!videoMotionView.isPlaying)
        }

        // Ratio Toggle
        btnRatioStory.setOnClickListener {
            selectedRatio = "9:16"
            btnRatioStory.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnRatioStory.setTextColor(Color.WHITE)
            btnRatioSquare.background = null
            btnRatioSquare.setTextColor(Color.parseColor("#94A3B8"))
            updateConfig()
        }
        btnRatioSquare.setOnClickListener {
            selectedRatio = "1:1"
            btnRatioSquare.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnRatioSquare.setTextColor(Color.WHITE)
            btnRatioStory.background = null
            btnRatioStory.setTextColor(Color.parseColor("#94A3B8"))
            updateConfig()
        }

        // Duration Selectors
        val durBtns = listOf(btnDur5s to 5, btnDur10s to 10, btnDur15s to 15)
        for ((btn, sec) in durBtns) {
            btn.setOnClickListener {
                selectedDuration = sec
                for ((b, _) in durBtns) {
                    if (b == btn) {
                        b.setBackgroundResource(R.drawable.bg_segment_btn_active)
                        b.setTextColor(Color.WHITE)
                    } else {
                        b.background = null
                        b.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
                updateConfig()
                videoMotionView.restart()
            }
        }

        // Tabs Toggle
        tabMotion.setOnClickListener { switchTab(0) }
        tabAudio.setOnClickListener { switchTab(1) }
        tabAnim.setOnClickListener { switchTab(2) }
        tabFont.setOnClickListener { switchTab(3) }

        // Bottom Actions
        btnSaveGallery.setOnClickListener {
            startExport(onComplete = { file ->
                val uri = VideoExporter.saveVideoToGallery(this, file, postAuthorName)
                if (uri != null) {
                    Toast.makeText(this, "تم حفظ الفيديو بنجاح في ألبوم Manchorati! 🎉", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "حدث خطأ أثناء حفظ الفيديو", Toast.LENGTH_SHORT).show()
                }
            })
        }

        btnShareWhatsApp.setOnClickListener {
            startExport(onComplete = { file ->
                VideoExporter.shareVideoToWhatsApp(this, file)
            })
        }

        btnShareGeneral.setOnClickListener {
            startExport(onComplete = { file ->
                VideoExporter.shareVideoGeneral(this, file)
            })
        }
    }

    private fun showPlayPauseFeedback(isPaused: Boolean) {
        ivPlayPauseIndicator.setImageResource(
            if (isPaused) R.drawable.ic_video_play else R.drawable.ic_video_pause
        )
        ivPlayPauseIndicator.visibility = View.VISIBLE
        ivPlayPauseIndicator.alpha = 1f
        ivPlayPauseIndicator.animate()
            .alpha(0f)
            .setDuration(450)
            .setStartDelay(200)
            .withEndAction { ivPlayPauseIndicator.visibility = View.GONE }
            .start()
    }

    private fun switchTab(tabIndex: Int) {
        val tabs = listOf(tabMotion, tabAudio, tabAnim, tabFont)
        for (i in tabs.indices) {
            if (i == tabIndex) {
                tabs[i].setBackgroundResource(R.drawable.bg_segment_btn_active)
                tabs[i].setTextColor(Color.WHITE)
            } else {
                tabs[i].background = null
                tabs[i].setTextColor(Color.parseColor("#94A3B8"))
            }
        }
        scrollMotion.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        scrollAudio.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        scrollAnim.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        scrollFont.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE
    }

    private fun populateMotionPresets() {
        layoutMotionPresets.removeAllViews()
        val presets = listOf(
            Triple("STARS", "✨ فضاء ونجوم", "#0D162B"),
            Triple("RAIN", "🌧️ مطر ناعم", "#131C2E"),
            Triple("AURORA", "🌌 شفق أخضر", "#0A1226"),
            Triple("GOLD", "🌟 بريق ذهبي", "#281F18"),
            Triple("OCEAN", "🌊 أمواج هادئة", "#0A2540"),
            Triple("CUSTOM_IMAGE", "🖼️ صورتي", "#1E293B")
        )

        for ((id, title, colorHex) in presets) {
            val chip = createStudioCard(title, id == selectedMotion, colorHex) {
                if (id == "CUSTOM_IMAGE") {
                    pickImageLauncher.launch("image/*")
                } else {
                    selectedMotion = id
                    customImageBitmap = null
                    populateMotionPresets()
                    updateConfig()
                }
            }
            layoutMotionPresets.addView(chip)
        }
    }

    private fun populateAudioPresets() {
        layoutAudioPresets.removeAllViews()
        val audios = listOf(
            Triple("piano", "🎹 بيانو هادئ", "#0369A1"),
            Triple("rain", "🌧️ صوت المطر", "#0284C7"),
            Triple("oud", "🪕 عود شرقي", "#B45309"),
            Triple("nature", "🍃 نسيم وطبيعة", "#047857"),
            Triple("mute", "🔇 صامت", "#334155"),
            Triple("custom", "📁 ملف صوتي", "#6D28D9")
        )

        for ((id, title, colorHex) in audios) {
            val chip = createStudioCard(title, id == selectedAudio, colorHex) {
                if (id == "custom") {
                    pickAudioLauncher.launch("audio/*")
                } else {
                    selectedAudio = id
                    customAudioUri = null
                    videoMotionView.customAudioUri = null
                    populateAudioPresets()
                    updateConfig()
                }
            }
            layoutAudioPresets.addView(chip)
        }
    }

    private fun populateAnimPresets() {
        layoutAnimStyles.removeAllViews()
        val anims = listOf(
            Triple("typewriter", "⌨️ كتابة حية", "#0284C7"),
            Triple("fade", "✨ ظهور ناعم", "#7C3AED"),
            Triple("zoom", "🔍 تقريب سينمائي", "#C026D3")
        )

        for ((id, title, colorHex) in anims) {
            val chip = createStudioCard(title, id == selectedAnim, colorHex) {
                selectedAnim = id
                populateAnimPresets()
                updateConfig()
                videoMotionView.restart()
            }
            layoutAnimStyles.addView(chip)
        }
    }

    private fun populateFontPresets() {
        layoutFontPresets.removeAllViews()
        val fonts = listOf(
            "tajawal" to "تجوال",
            "amiri" to "أميري",
            "cairo" to "كايرو",
            "changa" to "تشانجا",
            "marhey" to "مرحي",
            "noto_kufi" to "كوفي"
        )

        for ((fontId, fontName) in fonts) {
            val chip = createStudioCard(fontName, fontId == selectedFont, "#1E293B") {
                selectedFont = fontId
                populateFontPresets()
                updateConfig()
            }
            layoutFontPresets.addView(chip)
        }
    }

    private fun createStudioCard(title: String, isSelected: Boolean, colorHex: String, onClick: () -> Unit): View {
        val density = resources.displayMetrics.density
        val tv = TextView(this).apply {
            text = title
            textSize = 12.5f
            setPadding((16 * density).toInt(), (9 * density).toInt(), (16 * density).toInt(), (9 * density).toInt())
            setTextColor(Color.WHITE)

            val bg = GradientDrawable().apply {
                cornerRadius = 14 * density
                if (isSelected) {
                    setColor(Color.parseColor(colorHex))
                    setStroke((2 * density).toInt(), Color.parseColor("#38BDF8"))
                } else {
                    setColor(Color.parseColor("#141E33"))
                    setStroke((1 * density).toInt(), Color.parseColor("#25334E"))
                }
            }
            background = bg

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (10 * density).toInt()
            }
            layoutParams = params
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        return tv
    }

    private fun loadCustomImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (original != null) {
                val maxDim = 1280
                val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1.0f)
                customImageBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        original,
                        (original.width * scale).toInt(),
                        (original.height * scale).toInt(),
                        true
                    )
                } else {
                    original
                }

                selectedMotion = "CUSTOM_IMAGE"
                populateMotionPresets()
                updateConfig()
                Toast.makeText(this, "تم اختيار الصورة الشخصية بنجاح 🖼️", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "فشل تحميل الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCustomAudio(uri: Uri) {
        customAudioUri = uri
        selectedAudio = "custom"
        videoMotionView.customAudioUri = uri
        populateAudioPresets()
        updateConfig()
        Toast.makeText(this, "تم اختيار الملف الصوتي بنجاح 🎵", Toast.LENGTH_SHORT).show()
    }

    private fun updateConfig() {
        val config = VideoMotionRenderer.VideoConfig(
            content = postContent,
            authorName = postAuthorName,
            authorUsername = postAuthorUsername,
            authorBadge = postAuthorBadge,
            tag = postTag,
            bgMotionId = selectedMotion,
            audioPreset = selectedAudio,
            animStyle = selectedAnim,
            aspectRatio = selectedRatio,
            durationSec = selectedDuration,
            fontId = selectedFont,
            textColor = selectedTextColor,
            customImageBitmap = customImageBitmap
        )
        videoMotionView.config = config
    }

    private fun startExport(onComplete: (File) -> Unit) {
        videoMotionView.pause()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#0F172A"))
            gravity = android.view.Gravity.CENTER
        }

        val tvTitle = TextView(this).apply {
            text = "🎬 جاري تصدير الفيديو بجودة عالية..."
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 30
                bottomMargin = 20
            }
            layoutParams = params
        }

        val tvPercent = TextView(this).apply {
            text = "0%"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }

        container.addView(tvTitle)
        container.addView(progressBar)
        container.addView(tvPercent)

        dialog.setContentView(container)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        val config = videoMotionView.config

        VideoExporter.exportVideo(
            context = this,
            config = config,
            customAudioUri = customAudioUri,
            onProgress = { percent ->
                progressBar.progress = percent
                tvPercent.text = "$percent%"
            },
            onSuccess = { file ->
                dialog.dismiss()
                onComplete(file)
            },
            onError = { exc ->
                dialog.dismiss()
                Toast.makeText(this, "حدث خطأ أثناء تصدير الفيديو: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        videoMotionView.play()
    }

    override fun onPause() {
        super.onPause()
        videoMotionView.pause()
    }
}

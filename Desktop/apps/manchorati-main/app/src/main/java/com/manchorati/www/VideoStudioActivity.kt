package com.manchorati.www

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.io.InputStream
import java.util.Locale

class VideoStudioActivity : AppCompatActivity() {

    private lateinit var videoMotionView: VideoMotionView
    private lateinit var ivPlayPauseIndicator: ImageView

    // Top Bar
    private lateinit var btnBackContainer: FrameLayout
    private lateinit var btnEnregContainer: FrameLayout
    private lateinit var btnEnreg: TextView

    // CapCut Playback Controls
    private lateinit var tvTimeCurrent: TextView
    private lateinit var ivPlayPauseCenter: ImageView
    private lateinit var ivMuteToggle: ImageView

    // Timeline Filmstrip
    private lateinit var timelineFilmstripView: TimelineFilmstripView

    // Tool Panels
    private lateinit var layoutToolPanelContainer: LinearLayout
    private lateinit var tvToolPanelTitle: TextView
    private lateinit var btnCloseToolPanel: ImageView

    private lateinit var scrollAudio: HorizontalScrollView
    private lateinit var scrollAnim: HorizontalScrollView
    private lateinit var scrollFont: HorizontalScrollView
    private lateinit var scrollOptions: HorizontalScrollView
    private lateinit var scrollMotion: HorizontalScrollView

    private lateinit var layoutAudioPresets: LinearLayout
    private lateinit var layoutAnimStyles: LinearLayout
    private lateinit var layoutFontPresets: LinearLayout
    private lateinit var layoutMotionPresets: LinearLayout

    // Autocollant Panel Toggles & Selectors
    private lateinit var btnDur15s: TextView
    private lateinit var btnDur30s: TextView
    private lateinit var btnDur45s: TextView
    private lateinit var btnRatioStory: TextView
    private lateinit var btnRatioSquare: TextView
    private lateinit var btnToggleAuthor: TextView
    private lateinit var btnToggleTag: TextView

    // Bottom Toolbar Items (Exact CapCut Style)
    private lateinit var toolAudio: LinearLayout
    private lateinit var toolEffects: LinearLayout
    private lateinit var toolText: LinearLayout
    private lateinit var toolStickers: LinearLayout
    private lateinit var toolPhoto: LinearLayout

    // State
    private var selectedMotion = "STARS"
    private var selectedAudio = "piano"
    private var selectedAnim = "typewriter"
    private var selectedRatio = "9:16"
    private var selectedDuration = 15 // Default 15s
    private var selectedFont = "tajawal"
    private var selectedTextColor = "#FFFFFF"
    private var showAuthor = true
    private var showTag = false

    private var customImageBitmap: Bitmap? = null
    private var customAudioUri: Uri? = null
    private var downloadedAudioPath: String? = null

    private var postContent = ""
    private var postAuthorName = ""
    private var postAuthorUsername = ""
    private var postAuthorBadge = ""
    private var postTag = ""

    private var activeToolIndex = -1 // -1 = closed

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
        generateFilmstripThumb()
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
        btnBackContainer = findViewById(R.id.btnBackContainer)
        btnEnregContainer = findViewById(R.id.btnEnregContainer)
        btnEnreg = findViewById(R.id.btnEnreg)

        videoMotionView = findViewById(R.id.videoMotionView)
        ivPlayPauseIndicator = findViewById(R.id.ivPlayPauseIndicator)

        tvTimeCurrent = findViewById(R.id.tvTimeCurrent)
        ivPlayPauseCenter = findViewById(R.id.ivPlayPauseCenter)
        ivMuteToggle = findViewById(R.id.ivMuteToggle)

        timelineFilmstripView = findViewById(R.id.timelineFilmstripView)
        timelineFilmstripView.durationSec = selectedDuration

        layoutToolPanelContainer = findViewById(R.id.layoutToolPanelContainer)
        tvToolPanelTitle = findViewById(R.id.tvToolPanelTitle)
        btnCloseToolPanel = findViewById(R.id.btnCloseToolPanel)

        scrollAudio = findViewById(R.id.scrollAudio)
        scrollAnim = findViewById(R.id.scrollAnim)
        scrollFont = findViewById(R.id.scrollFont)
        scrollOptions = findViewById(R.id.scrollOptions)
        scrollMotion = findViewById(R.id.scrollMotion)

        layoutAudioPresets = findViewById(R.id.layoutAudioPresets)
        layoutAnimStyles = findViewById(R.id.layoutAnimStyles)
        layoutFontPresets = findViewById(R.id.layoutFontPresets)
        layoutMotionPresets = findViewById(R.id.layoutMotionPresets)

        btnDur15s = findViewById(R.id.btnDur15s)
        btnDur30s = findViewById(R.id.btnDur30s)
        btnDur45s = findViewById(R.id.btnDur45s)
        btnRatioStory = findViewById(R.id.btnRatioStory)
        btnRatioSquare = findViewById(R.id.btnRatioSquare)
        btnToggleAuthor = findViewById(R.id.btnToggleAuthor)
        btnToggleTag = findViewById(R.id.btnToggleTag)

        toolAudio = findViewById(R.id.toolAudio)
        toolEffects = findViewById(R.id.toolEffects)
        toolText = findViewById(R.id.toolText)
        toolStickers = findViewById(R.id.toolStickers)
        toolPhoto = findViewById(R.id.toolPhoto)

        tvTimeCurrent.text = formatTime(selectedDuration * 1000L)
    }

    private fun applyWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#121212")

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

            val navSpacer = findViewById<View>(R.id.navBarSpacer)
            if (navSpacer != null && navSpacer.layoutParams.height != navBar.bottom) {
                navSpacer.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    navBar.bottom
                )
            }

            insets
        }
    }

    private fun setupListeners() {
        btnBackContainer.setOnClickListener { finish() }

        // Top Right "Enreg." button opens export sheet
        btnEnregContainer.setOnClickListener {
            showExportActionSheet()
        }

        // Tap video canvas toggles play/pause
        videoMotionView.setOnClickListener {
            videoMotionView.togglePlay()
            showPlayPauseFeedback(!videoMotionView.isPlaying)
        }

        // Center Play/Pause button
        ivPlayPauseCenter.setOnClickListener {
            videoMotionView.togglePlay()
        }

        // Mute/Speaker button
        ivMuteToggle.setOnClickListener {
            videoMotionView.isMuted = !videoMotionView.isMuted
            ivMuteToggle.setImageResource(
                if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            )
            Toast.makeText(
                this,
                if (videoMotionView.isMuted) "تم كتم الصوت 🔇" else "تم تشغيل الصوت 🔊",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Play/Pause State Sync
        videoMotionView.onPlayStateChanged = { isPlaying ->
            ivPlayPauseCenter.setImageResource(
                if (isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play
            )
        }

        // Live Progress Sync with Timeline Filmstrip
        videoMotionView.onProgressUpdate = { curMs, totalMs, ratio ->
            timelineFilmstripView.currentRatio = ratio
            tvTimeCurrent.text = formatTime(curMs)
        }

        // Timeline Scrubbing / Seeking
        timelineFilmstripView.onSeek = { ratio ->
            videoMotionView.seekTo(ratio)
            val curMs = (ratio * selectedDuration * 1000L).toLong()
            tvTimeCurrent.text = formatTime(curMs)
        }

        // Plus icon on timeline opens options / ratio
        timelineFilmstripView.onPlusClick = {
            openToolPanel(3) // open Autocollant / Options
        }

        // Close Tool Panel button
        btnCloseToolPanel.setOnClickListener {
            closeToolPanel()
        }

        // Bottom 5 Tools (Exact CapCut Navigation)
        toolAudio.setOnClickListener { openToolPanel(0) }
        toolEffects.setOnClickListener { openToolPanel(1) }
        toolText.setOnClickListener { openToolPanel(2) }
        toolStickers.setOnClickListener { openToolPanel(3) }
        toolPhoto.setOnClickListener { openToolPanel(4) }

        // Duration Selectors in Options Panel
        val durBtns = listOf(btnDur15s to 15, btnDur30s to 30, btnDur45s to 45)
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
                timelineFilmstripView.durationSec = selectedDuration
                updateConfig()
                videoMotionView.restart()
            }
        }

        // Ratio Toggle in Options Panel
        btnRatioStory.setOnClickListener {
            selectedRatio = "9:16"
            btnRatioStory.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnRatioStory.setTextColor(Color.WHITE)
            btnRatioSquare.background = null
            btnRatioSquare.setTextColor(Color.parseColor("#94A3B8"))
            updateConfig()
            generateFilmstripThumb()
        }
        btnRatioSquare.setOnClickListener {
            selectedRatio = "1:1"
            btnRatioSquare.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnRatioSquare.setTextColor(Color.WHITE)
            btnRatioStory.background = null
            btnRatioStory.setTextColor(Color.parseColor("#94A3B8"))
            updateConfig()
            generateFilmstripThumb()
        }

        // Visibility Toggles in Options Panel
        btnToggleAuthor.setOnClickListener {
            showAuthor = !showAuthor
            if (showAuthor) {
                btnToggleAuthor.setBackgroundResource(R.drawable.bg_segment_btn_active)
                btnToggleAuthor.setTextColor(Color.WHITE)
            } else {
                btnToggleAuthor.background = null
                btnToggleAuthor.setTextColor(Color.parseColor("#94A3B8"))
            }
            updateConfig()
            generateFilmstripThumb()
        }

        btnToggleTag.setOnClickListener {
            showTag = !showTag
            if (showTag) {
                btnToggleTag.setBackgroundResource(R.drawable.bg_segment_btn_active)
                btnToggleTag.setTextColor(Color.WHITE)
            } else {
                btnToggleTag.background = null
                btnToggleTag.setTextColor(Color.parseColor("#94A3B8"))
            }
            updateConfig()
            generateFilmstripThumb()
        }
    }

    private fun openToolPanel(index: Int) {
        if (activeToolIndex == index && layoutToolPanelContainer.visibility == View.VISIBLE) {
            closeToolPanel()
            return
        }

        activeToolIndex = index
        layoutToolPanelContainer.visibility = View.VISIBLE

        scrollAudio.visibility = if (index == 0) View.VISIBLE else View.GONE
        scrollAnim.visibility = if (index == 1) View.VISIBLE else View.GONE
        scrollFont.visibility = if (index == 2) View.VISIBLE else View.GONE
        scrollOptions.visibility = if (index == 3) View.VISIBLE else View.GONE
        scrollMotion.visibility = if (index == 4) View.VISIBLE else View.GONE

        tvToolPanelTitle.text = when (index) {
            0 -> "Audio (الصوت والموسيقى)"
            1 -> "Effets (حركات النص)"
            2 -> "Ajouter Texte (الخطوط العربية)"
            3 -> "Autocollant (الخيارات والوسم)"
            else -> "Aj. Photo (الخلفيات والتأثيرات)"
        }
    }

    private fun closeToolPanel() {
        activeToolIndex = -1
        layoutToolPanelContainer.visibility = View.GONE
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
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

    private fun populateAudioPresets() {
        layoutAudioPresets.removeAllViews()

        // 1. Built-in Offline Loops (Stereo Normalized 44.1kHz)
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
                    downloadedAudioPath = null
                    videoMotionView.customAudioUri = null
                    populateAudioPresets()
                    updateConfig()
                }
            }
            layoutAudioPresets.addView(chip)
        }

        // 2. Downloadable Ambient Audios (Cached locally)
        for (onlineAudio in StudioAssetManager.onlineAudios) {
            val isDownloaded = StudioAssetManager.isAudioDownloaded(this, onlineAudio.id)
            val chipTitle = if (isDownloaded) onlineAudio.title else "${onlineAudio.title} ⬇️"
            val isSelected = selectedAudio == onlineAudio.id

            val chip = createStudioCard(chipTitle, isSelected, onlineAudio.colorHex) {
                if (isDownloaded) {
                    val path = StudioAssetManager.getDownloadedAudioPath(this, onlineAudio.id)
                    selectedAudio = onlineAudio.id
                    customAudioUri = null
                    downloadedAudioPath = path
                    videoMotionView.customAudioUri = null
                    populateAudioPresets()
                    updateConfig()
                } else {
                    Toast.makeText(this, "جاري تحميل الصوت ⏳", Toast.LENGTH_SHORT).show()
                    StudioAssetManager.downloadAudio(
                        this,
                        onlineAudio,
                        onSuccess = { path ->
                            runOnUiThread {
                                selectedAudio = onlineAudio.id
                                customAudioUri = null
                                downloadedAudioPath = path
                                videoMotionView.customAudioUri = null
                                populateAudioPresets()
                                updateConfig()
                                Toast.makeText(this, "تم تحميل الصوت بنجاح 🎵", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onError = { err ->
                            runOnUiThread {
                                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            layoutAudioPresets.addView(chip)
        }
    }

    private fun populateAnimPresets() {
        layoutAnimStyles.removeAllViews()
        val anims = listOf(
            Triple("typewriter", "⌨️ كتابة حية", "#0284C7"),
            Triple("word_by_word", "📖 كلمة بكلمة", "#10B981"),
            Triple("fade", "🌫️ تلاشي ناعم", "#7C3AED"),
            Triple("zoom", "🔍 زووم سينمائي", "#C026D3"),
            Triple("glow", "✨ توهج نيون", "#06B6D4"),
            Triple("slide_up", "⬆️ صعود تدريجي", "#F59E0B"),
            Triple("blur_in", "💫 تركيز عدسة", "#EC4899")
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

        // All 8 authentic Arabic fonts with live preview
        val fonts = listOf(
            "tajawal" to "تجوال",
            "cairoplay" to "كايرو",
            "arefruqaa" to "رقعة",
            "kufam" to "كوفي",
            "notonastaliqurdu" to "نستعليق",
            "marhey" to "مرحي",
            "blakahollow" to "مفرغ",
            "oi" to "عريض"
        )

        for ((fontId, fontName) in fonts) {
            val chip = createStudioCard(
                title = fontName,
                isSelected = fontId == selectedFont,
                colorHex = "#1E293B",
                fontId = fontId
            ) {
                selectedFont = fontId
                populateFontPresets()
                updateConfig()
                generateFilmstripThumb()
            }
            layoutFontPresets.addView(chip)
        }
    }

    private fun populateMotionPresets() {
        layoutMotionPresets.removeAllViews()

        // 1. Built-in Dynamic Motion Shaders
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
                    generateFilmstripThumb()
                }
            }
            layoutMotionPresets.addView(chip)
        }

        // 2. Curated Aesthetic Online Backgrounds (Unsplash CDN, cached locally)
        for (onlineBg in StudioAssetManager.onlineBackgrounds) {
            val isDownloaded = StudioAssetManager.isBackgroundDownloaded(this, onlineBg.id)
            val chipTitle = if (isDownloaded) onlineBg.title else "${onlineBg.title} ⬇️"
            val isSelected = selectedMotion == onlineBg.id

            val chip = createStudioCard(chipTitle, isSelected, onlineBg.previewColor) {
                if (isDownloaded) {
                    val bmp = StudioAssetManager.getDownloadedBackgroundBitmap(this, onlineBg.id)
                    if (bmp != null) {
                        customImageBitmap = bmp
                        selectedMotion = onlineBg.id
                        populateMotionPresets()
                        updateConfig()
                        generateFilmstripThumb()
                    }
                } else {
                    Toast.makeText(this, "جاري تحميل الخلفية ⏳", Toast.LENGTH_SHORT).show()
                    StudioAssetManager.downloadBackground(
                        this,
                        onlineBg,
                        onSuccess = { bmp ->
                            runOnUiThread {
                                customImageBitmap = bmp
                                selectedMotion = onlineBg.id
                                populateMotionPresets()
                                updateConfig()
                                generateFilmstripThumb()
                                Toast.makeText(this, "تم تحميل الخلفية بنجاح ✨", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onError = { err ->
                            runOnUiThread {
                                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
            layoutMotionPresets.addView(chip)
        }
    }

    private fun createStudioCard(
        title: String,
        isSelected: Boolean,
        colorHex: String,
        fontId: String? = null,
        onClick: () -> Unit
    ): View {
        val density = resources.displayMetrics.density
        val tv = TextView(this).apply {
            text = title
            textSize = 12.5f
            setPadding((16 * density).toInt(), (9 * density).toInt(), (16 * density).toInt(), (9 * density).toInt())
            setTextColor(Color.WHITE)

            if (fontId != null) {
                val tf = ThemeHelper.getFontTypeface(this@VideoStudioActivity, fontId)
                if (tf != null) {
                    typeface = tf
                    textSize = 14f
                }
            }

            val bg = GradientDrawable().apply {
                cornerRadius = 12 * density
                if (isSelected) {
                    setColor(Color.parseColor(colorHex))
                    setStroke((2 * density).toInt(), Color.parseColor("#38BDF8"))
                } else {
                    setColor(Color.parseColor("#1C202B"))
                    setStroke((1 * density).toInt(), Color.parseColor("#2E3748"))
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
                generateFilmstripThumb()
                Toast.makeText(this, "تم اختيار الصورة بنجاح 🖼️", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "فشل تحميل الصورة: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCustomAudio(uri: Uri) {
        customAudioUri = uri
        selectedAudio = "custom"
        downloadedAudioPath = null
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
            customImageBitmap = customImageBitmap,
            showTag = showTag,
            showAuthor = showAuthor,
            downloadedAudioPath = downloadedAudioPath
        )
        videoMotionView.config = config
    }

    private fun generateFilmstripThumb() {
        try {
            val w = 180
            val h = if (selectedRatio == "9:16") 320 else 180
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            VideoMotionRenderer.renderFrame(this, canvas, w, h, 2000L, videoMotionView.config)
            timelineFilmstripView.thumbnailBitmap = bmp
        } catch (_: Exception) {}
    }

    private fun showExportActionSheet() {
        videoMotionView.pause()

        val bottomSheet = BottomSheetDialog(this)
        val density = resources.displayMetrics.density

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#15171C"))
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (28 * density).toInt())
        }

        // Title
        val tvSheetTitle = TextView(this).apply {
            text = "تصدير الفيديو ومشاركته 🎬"
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, (20 * density).toInt())
        }
        container.addView(tvSheetTitle)

        // 1. WhatsApp Status Option
        val btnWhatsApp = createExportOptionRow(
            title = "مشاركة في حالة واتساب (Status)",
            desc = "نشر الفيديو مباشرة كحالة واتساب بجودة عالية",
            iconRes = R.drawable.ic_whatsapp,
            accentColor = "#25D366"
        ) {
            bottomSheet.dismiss()
            startExport { file ->
                VideoExporter.shareVideoToWhatsApp(this, file)
            }
        }
        container.addView(btnWhatsApp)

        // 2. Save to Gallery Option
        val btnGallery = createExportOptionRow(
            title = "حفظ الفيديو في المعرض (HD MP4)",
            desc = "تخزين دائم في ألبوم Manchorati بجودة أصلية",
            iconRes = R.drawable.ic_download,
            accentColor = "#0284C7"
        ) {
            bottomSheet.dismiss()
            startExport { file ->
                val uri = VideoExporter.saveVideoToGallery(this, file, postAuthorName)
                if (uri != null) {
                    Toast.makeText(this, "تم حفظ الفيديو بنجاح في ألبوم Manchorati! 🎉", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "حدث خطأ أثناء حفظ الفيديو", Toast.LENGTH_SHORT).show()
                }
            }
        }
        container.addView(btnGallery)

        // 3. General Share Option
        val btnGeneral = createExportOptionRow(
            title = "مشاركة في تطبيقات أخرى...",
            desc = "إنستغرام، تيك توك، فيسبوك، تيليجرام...",
            iconRes = R.drawable.ic_share_card,
            accentColor = "#8B5CF6"
        ) {
            bottomSheet.dismiss()
            startExport { file ->
                VideoExporter.shareVideoGeneral(this, file)
            }
        }
        container.addView(btnGeneral)

        bottomSheet.setContentView(container)
        bottomSheet.show()
    }

    private fun createExportOptionRow(
        title: String,
        desc: String,
        iconRes: Int,
        accentColor: String,
        onClick: () -> Unit
    ): View {
        val density = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
            isClickable = true
            isFocusable = true

            val bg = GradientDrawable().apply {
                cornerRadius = 14 * density
                setColor(Color.parseColor("#1E222B"))
            }
            background = bg

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }
            layoutParams = params
            setOnClickListener { onClick() }
        }

        // Icon Frame
        val iconFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                marginEnd = (14 * density).toInt()
            }
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(accentColor))
            }
            background = bg
        }
        val iv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((22 * density).toInt(), (22 * density).toInt()).apply {
                gravity = android.view.Gravity.CENTER
            }
            setImageResource(iconRes)
            setColorFilter(Color.WHITE)
        }
        iconFrame.addView(iv)

        // Texts
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val tvDesc = TextView(this).apply {
            text = desc
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
        }
        textContainer.addView(tvTitle)
        textContainer.addView(tvDesc)

        row.addView(iconFrame)
        row.addView(textContainer)
        return row
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

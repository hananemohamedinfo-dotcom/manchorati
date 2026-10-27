package com.manchorati.www

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
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
import java.util.Locale

class VideoStudioActivity : AppCompatActivity() {

    private lateinit var videoMotionView: VideoMotionView
    private lateinit var ivPlayPauseIndicator: ImageView

    // Video Player Controls
    private lateinit var btnControlsPlayPause: FrameLayout
    private lateinit var ivControlsPlayPause: ImageView
    private lateinit var tvControlsCurrentTime: TextView
    private lateinit var seekBarVideoProgress: SeekBar
    private lateinit var tvControlsTotalTime: TextView
    private lateinit var btnControlsMute: FrameLayout
    private lateinit var ivControlsMute: ImageView

    // Ratio, Duration & Visibility Toggles
    private lateinit var btnRatioStory: TextView
    private lateinit var btnRatioSquare: TextView
    private lateinit var btnDur15s: TextView
    private lateinit var btnDur30s: TextView
    private lateinit var btnDur45s: TextView
    private lateinit var btnToggleAuthor: TextView
    private lateinit var btnToggleTag: TextView

    // Tabs
    private lateinit var tabMotion: TextView
    private lateinit var tabAudio: TextView
    private lateinit var tabAnim: TextView
    private lateinit var tabFont: TextView
    private lateinit var scrollMotion: HorizontalScrollView
    private lateinit var scrollAudio: HorizontalScrollView
    private lateinit var scrollAnim: HorizontalScrollView
    private lateinit var scrollFont: HorizontalScrollView

    // Preset Containers
    private lateinit var layoutMotionPresets: LinearLayout
    private lateinit var layoutAudioPresets: LinearLayout
    private lateinit var layoutAnimStyles: LinearLayout
    private lateinit var layoutFontPresets: LinearLayout

    // Bottom Action Buttons (Pure Icon-Only)
    private lateinit var btnSaveGallery: FrameLayout
    private lateinit var btnShareWhatsApp: FrameLayout
    private lateinit var btnShareGeneral: FrameLayout

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

        // Player Controls
        btnControlsPlayPause = findViewById(R.id.btnControlsPlayPause)
        ivControlsPlayPause = findViewById(R.id.ivControlsPlayPause)
        tvControlsCurrentTime = findViewById(R.id.tvControlsCurrentTime)
        seekBarVideoProgress = findViewById(R.id.seekBarVideoProgress)
        tvControlsTotalTime = findViewById(R.id.tvControlsTotalTime)
        btnControlsMute = findViewById(R.id.btnControlsMute)
        ivControlsMute = findViewById(R.id.ivControlsMute)

        // Ratio & Duration
        btnRatioStory = findViewById(R.id.btnRatioStory)
        btnRatioSquare = findViewById(R.id.btnRatioSquare)
        btnDur15s = findViewById(R.id.btnDur15s)
        btnDur30s = findViewById(R.id.btnDur30s)
        btnDur45s = findViewById(R.id.btnDur45s)

        // Visibility Toggles
        btnToggleAuthor = findViewById(R.id.btnToggleAuthor)
        btnToggleTag = findViewById(R.id.btnToggleTag)

        // Tabs
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

        tvControlsTotalTime.text = formatTime(selectedDuration * 1000L)
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
                (24 * density).toInt(),
                (12 * density).toInt(),
                (24 * density).toInt(),
                navBar.bottom + (12 * density).toInt()
            )

            insets
        }
    }

    private fun setupListeners() {
        // Tap canvas toggles play/pause with quick visual feedback
        videoMotionView.setOnClickListener {
            videoMotionView.togglePlay()
            showPlayPauseFeedback(!videoMotionView.isPlaying)
        }

        // Play/Pause State Sync
        videoMotionView.onPlayStateChanged = { isPlaying ->
            ivControlsPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play
            )
        }

        // Live Progress Sync
        videoMotionView.onProgressUpdate = { curMs, totalMs, ratio ->
            seekBarVideoProgress.progress = (ratio * 1000).toInt()
            tvControlsCurrentTime.text = formatTime(curMs)
            tvControlsTotalTime.text = formatTime(totalMs)
        }

        // Player Controls
        btnControlsPlayPause.setOnClickListener {
            videoMotionView.togglePlay()
            ivControlsPlayPause.setImageResource(
                if (videoMotionView.isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play
            )
        }

        btnControlsMute.setOnClickListener {
            videoMotionView.isMuted = !videoMotionView.isMuted
            ivControlsMute.setImageResource(
                if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            )
            Toast.makeText(
                this,
                if (videoMotionView.isMuted) "تم كتم الصوت 🔇" else "تم تشغيل الصوت 🔊",
                Toast.LENGTH_SHORT
            ).show()
        }

        seekBarVideoProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val ratio = progress / 1000f
                    videoMotionView.seekTo(ratio)
                    val curMs = (ratio * selectedDuration * 1000L).toLong()
                    tvControlsCurrentTime.text = formatTime(curMs)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

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

        // Duration Selectors (15s, 30s, 45s)
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
                tvControlsTotalTime.text = formatTime(selectedDuration * 1000L)
                updateConfig()
                videoMotionView.restart()
            }
        }

        // Visibility Toggles
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
        }

        // Tabs Toggle
        tabMotion.setOnClickListener { switchTab(0) }
        tabAudio.setOnClickListener { switchTab(1) }
        tabAnim.setOnClickListener { switchTab(2) }
        tabFont.setOnClickListener { switchTab(3) }

        // Bottom Actions (Icon-Only, Top Video Editor Styling)
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
                }
            }
            layoutMotionPresets.addView(chip)
        }

        // 2. Curated Aesthetic Online Backgrounds (Downloaded on-demand and cached permanently)
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

        // All 8 authentic Arabic fonts loaded from res/font/
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
            }
            layoutFontPresets.addView(chip)
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

            // Live preview: apply exact font to font chip!
            if (fontId != null) {
                val tf = ThemeHelper.getFontTypeface(this@VideoStudioActivity, fontId)
                if (tf != null) {
                    typeface = tf
                    textSize = 14f
                }
            }

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

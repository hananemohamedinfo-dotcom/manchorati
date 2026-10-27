package com.manchorati.www

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
    private lateinit var tvTopRatioBadge: TextView
    private lateinit var btnEnregContainer: FrameLayout
    private lateinit var btnEnreg: TextView

    // Sleek Playback & Scrubber Controls
    private lateinit var ivPlayPauseCenter: ImageView
    private lateinit var tvTimeCurrent: TextView
    private lateinit var seekBarVideo: SeekBar
    private lateinit var tvTimeTotal: TextView
    private lateinit var ivMuteToggle: ImageView

    // Tool Panels
    private lateinit var layoutToolPanelContainer: LinearLayout
    private lateinit var tvToolPanelTitle: TextView
    private lateinit var btnCloseToolPanel: ImageView

    // Panel 1: Audio
    private lateinit var scrollAudio: HorizontalScrollView
    private lateinit var layoutAudioPresets: LinearLayout

    // Panel 2: Effects
    private lateinit var panelEffectsContainer: LinearLayout
    private lateinit var btnSpeed05x: TextView
    private lateinit var btnSpeed10x: TextView
    private lateinit var btnSpeed15x: TextView
    private lateinit var btnSpeed20x: TextView
    private lateinit var btnSpeed30x: TextView
    private lateinit var scrollAnim: HorizontalScrollView
    private lateinit var layoutAnimStyles: LinearLayout

    // Panel 3: Typography & Colors
    private lateinit var panelFontContainer: LinearLayout
    private lateinit var layoutColorChips: LinearLayout
    private lateinit var scrollFont: HorizontalScrollView
    private lateinit var layoutFontPresets: LinearLayout

    // Panel 4: Canvas (Backgrounds)
    private lateinit var panelCanvasContainer: LinearLayout
    private lateinit var tabBgAll: TextView
    private lateinit var tabBgMotion: TextView
    private lateinit var tabBgDark: TextView
    private lateinit var tabBgNature: TextView
    private lateinit var btnAdjustFraming: LinearLayout
    private lateinit var scrollMotion: HorizontalScrollView
    private lateinit var layoutMotionPresets: LinearLayout

    // Panel 5: Settings
    private lateinit var panelSettingsContainer: LinearLayout
    private lateinit var btnDur15s: TextView
    private lateinit var btnDur30s: TextView
    private lateinit var btnDur45s: TextView
    private lateinit var btnDur60s: TextView
    private lateinit var btnDurCustom: TextView
    private lateinit var layoutCustomDurationSlider: LinearLayout
    private lateinit var btnDurMinus: TextView
    private lateinit var seekBarCustomDuration: SeekBar
    private lateinit var btnDurPlus: TextView
    private lateinit var tvCustomDurationValue: TextView
    private lateinit var btnRatioStory: TextView
    private lateinit var btnRatioSquare: TextView
    private lateinit var btnToggleAuthor: TextView
    private lateinit var btnToggleTag: TextView

    // Bottom Pro Toolbar
    private lateinit var toolAudio: LinearLayout
    private lateinit var toolEffects: LinearLayout
    private lateinit var toolText: LinearLayout
    private lateinit var toolCanvas: LinearLayout
    private lateinit var toolSettings: LinearLayout

    // State
    private var selectedMotion = "STARS"
    private var selectedAudio = "oud" // Autoplay soothing Oud by default
    private var selectedAnim = "typewriter"
    private var selectedRatio = "9:16"
    private var selectedDuration = 15 // Default 15s
    private var selectedFont = "tajawal"
    private var selectedTextColor = "#FFFFFF"
    private var selectedAnimSpeed = 1.0f
    private var selectedImageFramingMode = "cover" // "cover", "fit_blur", "zoom"
    private var selectedImageFocusY = 0.5f // 0f = top, 0.5f = center, 1.0f = bottom
    private var showAuthor = true
    private var showTag = false

    private var customImageBitmap: Bitmap? = null
    private var customAudioUri: Uri? = null
    private var downloadedAudioPath: String? = null
    private var currentBgCategory = "all" // "all", "motion", "dark", "nature"

    private var postContent = ""
    private var postAuthorName = ""
    private var postAuthorUsername = ""
    private var postAuthorBadge = ""
    private var postTag = ""

    private var activeToolIndex = -1 // -1 = closed
    private var isUserTrackingSeek = false

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
        setupSpeedControls()
        setupCustomDurationControls()
        setupBgCategories()
        populateAudioPresets()
        populateAnimPresets()
        populateFontPresets()
        populateMotionPresets()
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
            ?: ""

        postAuthorBadge = intent.getStringExtra("EXTRA_AUTHOR_BADGE") ?: "verified"
        postTag = intent.getStringExtra("EXTRA_TAG") ?: "حكمة_اليوم"

        selectedMotion = intent.getStringExtra("EXTRA_BG_MOTION_ID") ?: "STARS"
        selectedAudio = intent.getStringExtra("EXTRA_AUDIO_PRESET") ?: "oud"
        selectedAnim = intent.getStringExtra("EXTRA_ANIM_STYLE") ?: "typewriter"
        selectedRatio = intent.getStringExtra("EXTRA_VIDEO_RATIO") ?: "9:16"
    }

    private fun initViews() {
        btnBackContainer = findViewById(R.id.btnBackContainer)
        tvTopRatioBadge = findViewById(R.id.tvTopRatioBadge)
        btnEnregContainer = findViewById(R.id.btnEnregContainer)
        btnEnreg = findViewById(R.id.btnEnreg)

        videoMotionView = findViewById(R.id.videoMotionView)
        ivPlayPauseIndicator = findViewById(R.id.ivPlayPauseIndicator)

        ivPlayPauseCenter = findViewById(R.id.ivPlayPauseCenter)
        tvTimeCurrent = findViewById(R.id.tvTimeCurrent)
        seekBarVideo = findViewById(R.id.seekBarVideo)
        tvTimeTotal = findViewById(R.id.tvTimeTotal)
        ivMuteToggle = findViewById(R.id.ivMuteToggle)

        layoutToolPanelContainer = findViewById(R.id.layoutToolPanelContainer)
        tvToolPanelTitle = findViewById(R.id.tvToolPanelTitle)
        btnCloseToolPanel = findViewById(R.id.btnCloseToolPanel)

        scrollAudio = findViewById(R.id.scrollAudio)
        layoutAudioPresets = findViewById(R.id.layoutAudioPresets)

        panelEffectsContainer = findViewById(R.id.panelEffectsContainer)
        btnSpeed05x = findViewById(R.id.btnSpeed05x)
        btnSpeed10x = findViewById(R.id.btnSpeed10x)
        btnSpeed15x = findViewById(R.id.btnSpeed15x)
        btnSpeed20x = findViewById(R.id.btnSpeed20x)
        btnSpeed30x = findViewById(R.id.btnSpeed30x)
        scrollAnim = findViewById(R.id.scrollAnim)
        layoutAnimStyles = findViewById(R.id.layoutAnimStyles)

        panelFontContainer = findViewById(R.id.panelFontContainer)
        layoutColorChips = findViewById(R.id.layoutColorChips)
        scrollFont = findViewById(R.id.scrollFont)
        layoutFontPresets = findViewById(R.id.layoutFontPresets)

        panelCanvasContainer = findViewById(R.id.panelCanvasContainer)
        tabBgAll = findViewById(R.id.tabBgAll)
        tabBgMotion = findViewById(R.id.tabBgMotion)
        tabBgDark = findViewById(R.id.tabBgDark)
        tabBgNature = findViewById(R.id.tabBgNature)
        btnAdjustFraming = findViewById(R.id.btnAdjustFraming)
        scrollMotion = findViewById(R.id.scrollMotion)
        layoutMotionPresets = findViewById(R.id.layoutMotionPresets)

        panelSettingsContainer = findViewById(R.id.panelSettingsContainer)
        btnDur15s = findViewById(R.id.btnDur15s)
        btnDur30s = findViewById(R.id.btnDur30s)
        btnDur45s = findViewById(R.id.btnDur45s)
        btnDur60s = findViewById(R.id.btnDur60s)
        btnDurCustom = findViewById(R.id.btnDurCustom)
        layoutCustomDurationSlider = findViewById(R.id.layoutCustomDurationSlider)
        btnDurMinus = findViewById(R.id.btnDurMinus)
        seekBarCustomDuration = findViewById(R.id.seekBarCustomDuration)
        btnDurPlus = findViewById(R.id.btnDurPlus)
        tvCustomDurationValue = findViewById(R.id.tvCustomDurationValue)
        btnRatioStory = findViewById(R.id.btnRatioStory)
        btnRatioSquare = findViewById(R.id.btnRatioSquare)
        btnToggleAuthor = findViewById(R.id.btnToggleAuthor)
        btnToggleTag = findViewById(R.id.btnToggleTag)

        toolAudio = findViewById(R.id.toolAudio)
        toolEffects = findViewById(R.id.toolEffects)
        toolText = findViewById(R.id.toolText)
        toolCanvas = findViewById(R.id.toolCanvas)
        toolSettings = findViewById(R.id.toolSettings)

        tvTimeTotal.text = formatTime(selectedDuration * 1000L)
        tvTopRatioBadge.text = selectedRatio
    }

    private fun applyWindowInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#10131A")

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            findViewById<View>(R.id.statusBarSpacer)?.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                statusBar.top
            )

            findViewById<View>(R.id.navBarSpacer)?.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                navBar.bottom
            )

            insets
        }
    }

    private fun setupListeners() {
        btnBackContainer.setOnClickListener { finish() }

        // Top Right "Enreg." / Save Button
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

        // Mute / Speaker button
        ivMuteToggle.setOnClickListener {
            videoMotionView.isMuted = !videoMotionView.isMuted
            ivMuteToggle.setImageResource(
                if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            )
            Toast.makeText(
                this,
                if (videoMotionView.isMuted) "تم كتم الصوت" else "تم تشغيل الصوت",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Play/Pause State Sync
        videoMotionView.onPlayStateChanged = { isPlaying ->
            ivPlayPauseCenter.setImageResource(
                if (isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play
            )
        }

        // Live Progress Sync with Scrubber SeekBar
        videoMotionView.onProgressUpdate = { curMs, totalMs, ratio ->
            if (!isUserTrackingSeek) {
                seekBarVideo.progress = (ratio * 1000).toInt()
                tvTimeCurrent.text = formatTime(curMs)
            }
        }

        // Smooth Scrubber Seeking
        seekBarVideo.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val ratio = progress / 1000f
                    val curMs = (ratio * selectedDuration * 1000L).toLong()
                    tvTimeCurrent.text = formatTime(curMs)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                isUserTrackingSeek = true
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                isUserTrackingSeek = false
                val ratio = (sb?.progress ?: 0) / 1000f
                videoMotionView.seekTo(ratio)
            }
        })

        // Close Tool Panel button
        btnCloseToolPanel.setOnClickListener {
            closeToolPanel()
        }

        // Bottom 5 Tools
        toolAudio.setOnClickListener { openToolPanel(0) }
        toolEffects.setOnClickListener { openToolPanel(1) }
        toolText.setOnClickListener { openToolPanel(2) }
        toolCanvas.setOnClickListener { openToolPanel(3) }
        toolSettings.setOnClickListener { openToolPanel(4) }

        // Adjust Framing Button
        btnAdjustFraming.setOnClickListener {
            showFramingAdjusterDialog()
        }

        // Duration Presets in Settings
        val durBtns = listOf(
            btnDur15s to 15,
            btnDur30s to 30,
            btnDur45s to 45,
            btnDur60s to 60
        )
        for ((btn, sec) in durBtns) {
            btn.setOnClickListener {
                selectedDuration = sec
                layoutCustomDurationSlider.visibility = View.GONE
                updateDurationPillHighlights(btn)
                tvTimeTotal.text = formatTime(selectedDuration * 1000L)
                updateConfig()
                videoMotionView.restart()
            }
        }

        btnDurCustom.setOnClickListener {
            layoutCustomDurationSlider.visibility = View.VISIBLE
            updateDurationPillHighlights(btnDurCustom)
            seekBarCustomDuration.progress = (selectedDuration - 5).coerceIn(0, 55)
            tvCustomDurationValue.text = "${selectedDuration} ث"
        }

        // Ratio Toggle in Settings
        btnRatioStory.setOnClickListener {
            selectedRatio = "9:16"
            tvTopRatioBadge.text = "9:16"
            btnRatioStory.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnRatioStory.setTextColor(Color.WHITE)
            btnRatioSquare.background = null
            btnRatioSquare.setTextColor(Color.parseColor("#94A3B8"))
            updateConfig()
        }
        btnRatioSquare.setOnClickListener {
            selectedRatio = "1:1"
            tvTopRatioBadge.text = "1:1"
            btnRatioSquare.setBackgroundResource(R.drawable.bg_segment_btn_active)
            btnRatioSquare.setTextColor(Color.WHITE)
            btnRatioStory.background = null
            btnRatioStory.setTextColor(Color.parseColor("#94A3B8"))
            updateConfig()
        }

        // Visibility Toggles
        btnToggleAuthor.setOnClickListener {
            showAuthor = !showAuthor
            btnToggleAuthor.setBackgroundResource(if (showAuthor) R.drawable.bg_segment_btn_active else R.drawable.bg_segment_container)
            btnToggleAuthor.setTextColor(if (showAuthor) Color.WHITE else Color.parseColor("#94A3B8"))
            updateConfig()
        }

        btnToggleTag.setOnClickListener {
            showTag = !showTag
            btnToggleTag.setBackgroundResource(if (showTag) R.drawable.bg_segment_btn_active else R.drawable.bg_segment_container)
            btnToggleTag.setTextColor(if (showTag) Color.WHITE else Color.parseColor("#94A3B8"))
            updateConfig()
        }
    }

    private fun setupSpeedControls() {
        val speedList = listOf(
            btnSpeed05x to 0.5f,
            btnSpeed10x to 1.0f,
            btnSpeed15x to 1.5f,
            btnSpeed20x to 2.0f,
            btnSpeed30x to 3.0f
        )
        for ((btn, spd) in speedList) {
            btn.setOnClickListener {
                selectedAnimSpeed = spd
                for ((b, _) in speedList) {
                    if (b == btn) {
                        b.setBackgroundResource(R.drawable.bg_segment_btn_active)
                        b.setTextColor(Color.WHITE)
                    } else {
                        b.setBackgroundResource(R.drawable.bg_segment_container)
                        b.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
                updateConfig()
                videoMotionView.restart()
            }
        }
    }

    private fun setupCustomDurationControls() {
        seekBarCustomDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    selectedDuration = progress + 5
                    tvCustomDurationValue.text = "${selectedDuration} ث"
                    tvTimeTotal.text = formatTime(selectedDuration * 1000L)
                    updateConfig()
                    videoMotionView.restart()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        btnDurMinus.setOnClickListener {
            if (selectedDuration > 5) {
                selectedDuration--
                seekBarCustomDuration.progress = selectedDuration - 5
                tvCustomDurationValue.text = "${selectedDuration} ث"
                tvTimeTotal.text = formatTime(selectedDuration * 1000L)
                updateConfig()
                videoMotionView.restart()
            }
        }

        btnDurPlus.setOnClickListener {
            if (selectedDuration < 60) {
                selectedDuration++
                seekBarCustomDuration.progress = selectedDuration - 5
                tvCustomDurationValue.text = "${selectedDuration} ث"
                tvTimeTotal.text = formatTime(selectedDuration * 1000L)
                updateConfig()
                videoMotionView.restart()
            }
        }
    }

    private fun updateDurationPillHighlights(activeBtn: TextView) {
        val allDur = listOf(btnDur15s, btnDur30s, btnDur45s, btnDur60s, btnDurCustom)
        for (b in allDur) {
            if (b == activeBtn) {
                b.setBackgroundResource(R.drawable.bg_segment_btn_active)
                b.setTextColor(Color.WHITE)
            } else {
                b.background = null
                b.setTextColor(Color.parseColor("#94A3B8"))
            }
        }
    }

    private fun setupBgCategories() {
        val tabs = listOf(
            tabBgAll to "all",
            tabBgMotion to "motion",
            tabBgDark to "dark",
            tabBgNature to "nature"
        )
        for ((tab, cat) in tabs) {
            tab.setOnClickListener {
                currentBgCategory = cat
                for ((t, _) in tabs) {
                    if (t == tab) {
                        t.setBackgroundResource(R.drawable.bg_segment_btn_active)
                        t.setTextColor(Color.WHITE)
                    } else {
                        t.setBackgroundResource(R.drawable.bg_segment_container)
                        t.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
                populateMotionPresets()
            }
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
        panelEffectsContainer.visibility = if (index == 1) View.VISIBLE else View.GONE
        panelFontContainer.visibility = if (index == 2) View.VISIBLE else View.GONE
        panelCanvasContainer.visibility = if (index == 3) View.VISIBLE else View.GONE
        panelSettingsContainer.visibility = if (index == 4) View.VISIBLE else View.GONE

        tvToolPanelTitle.text = when (index) {
            0 -> "Audio (الصوت والموسيقى)"
            1 -> "Effects (حركات النص والسرعة)"
            2 -> "Typography (الخطوط والألوان)"
            3 -> "Canvas (الخلفيات والتأثيرات)"
            else -> "Settings (المدة والأبعاد)"
        }
    }

    private fun closeToolPanel() {
        activeToolIndex = -1
        layoutToolPanelContainer.visibility = View.GONE
    }

    private fun showPlayPauseFeedback(playing: Boolean) {
        ivPlayPauseIndicator.setImageResource(if (playing) R.drawable.ic_video_play else R.drawable.ic_video_pause)
        ivPlayPauseIndicator.visibility = View.VISIBLE
        ivPlayPauseIndicator.alpha = 1f
        ivPlayPauseIndicator.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction { ivPlayPauseIndicator.visibility = View.GONE }
            .start()
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Audio Presets & Online Tracks with Progress
    // ─────────────────────────────────────────────────────────────
    private fun populateAudioPresets() {
        layoutAudioPresets.removeAllViews()
        val dp = resources.displayMetrics.density

        // Option A: "بدون صوت / No Audio"
        val muteCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams((86 * dp).toInt(), (76 * dp).toInt()).apply {
                marginEnd = (8 * dp).toInt()
            }
            val isSelected = selectedAudio.lowercase() in listOf("none", "mute")
            setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

            val iv = ImageView(this@VideoStudioActivity).apply {
                layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt())
                setImageResource(R.drawable.ic_volume_off)
                setColorFilter(Color.parseColor(if (isSelected) "#38BDF8" else "#94A3B8"))
            }
            val tv = TextView(this@VideoStudioActivity).apply {
                text = "بدون صوت"
                setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            addView(iv)
            addView(tv)
            setOnClickListener {
                selectedAudio = "none"
                customAudioUri = null
                downloadedAudioPath = null
                videoMotionView.customAudioUri = null
                updateConfig()
                populateAudioPresets()
            }
        }
        layoutAudioPresets.addView(muteCard)

        // Option B: "+ ملف من الهاتف / Device Audio"
        val customCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams((86 * dp).toInt(), (76 * dp).toInt()).apply {
                marginEnd = (8 * dp).toInt()
            }
            val isSelected = customAudioUri != null
            setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

            val iv = ImageView(this@VideoStudioActivity).apply {
                layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt())
                setImageResource(R.drawable.ic_plus)
                setColorFilter(Color.parseColor(if (isSelected) "#38BDF8" else "#94A3B8"))
            }
            val tv = TextView(this@VideoStudioActivity).apply {
                text = "من الهاتف"
                setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            addView(iv)
            addView(tv)
            setOnClickListener {
                pickAudioLauncher.launch("audio/*")
            }
        }
        layoutAudioPresets.addView(customCard)

        // Option C: Built-in Studio Loops
        val builtInLoops = listOf(
            Triple("oud", "عود أندلسي", R.drawable.ic_capcut_audio),
            Triple("piano", "بيانو هادئ", R.drawable.ic_capcut_audio),
            Triple("rain", "صوت المطر", R.drawable.ic_capcut_audio),
            Triple("nature", "طبيعة ورياح", R.drawable.ic_capcut_audio)
        )
        for ((id, title, iconRes) in builtInLoops) {
            val isSelected = selectedAudio == id && customAudioUri == null && downloadedAudioPath == null
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams((90 * dp).toInt(), (76 * dp).toInt()).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

                val iv = ImageView(this@VideoStudioActivity).apply {
                    layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt())
                    setImageResource(iconRes)
                    setColorFilter(Color.parseColor(if (isSelected) "#38BDF8" else "#94A3B8"))
                }
                val tv = TextView(this@VideoStudioActivity).apply {
                    text = title
                    setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                    textSize = 10.5f
                    gravity = Gravity.CENTER
                    setPadding(0, (4 * dp).toInt(), 0, 0)
                }
                addView(iv)
                addView(tv)
                setOnClickListener {
                    selectedAudio = id
                    customAudioUri = null
                    downloadedAudioPath = null
                    videoMotionView.customAudioUri = null
                    videoMotionView.isMuted = false
                    updateConfig()
                    populateAudioPresets()
                }
            }
            layoutAudioPresets.addView(card)
        }

        // Option D: Online Curated Tracks with Real Download Percentage
        for (onlineAudio in StudioAssetManager.onlineAudios) {
            val isDownloaded = StudioAssetManager.isAudioDownloaded(this, onlineAudio.id)
            val isSelected = downloadedAudioPath != null && downloadedAudioPath!!.contains(onlineAudio.id)

            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams((120 * dp).toInt(), (76 * dp).toInt()).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)
            }

            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                val tvTitle = TextView(this@VideoStudioActivity).apply {
                    text = onlineAudio.title
                    setTextColor(Color.parseColor(if (isSelected) "#38BDF8" else "#FFFFFF"))
                    textSize = 10.5f
                    maxLines = 1
                    gravity = Gravity.CENTER
                }
                val tvCat = TextView(this@VideoStudioActivity).apply {
                    text = onlineAudio.category
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 9f
                    gravity = Gravity.CENTER
                }
                val tvStatus = TextView(this@VideoStudioActivity).apply {
                    text = if (isDownloaded) "جاهز ✓" else "تحميل 📥"
                    setTextColor(Color.parseColor(if (isDownloaded) "#10B981" else "#38BDF8"))
                    textSize = 9.5f
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * dp).toInt(), 0, 0)
                }
                addView(tvTitle)
                addView(tvCat)
                addView(tvStatus)
            }
            frame.addView(contentLayout)

            // Progress Overlay
            val progressOverlay = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                visibility = View.GONE
                setBackgroundColor(Color.parseColor("#CC0D1117"))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                val pb = ProgressBar(this@VideoStudioActivity, null, android.R.attr.progressBarStyleSmall)
                val tvPercent = TextView(this@VideoStudioActivity).apply {
                    text = "0%"
                    setTextColor(Color.WHITE)
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * dp).toInt(), 0, 0)
                }
                addView(pb)
                addView(tvPercent)
            }
            frame.addView(progressOverlay)

            frame.setOnClickListener {
                if (StudioAssetManager.isAudioDownloaded(this, onlineAudio.id)) {
                    downloadedAudioPath = StudioAssetManager.getDownloadedAudioPath(this, onlineAudio.id)
                    selectedAudio = onlineAudio.id
                    customAudioUri = null
                    videoMotionView.customAudioUri = null
                    videoMotionView.isMuted = false
                    updateConfig()
                    populateAudioPresets()
                } else {
                    progressOverlay.visibility = View.VISIBLE
                    val tvPercent = progressOverlay.getChildAt(1) as TextView

                    StudioAssetManager.downloadAudio(
                        this,
                        onlineAudio,
                        onProgress = { percent ->
                            tvPercent.text = "${percent}%"
                        },
                        onSuccess = { savedPath ->
                            progressOverlay.visibility = View.GONE
                            downloadedAudioPath = savedPath
                            selectedAudio = onlineAudio.id
                            customAudioUri = null
                            videoMotionView.customAudioUri = null
                            videoMotionView.isMuted = false
                            updateConfig()
                            populateAudioPresets()
                        },
                        onError = { err ->
                            progressOverlay.visibility = View.GONE
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            layoutAudioPresets.addView(frame)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Motion Effects Presets (Clean Pro Icons & English Names)
    // ─────────────────────────────────────────────────────────────
    private fun populateAnimPresets() {
        layoutAnimStyles.removeAllViews()
        val dp = resources.displayMetrics.density

        val effectsList = listOf(
            Triple("typewriter", "Typewriter", R.drawable.ic_fx_typewriter),
            Triple("word_by_word", "Word by Word", R.drawable.ic_fx_words),
            Triple("fade", "Fade In", R.drawable.ic_fx_fade),
            Triple("zoom", "Zoom Pop", R.drawable.ic_fx_zoom),
            Triple("glow", "Glow Pulse", R.drawable.ic_fx_glow),
            Triple("slide_up", "Slide Up", R.drawable.ic_fx_slide),
            Triple("blur_in", "Blur Reveal", R.drawable.ic_fx_blur)
        )

        for ((id, title, iconRes) in effectsList) {
            val isSelected = selectedAnim == id
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams((94 * dp).toInt(), (76 * dp).toInt()).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

                val iv = ImageView(this@VideoStudioActivity).apply {
                    layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt())
                    setImageResource(iconRes)
                    setColorFilter(Color.parseColor(if (isSelected) "#38BDF8" else "#94A3B8"))
                }
                val tv = TextView(this@VideoStudioActivity).apply {
                    text = title
                    setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                    textSize = 10f
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setPadding(0, (4 * dp).toInt(), 0, 0)
                }
                addView(iv)
                addView(tv)
                setOnClickListener {
                    selectedAnim = id
                    updateConfig()
                    populateAnimPresets()
                    videoMotionView.restart()
                }
            }
            layoutAnimStyles.addView(card)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Arabic Font Presets & Color Chips
    // ─────────────────────────────────────────────────────────────
    private fun populateFontPresets() {
        layoutFontPresets.removeAllViews()
        layoutColorChips.removeAllViews()
        val dp = resources.displayMetrics.density

        // Color Chips
        val colors = listOf(
            "#FFFFFF" to "أبيض",
            "#FCD34D" to "ذهبي",
            "#38BDF8" to "سماوي",
            "#FEF3C7" to "عاجي",
            "#FDA4AF" to "وردي",
            "#34D399" to "زمردي"
        )
        for ((hex, name) in colors) {
            val isSelected = selectedTextColor.equals(hex, ignoreCase = true)
            val chip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (28 * dp).toInt()).apply {
                    marginEnd = (6 * dp).toInt()
                }
                background = GradientDrawable().apply {
                    cornerRadius = 14 * dp
                    setColor(Color.parseColor("#26FFFFFF"))
                    if (isSelected) setStroke((2 * dp).toInt(), Color.parseColor("#38BDF8"))
                }

                val dot = View(this@VideoStudioActivity).apply {
                    layoutParams = LinearLayout.LayoutParams((12 * dp).toInt(), (12 * dp).toInt()).apply {
                        marginEnd = (6 * dp).toInt()
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                    }
                }
                val tv = TextView(this@VideoStudioActivity).apply {
                    text = name
                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))
                    textSize = 10.5f
                }
                addView(dot)
                addView(tv)
                setOnClickListener {
                    selectedTextColor = hex
                    updateConfig()
                    populateFontPresets()
                }
            }
            layoutColorChips.addView(chip)
        }

        // Font Cards
        val fontList = listOf(
            "amiri" to "خط أميري",
            "cairo" to "خط كايرو",
            "tajawal" to "خط تجوال",
            "reem_kufi" to "ريم كوفي",
            "almarai" to "خط المراعي",
            "changa" to "خط تشانغا",
            "noto_kufi" to "نوتو كوفي",
            "el_messiri" to "المسيري",
            "ruqaa" to "خط الرقعة"
        )
        for ((id, name) in fontList) {
            val isSelected = selectedFont == id
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams((100 * dp).toInt(), (76 * dp).toInt()).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

                val tvPreview = TextView(this@VideoStudioActivity).apply {
                    text = "منشوراتي"
                    setTextColor(Color.parseColor(if (isSelected) "#38BDF8" else "#FFFFFF"))
                    textSize = 15f
                    gravity = Gravity.CENTER
                    typeface = ThemeHelper.getFontTypeface(this@VideoStudioActivity, id)
                }
                val tvName = TextView(this@VideoStudioActivity).apply {
                    text = name
                    setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                    textSize = 9.5f
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * dp).toInt(), 0, 0)
                }
                addView(tvPreview)
                addView(tvName)
                setOnClickListener {
                    selectedFont = id
                    updateConfig()
                    populateFontPresets()
                }
            }
            layoutFontPresets.addView(card)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Canvas / Backgrounds (Picsart-Style Mini Gallery)
    // ─────────────────────────────────────────────────────────────
    private fun populateMotionPresets() {
        layoutMotionPresets.removeAllViews()
        val dp = resources.displayMetrics.density

        // Card 1: Custom Image from Phone (+ من الهاتف)
        val customCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams((86 * dp).toInt(), (76 * dp).toInt()).apply {
                marginEnd = (8 * dp).toInt()
            }
            val isSelected = customImageBitmap != null
            setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

            val iv = ImageView(this@VideoStudioActivity).apply {
                layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt())
                setImageResource(R.drawable.ic_gallery)
                setColorFilter(Color.parseColor(if (isSelected) "#38BDF8" else "#94A3B8"))
            }
            val tv = TextView(this@VideoStudioActivity).apply {
                text = "من الهاتف"
                setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                textSize = 10f
                gravity = Gravity.CENTER
                setPadding(0, (4 * dp).toInt(), 0, 0)
            }
            addView(iv)
            addView(tv)
            setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }
        layoutMotionPresets.addView(customCard)

        // Shaders List
        val shaderList = listOf(
            Triple("STARS", "نجوم متحركة", "#0B1329"),
            Triple("NEBULA", "سديم كوني", "#1E112A"),
            Triple("AURORA", "شفق قطبي", "#082F24"),
            Triple("GOLD", "ذهب فاخر", "#281D05"),
            Triple("OCEAN", "أعماق البحر", "#042036")
        )

        if (currentBgCategory == "all" || currentBgCategory == "motion") {
            for ((id, title, colorHex) in shaderList) {
                val isSelected = selectedMotion == id && customImageBitmap == null
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
                    layoutParams = LinearLayout.LayoutParams((90 * dp).toInt(), (76 * dp).toInt()).apply {
                        marginEnd = (8 * dp).toInt()
                    }
                    setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

                    val previewDot = View(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams((20 * dp).toInt(), (20 * dp).toInt())
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor(colorHex))
                            setStroke((1.5f * dp).toInt(), Color.parseColor("#44FFFFFF"))
                        }
                    }
                    val tv = TextView(this@VideoStudioActivity).apply {
                        text = title
                        setTextColor(Color.parseColor(if (isSelected) "#FFFFFF" else "#94A3B8"))
                        textSize = 10f
                        gravity = Gravity.CENTER
                        setPadding(0, (4 * dp).toInt(), 0, 0)
                    }
                    addView(previewDot)
                    addView(tv)
                    setOnClickListener {
                        selectedMotion = id
                        customImageBitmap = null
                        btnAdjustFraming.visibility = View.GONE
                        updateConfig()
                        populateMotionPresets()
                    }
                }
                layoutMotionPresets.addView(card)
            }
        }

        // Online Unsplash Aesthetics with Real-time Progress Bar
        val filteredBgs = if (currentBgCategory == "all") {
            StudioAssetManager.onlineBackgrounds
        } else {
            StudioAssetManager.onlineBackgrounds.filter { it.category == currentBgCategory }
        }

        for (bg in filteredBgs) {
            val isDownloaded = StudioAssetManager.isBackgroundDownloaded(this, bg.id)
            val isSelected = selectedMotion == bg.id && customImageBitmap != null

            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams((110 * dp).toInt(), (76 * dp).toInt()).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)
            }

            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                val previewDot = View(this@VideoStudioActivity).apply {
                    layoutParams = LinearLayout.LayoutParams((18 * dp).toInt(), (18 * dp).toInt())
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(bg.previewColor))
                        setStroke((1.5f * dp).toInt(), Color.parseColor("#44FFFFFF"))
                    }
                }
                val tvTitle = TextView(this@VideoStudioActivity).apply {
                    text = bg.title
                    setTextColor(Color.parseColor(if (isSelected) "#38BDF8" else "#FFFFFF"))
                    textSize = 10f
                    maxLines = 1
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * dp).toInt(), 0, 0)
                }
                val tvStatus = TextView(this@VideoStudioActivity).apply {
                    text = if (isDownloaded) "جاهز ✓" else "تحميل 📥"
                    setTextColor(Color.parseColor(if (isDownloaded) "#10B981" else "#38BDF8"))
                    textSize = 9f
                    gravity = Gravity.CENTER
                }
                addView(previewDot)
                addView(tvTitle)
                addView(tvStatus)
            }
            frame.addView(contentLayout)

            // Progress Overlay
            val progressOverlay = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                visibility = View.GONE
                setBackgroundColor(Color.parseColor("#CC0D1117"))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                val pb = ProgressBar(this@VideoStudioActivity, null, android.R.attr.progressBarStyleSmall)
                val tvPercent = TextView(this@VideoStudioActivity).apply {
                    text = "0%"
                    setTextColor(Color.WHITE)
                    textSize = 10f
                    gravity = Gravity.CENTER
                    setPadding(0, (2 * dp).toInt(), 0, 0)
                }
                addView(pb)
                addView(tvPercent)
            }
            frame.addView(progressOverlay)

            frame.setOnClickListener {
                if (StudioAssetManager.isBackgroundDownloaded(this, bg.id)) {
                    val bmp = StudioAssetManager.getDownloadedBackgroundBitmap(this, bg.id)
                    if (bmp != null) {
                        customImageBitmap = bmp
                        selectedMotion = bg.id
                        btnAdjustFraming.visibility = View.VISIBLE
                        updateConfig()
                        populateMotionPresets()
                    }
                } else {
                    progressOverlay.visibility = View.VISIBLE
                    val tvPercent = progressOverlay.getChildAt(1) as TextView

                    StudioAssetManager.downloadBackground(
                        this,
                        bg,
                        onProgress = { percent ->
                            tvPercent.text = "${percent}%"
                        },
                        onSuccess = { bmp ->
                            progressOverlay.visibility = View.GONE
                            customImageBitmap = bmp
                            selectedMotion = bg.id
                            btnAdjustFraming.visibility = View.VISIBLE
                            updateConfig()
                            populateMotionPresets()
                        },
                        onError = { err ->
                            progressOverlay.visibility = View.GONE
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            layoutMotionPresets.addView(frame)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Custom Framing Adjuster Dialog
    // ─────────────────────────────────────────────────────────────
    private fun showFramingAdjusterDialog() {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_image_framing, null, false)
        dialog.setContentView(sheetView)

        val btnCover = sheetView.findViewById<TextView>(R.id.btnFramingCover)
        val btnFit = sheetView.findViewById<TextView>(R.id.btnFramingFit)
        val btnZoom = sheetView.findViewById<TextView>(R.id.btnFramingZoom)

        val btnFocusTop = sheetView.findViewById<TextView>(R.id.btnFocusTop)
        val btnFocusCenter = sheetView.findViewById<TextView>(R.id.btnFocusCenter)
        val btnFocusBottom = sheetView.findViewById<TextView>(R.id.btnFocusBottom)

        fun updateUI() {
            val modes = listOf(btnCover to "cover", btnFit to "fit_blur", btnZoom to "zoom")
            for ((btn, m) in modes) {
                if (m == selectedImageFramingMode) {
                    btn.setBackgroundResource(R.drawable.bg_segment_btn_active)
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundResource(R.drawable.bg_segment_container)
                    btn.setTextColor(Color.parseColor("#94A3B8"))
                }
            }

            val focuses = listOf(btnFocusTop to 0.0f, btnFocusCenter to 0.5f, btnFocusBottom to 1.0f)
            for ((btn, f) in focuses) {
                if (f == selectedImageFocusY) {
                    btn.setBackgroundResource(R.drawable.bg_segment_btn_active)
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.setBackgroundResource(R.drawable.bg_segment_container)
                    btn.setTextColor(Color.parseColor("#94A3B8"))
                }
            }
        }

        updateUI()

        btnCover.setOnClickListener { selectedImageFramingMode = "cover"; updateUI(); updateConfig() }
        btnFit.setOnClickListener { selectedImageFramingMode = "fit_blur"; updateUI(); updateConfig() }
        btnZoom.setOnClickListener { selectedImageFramingMode = "zoom"; updateUI(); updateConfig() }

        btnFocusTop.setOnClickListener { selectedImageFocusY = 0.0f; updateUI(); updateConfig() }
        btnFocusCenter.setOnClickListener { selectedImageFocusY = 0.5f; updateUI(); updateConfig() }
        btnFocusBottom.setOnClickListener { selectedImageFocusY = 1.0f; updateUI(); updateConfig() }

        sheetView.findViewById<View>(R.id.btnDoneFraming).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Config Update & Handlers
    // ─────────────────────────────────────────────────────────────
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
            downloadedAudioPath = downloadedAudioPath,
            animSpeed = selectedAnimSpeed,
            imageFramingMode = selectedImageFramingMode,
            imageFocusY = selectedImageFocusY
        )
        videoMotionView.config = config
    }

    private fun loadCustomImage(uri: Uri) {
        try {
            val input: InputStream? = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()
            if (bmp != null) {
                customImageBitmap = bmp
                selectedMotion = "CUSTOM_IMAGE"
                btnAdjustFraming.visibility = View.VISIBLE
                updateConfig()
                populateMotionPresets()
                showFramingAdjusterDialog()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر قراءة الصورة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCustomAudio(uri: Uri) {
        try {
            customAudioUri = uri
            selectedAudio = "custom"
            downloadedAudioPath = null
            videoMotionView.customAudioUri = uri
            videoMotionView.isMuted = false
            updateConfig()
            populateAudioPresets()
            Toast.makeText(this, "تم اختيار الصوت بنجاح", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر تحميل الصوت", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    // ─────────────────────────────────────────────────────────────
    // 7. Export Action Sheet & Progress Dialog
    // ─────────────────────────────────────────────────────────────
    private fun showExportActionSheet() {
        videoMotionView.pause()

        val bottomSheet = BottomSheetDialog(this)
        val density = resources.displayMetrics.density

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#15171C"))
            setPadding((24 * density).toInt(), (20 * density).toInt(), (24 * density).toInt(), (28 * density).toInt())
        }

        val tvSheetTitle = TextView(this).apply {
            text = "تصدير الفيديو ومشاركته"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, (18 * density).toInt())
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
                    Toast.makeText(this, "تم حفظ الفيديو بنجاح في ألبوم Manchorati!", Toast.LENGTH_LONG).show()
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
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt())
            isClickable = true
            isFocusable = true

            background = GradientDrawable().apply {
                cornerRadius = 14 * density
                setColor(Color.parseColor("#1E222B"))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }
            setOnClickListener { onClick() }
        }

        // Icon Frame
        val iconFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * density).toInt(), (44 * density).toInt()).apply {
                marginEnd = (14 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(accentColor))
                alpha = 35
            }
        }
        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((24 * density).toInt(), (24 * density).toInt()).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(iconRes)
            setColorFilter(Color.parseColor(accentColor))
        }
        iconFrame.addView(icon)
        row.addView(iconFrame)

        // Texts
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 14.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val tvDesc = TextView(this).apply {
            text = desc
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            setPadding(0, (2 * density).toInt(), 0, 0)
        }
        textContainer.addView(tvTitle)
        textContainer.addView(tvDesc)
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
            gravity = Gravity.CENTER
        }

        val tvTitle = TextView(this).apply {
            text = "جاري معالجة وتصدير الفيديو..."
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
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
            gravity = Gravity.CENTER
        }

        container.addView(tvTitle)
        container.addView(progressBar)
        container.addView(tvPercent)

        dialog.setContentView(container)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        VideoExporter.exportVideo(
            context = this,
            config = videoMotionView.config,
            customAudioUri = customAudioUri,
            onProgress = { percent ->
                runOnUiThread {
                    progressBar.progress = percent
                    tvPercent.text = "$percent%"
                }
            },
            onSuccess = { file ->
                runOnUiThread {
                    dialog.dismiss()
                    onComplete(file)
                }
            },
            onError = { exc ->
                runOnUiThread {
                    dialog.dismiss()
                    Toast.makeText(this, "حدث خطأ أثناء تصدير الفيديو: ${exc.message}", Toast.LENGTH_LONG).show()
                }
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

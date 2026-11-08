package com.manchorati.www



import android.text.TextUtils



import android.content.Context
import android.webkit.MimeTypeMap



import android.app.Dialog

import android.content.Intent

import android.graphics.Bitmap

import android.graphics.BitmapFactory

import android.graphics.Color

import android.graphics.Typeface

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

import com.bumptech.glide.Glide

import com.google.android.material.bottomsheet.BottomSheetDialog

import android.media.AudioAttributes

import android.media.MediaPlayer

import android.media.MediaMetadataRetriever

import android.provider.OpenableColumns

import android.os.Handler

import android.os.Looper



import java.io.File

import java.io.InputStream

import java.util.Locale



class VideoStudioActivity : AppCompatActivity() {



    private lateinit var videoMotionView: VideoMotionView

    private lateinit var ivPlayPauseIndicator: ImageView

    private var btnQuickEditText: View? = null

    private var btnEditTextInTypography: View? = null



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

    private var selectedColorFilter = "none"

    // VN Multi-Track Timeline Views (Removed to maximize screen space)
    private var layoutVnTracksContainer: LinearLayout? = null
    private var vnTrackText: LinearLayout? = null
    private var tvVnTrackTextContent: TextView? = null
    private var tvVnTrackTextAnimBadge: TextView? = null
    private var vnTrackAudio: LinearLayout? = null
    private var tvVnTrackAudioTitle: TextView? = null
    private var tvVnTrackAudioTimeBadge: TextView? = null
    private var vnTrackCanvas: LinearLayout? = null
    private var tvVnTrackCanvasTitle: TextView? = null
    private var tvVnTrackFilterBadge: TextView? = null

    // VN Filters Panel
    private lateinit var panelFiltersContainer: LinearLayout
    private lateinit var layoutFilterCards: LinearLayout
    private lateinit var toolFilters: LinearLayout
    private lateinit var ivToolFilters: ImageView
    private lateinit var tvToolFilters: TextView



    // Panel 0: Themes

    private lateinit var panelThemesContainer: LinearLayout

    private lateinit var btnShuffleTheme: LinearLayout

    private lateinit var scrollThemes: HorizontalScrollView

    private lateinit var layoutThemeCards: LinearLayout

    private lateinit var toolThemes: LinearLayout

    private lateinit var ivToolThemes: ImageView

    private lateinit var tvToolThemes: TextView



    private var currentAppliedThemeId: String? = null



    // Bottom Pro Toolbar

    private lateinit var toolAudio: LinearLayout

    private lateinit var toolEffects: LinearLayout

    private lateinit var toolText: LinearLayout

    private lateinit var toolCanvas: LinearLayout

    private lateinit var toolSettings: LinearLayout



    private lateinit var ivToolAudio: ImageView

    private lateinit var tvToolAudio: TextView

    private lateinit var ivToolEffects: ImageView

    private lateinit var tvToolEffects: TextView

    private lateinit var ivToolText: ImageView

    private lateinit var tvToolText: TextView

    private lateinit var ivToolCanvas: ImageView

    private lateinit var tvToolCanvas: TextView

    private lateinit var ivToolSettings: ImageView

    private lateinit var tvToolSettings: TextView



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

    private var selectedImagePanX = 0f

    private var selectedImagePanY = 0f

    private var selectedImageZoomScale = 1.0f

    private var showAuthor = true

    private var showTag = false



    private var customImageBitmap: Bitmap? = null

    private var customAudioUri: Uri? = null
    private var customAudioTitle: String = "صوتي الخاص"

    private var audioStartOffsetMs: Long = 0L

    private var downloadedAudioPath: String? = null

    private val downloadingAudioProgressMap = mutableMapOf<String, Int>()

    private var currentBgCategory = "all" // "all", "motion", "dark", "nature"

    private var selectedExportQuality = "1080p" // "1080p", "720p", "480p"



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



    // Audio Picker & Internal Storage Cache
    private fun copyAudioUriToInternalStorage(sourceUri: Uri): Uri? {
        return try {
            val ext = try {
                val mime = contentResolver.getType(sourceUri)
                if (mime != null) MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp3" else "mp3"
            } catch (_: Exception) { "mp3" }
            val destFile = java.io.File(filesDir, "current_custom_audio.$ext")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val safeUri = copyAudioUriToInternalStorage(uri) ?: uri
            customAudioUri = safeUri
            showAudioTrimmerDialog(safeUri)
        }
    }




    override fun attachBaseContext(newBase: Context) {

        super.attachBaseContext(LocaleHelper.applyLocale(newBase))

    }



    override fun onCreate(savedInstanceState: Bundle?) {

        LocaleHelper.applyLayoutDirection(this)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_video_studio)



        initIntentData()

        initViews()

        applyWindowInsets()

        setupListeners()

        setupSpeedControls()

        setupCustomDurationControls()

        setupBgCategories()

        populateThemeCards()

        populateAudioPresets()

        populateAnimPresets()

        populateFontPresets()

        populateMotionPresets()

        populateFilterCards()

        updateConfig()

        updateVnTracksUI()

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



        selectedRatio = intent.getStringExtra("EXTRA_VIDEO_RATIO") ?: "9:16"



        val hasExplicitMotion = intent.hasExtra("EXTRA_BG_MOTION_ID")

        val explicitThemeId = intent.getStringExtra("EXTRA_THEME_ID")



        if (explicitThemeId != null) {

            val theme = VideoTheme.getThemeById(explicitThemeId) ?: VideoTheme.getRandomTheme(this)

            applyThemeValues(theme)

        } else if (!hasExplicitMotion) {

            // Pick a fresh random theme on every launch!

            val freshTheme = VideoTheme.getRandomTheme(this)

            applyThemeValues(freshTheme)

            Toast.makeText(this, "قالب جديد: ${freshTheme.name}", Toast.LENGTH_SHORT).show()

        } else {

            selectedMotion = intent.getStringExtra("EXTRA_BG_MOTION_ID") ?: "STARS"

            selectedAudio = intent.getStringExtra("EXTRA_AUDIO_PRESET") ?: "oud"

            selectedAnim = intent.getStringExtra("EXTRA_ANIM_STYLE") ?: "typewriter"

            selectedFont = intent.getStringExtra("EXTRA_FONT_ID") ?: "tajawal"

            selectedTextColor = intent.getStringExtra("EXTRA_TEXT_COLOR") ?: "#FFFFFF"

        }

    }



    private fun applyThemeValues(theme: VideoTheme) {

        currentAppliedThemeId = theme.id

        selectedMotion = theme.bgMotionId

        selectedFont = theme.fontId

        selectedTextColor = theme.textColor

        selectedAudio = theme.audioPreset

        selectedAnim = theme.animStyle

        selectedAnimSpeed = theme.animSpeed

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



        panelThemesContainer = findViewById(R.id.panelThemesContainer)

        btnShuffleTheme = findViewById(R.id.btnShuffleTheme)

        scrollThemes = findViewById(R.id.scrollThemes)

        layoutThemeCards = findViewById(R.id.layoutThemeCards)

        toolThemes = findViewById(R.id.toolThemes)

        ivToolThemes = findViewById(R.id.ivToolThemes)

        tvToolThemes = findViewById(R.id.tvToolThemes)



        panelFiltersContainer = findViewById(R.id.panelFiltersContainer)
        layoutFilterCards = findViewById(R.id.layoutFilterCards)
        toolFilters = findViewById(R.id.toolFilters)
        ivToolFilters = findViewById(R.id.ivToolFilters)
        tvToolFilters = findViewById(R.id.tvToolFilters)

        toolAudio = findViewById(R.id.toolAudio)

        toolEffects = findViewById(R.id.toolEffects)

        toolText = findViewById(R.id.toolText)

        toolCanvas = findViewById(R.id.toolCanvas)

        toolSettings = findViewById(R.id.toolSettings)



        ivToolAudio = findViewById(R.id.ivToolAudio)

        tvToolAudio = findViewById(R.id.tvToolAudio)

        tvToolAudio.text = getString(R.string.studio_audio)

        ivToolEffects = findViewById(R.id.ivToolEffects)

        tvToolEffects = findViewById(R.id.tvToolEffects)

        tvToolEffects.text = getString(R.string.studio_animations)

        ivToolText = findViewById(R.id.ivToolText)

        tvToolText = findViewById(R.id.tvToolText)

        tvToolText.text = getString(R.string.studio_texts)

        ivToolCanvas = findViewById(R.id.ivToolCanvas)

        tvToolCanvas = findViewById(R.id.tvToolCanvas)

        tvToolCanvas.text = getString(R.string.studio_backgrounds)

        ivToolSettings = findViewById(R.id.ivToolSettings)

        tvToolSettings = findViewById(R.id.tvToolSettings)

        tvToolSettings.text = getString(R.string.studio_settings)



        btnQuickEditText = findViewById(R.id.btnQuickEditText)

        btnEditTextInTypography = findViewById(R.id.btnEditTextInTypography)



        tvTimeTotal.text = formatTime(selectedDuration * 1000L)

        tvTopRatioBadge.text = selectedRatio

        updateSettingsHighlights()

        updateToolBarHighlights(-1)

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



        // Quick Edit Quote Text

        btnQuickEditText?.setOnClickListener {

            showEditTextDialog()

        }

        btnEditTextInTypography?.setOnClickListener {

            showEditTextDialog()

        }



        // Direct finger gesture support on canvas (Pan & Pinch-to-Zoom)

        videoMotionView.onImageFramingChanged = { pX, pY, zoom ->

            selectedImagePanX = pX

            selectedImagePanY = pY

            selectedImageZoomScale = zoom

        }

        videoMotionView.onCanvasTap = {

            showPlayPauseFeedback(videoMotionView.isPlaying)

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

                if (videoMotionView.isMuted) getString(R.string.studio_audio_muted) else getString(R.string.studio_audio_unmuted),

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

                tvVnTrackAudioTimeBadge?.let { badge ->
                    val baseStart = if (customAudioUri != null) audioStartOffsetMs else 0L
                    badge.text = "${formatTime(baseStart + curMs)} / ${formatTime(baseStart + totalMs)}"
                }
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



        // Bottom 7 Tools (Themes, Audio, Effects, Filters, Text, Canvas, Settings)
        toolThemes.setOnClickListener { openToolPanel(0) }
        toolAudio.setOnClickListener { openToolPanel(1) }
        toolEffects.setOnClickListener { openToolPanel(2) }
        toolFilters.setOnClickListener { openToolPanel(3) }
        toolText.setOnClickListener { openToolPanel(4) }
        toolCanvas.setOnClickListener { openToolPanel(5) }
        toolSettings.setOnClickListener { openToolPanel(6) }



        btnShuffleTheme.setOnClickListener {

            val freshTheme = VideoTheme.getRandomTheme(this)

            applyTheme(freshTheme)

        }



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

                updateSettingsHighlights()

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

            updateSettingsHighlights()

            updateConfig()

        }

        btnRatioSquare.setOnClickListener {

            selectedRatio = "1:1"

            tvTopRatioBadge.text = "1:1"

            updateSettingsHighlights()

            updateConfig()

        }



        // Visibility Toggles

        btnToggleAuthor.setOnClickListener {

            showAuthor = !showAuthor

            updateSettingsHighlights()

            updateConfig()

        }



        btnToggleTag.setOnClickListener {

            showTag = !showTag

            updateSettingsHighlights()

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

                b.setBackgroundResource(R.drawable.bg_pill_active)

                b.setTextColor(Color.WHITE)

            } else {

                b.setBackgroundResource(R.drawable.bg_pill_inactive)

                b.setTextColor(Color.parseColor("#94A3B8"))

            }

        }

    }



    private fun updateSettingsHighlights() {

        val durBtn = when (selectedDuration) {

            15 -> btnDur15s

            30 -> btnDur30s

            45 -> btnDur45s

            60 -> btnDur60s

            else -> btnDurCustom

        }

        updateDurationPillHighlights(durBtn)



        if (selectedRatio == "9:16") {

            btnRatioStory.setBackgroundResource(R.drawable.bg_pill_active)

            btnRatioStory.setTextColor(Color.WHITE)

            btnRatioSquare.setBackgroundResource(R.drawable.bg_pill_inactive)

            btnRatioSquare.setTextColor(Color.parseColor("#94A3B8"))

        } else {

            btnRatioSquare.setBackgroundResource(R.drawable.bg_pill_active)

            btnRatioSquare.setTextColor(Color.WHITE)

            btnRatioStory.setBackgroundResource(R.drawable.bg_pill_inactive)

            btnRatioStory.setTextColor(Color.parseColor("#94A3B8"))

        }



        btnToggleAuthor.setBackgroundResource(if (showAuthor) R.drawable.bg_pill_active else R.drawable.bg_pill_inactive)

        btnToggleAuthor.setTextColor(if (showAuthor) Color.WHITE else Color.parseColor("#94A3B8"))

        btnToggleAuthor.text = if (showAuthor) getString(R.string.studio_profile_enabled) else getString(R.string.studio_profile_disabled)



        btnToggleTag.setBackgroundResource(if (showTag) R.drawable.bg_pill_active else R.drawable.bg_pill_inactive)

        btnToggleTag.setTextColor(if (showTag) Color.WHITE else Color.parseColor("#94A3B8"))

        btnToggleTag.text = if (showTag) getString(R.string.studio_tag_enabled) else getString(R.string.studio_tag_disabled)

    }



    private fun updateToolBarHighlights(index: Int) {

        val tools = listOf(
            toolThemes to ivToolThemes,
            toolAudio to ivToolAudio,
            toolEffects to ivToolEffects,
            toolFilters to ivToolFilters,
            toolText to ivToolText,
            toolCanvas to ivToolCanvas,
            toolSettings to ivToolSettings
        )
        val textViews = listOf(tvToolThemes, tvToolAudio, tvToolEffects, tvToolFilters, tvToolText, tvToolCanvas, tvToolSettings)

        textViews.forEach { it.visibility = View.GONE }



        for (i in tools.indices) {

            val (container, icon) = tools[i]

            if (i == index) {

                container.setBackgroundResource(R.drawable.bg_studio_tool_active)

                icon.setColorFilter(Color.WHITE)

            } else {

                container.background = null

                icon.setColorFilter(Color.parseColor("#94A3B8"))

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
        updateToolBarHighlights(index)

        panelThemesContainer.visibility = if (index == 0) View.VISIBLE else View.GONE
        scrollAudio.visibility = if (index == 1) View.VISIBLE else View.GONE
        panelEffectsContainer.visibility = if (index == 2) View.VISIBLE else View.GONE
        panelFiltersContainer.visibility = if (index == 3) View.VISIBLE else View.GONE
        panelFontContainer.visibility = if (index == 4) View.VISIBLE else View.GONE
        panelCanvasContainer.visibility = if (index == 5) View.VISIBLE else View.GONE
        panelSettingsContainer.visibility = if (index == 6) View.VISIBLE else View.GONE

        if (index == 3) {
            populateFilterCards()
        }
        if (index == 6) {
            updateSettingsHighlights()
        }

        tvToolPanelTitle.text = when (index) {
            0 -> "قوالب وثيمات جاهزة"
            1 -> getString(R.string.studio_audio)
            2 -> getString(R.string.studio_animations)
            3 -> "فلاتر تلوين سينمائية (LUTs)"
            4 -> getString(R.string.studio_texts)
            5 -> getString(R.string.studio_backgrounds)
            else -> getString(R.string.studio_settings)
        }
    }



    private fun closeToolPanel() {

        activeToolIndex = -1

        layoutToolPanelContainer.visibility = View.GONE

        updateToolBarHighlights(-1)

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

    // 0. Quote Text Editing Bottom Sheet

    // ─────────────────────────────────────────────────────────────

    private fun showEditTextDialog() {

        val dialog = BottomSheetDialog(this)

        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_quote, null, false)

        dialog.setContentView(sheetView)



        val etContent = sheetView.findViewById<EditText>(R.id.etEditQuoteContent)

        val tvCharCount = sheetView.findViewById<TextView>(R.id.tvEditCharCount)

        val btnCancel = sheetView.findViewById<View>(R.id.btnCancelEditQuote)

        val btnSave = sheetView.findViewById<View>(R.id.btnSaveEditQuote)



        etContent?.setText(postContent)

        etContent?.setSelection(postContent.length)

        tvCharCount?.text = getString(R.string.studio_char_count_format, postContent.length)



        etContent?.addTextChangedListener(object : android.text.TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val len = s?.length ?: 0

                tvCharCount?.text = getString(R.string.studio_char_count_format, len)

            }

            override fun afterTextChanged(s: android.text.Editable?) {}

        })



        btnCancel?.setOnClickListener {

            dialog.dismiss()

        }



        btnSave?.setOnClickListener {

            val newText = etContent?.text?.toString()?.trim() ?: ""

            if (newText.isNotEmpty()) {

                postContent = newText

                updateConfig()

                videoMotionView.restart()

                dialog.dismiss()

                Toast.makeText(this, getString(R.string.studio_text_updated), Toast.LENGTH_SHORT).show()

            } else {

                Toast.makeText(this, getString(R.string.studio_text_invalid), Toast.LENGTH_SHORT).show()

            }

        }



        dialog.show()

    }



    // ─────────────────────────────────────────────────────────────

    // 1. Audio Presets & Online Tracks with Progress

    // ─────────────────────────────────────────────────────────────

    private fun applyTheme(theme: VideoTheme) {

        applyThemeValues(theme)

        customImageBitmap = null

        customAudioUri = null

        downloadedAudioPath = null

        btnAdjustFraming.visibility = View.GONE



        videoMotionView.customAudioUri = null

        videoMotionView.isMuted = (theme.audioPreset == "none" || theme.audioPreset == "mute")

        ivMuteToggle.setImageResource(if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)

        videoMotionView.playAudioImmediately(theme.audioPreset)



        updateConfig()

        populateThemeCards()

        populateAudioPresets()

        populateAnimPresets()

        populateFontPresets()

        populateMotionPresets()

        videoMotionView.restart()



        Toast.makeText(this, "تم تطبيق: ${theme.name}", Toast.LENGTH_SHORT).show()

    }



    private fun populateThemeCards() {

        layoutThemeCards.removeAllViews()

        val dp = resources.displayMetrics.density



        for (theme in VideoTheme.PRESET_THEMES) {

            val isSelected = (currentAppliedThemeId == theme.id) ||

                    (selectedMotion == theme.bgMotionId && selectedFont == theme.fontId && selectedTextColor == theme.textColor)



            val card = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER_VERTICAL

                setPadding((8 * dp).toInt(), (5 * dp).toInt(), (8 * dp).toInt(), (5 * dp).toInt())

                layoutParams = LinearLayout.LayoutParams((100 * dp).toInt(), (52 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)



                val topRow = LinearLayout(this@VideoStudioActivity).apply {

                    orientation = LinearLayout.HORIZONTAL

                    gravity = Gravity.CENTER_VERTICAL

                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)



                    val tvName = TextView(this@VideoStudioActivity).apply {

                        text = theme.name

                        setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#E2E8F0"))

                        textSize = 10.5f

                        typeface = Typeface.DEFAULT_BOLD

                        maxLines = 1

                        ellipsize = TextUtils.TruncateAt.END

                    }

                    addView(tvName)

                }



                val bottomRow = LinearLayout(this@VideoStudioActivity).apply {

                    orientation = LinearLayout.HORIZONTAL

                    gravity = Gravity.CENTER_VERTICAL

                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                    setPadding(0, (2 * dp).toInt(), 0, 0)



                    val dot = View(this@VideoStudioActivity).apply {

                        layoutParams = LinearLayout.LayoutParams((6 * dp).toInt(), (6 * dp).toInt()).apply {

                            marginEnd = (4 * dp).toInt()

                        }

                        background = GradientDrawable().apply {

                            shape = GradientDrawable.OVAL

                            setColor(Color.parseColor(theme.textColor))

                        }

                    }

                    val tvTag = TextView(this@VideoStudioActivity).apply {

                        text = "${theme.bgMotionId} • ${theme.audioPreset}"

                        setTextColor(if (isSelected) Color.parseColor("#38BDF8") else Color.parseColor("#64748B"))

                        textSize = 8f

                        typeface = Typeface.DEFAULT_BOLD

                    }

                    addView(dot)

                    addView(tvTag)

                }



                addView(topRow)

                addView(bottomRow)



                setOnClickListener {

                    applyTheme(theme)

                }

            }

            layoutThemeCards.addView(card)

        }

    }



    private fun populateAudioPresets() {

        layoutAudioPresets.removeAllViews()

        val dp = resources.displayMetrics.density



        fun createAudioCard(

            title: String,

            iconRes: Int,

            isSelected: Boolean,

            subtitle: String? = null,

            onClick: () -> Unit,

            onLongClick: (() -> Boolean)? = null

        ): LinearLayout {

            return LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt())

                layoutParams = LinearLayout.LayoutParams((66 * dp).toInt(), (52 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)



                // Icon Well

                val iconWell = FrameLayout(this@VideoStudioActivity).apply {

                    layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())

                    setBackgroundResource(if (isSelected) R.drawable.bg_card_icon_well_active else R.drawable.bg_card_icon_well)



                    val iv = ImageView(this@VideoStudioActivity).apply {

                        layoutParams = FrameLayout.LayoutParams((13 * dp).toInt(), (13 * dp).toInt(), Gravity.CENTER)

                        setImageResource(iconRes)

                        setColorFilter(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    }

                    addView(iv)

                }



                val tv = TextView(this@VideoStudioActivity).apply {

                    text = if (!subtitle.isNullOrEmpty()) "$title\n$subtitle" else title

                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    textSize = if (!subtitle.isNullOrEmpty()) 7.5f else 8.5f

                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                    gravity = Gravity.CENTER

                    maxLines = 2

                    setPadding(0, (2 * dp).toInt(), 0, 0)

                }



                addView(iconWell)

                addView(tv)

                setOnClickListener { onClick() }

                if (onLongClick != null) {

                    setOnLongClickListener { onLongClick() }

                }

            }

        }



        // Option A: "صامت" / No Audio

        val isMuteSelected = selectedAudio.lowercase() in listOf("none", "mute")

        val muteCard = createAudioCard(

            title = getString(R.string.studio_audio_silent),

            iconRes = R.drawable.ic_volume_off,

            isSelected = isMuteSelected,

            onClick = {

                selectedAudio = "none"

                customAudioUri = null

                downloadedAudioPath = null

                videoMotionView.customAudioUri = null

                videoMotionView.isMuted = true

                ivMuteToggle.setImageResource(R.drawable.ic_volume_off)

                videoMotionView.playAudioImmediately("none")

                updateConfig()

                populateAudioPresets()

            }

        )

        layoutAudioPresets.addView(muteCard)



        // Option B: Custom Audio from Phone (Dedicated Trim & Change cards)
        if (customAudioUri != null) {
            val trimCard = createAudioCard(
                title = if (customAudioTitle.isNotBlank()) customAudioTitle else getString(R.string.studio_audio_my_device),
                iconRes = R.drawable.ic_capcut_audio,
                isSelected = true,
                subtitle = "تعديل القص",
                onClick = {
                    showAudioTrimmerDialog(customAudioUri!!)
                }
            )
            layoutAudioPresets.addView(trimCard)

            val changeCard = createAudioCard(
                title = "صوت آخر",
                iconRes = R.drawable.ic_plus,
                isSelected = false,
                subtitle = "من الهاتف",
                onClick = {
                    pickAudioLauncher.launch("audio/*")
                }
            )
            layoutAudioPresets.addView(changeCard)
        } else {
            val customCard = createAudioCard(
                title = getString(R.string.studio_audio_my_device),
                iconRes = R.drawable.ic_plus,
                isSelected = false,
                subtitle = "من هاتفي",
                onClick = {
                    pickAudioLauncher.launch("audio/*")
                }
            )
            layoutAudioPresets.addView(customCard)
        }



        // Option C: Built-in Studio Loops ("عود", "بيانو", "طبيعة") with Interactive CapCut Audio Trimmer
        val builtInLoops = listOf(
            Triple("oud", getString(R.string.audio_oud), R.drawable.ic_capcut_audio),
            Triple("piano", getString(R.string.audio_piano), R.drawable.ic_capcut_audio),
            Triple("nature", getString(R.string.audio_nature), R.drawable.ic_capcut_audio)
        )

        for ((id, title, iconRes) in builtInLoops) {
            val isSelected = (selectedAudio == id) && (downloadedAudioPath == null)
            val subtitle = if (isSelected) "تعديل القص" else null
            val card = createAudioCard(
                title = title,
                iconRes = iconRes,
                isSelected = isSelected,
                subtitle = subtitle,
                onClick = {
                    val audioUri = getAudioUriForBuiltInPreset(id)
                    selectedAudio = id
                    customAudioTitle = title
                    customAudioUri = audioUri
                    audioStartOffsetMs = 0L
                    downloadedAudioPath = null
                    videoMotionView.audioStartOffsetMs = 0L
                    videoMotionView.customAudioUri = audioUri
                    videoMotionView.isMuted = false
                    ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                    videoMotionView.playAudioPreset(id)
                    updateConfig()
                    populateAudioPresets()
                    // Open CapCut trimmer directly for all built-in audio
                    showAudioTrimmerDialog(audioUri)
                }
            )
            layoutAudioPresets.addView(card)
        }



        // Option D: Online Curated Tracks with Real Download Percentage & Smooth Non-blocking Progress

        for (onlineAudio in StudioAssetManager.onlineAudios) {

            val isDownloaded = StudioAssetManager.isAudioDownloaded(this, onlineAudio.id)

            val isDownloading = downloadingAudioProgressMap.containsKey(onlineAudio.id)

            val downloadPercent = downloadingAudioProgressMap[onlineAudio.id] ?: 0

            val isSelected = (selectedAudio == onlineAudio.id) || (downloadedAudioPath != null && downloadedAudioPath!!.contains(onlineAudio.id))



            val frame = FrameLayout(this).apply {

                layoutParams = LinearLayout.LayoutParams((86 * dp).toInt(), (52 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

            }



            val contentLayout = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setPadding((6 * dp).toInt(), (4 * dp).toInt(), (6 * dp).toInt(), (4 * dp).toInt())

                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)



                val tvTitle = TextView(this@VideoStudioActivity).apply {

                    text = onlineAudio.title

                    setTextColor(Color.parseColor(if (isSelected) "#38BDF8" else "#FFFFFF"))

                    textSize = 9.5f

                    maxLines = 1

                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                    gravity = Gravity.CENTER

                }

                val tvCat = TextView(this@VideoStudioActivity).apply {

                    text = onlineAudio.category

                    setTextColor(Color.parseColor("#94A3B8"))

                    textSize = 8.5f

                    gravity = Gravity.CENTER

                }

                val tvStatus = TextView(this@VideoStudioActivity).apply {

                    tag = "status_${onlineAudio.id}"

                    text = if (isDownloaded) "جاهز" else "تحميل"

                    setTextColor(Color.parseColor(if (isDownloaded) "#10B981" else "#38BDF8"))

                    textSize = 8.5f

                    gravity = Gravity.CENTER

                    setPadding(0, (1 * dp).toInt(), 0, 0)

                }

                addView(tvTitle)

                addView(tvCat)

                addView(tvStatus)

            }

            frame.addView(contentLayout)



            // Progress Overlay (Shown when downloading)

            val progressOverlay = LinearLayout(this).apply {

                tag = "overlay_${onlineAudio.id}"

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                visibility = if (isDownloading) View.VISIBLE else View.GONE

                setBackgroundColor(Color.parseColor("#E60D1117"))

                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)



                val pb = ProgressBar(this@VideoStudioActivity, null, android.R.attr.progressBarStyleSmall)

                val tvPercent = TextView(this@VideoStudioActivity).apply {

                    tag = "percent_${onlineAudio.id}"

                    text = "${downloadPercent}%"

                    setTextColor(Color.parseColor("#38BDF8"))

                    textSize = 9.5f

                    typeface = Typeface.DEFAULT_BOLD

                    gravity = Gravity.CENTER

                    setPadding(0, (1 * dp).toInt(), 0, 0)

                }

                addView(pb)

                addView(tvPercent)

            }

            frame.addView(progressOverlay)



            frame.setOnClickListener {

                if (StudioAssetManager.isAudioDownloaded(this, onlineAudio.id)) {

                    val path = StudioAssetManager.getDownloadedAudioPath(this, onlineAudio.id) ?: return@setOnClickListener
                    val fileUri = Uri.fromFile(File(path))
                    val isAlreadySelected = (selectedAudio == onlineAudio.id)

                    downloadedAudioPath = path
                    selectedAudio = onlineAudio.id
                    customAudioUri = fileUri
                    customAudioTitle = onlineAudio.title
                    audioStartOffsetMs = 0L
                    videoMotionView.audioStartOffsetMs = 0L
                    videoMotionView.customAudioUri = fileUri
                    videoMotionView.isMuted = false
                    ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                    videoMotionView.playAudioPreset(onlineAudio.id, localPath = path)
                    populateAudioPresets()
                    // Open CapCut trimmer directly for online audio
                    showAudioTrimmerDialog(fileUri)

                } else {

                    if (downloadingAudioProgressMap.containsKey(onlineAudio.id)) {

                        return@setOnClickListener

                    }

                    downloadingAudioProgressMap[onlineAudio.id] = 0

                    selectedAudio = onlineAudio.id

                    customAudioUri = null

                    downloadedAudioPath = null

                    videoMotionView.customAudioUri = null

                    videoMotionView.isMuted = false

                    ivMuteToggle.setImageResource(R.drawable.ic_volume_up)

                    videoMotionView.playAudioPreset(onlineAudio.id, streamUrl = onlineAudio.url)

                    populateAudioPresets()



                    StudioAssetManager.downloadAudio(

                        this,

                        onlineAudio,

                        onProgress = { percent ->

                            downloadingAudioProgressMap[onlineAudio.id] = percent

                            val overlay = layoutAudioPresets.findViewWithTag<View>("overlay_${onlineAudio.id}")

                            val tvPercent = layoutAudioPresets.findViewWithTag<TextView>("percent_${onlineAudio.id}")

                            overlay?.visibility = View.VISIBLE

                            tvPercent?.text = "$percent%"

                        },

                        onSuccess = { savedPath ->

                            downloadingAudioProgressMap.remove(onlineAudio.id)

                            downloadedAudioPath = savedPath

                            videoMotionView.playAudioPreset(onlineAudio.id, localPath = savedPath)

                            populateAudioPresets()

                            Toast.makeText(this, getString(R.string.studio_audio_download_success), Toast.LENGTH_SHORT).show()

                        },

                        onError = { errMsg ->

                            downloadingAudioProgressMap.remove(onlineAudio.id)

                            populateAudioPresets()

                            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show()

                        }

                    )

                }

            }

            layoutAudioPresets.addView(frame)

        }

    }



    private fun populateAnimPresets() {

        layoutAnimStyles.removeAllViews()

        val dp = resources.displayMetrics.density



        val effectsList = listOf(

            Triple("typewriter", getString(R.string.fx_typewriter), R.drawable.ic_fx_typewriter),

            Triple("scroll_up", getString(R.string.fx_scroll_up), R.drawable.ic_fx_scroll),

            Triple("word_by_word", getString(R.string.fx_words), R.drawable.ic_fx_words),

            Triple("fade", getString(R.string.fx_fade), R.drawable.ic_fx_fade),

            Triple("zoom", getString(R.string.fx_zoom), R.drawable.ic_fx_zoom),

            Triple("glow", getString(R.string.fx_glow), R.drawable.ic_fx_glow),

            Triple("slide_up", getString(R.string.fx_slide_up), R.drawable.ic_fx_slide),

            Triple("blur_in", getString(R.string.fx_blur_in), R.drawable.ic_fx_blur)

        )



        for ((id, title, iconRes) in effectsList) {

            val isSelected = selectedAnim == id

            val card = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt())

                layoutParams = LinearLayout.LayoutParams((66 * dp).toInt(), (52 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)



                val iconWell = FrameLayout(this@VideoStudioActivity).apply {

                    layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())

                    setBackgroundResource(if (isSelected) R.drawable.bg_card_icon_well_active else R.drawable.bg_card_icon_well)



                    val iv = ImageView(this@VideoStudioActivity).apply {

                        layoutParams = FrameLayout.LayoutParams((13 * dp).toInt(), (13 * dp).toInt(), Gravity.CENTER)

                        setImageResource(iconRes)

                        setColorFilter(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    }

                    addView(iv)

                }



                val tv = TextView(this@VideoStudioActivity).apply {

                    text = title

                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    textSize = 8.5f

                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                    gravity = Gravity.CENTER

                    maxLines = 1

                    setPadding(0, (2 * dp).toInt(), 0, 0)

                }



                addView(iconWell)

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



    private fun populateFontPresets() {

        layoutFontPresets.removeAllViews()

        layoutColorChips.removeAllViews()

        val dp = resources.displayMetrics.density



        // Modern Circular Color Swatches

        val colors = listOf(

            "#FFFFFF" to "أبيض",

            "#FCD34D" to "ذهبي",

            "#0EA5E9" to "سماوي",

            "#FEF3C7" to "عاجي",

            "#FDA4AF" to "وردي",

            "#34D399" to "زمردي",

            "#C084FC" to "بنفسجي",

            "#FB923C" to "برتقالي"

        )

        for ((hex, name) in colors) {

            val isSelected = selectedTextColor.equals(hex, ignoreCase = true)

            val swatchContainer = FrameLayout(this).apply {

                layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                isClickable = true

                isFocusable = true



                // Outer cyan selection ring

                if (isSelected) {

                    background = GradientDrawable().apply {

                        shape = GradientDrawable.OVAL

                        setStroke((2 * dp).toInt(), Color.parseColor("#0EA5E9"))

                        setColor(Color.TRANSPARENT)

                    }

                }



                // Inner vibrant color dot

                val innerCircle = View(this@VideoStudioActivity).apply {

                    val size = if (isSelected) (16 * dp).toInt() else (20 * dp).toInt()

                    layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)

                    background = GradientDrawable().apply {

                        shape = GradientDrawable.OVAL

                        setColor(Color.parseColor(hex))

                        if (hex == "#FFFFFF" || hex == "#FEF3C7") {

                            setStroke((1 * dp).toInt(), Color.parseColor("#475569"))

                        }

                    }

                }

                addView(innerCircle)



                setOnClickListener {

                    selectedTextColor = hex

                    updateConfig()

                    populateFontPresets()

                }

            }

            layoutColorChips.addView(swatchContainer)

        }



        // Sleek Modern Font Cards with the 8 Real Built-in Typography Fonts
        val fontList = listOf(
            "tajawal" to "خط تجوال",
            "cairo" to "خط كايرو",
            "arefruqaa" to "خط الرقعة",
            "amiri" to "خط أميري",
            "kufam" to "خط كوفي",
            "almarai" to "خط المراعي",
            "marhey" to "خط مرحي",
            "notonastaliqurdu" to "خط نستعليق"
        )

        for ((id, name) in fontList) {
            val isSelected = selectedFont.equals(id, ignoreCase = true) ||
                (id == "cairo" && selectedFont.equals("cairoplay", ignoreCase = true)) ||
                (id == "kufam" && (selectedFont.equals("kufi", ignoreCase = true) || selectedFont.equals("reem_kufi", ignoreCase = true))) ||
                (id == "arefruqaa" && selectedFont.equals("ruqaa", ignoreCase = true)) ||
                (id == "notonastaliqurdu" && (selectedFont.equals("nastaliq", ignoreCase = true) || selectedFont.equals("amiri", ignoreCase = true))) ||
                (id == "blakahollow" && selectedFont.equals("blaka", ignoreCase = true))

            val card = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt())

                layoutParams = LinearLayout.LayoutParams((72 * dp).toInt(), (52 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)



                val previewWell = FrameLayout(this@VideoStudioActivity).apply {

                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (22 * dp).toInt())

                    setBackgroundResource(if (isSelected) R.drawable.bg_card_icon_well_active else R.drawable.bg_card_icon_well)



                    val tvPreview = TextView(this@VideoStudioActivity).apply {

                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)

                        text = "منشوراتي"

                        setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#E2E8F0"))

                        textSize = 11.5f

                        typeface = ThemeHelper.getFontTypeface(this@VideoStudioActivity, id)

                    }

                    addView(tvPreview)

                }



                val tvName = TextView(this@VideoStudioActivity).apply {

                    text = name

                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    textSize = 8.5f

                    gravity = Gravity.CENTER

                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                    setPadding(0, (2 * dp).toInt(), 0, 0)

                    maxLines = 1

                }

                addView(previewWell)

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

        val customCard = FrameLayout(this).apply {

            layoutParams = LinearLayout.LayoutParams((66 * dp).toInt(), (52 * dp).toInt()).apply {

                marginEnd = (6 * dp).toInt()

            }

            val isSelected = customImageBitmap != null && selectedMotion == "CUSTOM_IMAGE"

            setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)



            val content = LinearLayout(this@VideoStudioActivity).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                setPadding((4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt())

                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                

                val iconWell = FrameLayout(this@VideoStudioActivity).apply {

                    layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())

                    setBackgroundResource(if (isSelected) R.drawable.bg_card_icon_well_active else R.drawable.bg_card_icon_well)

                    val iv = ImageView(this@VideoStudioActivity).apply {

                        layoutParams = FrameLayout.LayoutParams((13 * dp).toInt(), (13 * dp).toInt(), Gravity.CENTER)

                        setImageResource(R.drawable.ic_gallery)

                        setColorFilter(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    }

                    addView(iv)

                }

                val tv = TextView(this@VideoStudioActivity).apply {

                    text = getString(R.string.studio_audio_my_device)

                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))

                    textSize = 8.5f

                    gravity = Gravity.CENTER

                    typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                    setPadding(0, (2 * dp).toInt(), 0, 0)

                }

                addView(iconWell)

                addView(tv)

            }

            addView(content)

            setOnClickListener {

                pickImageLauncher.launch("image/*")

            }

        }

        layoutMotionPresets.addView(customCard)



        // Shaders List

        val shaderList = listOf(

            Triple("STARS", getString(R.string.bg_stars), "#0B1329"),

            Triple("NEBULA", getString(R.string.bg_nebula), "#1E112A"),

            Triple("AURORA", getString(R.string.bg_aurora), "#082F24"),

            Triple("GOLD", getString(R.string.bg_gold), "#281D05"),

            Triple("OCEAN", getString(R.string.bg_ocean), "#042036"),

            Triple("SUNSET", "غروب دافئ", "#3B1124"),

            Triple("NEON", "أضواء النيون", "#150826"),

            Triple("EMERALD", "واحة زمرد", "#052619"),

            Triple("RAIN", "قطرات المطر", "#090D16")

        )



        if (currentBgCategory == "all" || currentBgCategory == "motion") {

            for ((id, title, colorHex) in shaderList) {

                val isSelected = selectedMotion == id && customImageBitmap == null

                val card = FrameLayout(this).apply {

                    layoutParams = LinearLayout.LayoutParams((66 * dp).toInt(), (52 * dp).toInt()).apply {

                        marginEnd = (6 * dp).toInt()

                    }

                    background = GradientDrawable().apply {

                        cornerRadius = 12 * dp

                        if (isSelected) {

                            setStroke((1.8f * dp).toInt(), Color.parseColor("#0EA5E9"))

                        } else {

                            setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))

                        }

                        setColor(Color.parseColor(colorHex))

                    }

                    clipToOutline = true



                    val content = LinearLayout(this@VideoStudioActivity).apply {

                        orientation = LinearLayout.VERTICAL

                        gravity = Gravity.CENTER

                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                        val iv = ImageView(this@VideoStudioActivity).apply {

                            layoutParams = LinearLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt())

                            setImageResource(R.drawable.ic_fx_glow)

                            setColorFilter(Color.WHITE)

                        }

                        val tv = TextView(this@VideoStudioActivity).apply {

                            text = title

                            setTextColor(Color.WHITE)

                            textSize = 8.5f

                            gravity = Gravity.CENTER

                            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

                            maxLines = 1

                            setPadding(0, (2 * dp).toInt(), 0, 0)

                        }

                        addView(iv)

                        addView(tv)

                    }

                    addView(content)

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



        // Online Unsplash Aesthetics with Real-time Progress Bar & Glide Thumbnails (Stable Curated Order)

        val filteredBgs = if (currentBgCategory == "all") {

            StudioAssetManager.onlineBackgrounds

        } else {

            StudioAssetManager.onlineBackgrounds.filter { it.category == currentBgCategory }

        }



        for (bg in filteredBgs) {

            val isDownloaded = StudioAssetManager.isBackgroundDownloaded(this, bg.id)

            val isSelected = selectedMotion == bg.id && customImageBitmap != null



            val frame = FrameLayout(this).apply {

                layoutParams = LinearLayout.LayoutParams((66 * dp).toInt(), (52 * dp).toInt()).apply {

                    marginEnd = (6 * dp).toInt()

                }

                background = GradientDrawable().apply {

                    cornerRadius = 12 * dp

                    if (isSelected) {

                        setStroke((1.8f * dp).toInt(), Color.parseColor("#0EA5E9"))

                        setColor(Color.parseColor("#0C253E"))

                    } else {

                        setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))

                        setColor(Color.parseColor("#101726"))

                    }

                }

                clipToOutline = true

            }



            // Real Photo Thumbnail with Glide

            val ivThumb = ImageView(this).apply {

                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                scaleType = ImageView.ScaleType.CENTER_CROP

            }

            Glide.with(this)

                .load(bg.thumbUrl)

                .placeholder(ColorDrawable(Color.parseColor(bg.previewColor)))

                .error(ColorDrawable(Color.parseColor(bg.previewColor)))

                .centerCrop()

                .into(ivThumb)

            frame.addView(ivThumb)



            // Bottom Gradient Scrim to ensure text readability

            val scrim = View(this).apply {

                layoutParams = FrameLayout.LayoutParams(

                    FrameLayout.LayoutParams.MATCH_PARENT,

                    (26 * dp).toInt(),

                    Gravity.BOTTOM

                )

                background = GradientDrawable(

                    GradientDrawable.Orientation.TOP_BOTTOM,

                    intArrayOf(Color.TRANSPARENT, Color.parseColor("#EE0D1117"))

                )

            }

            frame.addView(scrim)



            // Text Info & Download Badge

            val infoLayout = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER_HORIZONTAL

                layoutParams = FrameLayout.LayoutParams(

                    FrameLayout.LayoutParams.MATCH_PARENT,

                    FrameLayout.LayoutParams.WRAP_CONTENT,

                    Gravity.BOTTOM

                ).apply {

                    bottomMargin = (2 * dp).toInt()

                    leftMargin = (2 * dp).toInt()

                    rightMargin = (2 * dp).toInt()

                }



                val tvTitle = TextView(this@VideoStudioActivity).apply {

                    text = bg.title

                    setTextColor(Color.WHITE)

                    textSize = 8f

                    maxLines = 1

                    gravity = Gravity.CENTER

                    typeface = Typeface.DEFAULT_BOLD

                }

                val tvStatus = TextView(this@VideoStudioActivity).apply {

                    text = if (isDownloaded) getString(R.string.studio_audio_downloaded) else getString(R.string.studio_audio_download)

                    setTextColor(Color.parseColor(if (isDownloaded) "#10B981" else "#38BDF8"))

                    textSize = 7.5f

                    gravity = Gravity.CENTER

                }

                addView(tvTitle)

                addView(tvStatus)

            }

            frame.addView(infoLayout)



            // Progress Overlay (when downloading)

            val progressOverlay = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                gravity = Gravity.CENTER

                visibility = View.GONE

                setBackgroundColor(Color.parseColor("#D9000000"))

                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)



                val pb = ProgressBar(this@VideoStudioActivity, null, android.R.attr.progressBarStyleSmall)

                val tvPercent = TextView(this@VideoStudioActivity).apply {

                    text = "0%"

                    setTextColor(Color.WHITE)

                    textSize = 10f

                    gravity = Gravity.CENTER

                    typeface = Typeface.DEFAULT_BOLD

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

            imageFocusY = selectedImageFocusY,

            imagePanX = selectedImagePanX,

            imagePanY = selectedImagePanY,

            imageZoomScale = selectedImageZoomScale,

            audioStartOffsetMs = audioStartOffsetMs
,
            colorFilter = selectedColorFilter        )

        videoMotionView.audioStartOffsetMs = audioStartOffsetMs

        videoMotionView.config = config

        updateVnTracksUI()
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

            Toast.makeText(this, getString(R.string.studio_image_read_error), Toast.LENGTH_SHORT).show()

        }

    }



    private fun getAudioUriForBuiltInPreset(presetId: String): Uri {
        val rawResId = when (presetId.lowercase()) {
            "rain" -> R.raw.ambient_rain
            "oud" -> R.raw.ambient_oud
            "nature" -> R.raw.ambient_nature
            else -> R.raw.ambient_piano
        }
        val cacheFile = File(cacheDir, "preset_${presetId.lowercase()}.wav")
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            try {
                resources.openRawResource(rawResId).use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return Uri.parse("android.resource://$packageName/$rawResId")
            }
        }
        return Uri.fromFile(cacheFile)
    }

    private fun loadCustomAudio(uri: Uri, startOffsetMs: Long = 0L) {

        try {

            customAudioUri = uri

            audioStartOffsetMs = startOffsetMs

            selectedAudio = "custom"

            downloadedAudioPath = null

            videoMotionView.audioStartOffsetMs = startOffsetMs

            videoMotionView.customAudioUri = uri

            videoMotionView.isMuted = false

            updateConfig()

            populateAudioPresets()

            videoMotionView.restart()

            Toast.makeText(this, getString(R.string.studio_audio_pick_success), Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {

            Toast.makeText(this, getString(R.string.studio_audio_pick_failed), Toast.LENGTH_SHORT).show()

        }

    }



    private fun showAudioTrimmerDialog(uri: Uri) {

        videoMotionView.pause()



        
        val safeUri = if (uri.scheme == "content") copyAudioUriToInternalStorage(uri) ?: uri else uri
var trackTitle = "مقطع صوتي"

        var trackDurationMs = 0L



        try {

            val retriever = MediaMetadataRetriever()

            retriever.setDataSource(this, safeUri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            trackDurationMs = durStr?.toLongOrNull() ?: 0L

            if (!title.isNullOrBlank()) {

                trackTitle = if (!artist.isNullOrBlank()) "$title - $artist" else title

            }

            retriever.release()

        } catch (_: Exception) {}



        if (trackTitle == "مقطع صوتي") {

            try {

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->

                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                    if (cursor.moveToFirst() && nameIdx >= 0) {

                        val name = cursor.getString(nameIdx)

                        if (!name.isNullOrBlank()) trackTitle = name

                    }

                }

            } catch (_: Exception) {}

        }



        val videoDurationMs = (selectedDuration * 1000L).coerceAtLeast(1000L)



        var previewPlayer: MediaPlayer? = null

        try {

            previewPlayer = MediaPlayer().apply {

                setAudioAttributes(

                    AudioAttributes.Builder()

                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

                        .setUsage(AudioAttributes.USAGE_MEDIA)

                        .build()

                )

                setDataSource(this@VideoStudioActivity, safeUri)

                prepare()

            }

            if (trackDurationMs <= 0L) {

                trackDurationMs = previewPlayer.duration.toLong().coerceAtLeast(videoDurationMs)

            }

        } catch (e: Exception) {

            loadCustomAudio(uri, 0L)

            return

        }



        val maxStartMs = (trackDurationMs - videoDurationMs).coerceAtLeast(0L)

        var currentStartMs = audioStartOffsetMs.coerceIn(0L, maxStartMs)



        val bottomSheet = BottomSheetDialog(this)

        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_audio_trimmer, null)

        bottomSheet.setContentView(dialogView)



        val tvTrackName = dialogView.findViewById<TextView>(R.id.tvTrimmerTrackName)

        val tvTotalDuration = dialogView.findViewById<TextView>(R.id.tvTrimmerTotalDuration)

        val tvVideoDuration = dialogView.findViewById<TextView>(R.id.tvTrimmerVideoDuration)

        val tvStartTime = dialogView.findViewById<TextView>(R.id.tvTrimmerStartTime)

        val tvEndTime = dialogView.findViewById<TextView>(R.id.tvTrimmerEndTime)

        val tvMaxTime = dialogView.findViewById<TextView?>(R.id.tvTrimmerMaxTime)

        val sbTrimmer = dialogView.findViewById<SeekBar?>(R.id.sbAudioTrimmer)

        val btnPlayContainer = dialogView.findViewById<View?>(R.id.btnTrimmerPlayContainer)

        val ivPlayIcon = dialogView.findViewById<ImageView>(R.id.ivTrimmerPlayIcon)

        val tvPlayLabel = dialogView.findViewById<TextView?>(R.id.tvTrimmerPlayLabel)

        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmTrim)

        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelTrim)



        // CapCut / Premiere Style Interactive Timeline & Controls

        val timelineView = dialogView.findViewById<AudioTimelineTrimmerView?>(R.id.audioTimelineTrimmerView)

        val btnPlayToggle = dialogView.findViewById<View?>(R.id.btnTrimmerPlayPause)

        val btnRewind5s = dialogView.findViewById<View?>(R.id.btnTrimmerRewind5s)

        val btnForward5s = dialogView.findViewById<View?>(R.id.btnTrimmerForward5s)



        tvTrackName.text = trackTitle

        tvTotalDuration.text = formatTime(trackDurationMs)

        tvVideoDuration.text = "المدة: ${selectedDuration} ثانية"

        tvMaxTime?.text = formatTime(trackDurationMs)



        fun updateTimeDisplay() {

            val endMs = minOf(trackDurationMs, currentStartMs + videoDurationMs)

            tvStartTime.text = formatTime(currentStartMs)

            tvEndTime.text = formatTime(endMs)

            tvTotalDuration.text = formatTime(trackDurationMs)

        }



        updateTimeDisplay()



        val handler = Handler(Looper.getMainLooper())

        var isPreviewPlaying = false



        // Configure Timeline View

        timelineView?.let { tl ->

            tl.trackTitle = trackTitle

            tl.totalDurationMs = trackDurationMs

            tl.clipDurationMs = videoDurationMs

            tl.currentStartMs = currentStartMs

            tl.currentPlayheadMs = currentStartMs



            tl.onTrimChanged = { startMs, _ ->

                currentStartMs = startMs

                updateTimeDisplay()

                if (isPreviewPlaying) {

                    previewPlayer?.seekTo(currentStartMs.toInt())

                }

            }



            tl.onSeek = { positionMs ->

                previewPlayer?.seekTo(positionMs.toInt())

                tvStartTime.text = formatTime(positionMs)

            }

        }



        val checkLoopRunnable = object : Runnable {

            override fun run() {

                previewPlayer?.let { mp ->

                    if (isPreviewPlaying && mp.isPlaying) {

                        val currentPos = mp.currentPosition.toLong()

                        timelineView?.currentPlayheadMs = currentPos

                        tvStartTime.text = formatTime(currentPos)

                        val endMs = currentStartMs + videoDurationMs

                        if (currentPos >= endMs || currentPos < currentStartMs) {

                            mp.seekTo(currentStartMs.toInt())

                            timelineView?.currentPlayheadMs = currentStartMs

                        }

                        handler.postDelayed(this, 60)

                    }

                }

            }

        }



        fun playPreview() {

            try {

                previewPlayer?.let { mp ->

                    mp.seekTo(currentStartMs.toInt())

                    mp.start()

                    isPreviewPlaying = true

                    ivPlayIcon.setImageResource(R.drawable.ic_video_pause)

                    tvPlayLabel?.text = "إيقاف الاستماع"

                    handler.post(checkLoopRunnable)

                }

            } catch (_: Exception) {}

        }



        fun pausePreview() {

            try {

                previewPlayer?.pause()

                isPreviewPlaying = false

                ivPlayIcon.setImageResource(R.drawable.ic_video_play)

                tvPlayLabel?.text = "استماع للمقطع المختار (${selectedDuration} ثانية)"

                handler.removeCallbacks(checkLoopRunnable)

            } catch (_: Exception) {}

        }



        btnPlayToggle?.setOnClickListener {

            if (isPreviewPlaying) pausePreview() else playPreview()

        }



        btnPlayContainer?.setOnClickListener {

            if (isPreviewPlaying) pausePreview() else playPreview()

        }



        btnRewind5s?.setOnClickListener {

            previewPlayer?.let { mp ->

                val newPos = (mp.currentPosition - 5000).coerceAtLeast(currentStartMs.toInt())

                mp.seekTo(newPos)

                timelineView?.currentPlayheadMs = newPos.toLong()

                tvStartTime.text = formatTime(newPos.toLong())

            }

        }



        btnForward5s?.setOnClickListener {

            previewPlayer?.let { mp ->

                val maxPos = (currentStartMs + videoDurationMs).toInt()

                val newPos = (mp.currentPosition + 5000).coerceAtMost(maxPos)

                mp.seekTo(newPos)

                timelineView?.currentPlayheadMs = newPos.toLong()

                tvStartTime.text = formatTime(newPos.toLong())

            }

        }



        val steps = 1000

        val initialProgress = if (maxStartMs > 0) ((currentStartMs.toDouble() / maxStartMs.toDouble()) * steps).toInt() else 0

        sbTrimmer?.max = steps

        sbTrimmer?.progress = initialProgress

        sbTrimmer?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                if (fromUser && maxStartMs > 0) {

                    currentStartMs = ((progress.toDouble() / steps.toDouble()) * maxStartMs).toLong()

                    timelineView?.currentStartMs = currentStartMs

                    updateTimeDisplay()

                    if (isPreviewPlaying) {

                        previewPlayer?.seekTo(currentStartMs.toInt())

                    }

                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                if (!isPreviewPlaying) {

                    previewPlayer?.seekTo(currentStartMs.toInt())

                }

            }

        })



                // Nudge Controls: [-5s] [-1s] [Reset 00:00] [+1s] [+5s]
        val btnNudgeMinus5s = dialogView.findViewById<TextView?>(R.id.btnNudgeMinus5s)
        val btnNudgeMinus1s = dialogView.findViewById<TextView?>(R.id.btnNudgeMinus1s)
        val btnNudgeResetStart = dialogView.findViewById<TextView?>(R.id.btnNudgeResetStart)
        val btnNudgePlus1s = dialogView.findViewById<TextView?>(R.id.btnNudgePlus1s)
        val btnNudgePlus5s = dialogView.findViewById<TextView?>(R.id.btnNudgePlus5s)

        btnNudgeMinus5s?.setOnClickListener {
            timelineView?.nudgeStartMs(-5000L)
            currentStartMs = timelineView?.currentStartMs ?: currentStartMs
            updateTimeDisplay()
            if (isPreviewPlaying) previewPlayer?.seekTo(currentStartMs.toInt())
        }
        btnNudgeMinus1s?.setOnClickListener {
            timelineView?.nudgeStartMs(-1000L)
            currentStartMs = timelineView?.currentStartMs ?: currentStartMs
            updateTimeDisplay()
            if (isPreviewPlaying) previewPlayer?.seekTo(currentStartMs.toInt())
        }
        btnNudgeResetStart?.setOnClickListener {
            timelineView?.currentStartMs = 0L
            currentStartMs = 0L
            updateTimeDisplay()
            if (isPreviewPlaying) previewPlayer?.seekTo(0)
        }
        btnNudgePlus1s?.setOnClickListener {
            timelineView?.nudgeStartMs(1000L)
            currentStartMs = timelineView?.currentStartMs ?: currentStartMs
            updateTimeDisplay()
            if (isPreviewPlaying) previewPlayer?.seekTo(currentStartMs.toInt())
        }
        btnNudgePlus5s?.setOnClickListener {
            timelineView?.nudgeStartMs(5000L)
            currentStartMs = timelineView?.currentStartMs ?: currentStartMs
            updateTimeDisplay()
            if (isPreviewPlaying) previewPlayer?.seekTo(currentStartMs.toInt())
        }

        val btnPickOther = dialogView.findViewById<TextView?>(R.id.btnPickOtherInTrimmer)
        btnPickOther?.setOnClickListener {
            pausePreview()
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
            } catch (_: Exception) {}
            bottomSheet.dismiss()
            pickAudioLauncher.launch("audio/*")
        }

        btnConfirm.setOnClickListener {
            pausePreview()
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
            } catch (_: Exception) {}
            bottomSheet.dismiss()
            customAudioTitle = trackTitle
            loadCustomAudio(safeUri, currentStartMs)
        }



        btnCancel.setOnClickListener {

            pausePreview()

            try {

                previewPlayer?.stop()

                previewPlayer?.release()

                previewPlayer = null

            } catch (_: Exception) {}

            bottomSheet.dismiss()

            videoMotionView.play()

        }



        bottomSheet.setOnDismissListener {

            pausePreview()

            try {

                previewPlayer?.stop()

                previewPlayer?.release()

                previewPlayer = null

            } catch (_: Exception) {}

        }



        bottomSheet.show()

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

            text = getString(R.string.studio_export_title)

            setTextColor(Color.WHITE)

            textSize = 16f

            typeface = android.graphics.Typeface.DEFAULT_BOLD

            gravity = Gravity.CENTER

            setPadding(0, 0, 0, (14 * density).toInt())

        }

        container.addView(tvSheetTitle)



        // Video Export Quality Selector (1080p FHD, 720p HD, 480p SD)

        val layoutQualitySection = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(0, 0, 0, (18 * density).toInt())

        }



        val tvQualityLabel = TextView(this).apply {

            text = getString(R.string.studio_resolution_prompt)

            setTextColor(Color.parseColor("#94A3B8"))

            textSize = 12f

            gravity = Gravity.START

            setPadding(0, 0, 0, (8 * density).toInt())

        }

        layoutQualitySection.addView(tvQualityLabel)



        val rowQualityPills = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            background = GradientDrawable().apply {

                cornerRadius = 12 * density

                setColor(Color.parseColor("#10131A"))

                setStroke((1 * density).toInt(), Color.parseColor("#22FFFFFF"))

            }

            setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())

            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * density).toInt())

        }



        val qualityList = listOf(

            Triple("1080p", "1080p FHD", "دقة فائقة"),

            Triple("720p", "720p HD", "متوازنة"),

            Triple("480p", "480p SD", "خفيفة")

        )



        val qualityBtnViews = mutableListOf<TextView>()

        for ((qId, qTitle, _) in qualityList) {

            val pill = TextView(this).apply {

                text = qTitle

                textSize = 11.5f

                typeface = android.graphics.Typeface.DEFAULT_BOLD

                gravity = Gravity.CENTER

                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)

                val isSel = selectedExportQuality.lowercase() == qId

                if (isSel) {

                    setBackgroundResource(R.drawable.bg_pill_active)

                    setTextColor(Color.WHITE)

                } else {

                    background = null

                    setTextColor(Color.parseColor("#94A3B8"))

                }

            }

            pill.setOnClickListener {

                selectedExportQuality = qId

                for (i in qualityList.indices) {

                    val targetView = qualityBtnViews[i]

                    if (qualityList[i].first == qId) {

                        targetView.setBackgroundResource(R.drawable.bg_pill_active)

                        targetView.setTextColor(Color.WHITE)

                    } else {

                        targetView.background = null

                        targetView.setTextColor(Color.parseColor("#94A3B8"))

                    }

                }

            }

            qualityBtnViews.add(pill)

            rowQualityPills.addView(pill)

        }

        layoutQualitySection.addView(rowQualityPills)

        container.addView(layoutQualitySection)



        // 1. WhatsApp Status Option

        val btnWhatsApp = createExportOptionRow(

            title = getString(R.string.studio_whatsapp_status_title),

            desc = getString(R.string.studio_whatsapp_status_desc),

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

            title = getString(R.string.studio_save_gallery_title),

            desc = getString(R.string.studio_save_gallery_desc),

            iconRes = R.drawable.ic_download,

            accentColor = "#0284C7"

        ) {

            bottomSheet.dismiss()

            startExport { file ->

                val uri = VideoExporter.saveVideoToGallery(this, file, postAuthorName)

                if (uri != null) {

                    Toast.makeText(this, getString(R.string.studio_save_gallery_success), Toast.LENGTH_LONG).show()

                } else {

                    Toast.makeText(this, getString(R.string.studio_save_gallery_error), Toast.LENGTH_SHORT).show()

                }

            }

        }

        container.addView(btnGallery)



        // 3. General Share Option

        val btnGeneral = createExportOptionRow(

            title = getString(R.string.studio_share_other_apps_title),

            desc = getString(R.string.studio_share_other_apps_desc),

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

            text = getString(R.string.studio_exporting_progress)

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

            quality = selectedExportQuality,

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

                    Toast.makeText(this, getString(R.string.studio_export_error, exc.message ?: ""), Toast.LENGTH_LONG).show()

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


    // ─────────────────────────────────────────────────────────────
    // VN Style Cinematic Filters & LUTs + Multi-Track Updates
    // ─────────────────────────────────────────────────────────────
    private fun populateFilterCards() {
        layoutFilterCards.removeAllViews()
        val dp = resources.displayMetrics.density

        data class FilterPreset(val id: String, val name: String, val previewColor1: String, val previewColor2: String)

        val filters = listOf(
            FilterPreset("none", "أصلي (None)", "#1E293B", "#0F172A"),
            FilterPreset("teal_orange", "سينمائي (Teal & Orange)", "#0E7490", "#EA580C"),
            FilterPreset("moody", "مودي (Moody Film)", "#1E1B4B", "#312E81"),
            FilterPreset("vintage_90s", "ريترو (Vintage 90s)", "#78350F", "#D97706"),
            FilterPreset("noir_bw", "أبيض وأسود (Noir B&W)", "#18181B", "#71717A"),
            FilterPreset("cyberpunk", "سايبر (Cyberpunk)", "#4C1D95", "#0284C7"),
            FilterPreset("sunset_gold", "غروب (Golden Hour)", "#9A3412", "#F59E0B")
        )

        for (flt in filters) {
            val isSelected = selectedColorFilter == flt.id
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams((90 * dp).toInt(), (52 * dp).toInt()).apply {
                    marginEnd = (6 * dp).toInt()
                }
                setBackgroundResource(if (isSelected) R.drawable.bg_tool_card_selected else R.drawable.bg_tool_card_normal)

                val swatch = View(this@VideoStudioActivity).apply {
                    layoutParams = LinearLayout.LayoutParams((28 * dp).toInt(), (14 * dp).toInt()).apply {
                        bottomMargin = (4 * dp).toInt()
                    }
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(Color.parseColor(flt.previewColor1), Color.parseColor(flt.previewColor2))
                    ).apply {
                        cornerRadius = 7 * dp
                        if (isSelected) {
                            setStroke((1.5f * dp).toInt(), Color.parseColor("#38BDF8"))
                        }
                    }
                }

                val tvName = TextView(this@VideoStudioActivity).apply {
                    text = flt.name
                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))
                    textSize = 9.5f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    maxLines = 1
                }

                addView(swatch)
                addView(tvName)

                setOnClickListener {
                    selectedColorFilter = flt.id
                    updateConfig()
                    populateFilterCards()
                }
            }
            layoutFilterCards.addView(card)
        }
    }

    private fun updateVnTracksUI() {
        val tvContent = tvVnTrackTextContent ?: return
        tvContent.text = postContent
        tvVnTrackTextAnimBadge?.text = when(selectedAnim.lowercase()) {
            "typewriter" -> "آلة كاتبة"
            "word_by_word" -> "كلمة بكلمة"
            "fade" -> "تلاشي ناعم"
            "zoom" -> "تكبير بوب"
            "glow" -> "توهج مضيء"
            "slide_up" -> "انزلاق سينمائي"
            "scroll_up", "scroll" -> "تصاعد عمودي"
            "blur_in" -> "ظهور ضبابي"
            else -> selectedAnim
        }
        val audioTitle = when {
            customAudioUri != null -> if (customAudioTitle.isNotBlank()) customAudioTitle else "صوتي الخاص"
            downloadedAudioPath != null -> StudioAssetManager.onlineAudios.find { it.id == selectedAudio }?.title ?: selectedAudio
            else -> when(selectedAudio) {
                "oud" -> "عود شرقي"
                "piano" -> "بيانو هادئ"
                "nature" -> "أصوات الطبيعة"
                "mute" -> "بدون صوت"
                else -> selectedAudio
            }
        }
        tvVnTrackAudioTitle?.text = audioTitle
        tvVnTrackAudioTimeBadge?.text = if (customAudioUri != null) {
            "قص: ${formatTime(audioStartOffsetMs)}"
        } else {
            "00:00 / ${formatTime((selectedDuration * 1000L))}"
        }

        val bgTitle = when {
            customImageBitmap != null -> "صورتي الخاصة"
            selectedMotion.startsWith("unsplash_") -> StudioAssetManager.onlineBackgrounds.find { it.id == selectedMotion }?.title ?: "صورة جمالية"
            else -> when(selectedMotion) {
                "STARS" -> "نجوم متحركة"
                "RAIN" -> "أمطار سينمائية"
                "AURORA" -> "شفق قطبي"
                "GOLD" -> "جزيئات ذهبية"
                "OCEAN" -> "أمواج هادئة"
                else -> selectedMotion
            }
        }
        tvVnTrackCanvasTitle?.text = "$bgTitle • $selectedRatio"

        tvVnTrackFilterBadge?.text = when(selectedColorFilter.lowercase()) {
            "teal_orange" -> "فلتر: Teal & Orange"
            "moody" -> "فلتر: Moody"
            "vintage_90s" -> "فلتر: Vintage 90s"
            "noir_bw" -> "فلتر: Noir B&W"
            "cyberpunk" -> "فلتر: Cyberpunk"
            "sunset_gold" -> "فلتر: Sunset Gold"
            else -> "فلتر: أصلي"
        }
    }

}

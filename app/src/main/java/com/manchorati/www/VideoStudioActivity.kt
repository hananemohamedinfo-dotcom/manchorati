package com.manchorati.www

import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants



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

import android.os.Build

import android.os.Bundle

import android.view.Gravity

import android.view.LayoutInflater

import android.view.View

import android.view.Window

import android.widget.*

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.FileProvider

import androidx.core.view.ViewCompat

import androidx.core.view.WindowCompat

import androidx.core.view.WindowInsetsCompat

import com.bumptech.glide.Glide

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior

import android.media.AudioAttributes

import android.media.MediaPlayer
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView

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
    private lateinit var btnTextColorSquare: FrameLayout
    private lateinit var vTextColorPreview: View
    private var selectedTextSizeScale: Float = 1.0f
    private lateinit var btnTextSizeMinus: TextView
    private lateinit var tvTextSizeBadge: TextView
    private lateinit var btnTextSizePlus: TextView
    private lateinit var scrollFont: HorizontalScrollView
    private lateinit var layoutFontPresets: LinearLayout



    // Panel 4: Canvas (Backgrounds)

    private lateinit var panelCanvasContainer: LinearLayout

    private lateinit var tabBgAll: TextView

    private lateinit var tabBgMotion: TextView

    private lateinit var tabBgDark: TextView

    private lateinit var tabBgNature: TextView
    private lateinit var tabBgVideo: TextView
    private lateinit var btnPickCustomVideoBg: LinearLayout
    private lateinit var textureBgVideo: TextureView
    private var bgVideoMediaPlayer: MediaPlayer? = null
    private var isSeekingBgVideo = false
    private var selectedVideoBgPath: String? = null
    private var selectedVideoBgStartSec: Float = 0f
    private var selectedVideoBgEndSec: Float = 0f
    private var selectedVideoBgSpeed: Float = 1.0f // 1.0f = normal, 0.5f = slow motion, 0.25f = ultra slow
    private val downloadingVideoBgProgressMap = mutableMapOf<String, Int>()
    private val downloadingBgProgressMap = mutableMapOf<String, Int>()

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

    // Panel 7: Stickers & GIFs
    private lateinit var panelStickersContainer: LinearLayout
    private lateinit var layoutStickerCards: LinearLayout
    private lateinit var tabStickerAll: TextView
    private lateinit var tabStickerIslamic: TextView
    private lateinit var tabStickerFeelings: TextView
    private lateinit var tabStickerMagic: TextView
    private lateinit var tabStickerReactions: TextView
    private lateinit var btnPickCustomSticker: View
    private lateinit var btnRemoveSticker: TextView
    private lateinit var toolStickers: LinearLayout
    private lateinit var ivToolStickers: ImageView
    private lateinit var tvToolStickers: TextView

    private var currentStickerCategory = "all"
    private var selectedStickerPath: String? = null
    private var selectedStickerX = 0.5f
    private var selectedStickerY = 0.76f
    private var selectedStickerScale = 1.0f
    private val downloadingStickerProgressMap = mutableMapOf<String, Int>()
    val activeStickers = mutableListOf<VideoMotionRenderer.StickerItem>()

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

    private lateinit var toolTrimVideo: LinearLayout
    private lateinit var ivToolTrimVideo: ImageView
    private lateinit var tvToolTrimVideo: TextView

    private lateinit var toolSlowMotion: LinearLayout
    private lateinit var ivToolSlowMotion: ImageView
    private lateinit var tvToolSlowMotion: TextView

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

    private var selectedAudio = "none" // Muted / No sound by default

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
    private var isPhoneCustomAudio: Boolean = false

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

    // Video Background Picker
    private val pickVideoBgLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            loadCustomVideoBg(uri)
        }
    }

    private fun loadCustomVideoBg(uri: Uri) {
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage("جاري تجهيز مقطع الفيديو...")
            setCancelable(false)
            show()
        }
        Thread {
            try {
                val cacheDir = File(cacheDir, "studio_custom_vbg").apply { mkdirs() }
                val destFile = File(cacheDir, "vbg_${System.currentTimeMillis()}.mp4")
                contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(destFile).use { out ->
                        input.copyTo(out)
                    }
                }
                var durSec = selectedDuration.toFloat()
                if (destFile.exists() && destFile.length() > 1024) {
                    try {
                        val retriever = MediaMetadataRetriever().apply { setDataSource(destFile.absolutePath) }
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        durSec = (durStr?.toLongOrNull() ?: (selectedDuration * 1000L)) / 1000f
                        retriever.release()
                    } catch (_: Exception) {}
                }
                runOnUiThread {
                    try { progressDialog.dismiss() } catch (_: Exception) {}
                    if (destFile.exists() && destFile.length() > 1024) {
                        selectedVideoBgPath = destFile.absolutePath
                        selectedVideoBgStartSec = 0f
                        selectedVideoBgEndSec = durSec.coerceAtMost(selectedDuration.toFloat())
                        selectedMotion = "VIDEO_BG"
                        customImageBitmap = null
                        btnAdjustFraming.visibility = View.GONE
                        setupOrUpdateVideoBackgroundPlayer()
                        updateConfig()
                        populateMotionPresets()
                    } else {
                        Toast.makeText(this@VideoStudioActivity, "تعذر قراءة ملف الفيديو", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    try { progressDialog.dismiss() } catch (_: Exception) {}
                    Toast.makeText(this@VideoStudioActivity, "حدث خطأ أثناء تحميل الفيديو", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

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

    private val pickStickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            handleCustomStickerSelected(uri)
        }
    }


    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val safeUri = copyAudioUriToInternalStorage(uri) ?: uri
            customAudioUri = safeUri
            isPhoneCustomAudio = true
            customAudioTitle = "صوت من هاتفي"
            loadCustomAudio(safeUri, 0L)
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
        setupTextSizeControls()

        setupCustomDurationControls()

        setupBgCategories()

        populateThemeCards()

        populateAudioPresets()

        populateAnimPresets()

        populateFontPresets()

        populateMotionPresets()

        populateFilterCards()

        populateStickerCards()

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
        } else {
            selectedMotion = intent.getStringExtra("EXTRA_BG_MOTION_ID") ?: "STARS"
            selectedAudio = intent.getStringExtra("EXTRA_AUDIO_PRESET") ?: "none"
            selectedAnim = intent.getStringExtra("EXTRA_ANIM_STYLE") ?: "typewriter"
            selectedFont = intent.getStringExtra("EXTRA_FONT_ID") ?: "tajawal"
            selectedTextColor = intent.getStringExtra("EXTRA_TEXT_COLOR") ?: "#FFFFFF"
        }

        // By default, ALL videos start with NO SOUND / MUTED unless explicitly requested via intent
        val explicitAudio = intent.getStringExtra("EXTRA_AUDIO_PRESET")
        selectedAudio = explicitAudio ?: "none"

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
        videoMotionView.isMuted = (selectedAudio == "none")
        ivMuteToggle.setImageResource(if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)



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
        btnTextColorSquare = findViewById(R.id.btnTextColorSquare)
        vTextColorPreview = findViewById(R.id.vTextColorPreview)
        scrollFont = findViewById(R.id.scrollFont)
        btnTextSizeMinus = findViewById(R.id.btnTextSizeMinus)
        tvTextSizeBadge = findViewById(R.id.tvTextSizeBadge)
        btnTextSizePlus = findViewById(R.id.btnTextSizePlus)
        layoutFontPresets = findViewById(R.id.layoutFontPresets)



        panelCanvasContainer = findViewById(R.id.panelCanvasContainer)

        tabBgAll = findViewById(R.id.tabBgAll)

        tabBgMotion = findViewById(R.id.tabBgMotion)

        tabBgDark = findViewById(R.id.tabBgDark)

        tabBgNature = findViewById(R.id.tabBgNature)
        tabBgVideo = findViewById(R.id.tabBgVideo)
        btnPickCustomVideoBg = findViewById(R.id.btnPickCustomVideoBg)
        textureBgVideo = findViewById(R.id.textureBgVideo)

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
        tvToolThemes.text = getString(R.string.studio_tool_ratio)

        panelFiltersContainer = findViewById(R.id.panelFiltersContainer)
        layoutFilterCards = findViewById(R.id.layoutFilterCards)
        toolFilters = findViewById(R.id.toolFilters)
        ivToolFilters = findViewById(R.id.ivToolFilters)
        tvToolFilters = findViewById(R.id.tvToolFilters)
        tvToolFilters.text = getString(R.string.studio_tool_filters)

        toolAudio = findViewById(R.id.toolAudio)

        toolEffects = findViewById(R.id.toolEffects)

        toolText = findViewById(R.id.toolText)

        toolCanvas = findViewById(R.id.toolCanvas)
        toolTrimVideo = findViewById(R.id.toolTrimVideo)
        toolSlowMotion = findViewById(R.id.toolSlowMotion)

        toolSettings = findViewById(R.id.toolSettings)

        ivToolTrimVideo = findViewById(R.id.ivToolTrimVideo)
        tvToolTrimVideo = findViewById(R.id.tvToolTrimVideo)
        tvToolTrimVideo.text = getString(R.string.studio_tool_trim_video)

        ivToolSlowMotion = findViewById(R.id.ivToolSlowMotion)
        tvToolSlowMotion = findViewById(R.id.tvToolSlowMotion)
        tvToolSlowMotion.text = getString(R.string.studio_tool_slow_motion)



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



        panelStickersContainer = findViewById(R.id.panelStickersContainer)
        layoutStickerCards = findViewById(R.id.layoutStickerCards)
        tabStickerAll = findViewById(R.id.tabStickerAll)
        tabStickerIslamic = findViewById(R.id.tabStickerIslamic)
        tabStickerFeelings = findViewById(R.id.tabStickerFeelings)
        tabStickerMagic = findViewById(R.id.tabStickerMagic)
        tabStickerReactions = findViewById(R.id.tabStickerReactions)
        btnPickCustomSticker = findViewById(R.id.btnPickCustomSticker)
        btnRemoveSticker = findViewById(R.id.btnRemoveSticker)
        toolStickers = findViewById(R.id.toolStickers)
        ivToolStickers = findViewById(R.id.ivToolStickers)
        tvToolStickers = findViewById(R.id.tvToolStickers)
        tvToolStickers.text = getString(R.string.studio_stickers)

        btnQuickEditText = findViewById(R.id.btnQuickEditText)



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

    private fun showExitConfirmationDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.studio_exit_title)
            .setMessage(R.string.studio_exit_message)
            .setPositiveButton(R.string.studio_exit_stay) { d, _ ->
                d.dismiss()
            }
            .setNegativeButton(R.string.studio_exit_confirm) { _, _ ->
                finish()
            }
            .show()
    }

    private fun setupListeners() {

        btnBackContainer.setOnClickListener { showExitConfirmationDialog() }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })



        // Top Right "Enreg." / Save Button

        btnEnregContainer.setOnClickListener {

            showExportActionSheet()

        }



        // Quick Edit Quote Text

        btnQuickEditText?.setOnClickListener {
            showEditTextDialog()
        }

        btnTextColorSquare.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            showTextColorPickerDialog()
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
        ivMuteToggle.setImageResource(if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)
        ivMuteToggle.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            videoMotionView.isMuted = !videoMotionView.isMuted
            ivMuteToggle.setImageResource(
                if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            )
        }



        // Play/Pause State Sync (Background Video + Motion View + Audio in complete sync)
        videoMotionView.onPlayStateChanged = { isPlaying ->
            ivPlayPauseCenter.setImageResource(
                if (isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play
            )
            try {
                if (selectedVideoBgPath != null && bgVideoMediaPlayer != null) {
                    if (isPlaying) {
                        if (!bgVideoMediaPlayer!!.isPlaying) {
                            bgVideoMediaPlayer?.start()
                        }
                    } else {
                        if (bgVideoMediaPlayer!!.isPlaying) {
                            bgVideoMediaPlayer?.pause()
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Seamless loop synchronization for background video
        videoMotionView.onLoopCompleted = {
            try {
                if (selectedVideoBgPath != null && bgVideoMediaPlayer != null && !isSeekingBgVideo) {
                    val loopStart = (selectedVideoBgStartSec * 1000).toInt()
                    isSeekingBgVideo = true
                    bgVideoMediaPlayer?.seekTo(loopStart)
                    if (videoMotionView.isPlaying) {
                        bgVideoMediaPlayer?.start()
                    }
                }
            } catch (_: Exception) {
                isSeekingBgVideo = false
            }
        }

        // Live Progress Sync with Scrubber SeekBar & Background Video
        videoMotionView.onProgressUpdate = { curMs, totalMs, ratio ->
            if (!isUserTrackingSeek) {
                seekBarVideo.progress = (ratio * 1000).toInt()
                tvTimeCurrent.text = formatTime(curMs)
                tvVnTrackAudioTimeBadge?.let { badge ->
                    val baseStart = if (customAudioUri != null) audioStartOffsetMs else 0L
                    badge.text = "${formatTime(baseStart + curMs)} / ${formatTime(baseStart + totalMs)}"
                }

                // If background video segment ended, loop it seamlessly without spamming seekTo
                try {
                    if (selectedVideoBgPath != null && bgVideoMediaPlayer != null && !isSeekingBgVideo) {
                        val segStartMs = (selectedVideoBgStartSec * 1000).toInt()
                        val segEndMs = if (selectedVideoBgEndSec > selectedVideoBgStartSec) (selectedVideoBgEndSec * 1000).toInt() else (bgVideoMediaPlayer?.duration ?: 0)
                        if (segEndMs > segStartMs) {
                            val mpPos = try { bgVideoMediaPlayer?.currentPosition ?: -1 } catch (_: Exception) { -1 }
                            if (mpPos >= segEndMs || (mpPos >= 0 && mpPos < segStartMs - 1000)) {
                                isSeekingBgVideo = true
                                bgVideoMediaPlayer?.seekTo(segStartMs)
                                if (videoMotionView.isPlaying) {
                                    bgVideoMediaPlayer?.start()
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    isSeekingBgVideo = false
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
                try {
                    if (selectedVideoBgPath != null && bgVideoMediaPlayer != null) {
                        val segStartMs = (selectedVideoBgStartSec * 1000).toLong()
                        val segEndMs = if (selectedVideoBgEndSec > selectedVideoBgStartSec) (selectedVideoBgEndSec * 1000).toLong() else bgVideoMediaPlayer!!.duration.toLong()
                        val segDurationMs = (segEndMs - segStartMs).coerceAtLeast(100L)
                        val curMs = (ratio * selectedDuration * 1000L).toLong()
                        val scaledTimeMs = (curMs * selectedVideoBgSpeed).toLong()
                        val targetBgMs = (segStartMs + (scaledTimeMs % segDurationMs)).toInt()
                        bgVideoMediaPlayer?.seekTo(targetBgMs)
                        if (videoMotionView.isPlaying) {
                            bgVideoMediaPlayer?.start()
                        } else {
                            bgVideoMediaPlayer?.pause()
                        }
                    }
                } catch (_: Exception) {}
            }
        })

        // Close Tool Panel button
        btnCloseToolPanel.setOnClickListener {
            closeToolPanel()
        }

        // Bottom Tools (Themes, Audio, Effects, Filters, Text, Canvas, TrimVideo, SlowMotion, Stickers, Settings)
        toolThemes.setOnClickListener { openToolPanel(0) }
        toolAudio.setOnClickListener {
            closeToolPanel()
            updateToolBarHighlights(1)
            showStudioAudioDialog()
        }
        toolEffects.setOnClickListener { openToolPanel(2) }
        toolFilters.setOnClickListener { openToolPanel(3) }
        toolText.setOnClickListener { openToolPanel(4) }
        toolCanvas.setOnClickListener {
            closeToolPanel()
            updateToolBarHighlights(5)
            showPicsartBackgroundsDialog()
        }
        toolTrimVideo.setOnClickListener {
            closeToolPanel()
            updateToolBarHighlights(6)
            if (selectedVideoBgPath != null) {
                val path = selectedVideoBgPath!!
                var durSec = 15f
                try {
                    val retriever = MediaMetadataRetriever().apply { setDataSource(path) }
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    durSec = (durStr?.toLongOrNull() ?: 15000L) / 1000f
                    retriever.release()
                } catch (_: Exception) {}
                showVideoBgTrimmerDialog(path, durSec.coerceAtLeast(1f))
            } else {
                Toast.makeText(this, "يرجى تعيين خلفية فيديو أولاً لقص وتعديل المقطع", Toast.LENGTH_SHORT).show()
                showPicsartBackgroundsDialog()
            }
        }
        toolSlowMotion.setOnClickListener {
            closeToolPanel()
            updateToolBarHighlights(7)
            if (selectedVideoBgPath != null) {
                showSlowMotionSpeedDialog { newSpeed ->
                    applyVideoBgSpeed(newSpeed)
                    Toast.makeText(this, "تم تغيير سرعة السلو موشن إلى: ${newSpeed}x", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "يرجى تعيين خلفية فيديو أولاً لتطبيق السلو موشن", Toast.LENGTH_SHORT).show()
                showPicsartBackgroundsDialog()
            }
        }
        toolStickers.setOnClickListener {
            closeToolPanel()
            updateToolBarHighlights(8)
            showPicsartStickersDialog()
        }
        toolSettings.setOnClickListener { openToolPanel(7) }

        setupStickerCategories()

        btnPickCustomSticker.setOnClickListener {
            pickStickerLauncher.launch("image/*")
        }

        btnRemoveSticker.setOnClickListener {
            activeStickers.clear()
            selectedStickerPath = null
            videoMotionView.activeStickerIndex = -1
            btnRemoveSticker.visibility = View.GONE
            updateConfig()
            populateStickerCards()
            Toast.makeText(this, getString(R.string.studio_sticker_remove), Toast.LENGTH_SHORT).show()
        }

        videoMotionView.onStickerPositionChanged = { x, y, scale ->
            selectedStickerX = x
            selectedStickerY = y
            selectedStickerScale = scale
        }

        videoMotionView.onStickersListChanged = { updatedList ->
            activeStickers.clear()
            activeStickers.addAll(updatedList)
            selectedStickerPath = activeStickers.lastOrNull()?.path
            btnRemoveSticker.visibility = if (activeStickers.isNotEmpty()) View.VISIBLE else View.GONE
            updateConfig()
        }

        videoMotionView.onStickerDeleted = {
            activeStickers.clear()
            activeStickers.addAll(videoMotionView.config.stickers)
            selectedStickerPath = activeStickers.lastOrNull()?.path
            btnRemoveSticker.visibility = if (activeStickers.isNotEmpty()) View.VISIBLE else View.GONE
            updateConfig()
            populateStickerCards()
            Toast.makeText(this, getString(R.string.studio_sticker_remove), Toast.LENGTH_SHORT).show()
        }

        videoMotionView.onStickerSelected = { stk, index ->
            // Single tap: select sticker for moving and dragging without interrupting dialog
        }

        videoMotionView.onStickerTimingRequested = { stk, index ->
            // Triggered only by tapping the ⏱️ badge or double-tapping the sticker!
            showStickerTimingDialog(stk)
        }



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



    private fun setupTextSizeControls() {
        fun updateTextSizeUI() {
            tvTextSizeBadge.text = "${(selectedTextSizeScale * 100).toInt()}%"
            updateConfig()
        }

        btnTextSizeMinus.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectedTextSizeScale = (selectedTextSizeScale - 0.10f).coerceIn(0.60f, 2.20f)
            updateTextSizeUI()
        }

        btnTextSizePlus.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectedTextSizeScale = (selectedTextSizeScale + 0.10f).coerceIn(0.60f, 2.20f)
            updateTextSizeUI()
        }

        tvTextSizeBadge.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            selectedTextSizeScale = 1.0f
            updateTextSizeUI()
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
            Triple(toolThemes, ivToolThemes, tvToolThemes),
            Triple(toolAudio, ivToolAudio, tvToolAudio),
            Triple(toolEffects, ivToolEffects, tvToolEffects),
            Triple(toolFilters, ivToolFilters, tvToolFilters),
            Triple(toolText, ivToolText, tvToolText),
            Triple(toolCanvas, ivToolCanvas, tvToolCanvas),
            Triple(toolTrimVideo, ivToolTrimVideo, tvToolTrimVideo),
            Triple(toolSlowMotion, ivToolSlowMotion, tvToolSlowMotion),
            Triple(toolStickers, ivToolStickers, tvToolStickers),
            Triple(toolSettings, ivToolSettings, tvToolSettings)
        )

        for (i in tools.indices) {
            val (container, icon, tv) = tools[i]
            if (i == index) {
                container.background = null
                icon.setBackgroundResource(R.drawable.bg_studio_tool_active)
                icon.setColorFilter(Color.WHITE)
                tv.setTextColor(Color.parseColor("#38BDF8"))
                tv.typeface = Typeface.DEFAULT_BOLD
            } else {
                container.background = null
                icon.background = null
                icon.setColorFilter(Color.parseColor("#94A3B8"))
                tv.setTextColor(Color.parseColor("#94A3B8"))
                tv.typeface = Typeface.DEFAULT
            }
        }
    }



    private fun setupBgCategories() {

        val tabs = listOf(
            tabBgAll to "all",
            tabBgMotion to "motion",
            tabBgVideo to "video",
            tabBgDark to "dark",
            tabBgNature to "nature"
        )

        btnPickCustomVideoBg.setOnClickListener {
            pickVideoBgLauncher.launch("video/*")
        }

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
        val highlightIndex = when (index) {
            0 -> 0
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6 -> 8
            7 -> 9
            else -> index
        }
        updateToolBarHighlights(highlightIndex)

        panelThemesContainer.visibility = if (index == 0) View.VISIBLE else View.GONE
        scrollAudio.visibility = if (index == 1) View.VISIBLE else View.GONE
        panelEffectsContainer.visibility = if (index == 2) View.VISIBLE else View.GONE
        panelFiltersContainer.visibility = if (index == 3) View.VISIBLE else View.GONE
        panelFontContainer.visibility = if (index == 4) View.VISIBLE else View.GONE
        panelCanvasContainer.visibility = if (index == 5) View.VISIBLE else View.GONE
        panelStickersContainer.visibility = if (index == 6) View.VISIBLE else View.GONE
        panelSettingsContainer.visibility = if (index == 7) View.VISIBLE else View.GONE

        if (index == 3) {
            populateFilterCards()
        }
        if (index == 6) {
            populateStickerCards()
        }
        if (index == 7) {
            updateSettingsHighlights()
        }

        tvToolPanelTitle.text = when (index) {
            0 -> getString(R.string.studio_presets_title)
            1 -> getString(R.string.studio_audio)
            2 -> getString(R.string.studio_animations)
            3 -> getString(R.string.studio_luts_title)
            4 -> getString(R.string.studio_texts)
            5 -> getString(R.string.studio_backgrounds)
            6 -> getString(R.string.studio_stickers_title)
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

        val keepMuted = videoMotionView.isMuted || selectedAudio == "none"
        if (keepMuted) {
            selectedAudio = "none"
            videoMotionView.isMuted = true
            ivMuteToggle.setImageResource(R.drawable.ic_volume_off)
            videoMotionView.playAudioImmediately("none")
        } else {
            videoMotionView.isMuted = (theme.audioPreset == "none" || theme.audioPreset == "mute")
            ivMuteToggle.setImageResource(if (videoMotionView.isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up)
            videoMotionView.playAudioImmediately(theme.audioPreset)
        }



        updateConfig()

        populateThemeCards()

        populateAudioPresets()

        populateAnimPresets()

        populateFontPresets()

        populateMotionPresets()

        videoMotionView.restart()
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
        if (isPhoneCustomAudio && customAudioUri != null) {
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
            val subtitle = if (isSelected) "نشط" else null
            val card = createAudioCard(
                title = title,
                iconRes = iconRes,
                isSelected = isSelected,
                subtitle = subtitle,
                onClick = {
                    val audioUri = getAudioUriForBuiltInPreset(id)
                    selectedAudio = id
                    isPhoneCustomAudio = false
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
                    downloadedAudioPath = path
                    selectedAudio = onlineAudio.id
                    isPhoneCustomAudio = false
                    customAudioUri = fileUri
                    customAudioTitle = onlineAudio.title
                    audioStartOffsetMs = 0L
                    videoMotionView.audioStartOffsetMs = 0L
                    videoMotionView.customAudioUri = fileUri
                    videoMotionView.isMuted = false
                    ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                    videoMotionView.playAudioPreset(onlineAudio.id, localPath = path)
                    populateAudioPresets()

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



    private fun updateTextColorSquare() {
        val dp = resources.displayMetrics.density
        vTextColorPreview.background = GradientDrawable().apply {
            cornerRadius = 6 * dp
            setColor(Color.parseColor(selectedTextColor))
            val isLight = selectedTextColor.equals("#FFFFFF", ignoreCase = true) ||
                          selectedTextColor.equals("#F1F5F9", ignoreCase = true) ||
                          selectedTextColor.equals("#FEF3C7", ignoreCase = true) ||
                          selectedTextColor.equals("#FEF08A", ignoreCase = true)
            setStroke((1 * dp).toInt(), Color.parseColor(if (isLight) "#475569" else "#334155"))
        }
    }

    private fun showTextColorPickerDialog() {
        if (isFinishing || isDestroyed) return
        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding((16 * dp).toInt(), (14 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt())
        }

        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * dp).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 2 * dp
            }
        }
        root.addView(handle)

        val headerRow = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * dp).toInt()
            }
        }
        val tvTitle = TextView(this).apply {
            text = "لون النص"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
        headerRow.addView(tvTitle)

        val btnClose = FrameLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams((30 * dp).toInt(), (30 * dp).toInt()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            val ivClose = ImageView(this@VideoStudioActivity).apply {
                setImageResource(R.drawable.ic_close_small)
                setColorFilter(Color.parseColor("#94A3B8"))
                layoutParams = FrameLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt(), Gravity.CENTER)
            }
            addView(ivClose)
            setOnClickListener { dialog.dismiss() }
        }
        headerRow.addView(btnClose)
        root.addView(headerRow)

        val colors = listOf(
            "#FFFFFF" to "أبيض",
            "#F1F5F9" to "أبيض ثلجي",
            "#CBD5E1" to "رمادي فاتح",
            "#94A3B8" to "فضي",
            "#475569" to "رمادي داكن",
            "#0F172A" to "أسود ناعم",

            "#FCD34D" to "ذهبي فاتح",
            "#F59E0B" to "ذهبي ملكي",
            "#D97706" to "كهرمان",
            "#FEF08A" to "أصفر هادئ",
            "#FEF3C7" to "عاجي كلاسيكي",
            "#FDE68A" to "كريمي دافئ",

            "#38BDF8" to "سماوي مضيء",
            "#0EA5E9" to "أزرق ناصع",
            "#0284C7" to "أزرق محيطي",
            "#60A5FA" to "أزرق فاتح",
            "#3B82F6" to "أزرق ملكي",
            "#818CF8" to "نيلي ساحر",

            "#34D399" to "زمردي مشرق",
            "#10B981" to "أخضر طبيعي",
            "#059669" to "أخضر غابي",
            "#A7F3D0" to "نعناعي فاتح",
            "#2DD4BF" to "تركواز هادئ",
            "#14B8A6" to "تركواز مائي",

            "#FDA4AF" to "وردي ناعم",
            "#F43F5E" to "وردي قرمزي",
            "#E11D48" to "ياقوتي فخم",
            "#FB7185" to "زهري مشرق",
            "#C084FC" to "بنفسجي فاتح",
            "#9333EA" to "أرجواني ملكي",
            "#A855F7" to "ليلكي هادئ",
            "#E879F9" to "فوشيا جذاب",

            "#FB923C" to "برتقالي دافئ",
            "#F97316" to "برتقالي ناصع",
            "#EA580C" to "ناري",
            "#FFEDD5" to "خوخي ناعم"
        )

        val gridScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (220 * dp).toInt())
        }
        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        gridScroll.addView(gridContainer)

        val numCols = 6
        val chunks = colors.chunked(numCols)
        for (chunk in chunks) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (10 * dp).toInt()
                }
            }
            for ((hex, _) in chunk) {
                val isSelected = selectedTextColor.equals(hex, ignoreCase = true)
                val swatch = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams((40 * dp).toInt(), (40 * dp).toInt()).apply {
                        setMargins((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(hex))
                        if (isSelected) {
                            setStroke((3 * dp).toInt(), Color.parseColor("#0EA5E9"))
                        } else if (hex.equals("#FFFFFF", ignoreCase = true) || hex.equals("#F1F5F9", ignoreCase = true) || hex.equals("#FEF3C7", ignoreCase = true)) {
                            setStroke((1 * dp).toInt(), Color.parseColor("#475569"))
                        }
                    }
                    if (isSelected) {
                        val ivCheck = ImageView(this@VideoStudioActivity).apply {
                            setImageResource(R.drawable.ic_check_small)
                            val darkIcon = hex.equals("#FFFFFF", ignoreCase = true) || hex.equals("#F1F5F9", ignoreCase = true) || hex.equals("#FEF08A", ignoreCase = true) || hex.equals("#FEF3C7", ignoreCase = true) || hex.equals("#FDE68A", ignoreCase = true) || hex.equals("#A7F3D0", ignoreCase = true) || hex.equals("#FFEDD5", ignoreCase = true)
                            setColorFilter(if (darkIcon) Color.parseColor("#0F172A") else Color.WHITE)
                            layoutParams = FrameLayout.LayoutParams((18 * dp).toInt(), (18 * dp).toInt(), Gravity.CENTER)
                        }
                        addView(ivCheck)
                    }
                    setOnClickListener {
                        it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        selectedTextColor = hex
                        updateTextColorSquare()
                        updateConfig()
                        dialog.dismiss()
                    }
                }
                row.addView(swatch)
            }
            gridContainer.addView(row)
        }

        root.addView(gridScroll)
        dialog.setContentView(root)
        (root.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
        dialog.show()
    }

    private fun populateFontPresets() {
        layoutFontPresets.removeAllViews()
        updateTextColorSquare()

        val dp = resources.displayMetrics.density

        val fontList = listOf(
            "tajawal" to "تجوال",
            "cairo" to "كايرو",
            "arefruqaa" to "الرقعة",
            "amiri" to "أميري",
            "kufam" to "كوفي",
            "almarai" to "المراعي",
            "marhey" to "مرحي",
            "notonastaliqurdu" to "نستعليق"
        )

        for ((id, name) in fontList) {
            val isSelected = selectedFont.equals(id, ignoreCase = true) ||
                (id == "cairo" && selectedFont.equals("cairoplay", ignoreCase = true)) ||
                (id == "kufam" && (selectedFont.equals("kufi", ignoreCase = true) || selectedFont.equals("reem_kufi", ignoreCase = true))) ||
                (id == "arefruqaa" && selectedFont.equals("ruqaa", ignoreCase = true)) ||
                (id == "notonastaliqurdu" && (selectedFont.equals("nastaliq", ignoreCase = true) || selectedFont.equals("amiri", ignoreCase = true))) ||
                (id == "blakahollow" && selectedFont.equals("blaka", ignoreCase = true))

            val tf = ThemeHelper.getFontTypeface(this@VideoStudioActivity, id)

            val card = FrameLayout(this).apply {
                setPadding((14 * dp).toInt(), (6 * dp).toInt(), (14 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (42 * dp).toInt()
                ).apply {
                    marginEnd = (8 * dp).toInt()
                }
                background = GradientDrawable().apply {
                    cornerRadius = 10 * dp
                    if (isSelected) {
                        setColor(Color.parseColor("#0C2A4A"))
                        setStroke((2 * dp).toInt(), Color.parseColor("#0EA5E9"))
                    } else {
                        setColor(Color.parseColor("#1E293B"))
                        setStroke((1 * dp).toInt(), Color.parseColor("#334155"))
                    }
                }

                val tvName = TextView(this@VideoStudioActivity).apply {
                    text = name
                    typeface = tf
                    setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#CBD5E1"))
                    textSize = 14f
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                }
                addView(tvName)

                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
        if (isFinishing || isDestroyed) return

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
                        selectedVideoBgPath = null
                        btnAdjustFraming.visibility = View.GONE
                        setupOrUpdateVideoBackgroundPlayer()
                        updateConfig()
                        populateMotionPresets()
                    }

                }

                layoutMotionPresets.addView(card)

            }

        }




        if (currentBgCategory == "video") {
            btnPickCustomVideoBg.visibility = View.VISIBLE

            for (vbg in StudioAssetManager.onlineVideoBackgrounds) {
                val isDownloaded = StudioAssetManager.isVideoBackgroundDownloaded(this, vbg.id)
                val localPath = StudioAssetManager.getDownloadedVideoBackgroundPath(this, vbg.id)
                val isSelected = selectedVideoBgPath != null && selectedVideoBgPath == localPath

                val card = FrameLayout(this).apply {
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

                // Thumbnail via Glide
                val ivThumb = ImageView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                Glide.with(applicationContext)
                    .load(vbg.thumbUrl)
                    .placeholder(ColorDrawable(Color.parseColor("#1A202C")))
                    .error(ColorDrawable(Color.parseColor("#1A202C")))
                    .centerCrop()
                    .into(ivThumb)
                card.addView(ivThumb)

                // Video indicator badge
                val badge = ImageView(this).apply {
                    setImageResource(R.drawable.ic_fx_glow)
                    setColorFilter(Color.WHITE)
                    val bSize = (12 * dp).toInt()
                    layoutParams = FrameLayout.LayoutParams(bSize, bSize).apply {
                        gravity = Gravity.TOP or Gravity.START
                        topMargin = (4 * dp).toInt()
                        marginStart = (4 * dp).toInt()
                    }
                }
                card.addView(badge)

                // Scrim
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
                card.addView(scrim)

                // Title & status
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
                        text = vbg.title
                        setTextColor(Color.WHITE)
                        textSize = 8f
                        maxLines = 1
                        gravity = Gravity.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    val tvStatus = TextView(this@VideoStudioActivity).apply {
                        text = if (isDownloaded) "جاهز" else "تحميل"
                        setTextColor(Color.parseColor(if (isDownloaded) "#10B981" else "#38BDF8"))
                        textSize = 7.5f
                        gravity = Gravity.CENTER
                    }

                    addView(tvTitle)
                    addView(tvStatus)
                }
                card.addView(infoLayout)

                // Progress overlay
                val isDownloading = downloadingVideoBgProgressMap.containsKey(vbg.id)
                val progressOverlay = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    visibility = if (isDownloading) View.VISIBLE else View.GONE
                    setBackgroundColor(Color.parseColor("#D9000000"))
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                    val pb = ProgressBar(this@VideoStudioActivity, null, android.R.attr.progressBarStyleSmall)
                    val tvPercent = TextView(this@VideoStudioActivity).apply {
                        tag = "vbg_prog_${vbg.id}"
                        text = "${downloadingVideoBgProgressMap[vbg.id] ?: 0}%"
                        setTextColor(Color.WHITE)
                        textSize = 9f
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(0, (2 * dp).toInt(), 0, 0)
                    }
                    addView(pb)
                    addView(tvPercent)
                }
                card.addView(progressOverlay)

                card.setOnClickListener {
                    if (downloadingVideoBgProgressMap.containsKey(vbg.id)) return@setOnClickListener

                    if (StudioAssetManager.isVideoBackgroundDownloaded(this, vbg.id)) {
                        val path = StudioAssetManager.getDownloadedVideoBackgroundPath(this, vbg.id)
                        selectedVideoBgPath = path
                        customImageBitmap = null
                        selectedMotion = "VIDEO_BG"
                        btnAdjustFraming.visibility = View.GONE
                        setupOrUpdateVideoBackgroundPlayer()
                        updateConfig()
                        populateMotionPresets()
                    } else {
                        downloadingVideoBgProgressMap[vbg.id] = 0
                        populateMotionPresets()
                        StudioAssetManager.downloadVideoBackground(
                            context = this,
                            vbg = vbg,
                            onProgress = { p ->
                                downloadingVideoBgProgressMap[vbg.id] = p
                                layoutMotionPresets.findViewWithTag<TextView>("vbg_prog_${vbg.id}")?.text = "$p%"
                            },
                            onSuccess = { savedPath ->
                                if (isFinishing || isDestroyed) return@downloadVideoBackground
                                downloadingVideoBgProgressMap.remove(vbg.id)
                                selectedVideoBgPath = savedPath
                                customImageBitmap = null
                                selectedMotion = "VIDEO_BG"
                                btnAdjustFraming.visibility = View.GONE
                                setupOrUpdateVideoBackgroundPlayer()
                                updateConfig()
                                populateMotionPresets()
                                Toast.makeText(this, "تم تحميل فيديو الخلفية بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                if (isFinishing || isDestroyed) return@downloadVideoBackground
                                downloadingVideoBgProgressMap.remove(vbg.id)
                                populateMotionPresets()
                                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                layoutMotionPresets.addView(card)
            }
        } else {
            btnPickCustomVideoBg.visibility = View.GONE
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

            Glide.with(applicationContext)

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
                        selectedVideoBgPath = null
                        btnAdjustFraming.visibility = View.VISIBLE
                        setupOrUpdateVideoBackgroundPlayer()
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
    // 5.5. Stickers & Animated GIFs Handling
    // ─────────────────────────────────────────────────────────────

    private fun setupStickerCategories() {
        val tabs = listOf(
            tabStickerAll to "all",
            tabStickerIslamic to "islamic",
            tabStickerFeelings to "feelings",
            tabStickerMagic to "magic",
            tabStickerReactions to "reactions"
        )

        for ((tab, cat) in tabs) {
            tab.setOnClickListener {
                currentStickerCategory = cat
                for ((otherTab, otherCat) in tabs) {
                    if (otherCat == cat) {
                        otherTab.setBackgroundResource(R.drawable.bg_segment_btn_active)
                        otherTab.setTextColor(Color.WHITE)
                        otherTab.setTypeface(null, Typeface.BOLD)
                    } else {
                        otherTab.background = null
                        otherTab.setTextColor(Color.parseColor("#94A3B8"))
                        otherTab.setTypeface(null, Typeface.NORMAL)
                    }
                }
                populateStickerCards()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Freeform Image Crop Dialog (قص حر للصورة)
    // ─────────────────────────────────────────────────────────────
    private fun showFreeCropDialog(sourceBitmap: Bitmap, title: String = "قص وتعديل الصورة", onCropped: (Bitmap) -> Unit) {
        if (isFinishing || isDestroyed) return
        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (18 * dp).toInt())
        }

        // Drag handle
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#475569"))
                cornerRadius = 3 * dp
            }
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (10 * dp).toInt()
            }
        }
        root.addView(handle)

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (6 * dp).toInt()
            }
        }
        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((28 * dp).toInt(), (28 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(tvTitle)
        header.addView(btnClose)
        root.addView(header)

        val tvSub = TextView(this).apply {
            text = "اسحب الحواف أو الزوايا لقص أي جزء تريده من الصورة بحرية تامة"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }
        root.addView(tvSub)

        // FreeCropView container
        val cropView = FreeCropView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (340 * dp).toInt())
            setSourceBitmap(sourceBitmap)
        }
        root.addView(cropView)

        // Toolbar: Rotate 90, Reset, Skip
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * dp).toInt()
                bottomMargin = (12 * dp).toInt()
            }
        }
        fun createToolBtn(label: String, onClick: () -> Unit): TextView {
            return TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 11f
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 8 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#334155"))
                }
                setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (6 * dp).toInt()
                }
                gravity = Gravity.CENTER
                setOnClickListener { onClick() }
            }
        }
        val btnRotate = createToolBtn("⤾ تدوير 90°") { cropView.rotate90() }
        val btnReset = createToolBtn("↺ إعادة ضبط") { cropView.resetCrop() }
        val btnSkip = createToolBtn("تخطي القص") {
            dialog.dismiss()
            onCropped(sourceBitmap)
        }
        toolbar.addView(btnRotate)
        toolbar.addView(btnReset)
        toolbar.addView(btnSkip)
        root.addView(toolbar)

        // Confirm Button
        val btnConfirm = TextView(this).apply {
            text = "تأكيد وقص الصورة"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0EA5E9"))
                cornerRadius = 10 * dp
            }
            setPadding(0, (11 * dp).toInt(), 0, (11 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val cropped = cropView.getCroppedBitmap() ?: sourceBitmap
                dialog.dismiss()
                onCropped(cropped)
            }
        }
        root.addView(btnConfirm)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun handleCustomStickerSelected(uri: Uri) {
        try {
            val mime = contentResolver.getType(uri) ?: ""
            val isGif = mime.contains("gif", ignoreCase = true)
            val dir = File(filesDir, "studio_stickers").apply { if (!exists()) mkdirs() }

            if (isGif) {
                // Animated GIF: save directly without static crop
                val targetFile = File(dir, "custom_${System.currentTimeMillis()}.gif")
                contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (targetFile.exists() && targetFile.length() > 0) {
                    addCustomStickerFileToStudio(targetFile)
                }
            } else {
                // Static Image: Open Freeform Image Cropper!
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    showFreeCropDialog(originalBitmap, "قص الملصق") { croppedBitmap ->
                        val targetFile = File(dir, "custom_${System.currentTimeMillis()}.png")
                        targetFile.outputStream().use { out ->
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        addCustomStickerFileToStudio(targetFile)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "تعذر قراءة الصورة", Toast.LENGTH_SHORT).show()
        }
    }


    private fun addCustomStickerFileToStudio(targetFile: File) {
        val newStk = VideoMotionRenderer.StickerItem(
            path = targetFile.absolutePath,
            x = 0.5f,
            y = (0.35f + (activeStickers.size * 0.08f) % 0.40f),
            scale = 1.0f,
            startSec = 0f,
            endSec = selectedDuration.toFloat()
        )
        activeStickers.add(newStk)
        videoMotionView.activeStickerIndex = activeStickers.lastIndex
        selectedStickerPath = targetFile.absolutePath
        btnRemoveSticker.visibility = View.VISIBLE
        updateConfig()
        populateStickerCards()
        Toast.makeText(this, "تمت إضافة الملصق بنجاح", Toast.LENGTH_SHORT).show()
        showStickerTimingDialog(newStk)
    }

    // ─────────────────────────────────────────────────────────────
    // Sticker Timing & Duration Control Dialog
    // ─────────────────────────────────────────────────────────────
    private fun showStickerTimingDialog(sticker: VideoMotionRenderer.StickerItem) {
        if (isFinishing || isDestroyed) return
        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding((18 * dp).toInt(), (14 * dp).toInt(), (18 * dp).toInt(), (20 * dp).toInt())
        }

        // Drag handle
        val handle = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#475569"))
                cornerRadius = 3 * dp
            }
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * dp).toInt()
            }
        }
        root.addView(handle)

        // Title Row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }
        val tvTitle = TextView(this).apply {
            text = "⏱️ توقيت ظهور الملصق في الفيديو"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((28 * dp).toInt(), (28 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(tvTitle)
        header.addView(btnClose)
        root.addView(header)

        var curStart = sticker.startSec
        var curEnd = sticker.endSec.coerceAtMost(selectedDuration.toFloat())

        // Subtitle badge showing duration
        val tvSummary = TextView(this).apply {
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1538BDF8"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#3338BDF8"))
            }
            setPadding((10 * dp).toInt(), (6 * dp).toInt(), (10 * dp).toInt(), (6 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * dp).toInt()
            }
        }
        fun updateSummary() {
            val dur = String.format("%.1f", (curEnd - curStart).coerceAtLeast(0.1f))
            tvSummary.text = "يظهر من: ${String.format("%.1f", curStart)} ث  إلى  ${String.format("%.1f", curEnd)} ث  (المدة: $dur ث)"
        }
        updateSummary()
        root.addView(tvSummary)

        // Presets Chips
        val tvPresetHeader = TextView(this).apply {
            text = "خيارات سريعة:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * dp).toInt()
            }
        }
        root.addView(tvPresetHeader)

        val presetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * dp).toInt()
            }
        }

        lateinit var timelineBar: RangeTimelineBar

        val midStart = (selectedDuration / 3f).coerceAtMost(selectedDuration.toFloat() - 1f)
        val midEnd = (selectedDuration * 2f / 3f).coerceAtMost(selectedDuration.toFloat())
        val presets = listOf(
            "كامل الفيديو" to (0f to selectedDuration.toFloat()),
            "أول 5 ثواني" to (0f to 5f.coerceAtMost(selectedDuration.toFloat())),
            "الوسط" to (midStart to midEnd),
            "آخر 5 ثواني" to (maxOf(0f, selectedDuration - 5f) to selectedDuration.toFloat())
        )

        for ((label, range) in presets) {
            val chip = TextView(this).apply {
                text = label
                textSize = 10f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 14 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#334155"))
                }
                setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (4 * dp).toInt()
                }
                gravity = Gravity.CENTER
                setOnClickListener {
                    curStart = range.first
                    curEnd = range.second
                    timelineBar.startSec = curStart
                    timelineBar.endSec = curEnd
                    updateSummary()
                    sticker.startSec = curStart
                    sticker.endSec = curEnd
                    updateConfig()
                    videoMotionView.seekTo((curStart / selectedDuration.toFloat()).coerceIn(0f, 1f))
                    Toast.makeText(this@VideoStudioActivity, "تم ضبط توقيت الملصق: $label", Toast.LENGTH_SHORT).show()
                }
            }
            presetRow.addView(chip)
        }
        root.addView(presetRow)

        // Custom Dual-Handle Horizontal Timeline Bar (Audio Trimmer Style)
        timelineBar = RangeTimelineBar(this).apply {
            totalDurationSec = selectedDuration.toFloat()
            startSec = curStart
            endSec = curEnd
            onRangeChanged = { s, e ->
                curStart = s
                curEnd = e
                updateSummary()
                sticker.startSec = curStart
                sticker.endSec = curEnd
                updateConfig()
                videoMotionView.seekTo((curStart / selectedDuration.toFloat()).coerceIn(0f, 1f))
            }
        }
        root.addView(timelineBar)

        // Fine-tuning Steppers Row
        val fineTuneCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141D2E"))
                cornerRadius = 12 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))
            }
            setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (6 * dp).toInt()
                bottomMargin = (16 * dp).toInt()
            }
        }

        fun createStepper(title: String, onMinus: () -> Unit, onPlus: () -> Unit): View {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val lbl = TextView(this).apply {
                text = title
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 10f
            }
            col.addView(lbl)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (4 * dp).toInt()
                }
            }
            val btnMinus = TextView(this).apply {
                text = "− 0.5ث"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 6 * dp
                }
                setPadding((8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
                setOnClickListener { onMinus() }
            }
            val btnPlus = TextView(this).apply {
                text = "+ 0.5ث"
                textSize = 11f
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 6 * dp
                }
                setPadding((8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = (6 * dp).toInt()
                }
                setOnClickListener { onPlus() }
            }
            row.addView(btnMinus)
            row.addView(btnPlus)
            col.addView(row)
            return col
        }

        val stepStart = createStepper(
            title = "ضبط البداية الدقيق",
            onMinus = {
                curStart = (curStart - 0.5f).coerceAtLeast(0f)
                timelineBar.startSec = curStart
                sticker.startSec = curStart
                updateSummary()
                updateConfig()
                videoMotionView.seekTo((curStart / selectedDuration.toFloat()).coerceIn(0f, 1f))
            },
            onPlus = {
                if (curStart + 0.5f < curEnd) {
                    curStart += 0.5f
                    timelineBar.startSec = curStart
                    sticker.startSec = curStart
                    updateSummary()
                    updateConfig()
                    videoMotionView.seekTo((curStart / selectedDuration.toFloat()).coerceIn(0f, 1f))
                }
            }
        )

        val div = View(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams((1 * dp).toInt(), (32 * dp).toInt())
        }

        val stepEnd = createStepper(
            title = "ضبط النهاية الدقيق",
            onMinus = {
                if (curEnd - 0.5f > curStart) {
                    curEnd -= 0.5f
                    timelineBar.endSec = curEnd
                    sticker.endSec = curEnd
                    updateSummary()
                    updateConfig()
                }
            },
            onPlus = {
                curEnd = (curEnd + 0.5f).coerceAtMost(selectedDuration.toFloat())
                timelineBar.endSec = curEnd
                sticker.endSec = curEnd
                updateSummary()
                updateConfig()
            }
        )

        fineTuneCard.addView(stepStart)
        fineTuneCard.addView(div)
        fineTuneCard.addView(stepEnd)
        root.addView(fineTuneCard)

        // Apply Button
        val btnApply = TextView(this).apply {
            text = "تطبيق وحفظ التوقيت"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0EA5E9"))
                cornerRadius = 10 * dp
            }
            setPadding(0, (11 * dp).toInt(), 0, (11 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                sticker.startSec = curStart
                sticker.endSec = curEnd
                updateConfig()
                dialog.dismiss()
                Toast.makeText(this@VideoStudioActivity, "تم حفظ توقيت ظهور الملصق بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(btnApply)

        dialog.setContentView(root)
        dialog.show()
    }

    // ─────────────────────────────────────────────────────────────
    // Video Background Trimmer Dialog (قص مقطع الفيديو بالثواني مع تايم لاين قابل للتمرير وقص حقيقي خفيف)
    // ─────────────────────────────────────────────────────────────
    private fun showVideoBgTrimmerDialog(filePath: String, totalDurationSec: Float) {
        if (isFinishing || isDestroyed) return
        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0B0F19"))
                cornerRadii = floatArrayOf(
                    24f * dp, 24f * dp,
                    24f * dp, 24f * dp,
                    0f, 0f, 0f, 0f
                )
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt())
        }

        // 1. Drag handle
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * dp).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 2 * dp
            }
        }
        root.addView(handle)

        // 2. Header: Title and Close (X)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }
        val tvTitle = TextView(this).apply {
            text = "قص وتحديد مقطع الفيديو"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(tvTitle)
        header.addView(btnClose)
        root.addView(header)

        val projectDurSec = selectedDuration.toFloat().coerceAtLeast(1f)
        var curStart = if (selectedVideoBgPath == filePath && selectedVideoBgStartSec >= 0f) selectedVideoBgStartSec else 0f
        var curEnd = if (selectedVideoBgPath == filePath && selectedVideoBgEndSec > 0f) {
            minOf(selectedVideoBgEndSec, curStart + projectDurSec)
        } else {
            minOf(totalDurationSec, curStart + projectDurSec)
        }

        fun formatTimeDisplay(sec: Float): String {
            val totalS = sec.toInt()
            val h = totalS / 3600
            val m = (totalS % 3600) / 60
            val s = totalS % 60
            val frac = ((sec - totalS) * 10).toInt()
            return when {
                h > 0 -> String.format(java.util.Locale.US, "%02d:%02d:%02d.%d", h, m, s, frac)
                m > 0 -> String.format(java.util.Locale.US, "%02d:%02d.%d", m, s, frac)
                else -> String.format(java.util.Locale.US, "%.1f ث", sec)
            }
        }

        // 3. Compact Summary Badge
        val summaryCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141A26"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))
            }
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (10 * dp).toInt()
            }
        }
        val tvSummary = TextView(this).apply {
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        summaryCard.addView(tvSummary)
        root.addView(summaryCard)

        fun updateSummary() {
            val dur = formatTimeDisplay((curEnd - curStart).coerceAtLeast(0.1f))
            val sStr = formatTimeDisplay(curStart)
            val eStr = formatTimeDisplay(curEnd)
            tvSummary.text = "$sStr - $eStr ($dur)"
        }
        updateSummary()

        lateinit var timelineBar: RangeTimelineBar
        lateinit var scrollTimeline: HorizontalScrollView
        var isUpdatingFromSeek = false
        var seekJump: SeekBar? = null
        var tvJumpTime: TextView? = null

        // 4. Quick Jump Bar for videos longer than 60 seconds (شريط القفز السريع للفيديوهات الطويلة)
        if (totalDurationSec > 60f) {
            val layoutQuickJump = LinearLayout(this).apply {
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#141A26"))
                    cornerRadius = 8 * dp
                }
                setPadding((10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * dp).toInt()
                }
            }

            val tvJumpLabel = TextView(this).apply {
                text = "قفز سريع:"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            layoutQuickJump.addView(tvJumpLabel)

            val sBar = SeekBar(this).apply {
                val initDur = (curEnd - curStart).coerceIn(1f, projectDurSec)
                val maxStart = (totalDurationSec - initDur).coerceAtLeast(0.1f)
                max = (maxStart * 10).toInt().coerceAtLeast(1)
                progress = (curStart * 10).toInt()
                progressDrawable = ContextCompat.getDrawable(this@VideoStudioActivity, R.drawable.bg_video_seekbar)
                thumb = ContextCompat.getDrawable(this@VideoStudioActivity, R.drawable.bg_video_seek_thumb)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)
                }
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, prog: Int, fromUser: Boolean) {
                        if (fromUser) {
                            val rangeDur = (curEnd - curStart).coerceIn(1f, projectDurSec)
                            val maxAllowedStart = (totalDurationSec - rangeDur).coerceAtLeast(0f)
                            curStart = (prog / 10f).coerceIn(0f, maxAllowedStart)
                            curEnd = (curStart + rangeDur).coerceAtMost(totalDurationSec)
                            isUpdatingFromSeek = true
                            timelineBar.setRange(curStart, curEnd)
                            updateSummary()
                            tvJumpTime?.text = formatTimeDisplay(curStart)
                            // Scroll timeline to match
                            scrollTimeline.post {
                                val targetX = timelineBar.secToX(curStart)
                                val screenW = resources.displayMetrics.widthPixels
                                val targetScroll = (targetX - (screenW / 2f)).toInt().coerceAtLeast(0)
                                scrollTimeline.smoothScrollTo(targetScroll, 0)
                            }
                            isUpdatingFromSeek = false
                        }
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) {
                        try { bgVideoMediaPlayer?.seekTo((curStart * 1000).toInt()) } catch (_: Exception) {}
                    }
                })
            }
            seekJump = sBar
            layoutQuickJump.addView(sBar)

            val tvJT = TextView(this).apply {
                text = formatTimeDisplay(curStart)
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            tvJumpTime = tvJT
            layoutQuickJump.addView(tvJT)

            root.addView(layoutQuickJump)
        }

        // 5. Horizontally Scrollable Timeline with Dynamic Width
        val timelineWidthDp = when {
            totalDurationSec > 3600f -> 3000f // 1h+ videos: 3000dp wide timeline for super precision
            totalDurationSec > 1800f -> 2200f // 30m - 1h
            totalDurationSec > 600f -> 1500f  // 10m - 30m
            totalDurationSec > 180f -> 1000f  // 3m - 10m
            totalDurationSec > 45f -> 650f    // 45s - 3m
            else -> 420f                      // Under 45s
        }

        scrollTimeline = HorizontalScrollView(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            isHorizontalScrollBarEnabled = true
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * dp).toInt()
            }
        }

        timelineBar = RangeTimelineBar(this).apply {
            this.totalDurationSec = totalDurationSec
            this.maxRangeSec = projectDurSec
            this.startSec = curStart
            this.endSec = curEnd
            this.customWidthDp = timelineWidthDp
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            onRangeChanged = { s, e ->
                curStart = s
                curEnd = e
                updateSummary()
                if (!isUpdatingFromSeek) {
                    val dur = (curEnd - curStart).coerceIn(1f, projectDurSec)
                    val maxSt = (totalDurationSec - dur).coerceAtLeast(0.1f)
                    seekJump?.max = (maxSt * 10).toInt().coerceAtLeast(1)
                    seekJump?.progress = (s * 10).toInt()
                    tvJumpTime?.text = formatTimeDisplay(s)
                }
            }
            onRangeChangeFinished = { s, e ->
                curStart = s
                curEnd = e
                updateSummary()
                try { bgVideoMediaPlayer?.seekTo((s * 1000).toInt()) } catch (_: Exception) {}
            }
        }
        scrollTimeline.addView(timelineBar)
        root.addView(scrollTimeline)

        // Center timeline around current selection initially
        scrollTimeline.post {
            val midSec = curStart + ((curEnd - curStart) / 2f)
            val targetX = timelineBar.secToX(midSec)
            val screenW = resources.displayMetrics.widthPixels
            val targetScroll = (targetX - (screenW / 2f)).toInt().coerceAtLeast(0)
            scrollTimeline.scrollTo(targetScroll, 0)
        }

        // 6. Fine-Tuning & Presets Row
        val fineTuneRow = LinearLayout(this).apply {
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        fun centerOnCurrentRange() {
            scrollTimeline.post {
                val midSec = curStart + ((curEnd - curStart) / 2f)
                val targetX = timelineBar.secToX(midSec)
                val screenW = resources.displayMetrics.widthPixels
                val targetScroll = (targetX - (screenW / 2f)).toInt().coerceAtLeast(0)
                scrollTimeline.smoothScrollTo(targetScroll, 0)
            }
            val dur = (curEnd - curStart).coerceIn(1f, projectDurSec)
            val maxSt = (totalDurationSec - dur).coerceAtLeast(0.1f)
            seekJump?.max = (maxSt * 10).toInt().coerceAtLeast(1)
            seekJump?.progress = (curStart * 10).toInt()
            tvJumpTime?.text = formatTimeDisplay(curStart)
            try { bgVideoMediaPlayer?.seekTo((curStart * 1000).toInt()) } catch (_: Exception) {}
        }

        fun shiftRangeBy(deltaSec: Float) {
            val dur = (curEnd - curStart).coerceIn(1f, projectDurSec)
            var newStart = curStart + deltaSec
            var newEnd = newStart + dur
            if (newStart < 0f) {
                newStart = 0f
                newEnd = dur
            }
            if (newEnd > totalDurationSec) {
                newEnd = totalDurationSec
                newStart = (totalDurationSec - dur).coerceAtLeast(0f)
            }
            curStart = Math.round(newStart * 10f) / 10f
            curEnd = Math.round(newEnd * 10f) / 10f
            timelineBar.setRange(curStart, curEnd)
            updateSummary()
            centerOnCurrentRange()
        }

        val stepButtons = listOf(
            "-5ث" to -5f,
            "-1ث" to -1f,
            "+1ث" to 1f,
            "+5ث" to 5f
        )

        for ((label, delta) in stepButtons) {
            val btnStep = TextView(this).apply {
                text = label
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 8 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#334155"))
                }
                setPadding((6 * dp).toInt(), (5 * dp).toInt(), (6 * dp).toInt(), (5 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((2 * dp).toInt(), 0, (2 * dp).toInt(), 0)
                }
                gravity = Gravity.CENTER
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    shiftRangeBy(delta)
                }
            }
            fineTuneRow.addView(btnStep)
        }

        // Preset Chips (البداية / الوسط / النهاية)
        val currentDur = (curEnd - curStart).coerceIn(1f, projectDurSec)
        val midStart = ((totalDurationSec - currentDur) / 2f).coerceAtLeast(0f)
        val midEnd = (midStart + currentDur).coerceAtMost(totalDurationSec)

        val presets = mutableListOf<Pair<String, Pair<Float, Float>>>()
        presets.add("البداية" to (0f to currentDur))
        if (totalDurationSec > currentDur) {
            presets.add("الوسط" to (midStart to midEnd))
            presets.add("النهاية" to ((totalDurationSec - currentDur).coerceAtLeast(0f) to totalDurationSec))
        }

        for ((label, range) in presets) {
            val chip = TextView(this).apply {
                text = label
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#141A26"))
                    cornerRadius = 8 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#0284C7"))
                }
                setPadding((6 * dp).toInt(), (5 * dp).toInt(), (6 * dp).toInt(), (5 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins((2 * dp).toInt(), 0, (2 * dp).toInt(), 0)
                }
                gravity = Gravity.CENTER
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    val activeDur = (curEnd - curStart).coerceIn(1f, projectDurSec)
                    val (pStart, pEnd) = when (label) {
                        "البداية" -> 0f to activeDur
                        "الوسط" -> {
                            val ms = ((totalDurationSec - activeDur) / 2f).coerceAtLeast(0f)
                            ms to (ms + activeDur)
                        }
                        "النهاية" -> {
                            val es = (totalDurationSec - activeDur).coerceAtLeast(0f)
                            es to totalDurationSec
                        }
                        else -> range
                    }
                    curStart = Math.round(pStart * 10f) / 10f
                    curEnd = Math.round(pEnd * 10f) / 10f
                    timelineBar.setRange(curStart, curEnd)
                    updateSummary()
                    centerOnCurrentRange()
                }
            }
            fineTuneRow.addView(chip)
        }
        root.addView(fineTuneRow)

        // 7. Apply Button with Lossless Trimming & Zero-Bloat Auto-Clean
        val btnApply = TextView(this).apply {
            text = "تطبيق وحفظ المقطع"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0EA5E9"))
                cornerRadius = 10 * dp
            }
            setPadding(0, (11 * dp).toInt(), 0, (11 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                it.isEnabled = false
                try {
                    bgVideoMediaPlayer?.stop()
                    bgVideoMediaPlayer?.release()
                    bgVideoMediaPlayer = null
                } catch (_: Exception) {}
                val isTrimmed = (curEnd - curStart) < (totalDurationSec - 0.5f)
                if (isTrimmed) {
                    val progress = android.app.ProgressDialog(this@VideoStudioActivity).apply {
                        setMessage("جاري قص المقطع واستخراج اللقطة المحددة...")
                        setCancelable(false)
                        show()
                    }
                    Thread {
                        val cacheDir = File(cacheDir, "studio_custom_vbg").apply { mkdirs() }
                        val trimmedFile = File(cacheDir, "vbg_trimmed_${System.currentTimeMillis()}.mp4")
                        val ok = VideoTrimmerHelper.trimVideo(filePath, trimmedFile.absolutePath, curStart, curEnd)
                        runOnUiThread {
                            try { progress.dismiss() } catch (_: Exception) {}
                            if (ok && trimmedFile.exists() && trimmedFile.length() > 0) {
                                // Delete the huge raw file to free memory and prevent bloat!
                                try { File(filePath).delete() } catch (_: Exception) {}
                                selectedVideoBgPath = trimmedFile.absolutePath
                                selectedVideoBgStartSec = 0f
                                selectedVideoBgEndSec = curEnd - curStart
                                VideoTrimmerHelper.cleanOldCache(this@VideoStudioActivity, trimmedFile.absolutePath)
                            } else {
                                // Fallback soft crop
                                selectedVideoBgPath = filePath
                                selectedVideoBgStartSec = curStart
                                selectedVideoBgEndSec = curEnd
                            }
                            selectedMotion = "VIDEO_BG"
                            customImageBitmap = null
                            btnAdjustFraming.visibility = View.GONE
                            setupOrUpdateVideoBackgroundPlayer()
                            updateConfig()
                            populateMotionPresets()
                            dialog.dismiss()
                        }
                    }.start()
                } else {
                    selectedVideoBgPath = filePath
                    selectedVideoBgStartSec = curStart
                    selectedVideoBgEndSec = curEnd
                    selectedMotion = "VIDEO_BG"
                    customImageBitmap = null
                    btnAdjustFraming.visibility = View.GONE
                    setupOrUpdateVideoBackgroundPlayer()
                    updateConfig()
                    populateMotionPresets()
                    dialog.dismiss()
                }
            }
        }
        root.addView(btnApply)

        // Clean up temporary huge video file if dismissed without applying!
        dialog.setOnDismissListener {
            if (selectedVideoBgPath != filePath) {
                try { File(filePath).delete() } catch (_: Exception) {}
            }
        }

        dialog.setContentView(root)
        dialog.show()
    }

    private fun applyVideoBgSpeed(speed: Float) {
        selectedVideoBgSpeed = speed.coerceIn(0.1f, 5.0f)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bgVideoMediaPlayer?.let { mp ->
                    val params = mp.playbackParams
                    params.speed = selectedVideoBgSpeed
                    mp.playbackParams = params
                }
            }
        } catch (_: Exception) {}
        updateConfig()
    }

    private fun showSlowMotionSpeedDialog(onSpeedSelected: ((Float) -> Unit)? = null) {
        if (isFinishing || isDestroyed) return
        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0B0F19"))
                cornerRadii = floatArrayOf(
                    24f * dp, 24f * dp,
                    24f * dp, 24f * dp,
                    0f, 0f, 0f, 0f
                )
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt())
        }

        // Drag handle
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * dp).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 2 * dp
            }
        }
        root.addView(handle)

        // Header: Title and Close button
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (16 * dp).toInt()
            }
        }
        val tvTitle = TextView(this).apply {
            text = "سرعة الفيديو"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(tvTitle)
        header.addView(btnClose)
        root.addView(header)

        // Big Speed Display Badge (Numeric only, no text)
        var curSpeed = selectedVideoBgSpeed
        val tvSpeedBadge = TextView(this).apply {
            val formatted = if (curSpeed % 1f == 0f) "${curSpeed.toInt()}x" else "${curSpeed}x"
            text = formatted
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * dp).toInt()
            }
        }
        root.addView(tvSpeedBadge)

        fun updateBadgeText(sp: Float) {
            val formatted = if (sp % 1f == 0f) "${sp.toInt()}x" else "${sp}x"
            tvSpeedBadge.text = formatted
        }

        // Progress Bar with Ticks ("xrati") from 0.1 to 5.0
        val rulerBar = SpeedRulerBar(this).apply {
            speed = curSpeed
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (64 * dp).toInt()).apply {
                bottomMargin = (16 * dp).toInt()
            }
            onSpeedChanged = { newSpeed ->
                curSpeed = newSpeed
                updateBadgeText(newSpeed)
                applyVideoBgSpeed(newSpeed)
            }
        }
        root.addView(rulerBar)

        // Apply / Confirm button
        val btnConfirm = TextView(this).apply {
            text = "حفظ السرعة"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0EA5E9"))
                cornerRadius = 10 * dp
            }
            setPadding(0, (11 * dp).toInt(), 0, (11 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                applyVideoBgSpeed(curSpeed)
                onSpeedSelected?.invoke(curSpeed)
                dialog.dismiss()
            }
        }
        root.addView(btnConfirm)

        dialog.setContentView(root)
        dialog.show()
    }

    // ─────────────────────────────────────────────────────────────
    // Studio Audio BottomSheet Tray (PicsArt / CapCut Style)
    // ─────────────────────────────────────────────────────────────
    private fun getCurrentAudioUri(): Uri? {
        if (customAudioUri != null) return customAudioUri
        if (downloadedAudioPath != null && File(downloadedAudioPath!!).exists()) {
            return Uri.fromFile(File(downloadedAudioPath!!))
        }
        if (selectedAudio.isNotBlank() && selectedAudio != "none") {
            val onlinePath = StudioAssetManager.getDownloadedAudioPath(this, selectedAudio)
            if (onlinePath != null && File(onlinePath).exists()) {
                return Uri.fromFile(File(onlinePath))
            }
            return getAudioUriForBuiltInPreset(selectedAudio)
        }
        return null
    }

    private fun getAudioTitleForDisplay(): String {
        if (videoMotionView.isMuted || selectedAudio == "none") return "صامت"
        if (customAudioTitle.isNotBlank()) return customAudioTitle
        return when (selectedAudio) {
            "piano" -> getString(R.string.audio_piano)
            "oud" -> getString(R.string.audio_oud)
            "nature" -> getString(R.string.audio_nature)
            else -> {
                val online = StudioAssetManager.onlineAudios.firstOrNull { it.id == selectedAudio }
                online?.title ?: "صوت نشط"
            }
        }
    }

    private fun showStudioAudioDialog() {
        if (isFinishing || isDestroyed) return

        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0B0F19"))
                cornerRadii = floatArrayOf(
                    24f * dp, 24f * dp,
                    24f * dp, 24f * dp,
                    0f, 0f, 0f, 0f
                )
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        // 1. Drag Handle
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (10 * dp).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 2 * dp
            }
        }
        root.addView(handle)

        // 2. Header Row: Title, Active status badge, Close button
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }

        val tvTitle = TextView(this).apply {
            text = getString(R.string.studio_audio_effects_title)
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvTitle)

        val activeBadge = TextView(this).apply {
            text = getAudioTitleForDisplay()
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = (130 * dp).toInt()
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#38BDF8"))
            }
            setPadding((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (8 * dp).toInt()
            }
        }
        header.addView(activeBadge)

        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(btnClose)
        root.addView(header)

        // 3. Action Row: [+] [Icon Volume] [Icon Scissors + تعديل المقطع]
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        // Plus icon button (+)
        val btnPickPhoneAudio = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#38BDF8"))
            }
            layoutParams = LinearLayout.LayoutParams((42 * dp).toInt(), (36 * dp).toInt()).apply {
                marginEnd = (8 * dp).toInt()
            }
            val ivPlus = ImageView(this@VideoStudioActivity).apply {
                setImageResource(R.drawable.ic_plus)
                setColorFilter(Color.parseColor("#38BDF8"))
                val sz = (18 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(sz, sz)
            }
            addView(ivPlus)
            setOnClickListener {
                dialog.dismiss()
                pickAudioLauncher.launch("audio/*")
            }
        }
        actionRow.addView(btnPickPhoneAudio)

        // Trim button with vector scissors icon (NO EMOJI!)
        val btnTrimAudio = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#0EA5E9"))
            }
            setPadding((12 * dp).toInt(), (7 * dp).toInt(), (12 * dp).toInt(), (7 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(0, (36 * dp).toInt(), 1f)
            val ivScissors = ImageView(this@VideoStudioActivity).apply {
                setImageResource(R.drawable.ic_scissors)
                setColorFilter(Color.parseColor("#38BDF8"))
                val sz = (16 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                    marginEnd = (6 * dp).toInt()
                }
            }
            val tv = TextView(this@VideoStudioActivity).apply {
                text = getString(R.string.studio_trim_audio_btn)
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(ivScissors)
            addView(tv)
            setOnClickListener {
                val currentUri = getCurrentAudioUri()
                if (currentUri != null && selectedAudio != "none" && !videoMotionView.isMuted) {
                    dialog.dismiss()
                    showAudioTrimmerDialog(currentUri)
                } else {
                    Toast.makeText(this@VideoStudioActivity, "اختر صوتاً أولاً لتعديل مقطعه ومدته", Toast.LENGTH_SHORT).show()
                }
            }
        }
        actionRow.addView(btnTrimAudio)
        root.addView(actionRow)

        // 4. Category Tabs
        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categories = listOf(
            "all" to getString(R.string.studio_audio_cat_all),
            "موسيقى" to getString(R.string.studio_audio_cat_music),
            "طبيعة" to getString(R.string.studio_audio_cat_nature),
            "مطر" to getString(R.string.studio_audio_cat_rain),
            "هدوء" to getString(R.string.studio_audio_cat_calm),
            "custom" to getString(R.string.studio_audio_cat_phone)
        )

        var currentSelectedCat = "all"
        val categoryTabs = mutableListOf<TextView>()

        // 5. Vertical Scroll Grid Container
        val gridScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.52).toInt()
            )
        }
        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        gridScroll.addView(gridContainer)

        class StudioAudioItem(
            val id: String,
            val title: String,
            val category: String,
            val isMute: Boolean = false,
            val isBuiltIn: Boolean = false,
            val isCustomPhone: Boolean = false,
            val onlineAudio: StudioAssetManager.OnlineAudio? = null,
            val colorHex: String = "#38BDF8"
        )

        val allAudioItems = mutableListOf<StudioAudioItem>()
        allAudioItems.add(StudioAudioItem("none", "صامت (بدون صوت)", "all", isMute = true, colorHex = "#64748B"))
        if (customAudioUri != null && isPhoneCustomAudio) {
            allAudioItems.add(
                StudioAudioItem(
                    id = "custom_phone",
                    title = if (customAudioTitle.isNotBlank()) customAudioTitle else "صوت من هاتفي",
                    category = "custom",
                    isCustomPhone = true,
                    colorHex = "#F59E0B"
                )
            )
        }
        allAudioItems.add(StudioAudioItem("piano", getString(R.string.audio_piano), "موسيقى", isBuiltIn = true, colorHex = "#38BDF8"))
        allAudioItems.add(StudioAudioItem("oud", getString(R.string.audio_oud), "موسيقى", isBuiltIn = true, colorHex = "#F59E0B"))
        allAudioItems.add(StudioAudioItem("nature", getString(R.string.audio_nature), "طبيعة", isBuiltIn = true, colorHex = "#10B981"))

        for (oa in StudioAssetManager.onlineAudios) {
            allAudioItems.add(
                StudioAudioItem(
                    id = oa.id,
                    title = oa.title,
                    category = oa.category,
                    onlineAudio = oa,
                    colorHex = oa.colorHex
                )
            )
        }

        fun renderAudioGrid(cat: String) {
            gridContainer.removeAllViews()
            val filtered = when (cat) {
                "all" -> allAudioItems
                "custom" -> allAudioItems.filter { it.isCustomPhone }
                else -> allAudioItems.filter { it.category == cat }
            }

            val chunks = filtered.chunked(2)
            for (chunk in chunks) {
                val row = LinearLayout(this@VideoStudioActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * dp).toInt()
                    }
                }

                for (item in chunk) {
                    val isSelected = when {
                        item.isMute -> (selectedAudio == "none" || videoMotionView.isMuted)
                        item.isCustomPhone -> (selectedAudio == "custom" || isPhoneCustomAudio) && !videoMotionView.isMuted
                        item.isBuiltIn -> (selectedAudio == item.id) && (downloadedAudioPath == null) && !isPhoneCustomAudio && !videoMotionView.isMuted
                        item.onlineAudio != null -> (selectedAudio == item.id || (downloadedAudioPath != null && downloadedAudioPath!!.contains(item.id))) && !videoMotionView.isMuted
                        else -> false
                    }

                    val isDownloaded = when {
                        item.isMute || item.isBuiltIn || item.isCustomPhone -> true
                        item.onlineAudio != null -> StudioAssetManager.isAudioDownloaded(this@VideoStudioActivity, item.id)
                        else -> true
                    }

                    val isDownloading = item.onlineAudio != null && downloadingAudioProgressMap.containsKey(item.id)
                    val downloadPercent = if (item.onlineAudio != null) downloadingAudioProgressMap[item.id] ?: 0 else 0

                    val card = FrameLayout(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (62 * dp).toInt(), 1f).apply {
                            setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                        }
                        background = GradientDrawable().apply {
                            cornerRadius = 12 * dp
                            if (isSelected) {
                                setStroke((2 * dp).toInt(), Color.parseColor("#0EA5E9"))
                                setColor(Color.parseColor("#0C253E"))
                            } else {
                                setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))
                                setColor(Color.parseColor("#141A26"))
                            }
                        }
                    }

                    val cardContent = LinearLayout(this@VideoStudioActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }

                    // Icon Circle Badge
                    val iconBadge = FrameLayout(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt()).apply {
                            marginEnd = (8 * dp).toInt()
                        }
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#1E293B"))
                            setStroke((1 * dp).toInt(), Color.parseColor(item.colorHex))
                        }
                        val iv = ImageView(this@VideoStudioActivity).apply {
                            setImageResource(if (item.isMute) R.drawable.ic_volume_off else R.drawable.ic_capcut_audio)
                            setColorFilter(Color.parseColor(item.colorHex))
                            val sz = (16 * dp).toInt()
                            layoutParams = FrameLayout.LayoutParams(sz, sz, Gravity.CENTER)
                        }
                        addView(iv)
                    }
                    cardContent.addView(iconBadge)

                    // Text Info
                    val textCol = LinearLayout(this@VideoStudioActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val tvItemTitle = TextView(this@VideoStudioActivity).apply {
                        text = item.title
                        setTextColor(if (isSelected) Color.parseColor("#38BDF8") else Color.WHITE)
                        textSize = 10.5f
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                    }
                    textCol.addView(tvItemTitle)

                    val tvSub = TextView(this@VideoStudioActivity).apply {
                        val status = when {
                            item.isMute -> "صامت"
                            item.isBuiltIn -> "محلي"
                            item.isCustomPhone -> "من الهاتف"
                            isDownloaded -> "جاهز"
                            else -> "تحميل"
                        }
                        val catDisplay = when (item.category) {
                            "all" -> "عام"
                            "custom" -> "هاتفي"
                            "هدوء" -> "استرخاء"
                            else -> item.category
                        }
                        text = if (item.isMute) "صامت • بدون صوت" else "$catDisplay • $status"
                        setTextColor(Color.parseColor(if (isDownloaded || item.isBuiltIn) "#10B981" else "#94A3B8"))
                        textSize = 8.5f
                        maxLines = 1
                    }
                    textCol.addView(tvSub)
                    cardContent.addView(textCol)

                    // Right status icon/indicator
                    if (isSelected) {
                        val activeDot = View(this@VideoStudioActivity).apply {
                            layoutParams = LinearLayout.LayoutParams((8 * dp).toInt(), (8 * dp).toInt()).apply {
                                marginStart = (4 * dp).toInt()
                            }
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(Color.parseColor("#38BDF8"))
                            }
                        }
                        cardContent.addView(activeDot)
                    }

                    card.addView(cardContent)

                    // Download progress overlay if downloading
                    val progressOverlay = LinearLayout(this@VideoStudioActivity).apply {
                        tag = "overlay_${item.id}"
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER
                        visibility = if (isDownloading) View.VISIBLE else View.GONE
                        setBackgroundColor(Color.parseColor("#E60D1117"))
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)

                        val pb = ProgressBar(this@VideoStudioActivity, null, android.R.attr.progressBarStyleSmall)
                        val tvP = TextView(this@VideoStudioActivity).apply {
                            tag = "percent_${item.id}"
                            text = " $downloadPercent%"
                            setTextColor(Color.parseColor("#38BDF8"))
                            textSize = 9.5f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        addView(pb)
                        addView(tvP)
                    }
                    card.addView(progressOverlay)

                    // Click Listener (PLAYS IMMEDIATELY, NEVER LAUNCHES TRIMMER DIALOG!)
                    card.setOnClickListener {
                        if (item.isMute) {
                            selectedAudio = "none"
                            customAudioUri = null
                            downloadedAudioPath = null
                            isPhoneCustomAudio = false
                            customAudioTitle = ""
                            videoMotionView.isMuted = true
                            ivMuteToggle.setImageResource(R.drawable.ic_volume_off)
                            videoMotionView.playAudioImmediately("none")
                            updateConfig()
                            activeBadge.text = getAudioTitleForDisplay()
                            renderAudioGrid(currentSelectedCat)
                            return@setOnClickListener
                        }

                        if (item.isCustomPhone && customAudioUri != null) {
                            selectedAudio = "custom"
                            isPhoneCustomAudio = true
                            audioStartOffsetMs = 0L
                            downloadedAudioPath = null
                            videoMotionView.audioStartOffsetMs = 0L
                            videoMotionView.customAudioUri = customAudioUri
                            videoMotionView.isMuted = false
                            ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                            loadCustomAudio(customAudioUri!!, 0L)
                            updateConfig()
                            activeBadge.text = getAudioTitleForDisplay()
                            renderAudioGrid(currentSelectedCat)
                            return@setOnClickListener
                        }

                        if (item.isBuiltIn) {
                            val audioUri = getAudioUriForBuiltInPreset(item.id)
                            selectedAudio = item.id
                            isPhoneCustomAudio = false
                            customAudioTitle = item.title
                            customAudioUri = audioUri
                            audioStartOffsetMs = 0L
                            downloadedAudioPath = null
                            videoMotionView.audioStartOffsetMs = 0L
                            videoMotionView.customAudioUri = audioUri
                            videoMotionView.isMuted = false
                            ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                            videoMotionView.playAudioPreset(item.id)
                            updateConfig()
                            activeBadge.text = getAudioTitleForDisplay()
                            renderAudioGrid(currentSelectedCat)
                            return@setOnClickListener
                        }

                        val oa = item.onlineAudio
                        if (oa != null) {
                            if (StudioAssetManager.isAudioDownloaded(this@VideoStudioActivity, oa.id)) {
                                val path = StudioAssetManager.getDownloadedAudioPath(this@VideoStudioActivity, oa.id) ?: return@setOnClickListener
                                val fileUri = Uri.fromFile(File(path))
                                downloadedAudioPath = path
                                selectedAudio = oa.id
                                isPhoneCustomAudio = false
                                customAudioUri = fileUri
                                customAudioTitle = oa.title
                                audioStartOffsetMs = 0L
                                videoMotionView.audioStartOffsetMs = 0L
                                videoMotionView.customAudioUri = fileUri
                                videoMotionView.isMuted = false
                                ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                                videoMotionView.playAudioPreset(oa.id, localPath = path)
                                updateConfig()
                                activeBadge.text = getAudioTitleForDisplay()
                                renderAudioGrid(currentSelectedCat)
                            } else {
                                if (downloadingAudioProgressMap.containsKey(oa.id)) return@setOnClickListener
                                downloadingAudioProgressMap[oa.id] = 0
                                selectedAudio = oa.id
                                customAudioUri = null
                                downloadedAudioPath = null
                                videoMotionView.customAudioUri = null
                                videoMotionView.isMuted = false
                                ivMuteToggle.setImageResource(R.drawable.ic_volume_up)
                                videoMotionView.playAudioPreset(oa.id, streamUrl = oa.url)
                                activeBadge.text = getAudioTitleForDisplay()
                                renderAudioGrid(currentSelectedCat)

                                StudioAssetManager.downloadAudio(
                                    this@VideoStudioActivity,
                                    oa,
                                    onProgress = { percent ->
                                        downloadingAudioProgressMap[oa.id] = percent
                                        runOnUiThread {
                                            val ov = gridContainer.findViewWithTag<View>("overlay_${oa.id}")
                                            ov?.visibility = View.VISIBLE
                                            gridContainer.findViewWithTag<TextView>("percent_${oa.id}")?.text = " $percent%"
                                        }
                                    },
                                    onSuccess = { localPath ->
                                        downloadingAudioProgressMap.remove(oa.id)
                                        runOnUiThread {
                                            if (selectedAudio == oa.id) {
                                                downloadedAudioPath = localPath
                                                val fileUri = Uri.fromFile(File(localPath))
                                                customAudioUri = fileUri
                                                customAudioTitle = oa.title
                                                videoMotionView.customAudioUri = fileUri
                                                videoMotionView.playAudioPreset(oa.id, localPath = localPath)
                                                updateConfig()
                                            }
                                            renderAudioGrid(currentSelectedCat)
                                        }
                                    },
                                    onError = { _ ->
                                        downloadingAudioProgressMap.remove(oa.id)
                                        runOnUiThread {
                                            renderAudioGrid(currentSelectedCat)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    row.addView(card)
                }

                if (chunk.size == 1) {
                    val dummy = View(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (62 * dp).toInt(), 1f).apply {
                            setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                        }
                    }
                    row.addView(dummy)
                }

                gridContainer.addView(row)
            }
        }

        for ((key, label) in categories) {
            val tab = TextView(this).apply {
                text = label
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding((14 * dp).toInt(), (7 * dp).toInt(), (14 * dp).toInt(), (7 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (6 * dp).toInt()
                }
            }
            categoryTabs.add(tab)

            fun updateTabStyle() {
                val isSel = currentSelectedCat == key
                tab.setTextColor(if (isSel) Color.WHITE else Color.parseColor("#94A3B8"))
                tab.background = GradientDrawable().apply {
                    cornerRadius = 18 * dp
                    if (isSel) {
                        setColor(Color.parseColor("#0C253E"))
                        setStroke((1.5f * dp).toInt(), Color.parseColor("#0EA5E9"))
                    } else {
                        setColor(Color.parseColor("#1E293B"))
                        setStroke(0, Color.TRANSPARENT)
                    }
                }
            }
            updateTabStyle()

            tab.setOnClickListener {
                currentSelectedCat = key
                for (t in categoryTabs) {
                    val isT = t == tab
                    t.setTextColor(if (isT) Color.WHITE else Color.parseColor("#94A3B8"))
                    t.background = GradientDrawable().apply {
                        cornerRadius = 18 * dp
                        if (isT) {
                            setColor(Color.parseColor("#0C253E"))
                            setStroke((1.5f * dp).toInt(), Color.parseColor("#0EA5E9"))
                        } else {
                            setColor(Color.parseColor("#1E293B"))
                            setStroke(0, Color.TRANSPARENT)
                        }
                    }
                }
                renderAudioGrid(currentSelectedCat)
            }
            categoryLayout.addView(tab)
        }

        categoryScroll.addView(categoryLayout)
        root.addView(categoryScroll)
        root.addView(gridScroll)

        renderAudioGrid("all")

        dialog.setContentView(root)
        dialog.show()
    }

    // ─────────────────────────────────────────────────────────────
    // PicsArt / CapCut Style Backgrounds & Motion Bottom Sheet Tray
    // ─────────────────────────────────────────────────────────────
    private fun showPicsartBackgroundsDialog() {
        if (isFinishing || isDestroyed) return

        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0B0F19"))
                cornerRadii = floatArrayOf(
                    24f * dp, 24f * dp,
                    24f * dp, 24f * dp,
                    0f, 0f, 0f, 0f
                )
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        // 1. Drag Handle
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (10 * dp).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 2 * dp
            }
        }
        root.addView(handle)

        // 2. Header Row 1: Title, Active status, Close (X)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }

        val tvTitle = TextView(this).apply {
            text = getString(R.string.studio_bg_motion_title)
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvTitle)

        val activeBadge = TextView(this).apply {
            val statusText = when {
                selectedVideoBgPath != null -> getString(R.string.studio_bg_active_video)
                customImageBitmap != null -> getString(R.string.studio_bg_active_image)
                else -> selectedMotion
            }
            text = statusText
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#38BDF8"))
            }
            setPadding((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = (8 * dp).toInt()
            }
        }
        header.addView(activeBadge)

        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(btnClose)
        root.addView(header)

        // 2b. Header Row 2: Action Buttons (+ صورة, + فيديو, تعديل التوقيت)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        val btnCustomImage = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#38BDF8"))
            }
            setPadding((10 * dp).toInt(), (6 * dp).toInt(), (10 * dp).toInt(), (6 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (5 * dp).toInt()
            }
            val tvPlusImg = TextView(this@VideoStudioActivity).apply {
                text = getString(R.string.studio_btn_add_image)
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tvPlusImg)
            setOnClickListener {
                dialog.dismiss()
                pickImageLauncher.launch("image/*")
            }
        }
        actionRow.addView(btnCustomImage)

        val btnCustomVideo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#F59E0B"))
            }
            setPadding((10 * dp).toInt(), (6 * dp).toInt(), (10 * dp).toInt(), (6 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (5 * dp).toInt()
            }
            val tvPlusVid = TextView(this@VideoStudioActivity).apply {
                text = getString(R.string.studio_btn_add_video)
                setTextColor(Color.parseColor("#F59E0B"))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tvPlusVid)
            setOnClickListener {
                dialog.dismiss()
                pickVideoBgLauncher.launch("video/*")
            }
        }
        actionRow.addView(btnCustomVideo)

        fun refreshHeaderAndActionRow() {
            val statusText = when {
                selectedVideoBgPath != null -> getString(R.string.studio_bg_active_video)
                customImageBitmap != null -> getString(R.string.studio_bg_active_image)
                else -> selectedMotion
            }
            activeBadge.text = statusText

            actionRow.removeAllViews()
            actionRow.addView(btnCustomImage)
            actionRow.addView(btnCustomVideo)

            if (selectedVideoBgPath != null) {
                val btnTrimCurrent = LinearLayout(this@VideoStudioActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#1E293B"))
                        cornerRadius = 8 * dp
                        setStroke((1 * dp).toInt(), Color.parseColor("#0EA5E9"))
                    }
                    setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = (5 * dp).toInt()
                    }
                    val ivScissors = ImageView(this@VideoStudioActivity).apply {
                        setImageResource(R.drawable.ic_scissors)
                        setColorFilter(Color.parseColor("#38BDF8"))
                        val sz = (14 * dp).toInt()
                        layoutParams = LinearLayout.LayoutParams(sz, sz).apply {
                            marginEnd = (4 * dp).toInt()
                        }
                    }
                    val tvT = TextView(this@VideoStudioActivity).apply {
                        text = "تعديل المقطع"
                        setTextColor(Color.parseColor("#38BDF8"))
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                    addView(ivScissors)
                    addView(tvT)
                    setOnClickListener {
                        dialog.dismiss()
                        val path = selectedVideoBgPath ?: return@setOnClickListener
                        var durSec = 15f
                        try {
                            val retriever = MediaMetadataRetriever().apply { setDataSource(path) }
                            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            durSec = (durStr?.toLongOrNull() ?: 15000L) / 1000f
                            retriever.release()
                        } catch (_: Exception) {}
                        showVideoBgTrimmerDialog(path, durSec.coerceAtLeast(1f))
                    }
                }
                actionRow.addView(btnTrimCurrent)
            }
        }
        refreshHeaderAndActionRow()

        root.addView(actionRow)

        // 3. Category Tabs
        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categories = listOf(
            "all" to getString(R.string.studio_audio_cat_all),
            "video" to getString(R.string.studio_bg_cat_motion_videos),
            "motion" to getString(R.string.studio_bg_cat_shaders),
            "nature" to getString(R.string.studio_bg_cat_nature),
            "dark" to getString(R.string.studio_bg_cat_dark),
            "islamic" to getString(R.string.studio_bg_cat_islamic),
            "gradient" to getString(R.string.studio_bg_cat_gradient)
        )

        var currentSelectedCat = "all"
        val categoryTabs = mutableListOf<TextView>()

        // 4. Vertical Scroll Grid Container
        val gridScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.52).toInt()
            )
        }
        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        gridScroll.addView(gridContainer)

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

        data class StudioBgEntry(
            val id: String,
            val title: String,
            val type: String,
            val category: String,
            val thumbUrl: String? = null,
            val colorHex: String? = null,
            val videoBg: StudioAssetManager.OnlineVideoBg? = null,
            val photoBg: StudioAssetManager.OnlineBackground? = null
        )

        val allEntries = mutableListOf<StudioBgEntry>()
        for ((sId, sTitle, sColor) in shaderList) {
            allEntries.add(StudioBgEntry(id = sId, title = sTitle, type = "shader", category = "motion", colorHex = sColor))
        }
        for (v in StudioAssetManager.onlineVideoBackgrounds) {
            allEntries.add(StudioBgEntry(id = v.id, title = v.title, type = "video", category = v.category, thumbUrl = v.thumbUrl, videoBg = v))
        }
        for (p in StudioAssetManager.onlineBackgrounds) {
            allEntries.add(StudioBgEntry(id = p.id, title = p.title, type = "photo", category = p.category, thumbUrl = p.thumbUrl, colorHex = p.previewColor, photoBg = p))
        }

        fun renderGrid(cat: String) {
            gridContainer.removeAllViews()
            val filtered = when (cat) {
                "all" -> allEntries
                "video" -> allEntries.filter { it.type == "video" }
                "motion" -> allEntries.filter { it.type == "shader" }
                else -> allEntries.filter { it.category == cat }
            }

            val chunkSize = 3
            val chunks = filtered.chunked(chunkSize)

            for (chunk in chunks) {
                val row = LinearLayout(this@VideoStudioActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * dp).toInt()
                    }
                }

                for (item in chunk) {
                    val isSelected = when (item.type) {
                        "shader" -> selectedMotion == item.id && customImageBitmap == null && selectedVideoBgPath == null
                        "video" -> {
                            val local = StudioAssetManager.getDownloadedVideoBackgroundPath(this@VideoStudioActivity, item.id)
                            selectedVideoBgPath != null && (selectedVideoBgPath == local || selectedVideoBgPath!!.contains(item.id))
                        }
                        "photo" -> selectedMotion == item.id && customImageBitmap != null
                        else -> false
                    }

                    val isDownloaded = when (item.type) {
                        "shader" -> true
                        "video" -> StudioAssetManager.isVideoBackgroundDownloaded(this@VideoStudioActivity, item.id)
                        "photo" -> StudioAssetManager.isBackgroundDownloaded(this@VideoStudioActivity, item.id)
                        else -> true
                    }

                    val card = FrameLayout(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (108 * dp).toInt(), 1f).apply {
                            setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                        }
                        background = GradientDrawable().apply {
                            cornerRadius = 14 * dp
                            if (isSelected) {
                                setStroke((2 * dp).toInt(), Color.parseColor("#0EA5E9"))
                                setColor(Color.parseColor("#0C253E"))
                            } else {
                                setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))
                                setColor(Color.parseColor("#141A26"))
                            }
                        }
                        clipToOutline = true
                    }

                    // Thumbnail / Visual
                    if (item.type == "shader") {
                        val colorV = View(this@VideoStudioActivity).apply {
                            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            setBackgroundColor(Color.parseColor(item.colorHex ?: "#0B1329"))
                        }
                        card.addView(colorV)
                        val ivGlow = ImageView(this@VideoStudioActivity).apply {
                            setImageResource(R.drawable.ic_fx_glow)
                            setColorFilter(Color.WHITE)
                            layoutParams = FrameLayout.LayoutParams((28 * dp).toInt(), (28 * dp).toInt(), Gravity.CENTER)
                        }
                        card.addView(ivGlow)
                    } else {
                        val ivThumb = ImageView(this@VideoStudioActivity).apply {
                            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        val placeholder = ColorDrawable(Color.parseColor(item.colorHex ?: "#141A26"))
                        Glide.with(applicationContext)
                            .load(item.thumbUrl)
                            .placeholder(placeholder)
                            .error(placeholder)
                            .centerCrop()
                            .into(ivThumb)
                        card.addView(ivThumb)
                    }

                    // Type Badge (top start)
                    val tvTypeBadge = TextView(this@VideoStudioActivity).apply {
                        text = when (item.type) {
                            "video" -> "فيديو"
                            "shader" -> "شيدر"
                            else -> "صورة"
                        }
                        setTextColor(Color.WHITE)
                        textSize = 8.5f
                        typeface = Typeface.DEFAULT_BOLD
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor(when (item.type) {
                                "video" -> "#D9F59E0B"
                                "shader" -> "#D90EA5E9"
                                else -> "#D910B981"
                            }))
                            cornerRadius = 5 * dp
                        }
                        setPadding((6 * dp).toInt(), (2 * dp).toInt(), (6 * dp).toInt(), (2 * dp).toInt())
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.TOP or Gravity.START
                            topMargin = (5 * dp).toInt()
                            marginStart = (5 * dp).toInt()
                        }
                    }
                    card.addView(tvTypeBadge)

                    // Download Icon if not downloaded
                    if (!isDownloaded) {
                        val badge = ImageView(this@VideoStudioActivity).apply {
                            setImageResource(R.drawable.ic_download)
                            setColorFilter(Color.WHITE)
                            val bSize = (14 * dp).toInt()
                            layoutParams = FrameLayout.LayoutParams(bSize, bSize).apply {
                                gravity = Gravity.TOP or Gravity.END
                                topMargin = (5 * dp).toInt()
                                marginEnd = (5 * dp).toInt()
                            }
                        }
                        card.addView(badge)
                    }

                    // Scrim at bottom
                    val scrim = View(this@VideoStudioActivity).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            (34 * dp).toInt(),
                            Gravity.BOTTOM
                        )
                        background = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(Color.TRANSPARENT, Color.parseColor("#F20B0F19"))
                        )
                    }
                    card.addView(scrim)

                    // Title
                    val tvCardTitle = TextView(this@VideoStudioActivity).apply {
                        text = item.title
                        setTextColor(Color.WHITE)
                        textSize = 9f
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        gravity = Gravity.CENTER
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM
                        ).apply {
                            bottomMargin = (5 * dp).toInt()
                            marginStart = (4 * dp).toInt()
                            marginEnd = (4 * dp).toInt()
                        }
                    }
                    card.addView(tvCardTitle)

                    // Download Progress Overlay
                    val isDownloading = downloadingVideoBgProgressMap.containsKey(item.id) || downloadingBgProgressMap.containsKey(item.id)
                    val progressOverlay = FrameLayout(this@VideoStudioActivity).apply {
                        visibility = if (isDownloading) View.VISIBLE else View.GONE
                        setBackgroundColor(Color.parseColor("#D90B0F19"))
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    val tvProg = TextView(this@VideoStudioActivity).apply {
                        tag = "picsart_bg_prog_${item.id}"
                        text = "${downloadingVideoBgProgressMap[item.id] ?: downloadingBgProgressMap[item.id] ?: 0}%"
                        setTextColor(Color.WHITE)
                        textSize = 12f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                    }
                    progressOverlay.addView(tvProg)
                    card.addView(progressOverlay)

                    card.setOnClickListener {
                        when (item.type) {
                            "shader" -> {
                                selectedMotion = item.id
                                customImageBitmap = null
                                selectedVideoBgPath = null
                                selectedVideoBgStartSec = 0f
                                selectedVideoBgEndSec = 0f
                                btnAdjustFraming.visibility = View.GONE
                                setupOrUpdateVideoBackgroundPlayer()
                                updateConfig()
                                populateMotionPresets()
                                refreshHeaderAndActionRow()
                                renderGrid(currentSelectedCat)
                            }
                            "video" -> {
                                val vbg = item.videoBg ?: return@setOnClickListener
                                if (StudioAssetManager.isVideoBackgroundDownloaded(this@VideoStudioActivity, vbg.id)) {
                                    val localPath = StudioAssetManager.getDownloadedVideoBackgroundPath(this@VideoStudioActivity, vbg.id)
                                    if (localPath != null) {
                                        selectedVideoBgPath = localPath
                                        selectedVideoBgStartSec = 0f
                                        selectedVideoBgEndSec = vbg.durationSec.toFloat()
                                        customImageBitmap = null
                                        selectedMotion = "VIDEO_BG"
                                        btnAdjustFraming.visibility = View.GONE
                                        setupOrUpdateVideoBackgroundPlayer()
                                        updateConfig()
                                        populateMotionPresets()
                                        refreshHeaderAndActionRow()
                                        renderGrid(currentSelectedCat)
                                    }
                                } else {
                                    if (downloadingVideoBgProgressMap.containsKey(vbg.id)) return@setOnClickListener
                                    downloadingVideoBgProgressMap[vbg.id] = 0
                                    progressOverlay.visibility = View.VISIBLE
                                    StudioAssetManager.downloadVideoBackground(
                                        context = this@VideoStudioActivity,
                                        vbg = vbg,
                                        onProgress = { p ->
                                            downloadingVideoBgProgressMap[vbg.id] = p
                                            card.findViewWithTag<TextView>("picsart_bg_prog_${item.id}")?.text = "$p%"
                                        },
                                        onSuccess = { savedPath ->
                                            if (isFinishing || isDestroyed) return@downloadVideoBackground
                                            downloadingVideoBgProgressMap.remove(vbg.id)
                                            selectedVideoBgPath = savedPath
                                            selectedVideoBgStartSec = 0f
                                            selectedVideoBgEndSec = vbg.durationSec.toFloat()
                                            customImageBitmap = null
                                            selectedMotion = "VIDEO_BG"
                                            btnAdjustFraming.visibility = View.GONE
                                            setupOrUpdateVideoBackgroundPlayer()
                                            updateConfig()
                                            populateMotionPresets()
                                            refreshHeaderAndActionRow()
                                            renderGrid(currentSelectedCat)
                                            Toast.makeText(this@VideoStudioActivity, "تم تحميل وتعيين خلفية الفيديو بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            if (isFinishing || isDestroyed) return@downloadVideoBackground
                                            downloadingVideoBgProgressMap.remove(vbg.id)
                                            renderGrid(currentSelectedCat)
                                            Toast.makeText(this@VideoStudioActivity, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            "photo" -> {
                                val pbg = item.photoBg ?: return@setOnClickListener
                                if (StudioAssetManager.isBackgroundDownloaded(this@VideoStudioActivity, pbg.id)) {
                                    val bmp = StudioAssetManager.getDownloadedBackgroundBitmap(this@VideoStudioActivity, pbg.id)
                                    if (bmp != null) {
                                        customImageBitmap = bmp
                                        selectedMotion = pbg.id
                                        selectedVideoBgPath = null
                                        selectedVideoBgStartSec = 0f
                                        selectedVideoBgEndSec = 0f
                                        btnAdjustFraming.visibility = View.VISIBLE
                                        setupOrUpdateVideoBackgroundPlayer()
                                        updateConfig()
                                        populateMotionPresets()
                                        refreshHeaderAndActionRow()
                                        renderGrid(currentSelectedCat)
                                    }
                                } else {
                                    if (downloadingBgProgressMap.containsKey(pbg.id)) return@setOnClickListener
                                    downloadingBgProgressMap[pbg.id] = 0
                                    progressOverlay.visibility = View.VISIBLE
                                    StudioAssetManager.downloadBackground(
                                        context = this@VideoStudioActivity,
                                        bg = pbg,
                                        onProgress = { percent ->
                                            downloadingBgProgressMap[pbg.id] = percent
                                            card.findViewWithTag<TextView>("picsart_bg_prog_${item.id}")?.text = "$percent%"
                                        },
                                        onSuccess = { bmp ->
                                            if (isFinishing || isDestroyed) return@downloadBackground
                                            downloadingBgProgressMap.remove(pbg.id)
                                            customImageBitmap = bmp
                                            selectedMotion = pbg.id
                                            selectedVideoBgPath = null
                                            selectedVideoBgStartSec = 0f
                                            selectedVideoBgEndSec = 0f
                                            btnAdjustFraming.visibility = View.VISIBLE
                                            setupOrUpdateVideoBackgroundPlayer()
                                            updateConfig()
                                            populateMotionPresets()
                                            refreshHeaderAndActionRow()
                                            renderGrid(currentSelectedCat)
                                            Toast.makeText(this@VideoStudioActivity, "تم تحميل وتعيين الخلفية بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            if (isFinishing || isDestroyed) return@downloadBackground
                                            downloadingBgProgressMap.remove(pbg.id)
                                            renderGrid(currentSelectedCat)
                                            Toast.makeText(this@VideoStudioActivity, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    row.addView(card)
                }

                // Balance row if less than 3 items
                for (i in chunk.size until chunkSize) {
                    val dummy = View(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (108 * dp).toInt(), 1f).apply {
                            setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                        }
                    }
                    row.addView(dummy)
                }

                gridContainer.addView(row)
            }
        }

        for ((catId, catTitle) in categories) {
            val isCurrent = catId == currentSelectedCat
            val tab = TextView(this).apply {
                text = catTitle
                textSize = 12f
                typeface = if (isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (isCurrent) Color.WHITE else Color.parseColor("#94A3B8"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (isCurrent) "#0EA5E9" else "#1E293B"))
                    cornerRadius = 16 * dp
                }
                setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (6 * dp).toInt()
                }
                setOnClickListener {
                    currentSelectedCat = catId
                    for (t in categoryTabs) {
                        t.setTextColor(Color.parseColor("#94A3B8"))
                        t.typeface = Typeface.DEFAULT
                        (t.background as? GradientDrawable)?.setColor(Color.parseColor("#1E293B"))
                    }
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    (background as? GradientDrawable)?.setColor(Color.parseColor("#0EA5E9"))
                    renderGrid(catId)
                }
            }
            categoryTabs.add(tab)
            categoryLayout.addView(tab)
        }
        categoryScroll.addView(categoryLayout)
        root.addView(categoryScroll)
        root.addView(gridScroll)

        renderGrid(currentSelectedCat)

        dialog.setContentView(root)
        dialog.show()
    }

    // ─────────────────────────────────────────────────────────────
    // PicsArt / CapCut Style 3D Stickers Bottom Sheet Tray
    // ─────────────────────────────────────────────────────────────
    private fun showPicsartStickersDialog() {
        if (isFinishing || isDestroyed) return

        val dialog = BottomSheetDialog(this)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0B0F19"))
                cornerRadii = floatArrayOf(
                    24f * dp, 24f * dp,
                    24f * dp, 24f * dp,
                    0f, 0f, 0f, 0f
                )
            }
            setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        // 1. Drag Handle
        val handle = View(this).apply {
            layoutParams = LinearLayout.LayoutParams((38 * dp).toInt(), (4 * dp).toInt()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * dp).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 2 * dp
            }
        }
        root.addView(handle)

        // 2. Header Row 1: Title, Active Counter, Close (X)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * dp).toInt()
            }
        }

        val tvTitle = TextView(this).apply {
            text = "ملصقات وطبقات متحركة"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(tvTitle)

        if (activeStickers.isNotEmpty()) {
            val badgeActive = TextView(this).apply {
                text = "${activeStickers.size} نشط"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 8 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#38BDF8"))
                }
                setPadding((8 * dp).toInt(), (3 * dp).toInt(), (8 * dp).toInt(), (3 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * dp).toInt()
                }
            }
            header.addView(badgeActive)
        }

        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams((32 * dp).toInt(), (32 * dp).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1E293B"))
            }
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(btnClose)
        root.addView(header)

        // 2b. Header Row 2: Actions Bar (+ صورة, + فيديو, التوقيت, حذف الكل)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        val btnCustomImage = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 8 * dp
                setStroke((1 * dp).toInt(), Color.parseColor("#38BDF8"))
            }
            setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (6 * dp).toInt()
            }
            val tvPlusImg = TextView(this@VideoStudioActivity).apply {
                text = "+ صورة"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(tvPlusImg)
            setOnClickListener {
                dialog.dismiss()
                pickStickerLauncher.launch("image/*")
            }
        }
        actionRow.addView(btnCustomImage)

        if (activeStickers.isNotEmpty()) {
            val btnTiming = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 8 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#0EA5E9"))
                }
                setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (5 * dp).toInt()
                }
                val tvT = TextView(this@VideoStudioActivity).apply {
                    text = "التوقيت"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(tvT)
                setOnClickListener {
                    val target = activeStickers.lastOrNull()
                    if (target != null) {
                        dialog.dismiss()
                        showStickerTimingDialog(target)
                    }
                }
            }
            actionRow.addView(btnTiming)

            val btnRemove = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#26F43F5E"))
                    cornerRadius = 8 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#4DF43F5E"))
                }
                setPadding((8 * dp).toInt(), (6 * dp).toInt(), (8 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                val tvR = TextView(this@VideoStudioActivity).apply {
                    text = "حذف الكل"
                    setTextColor(Color.parseColor("#F43F5E"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(tvR)
                setOnClickListener {
                    activeStickers.clear()
                    selectedStickerPath = null
                    videoMotionView.activeStickerIndex = -1
                    btnRemoveSticker.visibility = View.GONE
                    updateConfig()
                    populateStickerCards()
                    dialog.dismiss()
                    Toast.makeText(this@VideoStudioActivity, getString(R.string.studio_sticker_remove), Toast.LENGTH_SHORT).show()
                }
            }
            actionRow.addView(btnRemove)
        }
        root.addView(actionRow)

        // 3. Category Filter Tabs
        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (12 * dp).toInt()
            }
        }

        val categoryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categories = listOf(
            "all" to "الكل",
            "islamic" to "إسلاميات",
            "feelings" to "مشاعر",
            "magic" to "تألق",
            "reactions" to "تفاعلات",
            "vibes" to "حماس"
        )

        var currentSelectedCategory = currentStickerCategory
        val categoryTabs = mutableListOf<TextView>()

        // 4. Vertical Scroll Grid Container
        val gridScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.52).toInt()
            )
        }
        val gridContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        gridScroll.addView(gridContainer)

        fun renderGrid(cat: String) {
            gridContainer.removeAllViews()
            val filteredStickers = if (cat == "all") {
                StudioAssetManager.onlineStickers
            } else {
                StudioAssetManager.onlineStickers.filter { it.category == cat }
            }

            val chunkSize = 4
            val chunks = filteredStickers.chunked(chunkSize)

            for (chunk in chunks) {
                val row = LinearLayout(this@VideoStudioActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = (8 * dp).toInt()
                    }
                }

                for (sticker in chunk) {
                    val isDownloaded = StudioAssetManager.isStickerDownloaded(this@VideoStudioActivity, sticker.id)
                    val localPath = StudioAssetManager.getDownloadedStickerPath(this@VideoStudioActivity, sticker.id)
                    val isSelected = localPath != null && activeStickers.any { it.path == localPath }

                    val card = FrameLayout(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (82 * dp).toInt(), 1f).apply {
                            setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                        }
                        background = GradientDrawable().apply {
                            cornerRadius = 14 * dp
                            if (isSelected) {
                                setStroke((2 * dp).toInt(), Color.parseColor("#0EA5E9"))
                                setColor(Color.parseColor("#0C253E"))
                            } else {
                                setStroke((1 * dp).toInt(), Color.parseColor("#1E293B"))
                                setColor(Color.parseColor("#141A26"))
                            }
                        }
                        clipToOutline = true
                    }

                    val contentCol = LinearLayout(this@VideoStudioActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        setPadding((4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt(), (4 * dp).toInt())
                    }

                    // 3D Preview Image
                    val ivThumb = ImageView(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt())
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    Glide.with(applicationContext)
                        .load(sticker.thumbUrl)
                        .placeholder(ColorDrawable(Color.parseColor("#141A26")))
                        .error(R.drawable.ic_capcut_sticker)
                        .into(ivThumb)
                    contentCol.addView(ivThumb)

                    // Title
                    val tvStickerTitle = TextView(this@VideoStudioActivity).apply {
                        text = sticker.title
                        setTextColor(if (isSelected) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
                        textSize = 8f
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        gravity = Gravity.CENTER
                        typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                        setPadding(0, (2 * dp).toInt(), 0, 0)
                    }
                    contentCol.addView(tvStickerTitle)
                    card.addView(contentCol)

                    // Download badge if not yet downloaded
                    if (!isDownloaded) {
                        val badge = ImageView(this@VideoStudioActivity).apply {
                            setImageResource(R.drawable.ic_download)
                            setColorFilter(Color.parseColor("#64748B"))
                            val bSize = (11 * dp).toInt()
                            layoutParams = FrameLayout.LayoutParams(bSize, bSize).apply {
                                gravity = Gravity.TOP or Gravity.START
                                topMargin = (4 * dp).toInt()
                                marginStart = (4 * dp).toInt()
                            }
                        }
                        card.addView(badge)
                    }

                    // Download Progress Overlay
                    val isDownloading = downloadingStickerProgressMap.containsKey(sticker.id)
                    val progressOverlay = FrameLayout(this@VideoStudioActivity).apply {
                        visibility = if (isDownloading) View.VISIBLE else View.GONE
                        setBackgroundColor(Color.parseColor("#CC0B0F19"))
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }
                    val tvProg = TextView(this@VideoStudioActivity).apply {
                        tag = "sheet_sticker_prog_${sticker.id}"
                        text = "${downloadingStickerProgressMap[sticker.id] ?: 0}%"
                        setTextColor(Color.WHITE)
                        textSize = 10f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                    }
                    progressOverlay.addView(tvProg)
                    card.addView(progressOverlay)

                    card.setOnClickListener {
                        if (downloadingStickerProgressMap.containsKey(sticker.id)) return@setOnClickListener

                        fun addStickerToStudio(path: String) {
                            val count = activeStickers.size
                            val staggerY = (0.35f + (count * 0.07f) % 0.38f)
                            val staggerX = (0.42f + (count * 0.08f) % 0.24f)
                            val newStk = VideoMotionRenderer.StickerItem(
                                path = path,
                                x = staggerX,
                                y = staggerY,
                                scale = 1.0f,
                                startSec = 0f,
                                endSec = selectedDuration.toFloat()
                            )
                            activeStickers.add(newStk)
                            videoMotionView.activeStickerIndex = activeStickers.lastIndex
                            selectedStickerPath = path
                            btnRemoveSticker.visibility = View.VISIBLE
                            updateConfig()
                            populateStickerCards()
                            renderGrid(currentSelectedCategory)
                            Toast.makeText(this@VideoStudioActivity, "تمت إضافة الملصق (${activeStickers.size}) بنجاح", Toast.LENGTH_SHORT).show()
                        }

                        if (StudioAssetManager.isStickerDownloaded(this@VideoStudioActivity, sticker.id)) {
                            val path = StudioAssetManager.getDownloadedStickerPath(this@VideoStudioActivity, sticker.id)
                            if (path != null) {
                                addStickerToStudio(path)
                            }
                        } else {
                            downloadingStickerProgressMap[sticker.id] = 0
                            progressOverlay.visibility = View.VISIBLE
                            StudioAssetManager.downloadSticker(
                                context = this@VideoStudioActivity,
                                sticker = sticker,
                                onProgress = { p ->
                                    downloadingStickerProgressMap[sticker.id] = p
                                    card.findViewWithTag<TextView>("sheet_sticker_prog_${sticker.id}")?.text = "$p%"
                                },
                                onSuccess = { savedPath ->
                                    if (isFinishing || isDestroyed) return@downloadSticker
                                    downloadingStickerProgressMap.remove(sticker.id)
                                    progressOverlay.visibility = View.GONE
                                    addStickerToStudio(savedPath)
                                },
                                onError = { err ->
                                    if (isFinishing || isDestroyed) return@downloadSticker
                                    downloadingStickerProgressMap.remove(sticker.id)
                                    progressOverlay.visibility = View.GONE
                                    Toast.makeText(this@VideoStudioActivity, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    row.addView(card)
                }

                // If chunk has less than 4 items, add dummy empty views to balance the row
                for (i in chunk.size until chunkSize) {
                    val dummy = View(this@VideoStudioActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(0, (82 * dp).toInt(), 1f).apply {
                            setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                        }
                    }
                    row.addView(dummy)
                }

                gridContainer.addView(row)
            }
        }

        for ((catId, catTitle) in categories) {
            val isCurrent = catId == currentSelectedCategory
            val tab = TextView(this).apply {
                text = catTitle
                textSize = 12f
                typeface = if (isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                setTextColor(if (isCurrent) Color.WHITE else Color.parseColor("#94A3B8"))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (isCurrent) "#0EA5E9" else "#1E293B"))
                    cornerRadius = 16 * dp
                }
                setPadding((12 * dp).toInt(), (6 * dp).toInt(), (12 * dp).toInt(), (6 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (6 * dp).toInt()
                }
                setOnClickListener {
                    currentSelectedCategory = catId
                    for (t in categoryTabs) {
                        t.setTextColor(Color.parseColor("#94A3B8"))
                        t.typeface = Typeface.DEFAULT
                        (t.background as? GradientDrawable)?.setColor(Color.parseColor("#1E293B"))
                    }
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    (background as? GradientDrawable)?.setColor(Color.parseColor("#0EA5E9"))
                    renderGrid(catId)
                }
            }
            categoryTabs.add(tab)
            categoryLayout.addView(tab)
        }
        categoryScroll.addView(categoryLayout)
        root.addView(categoryScroll)
        root.addView(gridScroll)

        renderGrid(currentSelectedCategory)

        dialog.setContentView(root)
        dialog.show()
    }

    private fun populateStickerCards() {
        if (isFinishing || isDestroyed) return
        layoutStickerCards.removeAllViews()

        val filtered = StudioAssetManager.onlineStickers.filter {
            currentStickerCategory == "all" || it.category == currentStickerCategory
        }

        val S = resources.displayMetrics.density

        for (sticker in filtered) {
            val isDownloaded = StudioAssetManager.isStickerDownloaded(this, sticker.id)
            val isDownloading = downloadingStickerProgressMap.containsKey(sticker.id)
            val isSelected = activeStickers.any { it.path.contains(sticker.id) } || (selectedStickerPath != null && selectedStickerPath!!.contains(sticker.id))

            val card = FrameLayout(this).apply {
                val cardW = (84f * S).toInt()
                val cardH = (96f * S).toInt()
                layoutParams = LinearLayout.LayoutParams(cardW, cardH).apply {
                    marginEnd = (10f * S).toInt()
                }
                val bg = GradientDrawable().apply {
                    cornerRadius = 14f * S
                    if (isSelected) {
                        setColor(Color.parseColor("#2638BDF8"))
                        setStroke((2f * S).toInt(), Color.parseColor("#38BDF8"))
                    } else {
                        setColor(Color.parseColor("#151C28"))
                        setStroke((1f * S).toInt(), Color.parseColor("#25FFFFFF"))
                    }
                }
                background = bg
                isClickable = true
                isFocusable = true
            }

            val contentCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                    setPadding((6f * S).toInt(), (6f * S).toInt(), (6f * S).toInt(), (6f * S).toInt())
                }
            }

            val ivPreview = ImageView(this).apply {
                val iconSize = (54f * S).toInt()
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            Glide.with(applicationContext)
                .load(sticker.thumbUrl)
                .placeholder(ColorDrawable(Color.parseColor("#141A26")))
                .error(R.drawable.ic_capcut_sticker)
                .into(ivPreview)

            val tvTitle = TextView(this).apply {
                text = sticker.title
                textSize = 9.5f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setTextColor(if (isSelected) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (4f * S).toInt()
                }
            }

            contentCol.addView(ivPreview)
            contentCol.addView(tvTitle)
            card.addView(contentCol)

            if (isDownloading) {
                val overlay = FrameLayout(this).apply {
                    setBackgroundColor(Color.parseColor("#AA000000"))
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                }
                val tvProg = TextView(this).apply {
                    tag = "sticker_prog_${sticker.id}"
                    text = "${downloadingStickerProgressMap[sticker.id]}%"
                    setTextColor(Color.WHITE)
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
                }
                overlay.addView(tvProg)
                card.addView(overlay)
            } else if (!isDownloaded) {
                val badge = ImageView(this).apply {
                    setImageResource(R.drawable.ic_download)
                    setColorFilter(Color.parseColor("#94A3B8"))
                    val bSize = (14f * S).toInt()
                    layoutParams = FrameLayout.LayoutParams(bSize, bSize).apply {
                        gravity = Gravity.TOP or Gravity.START
                        topMargin = (4f * S).toInt()
                        marginStart = (4f * S).toInt()
                    }
                }
                card.addView(badge)
            }

            card.setOnClickListener {
                if (isDownloading) return@setOnClickListener

                fun addStickerToStudio(path: String) {
                    val count = activeStickers.size
                    val staggerY = (0.35f + (count * 0.07f) % 0.38f)
                    val staggerX = (0.42f + (count * 0.08f) % 0.24f)
                    val newStk = VideoMotionRenderer.StickerItem(
                        path = path,
                        x = staggerX,
                        y = staggerY,
                        scale = 1.0f,
                        startSec = 0f,
                        endSec = selectedDuration.toFloat()
                    )
                    activeStickers.add(newStk)
                    videoMotionView.activeStickerIndex = activeStickers.lastIndex
                    selectedStickerPath = path
                    btnRemoveSticker.visibility = View.VISIBLE
                    updateConfig()
                    populateStickerCards()
                    Toast.makeText(this@VideoStudioActivity, "تمت إضافة الملصق (${activeStickers.size}) بنجاح", Toast.LENGTH_SHORT).show()
                }

                if (StudioAssetManager.isStickerDownloaded(this, sticker.id)) {
                    val localPath = StudioAssetManager.getDownloadedStickerPath(this, sticker.id)
                    if (localPath != null) {
                        addStickerToStudio(localPath)
                    }
                } else {
                    downloadingStickerProgressMap[sticker.id] = 0
                    populateStickerCards()
                    StudioAssetManager.downloadSticker(
                        context = this,
                        sticker = sticker,
                        onProgress = { p ->
                            downloadingStickerProgressMap[sticker.id] = p
                            layoutStickerCards.findViewWithTag<TextView>("sticker_prog_${sticker.id}")?.text = "$p%"
                        },
                        onSuccess = { savedPath ->
                            if (isFinishing || isDestroyed) return@downloadSticker
                            downloadingStickerProgressMap.remove(sticker.id)
                            addStickerToStudio(savedPath)
                        },
                        onError = { err ->
                            if (isFinishing || isDestroyed) return@downloadSticker
                            downloadingStickerProgressMap.remove(sticker.id)
                            populateStickerCards()
                            Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            layoutStickerCards.addView(card)
        }
    }

    // ─────────────────────────────────────────────────────────────

    // 6. Config Update & Handlers

    // ─────────────────────────────────────────────────────────────

    
    private fun setupOrUpdateVideoBackgroundPlayer() {
        val path = selectedVideoBgPath
        if (path.isNullOrEmpty() || !File(path).exists()) {
            textureBgVideo.visibility = View.GONE
            try {
                bgVideoMediaPlayer?.stop()
                bgVideoMediaPlayer?.release()
            } catch (_: Exception) {}
            bgVideoMediaPlayer = null
            return
        }

        textureBgVideo.visibility = View.VISIBLE
        if (textureBgVideo.isAvailable) {
            val surfaceTexture = textureBgVideo.surfaceTexture
            if (surfaceTexture != null) {
                initMediaPlayerForSurface(Surface(surfaceTexture), path)
            }
        } else {
            textureBgVideo.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    initMediaPlayerForSurface(Surface(surface), path)
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    try {
                        bgVideoMediaPlayer?.release()
                    } catch (_: Exception) {}
                    bgVideoMediaPlayer = null
                    return true
                }
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }

    private fun initMediaPlayerForSurface(surface: Surface, path: String) {
        try {
            isSeekingBgVideo = false
            bgVideoMediaPlayer?.release()
            bgVideoMediaPlayer = MediaPlayer().apply {
                setSurface(surface)
                setDataSource(path)
                setVolume(0f, 0f)
                setOnErrorListener { _, _, _ ->
                    isSeekingBgVideo = false
                    true // Consume error to prevent crash loops and ANRs
                }
                setOnSeekCompleteListener {
                    isSeekingBgVideo = false
                }
                val startMs = (selectedVideoBgStartSec * 1000).toInt()
                val endMs = if (selectedVideoBgEndSec > selectedVideoBgStartSec) (selectedVideoBgEndSec * 1000).toInt() else 0
                isLooping = (endMs <= 0 && startMs <= 0)
                setOnPreparedListener { mp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val params = mp.playbackParams
                            params.speed = selectedVideoBgSpeed.coerceIn(0.1f, 5.0f)
                            mp.playbackParams = params
                        } catch (_: Exception) {}
                    }
                    if (startMs > 0) {
                        isSeekingBgVideo = true
                        mp.seekTo(startMs)
                    }
                    if (videoMotionView.isPlaying) {
                        mp.start()
                    }
                }
                setOnCompletionListener { mp ->
                    val loopStart = (selectedVideoBgStartSec * 1000).toInt()
                    isSeekingBgVideo = true
                    mp.seekTo(loopStart)
                    mp.start()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            isSeekingBgVideo = false
            e.printStackTrace()
        }
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

            downloadedAudioPath = downloadedAudioPath,

            animSpeed = selectedAnimSpeed,

            imageFramingMode = selectedImageFramingMode,

            imageFocusY = selectedImageFocusY,

            imagePanX = selectedImagePanX,

            imagePanY = selectedImagePanY,

            imageZoomScale = selectedImageZoomScale,

            audioStartOffsetMs = audioStartOffsetMs
,
            colorFilter = selectedColorFilter,
            textSizeScale = selectedTextSizeScale,
            stickerPath = activeStickers.lastOrNull()?.path ?: selectedStickerPath,
            stickerX = selectedStickerX,
            stickerY = selectedStickerY,
            stickerScale = selectedStickerScale,
            stickers = activeStickers.toList(),
            bgVideoPath = selectedVideoBgPath,
            bgVideoStartSec = selectedVideoBgStartSec,
            bgVideoEndSec = selectedVideoBgEndSec,
            bgVideoSpeed = selectedVideoBgSpeed
        )

        videoMotionView.audioStartOffsetMs = audioStartOffsetMs
        videoMotionView.config = config
        setupOrUpdateVideoBackgroundPlayer()
        updateVnTracksUI()
    }



    private fun loadCustomImage(uri: Uri) {
        try {
            val input: InputStream? = contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(input)
            input?.close()

            if (bmp != null) {
                showFreeCropDialog(bmp, "قص خلفية الفيديو") { croppedBmp ->
                    customImageBitmap = croppedBmp
                    selectedMotion = "CUSTOM_IMAGE"
                    selectedVideoBgPath = null
                    btnAdjustFraming.visibility = View.VISIBLE
                    setupOrUpdateVideoBackgroundPlayer()
                    updateConfig()
                    populateMotionPresets()
                    Toast.makeText(this, "تم قص وتعيين الخلفية بنجاح", Toast.LENGTH_SHORT).show()
                }
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
        try {
            if (selectedVideoBgPath != null && bgVideoMediaPlayer?.isPlaying == false) {
                bgVideoMediaPlayer?.start()
            }
        } catch (_: Exception) {}
    }



    override fun onPause() {
        super.onPause()
        videoMotionView.pause()
        try {
            bgVideoMediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bgVideoMediaPlayer?.stop()
            bgVideoMediaPlayer?.release()
        } catch (_: Exception) {}
        bgVideoMediaPlayer = null
    }


    // ─────────────────────────────────────────────────────────────
    // VN Style Cinematic Filters & LUTs + Multi-Track Updates
    // ─────────────────────────────────────────────────────────────
    private fun populateFilterCards() {
        layoutFilterCards.removeAllViews()
        val dp = resources.displayMetrics.density

        data class FilterPreset(val id: String, val name: String, val previewColor1: String, val previewColor2: String)

        val filters = listOf(
            FilterPreset("none", "أصلي", "#1E293B", "#0F172A"),
            FilterPreset("teal_orange", "سينمائي", "#0E7490", "#EA580C"),
            FilterPreset("moody", "مودي سينما", "#1E1B4B", "#312E81"),
            FilterPreset("vintage_90s", "كلاسيكي", "#78350F", "#D97706"),
            FilterPreset("noir_bw", "أبيض وأسود", "#18181B", "#71717A"),
            FilterPreset("cyberpunk", "سايبر نيون", "#4C1D95", "#0284C7"),
            FilterPreset("sunset_gold", "غروب ذهبي", "#9A3412", "#F59E0B")
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

package com.manchorati.www

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ReelPlayerActivity : AppCompatActivity() {

    private lateinit var reelMotionView: VideoMotionView
    private lateinit var ivReelPlayIndicator: ImageView
    private lateinit var btnReelClose: ImageView
    private lateinit var btnReelLike: FrameLayout
    private lateinit var ivReelLike: ImageView
    private lateinit var tvReelLikesCount: TextView
    private lateinit var btnReelExportMp4: FrameLayout
    private lateinit var btnReelWhatsApp: FrameLayout

    private var postId = ""
    private var postContent = ""
    private var postAuthorName = ""
    private var postAuthorUsername = ""
    private var postAuthorBadge = ""
    private var postTag = ""
    private var bgMotionId = "STARS"
    private var audioPreset = "piano"
    private var animStyle = "typewriter"
    private var videoRatio = "9:16"
    private var fontId = "tajawal"
    private var textColor = "#FFFFFF"

    private var likesCount = 0L
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reel_player)

        initData()
        initViews()
        setupListeners()
    }

    private fun initData() {
        postId = intent.getStringExtra("EXTRA_POST_ID") ?: ""
        postContent = intent.getStringExtra("EXTRA_CONTENT") ?: ""
        postAuthorName = intent.getStringExtra("EXTRA_AUTHOR_NAME") ?: "منشوراتي"
        postAuthorUsername = intent.getStringExtra("EXTRA_AUTHOR_USERNAME") ?: "manchorati"
        postAuthorBadge = intent.getStringExtra("EXTRA_AUTHOR_BADGE") ?: ""
        postTag = intent.getStringExtra("EXTRA_TAG") ?: "فيديو"
        bgMotionId = intent.getStringExtra("EXTRA_BG_MOTION_ID") ?: "STARS"
        audioPreset = intent.getStringExtra("EXTRA_AUDIO_PRESET") ?: "piano"
        animStyle = intent.getStringExtra("EXTRA_ANIM_STYLE") ?: "typewriter"
        videoRatio = intent.getStringExtra("EXTRA_VIDEO_RATIO") ?: "9:16"
        fontId = intent.getStringExtra("EXTRA_FONT_ID") ?: "tajawal"
        textColor = intent.getStringExtra("EXTRA_TEXT_COLOR") ?: "#FFFFFF"

        likesCount = intent.getLongExtra("EXTRA_LIKES_COUNT", 0L)
        val likedBy = intent.getStringArrayListExtra("EXTRA_LIKED_BY") ?: arrayListOf()
        val currentUid = AuthManager.currentUser?.uid ?: ""
        isLiked = currentUid.isNotEmpty() && likedBy.contains(currentUid)
    }

    private fun initViews() {
        reelMotionView = findViewById(R.id.reelMotionView)
        ivReelPlayIndicator = findViewById(R.id.ivReelPlayIndicator)
        btnReelClose = findViewById(R.id.btnReelClose)
        btnReelLike = findViewById(R.id.btnReelLike)
        ivReelLike = findViewById(R.id.ivReelLike)
        tvReelLikesCount = findViewById(R.id.tvReelLikesCount)
        btnReelExportMp4 = findViewById(R.id.btnReelExportMp4)
        btnReelWhatsApp = findViewById(R.id.btnReelWhatsApp)

        updateLikeUI()

        val config = VideoMotionRenderer.VideoConfig(
            content = postContent,
            authorName = postAuthorName,
            authorUsername = postAuthorUsername,
            authorBadge = postAuthorBadge,
            tag = postTag,
            bgMotionId = bgMotionId,
            audioPreset = audioPreset,
            animStyle = animStyle,
            aspectRatio = videoRatio,
            durationSec = 10,
            fontId = fontId,
            textColor = textColor
        )
        reelMotionView.config = config
    }

    private fun setupListeners() {
        btnReelClose.setOnClickListener { finish() }

        // Tap on screen toggles pause/play
        reelMotionView.setOnClickListener {
            reelMotionView.togglePlay()
            showPlayPauseIndicator(!reelMotionView.isPlaying)
        }

        btnReelLike.setOnClickListener {
            val uid = AuthManager.currentUser?.uid ?: ""
            if (uid.isEmpty()) {
                Toast.makeText(this, "يرجى تسجيل الدخول للإعجاب", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isLiked = !isLiked
            likesCount = if (isLiked) likesCount + 1 else maxOf(0L, likesCount - 1)
            updateLikeUI()

            if (postId.isNotEmpty()) {
                FirestoreManager.toggleLike(postId, uid, isLiked) {}
            }
        }

        btnReelExportMp4.setOnClickListener {
            exportVideoAndProceed { file ->
                val uri = VideoExporter.saveVideoToGallery(this, file, postAuthorName)
                if (uri != null) {
                    Toast.makeText(this, "تم حفظ الفيديو بنجاح في المعرض! 🎉", Toast.LENGTH_LONG).show()
                } else {
                    VideoExporter.shareVideoGeneral(this, file)
                }
            }
        }

        btnReelWhatsApp.setOnClickListener {
            exportVideoAndProceed { file ->
                VideoExporter.shareVideoToWhatsApp(this, file)
            }
        }
    }

    private fun updateLikeUI() {
        if (isLiked) {
            ivReelLike.setImageResource(R.drawable.ic_heart_filled)
            ivReelLike.setColorFilter(Color.parseColor("#EF4444"))
        } else {
            ivReelLike.setImageResource(R.drawable.ic_heart_outline)
            ivReelLike.setColorFilter(Color.WHITE)
        }
        tvReelLikesCount.text = likesCount.formatCount()
    }

    private fun showPlayPauseIndicator(isPaused: Boolean) {
        ivReelPlayIndicator.setImageResource(
            if (isPaused) R.drawable.ic_video_play else R.drawable.ic_video_pause
        )
        ivReelPlayIndicator.visibility = View.VISIBLE
        ivReelPlayIndicator.alpha = 1f
        ivReelPlayIndicator.animate()
            .alpha(0f)
            .setDuration(600)
            .setStartDelay(400)
            .withEndAction { ivReelPlayIndicator.visibility = View.GONE }
            .start()
    }

    private fun exportVideoAndProceed(onComplete: (java.io.File) -> Unit) {
        reelMotionView.pause()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#0F172A"))
            gravity = android.view.Gravity.CENTER
        }
        val tvTitle = TextView(this).apply {
            text = "🎬 جاري تجهيز وتصدير الريل كفيديو MP4..."
            setTextColor(Color.WHITE)
            textSize = 15f
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
            textSize = 13f
            gravity = android.view.Gravity.CENTER
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
            config = reelMotionView.config,
            onProgress = { p ->
                progressBar.progress = p
                tvPercent.text = "$p%"
            },
            onSuccess = { file ->
                dialog.dismiss()
                onComplete(file)
            },
            onError = { e ->
                dialog.dismiss()
                Toast.makeText(this, "فشل تصدير الفيديو: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onResume() {
        super.onResume()
        reelMotionView.play()
    }

    override fun onPause() {
        super.onPause()
        reelMotionView.pause()
    }
}

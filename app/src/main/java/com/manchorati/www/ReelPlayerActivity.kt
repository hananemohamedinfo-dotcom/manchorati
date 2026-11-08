package com.manchorati.www

import androidx.core.content.ContextCompat

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ReelPlayerActivity : AppCompatActivity() {

    private lateinit var reelMotionView: VideoMotionView
    private lateinit var ivReelPlayIndicator: ImageView
    private lateinit var ivReelDoubleTapHeart: ImageView
    private lateinit var btnReelClose: View
    private lateinit var ivReelAuthorAvatar: ImageView
    private lateinit var tvReelAuthorName: TextView
    private lateinit var tvReelAuthorBadge: TextView
    private lateinit var tvReelAuthorUsername: TextView
    private lateinit var tvReelContent: TextView
    private lateinit var tvReelTag: TextView
    private lateinit var tvReelAudioTrack: TextView

    private lateinit var btnReelLike: View
    private lateinit var ivReelLike: ImageView
    private lateinit var tvReelLikesCount: TextView
    private lateinit var btnReelWhatsApp: View
    private lateinit var btnReelExportMp4: View
    private lateinit var btnReelShareGeneral: View

    private var postId = ""
    private var postContent = ""
    private var postAuthorName = ""
    private var postAuthorUsername = ""
    private var postAuthorBadge = ""
    private var postAuthorPhotoUrl = ""
    private var postTag = ""
    private var bgMotionId = "STARS"
    private var audioPreset = "piano"
    private var animStyle = "typewriter"
    private var videoRatio = "9:16"
    private var fontId = "tajawal"
    private var textColor = "#FFFFFF"

    private var likesCount = 0L
    private var isLiked = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applyLayoutDirection(this)
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
        postAuthorPhotoUrl = intent.getStringExtra("EXTRA_AUTHOR_PHOTO_URL") ?: ""
        postTag = intent.getStringExtra("EXTRA_TAG") ?: ""
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
        ivReelDoubleTapHeart = findViewById(R.id.ivReelDoubleTapHeart)
        btnReelClose = findViewById(R.id.btnReelClose)
        ivReelAuthorAvatar = findViewById(R.id.ivReelAuthorAvatar)
        tvReelAuthorName = findViewById(R.id.tvReelAuthorName)
        tvReelAuthorBadge = findViewById(R.id.tvReelAuthorBadge)
        tvReelAuthorUsername = findViewById(R.id.tvReelAuthorUsername)
        tvReelContent = findViewById(R.id.tvReelContent)
        tvReelTag = findViewById(R.id.tvReelTag)
        tvReelAudioTrack = findViewById(R.id.tvReelAudioTrack)

        btnReelLike = findViewById(R.id.btnReelLike)
        ivReelLike = findViewById(R.id.ivReelLike)
        tvReelLikesCount = findViewById(R.id.tvReelLikesCount)
        btnReelWhatsApp = findViewById(R.id.btnReelWhatsApp)
        btnReelExportMp4 = findViewById(R.id.btnReelExportMp4)
        btnReelShareGeneral = findViewById(R.id.btnReelShareGeneral)

        // Bind Author Info
        tvReelAuthorName.text = if (postAuthorName.isNotEmpty()) postAuthorName else getString(R.string.app_name)
        val cleanHandle = postAuthorUsername.removePrefix("@").trim()
        tvReelAuthorUsername.text = if (cleanHandle.isNotEmpty()) "@$cleanHandle" else "@manchorati"
        tvReelContent.text = postContent

        // Avatar
        ivReelAuthorAvatar.loadUserAvatar(postAuthorPhotoUrl)

        // Author Badge
        if (postAuthorName == "منشوراتي" || postAuthorUsername.equals("manchorati", ignoreCase = true) || postAuthorBadge == "official" || postAuthorBadge.isEmpty()) {
            tvReelAuthorBadge.visibility = View.GONE
        } else {
            val badge = BadgeHelper.getBadgeForProfile(explicitBadge = postAuthorBadge, email = null)
            if (badge.iconResId != 0 && badge.id != BadgeHelper.BADGE_BEGINNER.id && badge.id != "official") {
                val badgeDrawable = ContextCompat.getDrawable(this, badge.iconResId)?.mutate()
                if (badgeDrawable != null) {
                    badgeDrawable.setTint(Color.parseColor(badge.textColorHex))
                    val sizePx = (14 * resources.displayMetrics.density).toInt()
                    badgeDrawable.setBounds(0, 0, sizePx, sizePx)
                    tvReelAuthorBadge.setCompoundDrawables(badgeDrawable, null, null, null)
                    tvReelAuthorBadge.text = ""
                    tvReelAuthorBadge.visibility = View.VISIBLE
                } else {
                    tvReelAuthorBadge.visibility = View.GONE
                }
            } else {
                tvReelAuthorBadge.visibility = View.GONE
            }
        }

        // Tag
        if (postTag.isNotEmpty()) {
            tvReelTag.visibility = View.VISIBLE
            tvReelTag.text = if (postTag.startsWith("#")) postTag else "#$postTag"
        } else {
            tvReelTag.visibility = View.GONE
        }

        // Audio preset label
        val audioLabel = when (audioPreset.lowercase()) {
            "piano" -> "عزف بيانو هادئ"
            "rain" -> "صوت المطر والسكينة"
            "oud" -> "تقاسيم عود شرقي"
            "nature" -> "طبيعة وأجواء هادئة"
            "mute" -> "فيديو صامت"
            else -> "موسيقى تصويرية"
        }
        tvReelAudioTrack.text = audioLabel

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

        // Gesture detector for single-tap (pause/play) and double-tap (like)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                reelMotionView.togglePlay()
                showPlayPauseIndicator(!reelMotionView.isPlaying)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                showDoubleTapHeartBurst()
                val uid = AuthManager.currentUser?.uid ?: ""
                if (uid.isNotEmpty()) {
                    if (!isLiked) {
                        isLiked = true
                        likesCount++
                        updateLikeUI()
                        if (postId.isNotEmpty()) {
                            FirestoreManager.toggleLike(postId, uid, true) {}
                        }
                    }
                } else {
                    Toast.makeText(this@ReelPlayerActivity, getString(R.string.login_required_like), Toast.LENGTH_SHORT).show()
                }
                return true
            }
        })

        reelMotionView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        btnReelLike.setOnClickListener {
            val uid = AuthManager.currentUser?.uid ?: ""
            if (uid.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_required_like), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isLiked = !isLiked
            likesCount = if (isLiked) likesCount + 1 else maxOf(0L, likesCount - 1)
            updateLikeUI()

            // Micro bounce animation
            ivReelLike.scaleX = 0.6f
            ivReelLike.scaleY = 0.6f
            ivReelLike.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(160)
                .withEndAction {
                    ivReelLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()

            if (postId.isNotEmpty()) {
                FirestoreManager.toggleLike(postId, uid, isLiked) {}
            }
        }

        btnReelExportMp4.setOnClickListener {
            exportVideoAndProceed { file ->
                val uri = VideoExporter.saveVideoToGallery(this, file, postAuthorName)
                if (uri != null) {
                    Toast.makeText(this, getString(R.string.reel_saved_gallery_success), Toast.LENGTH_LONG).show()
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

        btnReelShareGeneral.setOnClickListener {
            exportVideoAndProceed { file ->
                VideoExporter.shareVideoGeneral(this, file)
            }
        }

        val btnReelEditInStudio: View? = findViewById(R.id.btnReelEditInStudio)
        btnReelEditInStudio?.setOnClickListener {
            val vIntent = android.content.Intent(this, VideoStudioActivity::class.java).apply {
                putExtra("EXTRA_CONTENT", postContent)
                putExtra("EXTRA_AUTHOR_NAME", postAuthorName)
                putExtra("EXTRA_AUTHOR_USERNAME", postAuthorUsername)
                putExtra("EXTRA_AUTHOR_BADGE", postAuthorBadge)
                putExtra("EXTRA_TAG", postTag)
                putExtra("EXTRA_BG_MOTION_ID", bgMotionId)
                putExtra("EXTRA_AUDIO_PRESET", audioPreset)
                putExtra("EXTRA_ANIM_STYLE", animStyle)
                putExtra("EXTRA_VIDEO_RATIO", videoRatio)
                putExtra("EXTRA_FONT_ID", fontId)
                putExtra("EXTRA_TEXT_COLOR", textColor)
            }
            startActivity(vIntent)
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

    private fun showDoubleTapHeartBurst() {
        ivReelDoubleTapHeart.visibility = View.VISIBLE
        ivReelDoubleTapHeart.alpha = 1f
        ivReelDoubleTapHeart.scaleX = 0.2f
        ivReelDoubleTapHeart.scaleY = 0.2f
        ivReelDoubleTapHeart.animate()
            .scaleX(1.35f)
            .scaleY(1.35f)
            .setDuration(180)
            .withEndAction {
                ivReelDoubleTapHeart.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .withEndAction {
                        ivReelDoubleTapHeart.animate()
                            .alpha(0f)
                            .scaleX(0.7f)
                            .scaleY(0.7f)
                            .setDuration(220)
                            .setStartDelay(200)
                            .withEndAction {
                                ivReelDoubleTapHeart.visibility = View.GONE
                            }.start()
                    }.start()
            }.start()
    }

    private fun exportVideoAndProceed(onComplete: (java.io.File) -> Unit) {
        reelMotionView.pause()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val density = resources.displayMetrics.density
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((28 * density).toInt(), (24 * density).toInt(), (28 * density).toInt(), (24 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E293B"))
                cornerRadius = 24f * density
                setStroke((1f * density).toInt(), Color.parseColor("#334155"))
            }
            gravity = android.view.Gravity.CENTER
        }

        val tvTitle = TextView(this).apply {
            text = getString(R.string.reel_exporting_progress)
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            val params = LinearLayout.LayoutParams(
                (240 * density).toInt(),
                (8 * density).toInt()
            ).apply {
                topMargin = (20 * density).toInt()
                bottomMargin = (14 * density).toInt()
            }
            layoutParams = params
        }

        val tvPercent = TextView(this).apply {
            text = "0%"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 13.5f
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
                Toast.makeText(this, getString(R.string.reel_export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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

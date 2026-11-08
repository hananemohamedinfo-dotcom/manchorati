package com.manchorati.www

import androidx.core.content.ContextCompat

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class ReelsAdapter(
    private val context: Context,
    private val reels: MutableList<CommunityPost>
) : RecyclerView.Adapter<ReelsAdapter.ReelViewHolder>() {

    private val activeHolders = mutableMapOf<Int, ReelViewHolder>()

    fun getViewHolderForPosition(position: Int): ReelViewHolder? {
        return activeHolders[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel_page, parent, false)
        return ReelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelViewHolder, position: Int) {
        val post = reels[position]
        holder.bind(post, position)
    }

    override fun onViewAttachedToWindow(holder: ReelViewHolder) {
        super.onViewAttachedToWindow(holder)
        activeHolders[holder.bindingAdapterPosition] = holder
    }

    override fun onViewDetachedFromWindow(holder: ReelViewHolder) {
        super.onViewDetachedFromWindow(holder)
        activeHolders.remove(holder.bindingAdapterPosition)
        holder.pause()
    }

    override fun getItemCount(): Int = reels.size

    inner class ReelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reelMotionView: VideoMotionView = itemView.findViewById(R.id.reelMotionView)
        private val ivReelPlayIndicator: ImageView = itemView.findViewById(R.id.ivReelPlayIndicator)
        private val ivReelDoubleTapHeart: ImageView = itemView.findViewById(R.id.ivReelDoubleTapHeart)
        private val ivReelAuthorAvatar: ImageView = itemView.findViewById(R.id.ivReelAuthorAvatar)
        private val tvReelAuthorName: TextView = itemView.findViewById(R.id.tvReelAuthorName)
        private val tvReelAuthorBadge: TextView = itemView.findViewById(R.id.tvReelAuthorBadge)
        private val tvReelAuthorUsername: TextView = itemView.findViewById(R.id.tvReelAuthorUsername)
        private val tvReelContent: TextView = itemView.findViewById(R.id.tvReelContent)
        private val tvReelTag: TextView = itemView.findViewById(R.id.tvReelTag)
        private val tvReelAudioTrack: TextView = itemView.findViewById(R.id.tvReelAudioTrack)

        private val btnReelLike: View = itemView.findViewById(R.id.btnReelLike)
        private val ivReelLike: ImageView = itemView.findViewById(R.id.ivReelLike)
        private val tvReelLikesCount: TextView = itemView.findViewById(R.id.tvReelLikesCount)
        private val btnReelWhatsApp: View = itemView.findViewById(R.id.btnReelWhatsApp)
        private val btnReelExportMp4: View = itemView.findViewById(R.id.btnReelExportMp4)
        private val btnReelShareGeneral: View = itemView.findViewById(R.id.btnReelShareGeneral)
        private val btnReelEditInStudio: View? = itemView.findViewById(R.id.btnReelEditInStudio)

        private var currentPost: CommunityPost? = null
        private var isLiked = false
        private var likesCount = 0L

        fun bind(post: CommunityPost, position: Int) {
            currentPost = post
            likesCount = post.likesCount
            val currentUid = AuthManager.currentUser?.uid ?: ""
            isLiked = currentUid.isNotEmpty() && post.likedBy.contains(currentUid)

            // Author Info
            tvReelAuthorName.text = if (post.authorName.isNotEmpty()) post.authorName else context.getString(R.string.app_name)
            val cleanHandle = post.authorUsername.removePrefix("@").trim()
            tvReelAuthorUsername.text = if (cleanHandle.isNotEmpty()) "@$cleanHandle" else "@manchorati"
            tvReelContent.text = post.content

            // Avatar
            ivReelAuthorAvatar.loadUserAvatar(post.authorPhotoUrl)

            // Author Badge
            if (post.authorName == "منشوراتي" || post.authorUsername.equals("manchorati", ignoreCase = true) || post.authorBadge == "official" || post.authorBadge.isEmpty()) {
                tvReelAuthorBadge.visibility = View.GONE
            } else {
                val badge = BadgeHelper.getBadgeForProfile(explicitBadge = post.authorBadge, email = null)
                if (badge.iconEmoji.isNotEmpty() && badge.id != BadgeHelper.BADGE_BEGINNER.id && badge.id != "official") {
                    tvReelAuthorBadge.visibility = View.VISIBLE
                    tvReelAuthorBadge.text = badge.iconEmoji
                } else {
                    tvReelAuthorBadge.visibility = View.GONE
                }
            }

            // Tag
            if (post.tag.isNotEmpty()) {
                tvReelTag.visibility = View.VISIBLE
                tvReelTag.text = if (post.tag.startsWith("#")) post.tag else "#${post.tag}"
            } else {
                tvReelTag.visibility = View.GONE
            }

            // Audio Preset Label
            val audioKey = post.audioPreset.ifEmpty { "piano" }
            val audioLabel = when (audioKey.lowercase()) {
                "piano" -> "عزف بيانو هادئ"
                "rain" -> "صوت المطر والسكينة"
                "oud" -> "تقاسيم عود شرقي"
                "nature" -> "طبيعة وأجواء هادئة"
                "mute" -> "فيديو صامت"
                else -> "موسيقى تصويرية"
            }
            tvReelAudioTrack.text = audioLabel

            updateLikeUI()

            // Video Motion Config
            val bgMotion = post.bgMotionId.ifEmpty {
                val bgs = listOf("STARS", "AURORA", "GOLD", "OCEAN", "RAIN")
                bgs[position % bgs.size]
            }
            val anim = post.animStyle.ifEmpty { "typewriter" }

            val config = VideoMotionRenderer.VideoConfig(
                content = post.content,
                authorName = post.authorName,
                authorUsername = post.authorUsername,
                authorBadge = post.authorBadge,
                tag = post.tag,
                bgMotionId = bgMotion,
                audioPreset = audioKey,
                animStyle = anim,
                aspectRatio = "9:16",
                durationSec = 10,
                fontId = post.fontId.ifEmpty { "tajawal" },
                textColor = post.textColor.ifEmpty { "#FFFFFF" }
            )
            reelMotionView.config = config

            setupListeners(post)
        }

        private fun setupListeners(post: CommunityPost) {
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
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
                            if (post.id.isNotEmpty()) {
                                FirestoreManager.toggleLike(post.id, uid, true) {}
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.login_required_like), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, context.getString(R.string.login_required_like), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                isLiked = !isLiked
                likesCount = if (isLiked) likesCount + 1 else maxOf(0L, likesCount - 1)
                updateLikeUI()

                ivReelLike.scaleX = 0.6f
                ivReelLike.scaleY = 0.6f
                ivReelLike.animate().scaleX(1.25f).scaleY(1.25f).setDuration(160).withEndAction {
                    ivReelLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()

                if (post.id.isNotEmpty()) {
                    FirestoreManager.toggleLike(post.id, uid, isLiked) {}
                }
            }

            btnReelExportMp4.setOnClickListener {
                exportVideoAndProceed { file ->
                    val uri = VideoExporter.saveVideoToGallery(context, file, post.authorName)
                    if (uri != null) {
                        Toast.makeText(context, context.getString(R.string.reel_saved_gallery_success), Toast.LENGTH_LONG).show()
                    } else {
                        VideoExporter.shareVideoGeneral(context, file)
                    }
                }
            }

            btnReelWhatsApp.setOnClickListener {
                exportVideoAndProceed { file ->
                    VideoExporter.shareVideoToWhatsApp(context, file)
                }
            }

            btnReelShareGeneral.setOnClickListener {
                exportVideoAndProceed { file ->
                    VideoExporter.shareVideoGeneral(context, file)
                }
            }

            btnReelEditInStudio?.setOnClickListener {
                val vIntent = Intent(context, VideoStudioActivity::class.java).apply {
                    putExtra("EXTRA_CONTENT", post.content)
                    putExtra("EXTRA_AUTHOR_NAME", post.authorName)
                    putExtra("EXTRA_AUTHOR_USERNAME", post.authorUsername)
                    putExtra("EXTRA_AUTHOR_BADGE", post.authorBadge)
                    putExtra("EXTRA_TAG", post.tag)
                    putExtra("EXTRA_BG_MOTION_ID", reelMotionView.config.bgMotionId)
                    putExtra("EXTRA_AUDIO_PRESET", reelMotionView.config.audioPreset)
                    putExtra("EXTRA_ANIM_STYLE", reelMotionView.config.animStyle)
                    putExtra("EXTRA_VIDEO_RATIO", "9:16")
                    putExtra("EXTRA_FONT_ID", reelMotionView.config.fontId)
                    putExtra("EXTRA_TEXT_COLOR", reelMotionView.config.textColor)
                }
                context.startActivity(vIntent)
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

            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val density = context.resources.displayMetrics.density
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((28 * density).toInt(), (24 * density).toInt(), (28 * density).toInt(), (24 * density).toInt())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = 24f * density
                    setStroke((1f * density).toInt(), Color.parseColor("#334155"))
                }
                gravity = android.view.Gravity.CENTER
            }

            val tvTitle = TextView(context).apply {
                text = context.getString(R.string.reel_exporting_progress)
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = android.view.Gravity.CENTER
            }

            val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
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

            val tvPercent = TextView(context).apply {
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
                context = context,
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
                    Toast.makeText(context, context.getString(R.string.reel_export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            )
        }

        fun play() {
            reelMotionView.play()
        }

        fun pause() {
            reelMotionView.pause()
        }
    }
}

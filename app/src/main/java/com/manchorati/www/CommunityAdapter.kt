package com.manchorati.www

import androidx.core.content.ContextCompat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class CommunityAdapter(
    private var posts: List<CommunityPost>,
    private var currentUserId: String,
    private val onLikeClicked: (post: CommunityPost, isLiked: Boolean) -> Unit,
    private val onCommentClicked: (post: CommunityPost) -> Unit,
    private val onAuthorClicked: (authorId: String, authorName: String) -> Unit,
    private val onEditClicked: (post: CommunityPost) -> Unit,
    private val onDeleteClicked: (post: CommunityPost) -> Unit,
    private val onReportClicked: (post: CommunityPost) -> Unit,
    private val onTagClicked: ((tag: String) -> Unit)? = null
) : RecyclerView.Adapter<CommunityAdapter.PostViewHolder>() {

    private var postsList: MutableList<CommunityPost> = posts.toMutableList()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return if (position in postsList.indices) {
            postsList[position].id.hashCode().toLong()
        } else {
            RecyclerView.NO_ID
        }
    }

    private class PostDiffCallback(
        private val oldList: List<CommunityPost>,
        private val newList: List<CommunityPost>
    ) : androidx.recyclerview.widget.DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.likesCount == new.likesCount &&
                    old.commentsCount == new.commentsCount &&
                    old.likedBy == new.likedBy &&
                    old.content == new.content &&
                    old.authorBadge == new.authorBadge &&
                    old.bgId == new.bgId &&
                    old.fontId == new.fontId
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return "PAYLOAD_STATS"
        }
    }

    fun getEffectiveUserId(): String {
        return if (currentUserId.isNotEmpty()) currentUserId else (AuthManager.currentUser?.uid ?: "")
    }

    fun updateData(newPosts: List<CommunityPost>) {
        val diffCallback = PostDiffCallback(postsList, newPosts)
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        postsList = newPosts.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }

    fun updatePostStats(updatedPost: CommunityPost) {
        val index = postsList.indexOfFirst { it.id == updatedPost.id }
        if (index != -1) {
            val oldPost = postsList[index]
            if (oldPost.likesCount != updatedPost.likesCount ||
                oldPost.commentsCount != updatedPost.commentsCount ||
                oldPost.likedBy != updatedPost.likedBy) {
                postsList[index] = oldPost.copy(
                    likesCount = updatedPost.likesCount,
                    commentsCount = updatedPost.commentsCount,
                    likedBy = updatedPost.likedBy
                )
                notifyItemChanged(index, "PAYLOAD_STATS")
            }
        }
    }

    fun appendPosts(morePosts: List<CommunityPost>) {
        if (morePosts.isEmpty()) return
        val existingIds = postsList.map { it.id }.toSet()
        val uniqueNew = morePosts.filterNot { existingIds.contains(it.id) }
        if (uniqueNew.isEmpty()) return
        val startPos = postsList.size
        postsList.addAll(uniqueNew)
        notifyItemRangeInserted(startPos, uniqueNew.size)
    }

    fun insertPostAtTop(post: CommunityPost) {
        val index = postsList.indexOfFirst { it.id == post.id }
        if (index == -1) {
            postsList.add(0, post)
            notifyItemInserted(0)
        } else {
            postsList[index] = post
            notifyItemChanged(index)
        }
    }

    fun removePost(postId: String) {
        val index = postsList.indexOfFirst { it.id == postId }
        if (index != -1) {
            postsList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updateCurrentUserId(newUid: String) {
        currentUserId = newUid
        notifyDataSetChanged()
    }

    fun getPosts(): List<CommunityPost> = postsList

    private var highlightedPostId: String? = null

    fun highlightPost(postId: String) {
        highlightedPostId = postId
        val index = postsList.indexOfFirst { it.id == postId }
        if (index != -1) {
            notifyItemChanged(index)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (highlightedPostId == postId) {
                    highlightedPostId = null
                    notifyItemChanged(index)
                }
            }, 3000)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postsList[position])
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            holder.bindStatsOnly(postsList[position])
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = postsList.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutAuthorProfile: LinearLayout = itemView.findViewById(R.id.layoutAuthorProfile)
        private val ivAuthorAvatar: ImageView = itemView.findViewById(R.id.ivAuthorAvatar)
        private val ivAuthorLaurelWreath: ImageView? = itemView.findViewById(R.id.ivAuthorLaurelWreath)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvAuthorBadge: TextView = itemView.findViewById(R.id.tvAuthorBadge)
        private val tvAuthorStreak: TextView? = itemView.findViewById(R.id.tvAuthorStreak)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvPostUsername)
        private val layoutPostTagContainer: View? = itemView.findViewById(R.id.layoutPostTagContainer)
        private val tvPostTag: TextView? = itemView.findViewById(R.id.tvPostTag)
        private val btnPostMenu: ImageView = itemView.findViewById(R.id.btnPostMenu)

        private val badgeVideoReel: LinearLayout = itemView.findViewById(R.id.badgeVideoReel)
        private val btnCardPlayVideo: FrameLayout = itemView.findViewById(R.id.btnCardPlayVideo)
        private val layoutPostCard: FrameLayout = itemView.findViewById(R.id.layoutPostCard)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        private val ivDoubleTapHeart: ImageView = itemView.findViewById(R.id.ivDoubleTapHeart)

        private val btnLikeContainer: LinearLayout = itemView.findViewById(R.id.btnLikeContainer)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tvLikesCount)

        private val btnCommentContainer: LinearLayout = itemView.findViewById(R.id.btnCommentContainer)
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val tvCommentsCount: TextView = itemView.findViewById(R.id.tvCommentsCount)

        private val btnDownloadPostContainer: LinearLayout = itemView.findViewById(R.id.btnDownloadPostContainer)
        private val btnShareStoryContainer: LinearLayout = itemView.findViewById(R.id.btnShareStoryContainer)
        private val btnBookmarkContainer: LinearLayout = itemView.findViewById(R.id.btnBookmarkContainer)
        private val ivBookmark: ImageView = itemView.findViewById(R.id.ivBookmark)

        private var lastCardClickTime: Long = 0L

        private var boundPost: CommunityPost? = null

        fun getCurrentPost(): CommunityPost {
            val pos = bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION && pos in postsList.indices) {
                return postsList[pos]
            }
            val fallbackId = itemView.tag as? String
            if (!fallbackId.isNullOrEmpty()) {
                val found = postsList.find { it.id == fallbackId }
                if (found != null) return found
            }
            return boundPost ?: postsList.firstOrNull() ?: CommunityPost()
        }

        fun bindStatsOnly(post: CommunityPost) {
            boundPost = post
            val currentPost = getCurrentPost()
            tvLikesCount.text = currentPost.likesCount.formatCount()
            tvCommentsCount.text = currentPost.commentsCount.formatCount()
            val effectiveUid = getEffectiveUserId()
            val isLiked = effectiveUid.isNotEmpty() && currentPost.likedBy.contains(effectiveUid)
            updateLikeUI(isLiked, currentPost.likesCount)
        }

        fun bind(post: CommunityPost) {
            boundPost = post
            itemView.tag = post.id

            val isHighlighted = post.id == highlightedPostId
            if (isHighlighted) {
                itemView.animate().scaleX(1.03f).scaleY(1.03f).setDuration(250).withEndAction {
                    itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(250).start()
                }.start()
            }
            tvAuthor.text = post.authorName
            val cleanAuthorHandle = post.authorUsername.removePrefix("@").trim()
            if (cleanAuthorHandle.isNotEmpty()) {
                tvUsername.visibility = View.VISIBLE
                tvUsername.text = "@$cleanAuthorHandle"
            } else {
                tvUsername.text = itemView.context.getString(R.string.user_default_handle)
            }
            tvContent.text = post.content

            val isVideo = post.postType.equals("VIDEO", ignoreCase = true) || post.bgMotionId.isNotEmpty()
            if (isVideo) {
                badgeVideoReel.visibility = View.VISIBLE
                btnCardPlayVideo.visibility = View.VISIBLE
                btnCardPlayVideo.setOnClickListener {
                    openVideoStudio(itemView.context, getCurrentPost())
                }
            } else {
                badgeVideoReel.visibility = View.GONE
                btnCardPlayVideo.visibility = View.GONE
            }


            // 1. شارة تصنيف ورتبة الكاتب (تظهر دائماً بجانب اسمه)
            val initialBadge = BadgeHelper.getBadgeForPost(post)
            bindAuthorBadge(initialBadge, post)

            // 2. شارة سلسلة الأيام والتفاعل
            bindAuthorStreak(post)

            // جلب وتحديث رتبة الكاتب الحية من قاعدة البيانات في الخلفية
            BadgeHelper.fetchAndCacheAuthorBadge(post.authorId) { liveBadge ->
                if (itemView.tag == post.id) {
                    bindAuthorBadge(liveBadge, post)
                }
            }

            // تحميل الصورة الشخصية أو الأفاتار
            // فحص ما إذا كان صاحب المنشور من متصدري الأسبوع (المتوجين بإكليل الغار الذهبي أو الفضي أو البرونزي)
            val champRank = WriterOfTheWeekHelper.getChampionRank(itemView.context, post.authorId)
            if (champRank in 1..3) {
                ivAuthorLaurelWreath?.visibility = View.VISIBLE
                val wreathColor = when (champRank) {
                    1 -> Color.parseColor("#F59E0B") // ذهبي
                    2 -> Color.parseColor("#CBD5E1") // فضي
                    else -> Color.parseColor("#CD7F32") // برونزي
                }
                ivAuthorLaurelWreath?.setColorFilter(wreathColor)
                ivAuthorLaurelWreath?.setOnClickListener {
                    ivAuthorLaurelWreath?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    WriterOfTheWeekHelper.showRankCelebrationDialog(itemView.context, champRank, post.authorName, isSelf = false)
                }
            } else {
                ivAuthorLaurelWreath?.visibility = View.GONE
                ivAuthorLaurelWreath?.setOnClickListener(null)
            }

            ivAuthorAvatar.loadUserAvatar(post.authorPhotoUrl)

            // وسم الهاشتاغ التفاعلي أسفل البطاقة
            if (!post.tag.isNullOrBlank()) {
                val rawTag = post.tag.trim()
                val cleanTag = rawTag.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
                val hashtagText = if (cleanTag.isNotEmpty()) {
                    "#" + cleanTag.replace("\\s+".toRegex(), "_")
                } else {
                    "#$rawTag"
                }
                tvPostTag?.text = hashtagText
                tvPostTag?.visibility = View.VISIBLE
                layoutPostTagContainer?.visibility = View.VISIBLE

                tvPostTag?.setOnClickListener {
                    tvPostTag.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    onTagClicked?.invoke(rawTag)
                }
            } else {
                tvPostTag?.visibility = View.GONE
                layoutPostTagContainer?.visibility = View.GONE
                tvPostTag?.setOnClickListener(null)
            }

            // تلوين بطاقة المنشور والخط
            ThemeHelper.applyBackground(layoutPostCard, post.bgId, 12f)
            tvContent.typeface = ThemeHelper.getFontTypeface(itemView.context, post.fontId)
            val template = ThemeHelper.getCardTemplate(post.bgId)
            val effectiveTextColor = if (template != null) {
                template.textColor
            } else if (!post.textColor.isNullOrEmpty()) {
                try {
                    Color.parseColor(post.textColor)
                } catch (e: Exception) {
                    ThemeHelper.getTextColor(post.bgId)
                }
            } else {
                ThemeHelper.getTextColor(post.bgId)
            }
            tvContent.setTextColor(effectiveTextColor)

            // العدادات السفلية بصيغة K / M
            tvLikesCount.text = post.likesCount.formatCount()
            tvCommentsCount.text = post.commentsCount.formatCount()

            val effectiveUid = getEffectiveUserId()

            // حالة الإعجاب
            val isLiked = effectiveUid.isNotEmpty() && post.likedBy.contains(effectiveUid)
            updateLikeUI(isLiked, post.likesCount)

            // دالة الإعجاب الموحدة (تقرأ المنشور الحالي دائماً لضمان التبديل التام بين الإعجاب وإلغائه)
            fun performLikeToggle() {
                val currentPost = getCurrentPost()
                val currentUid = getEffectiveUserId()
                val isVirtual = currentPost.id.startsWith("curated_") || currentPost.id.startsWith("vpost_")

                if (currentUid.isEmpty()) {
                    if (isVirtual) {
                        val guestUid = "local_guest"
                        val currentlyLiked = currentPost.likedBy.contains(guestUid)
                        val willBeLiked = !currentlyLiked
                        val updatedLikedBy = if (willBeLiked) listOf(guestUid) else emptyList<String>()
                        val updatedLikesCount = if (willBeLiked) currentPost.likesCount + 1 else maxOf(0L, currentPost.likesCount - 1)
                        val updatedPost = currentPost.copy(
                            likedBy = updatedLikedBy,
                            likesCount = updatedLikesCount
                        )
                        val pos = bindingAdapterPosition
                        if (pos != RecyclerView.NO_POSITION && pos < postsList.size) {
                            postsList[pos] = updatedPost
                        }
                        updateLikeUI(willBeLiked, updatedLikesCount)
                        btnLike.animate()
                            .scaleX(1.3f)
                            .scaleY(1.3f)
                            .setDuration(120)
                            .withEndAction {
                                btnLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                            }.start()
                        onLikeClicked(updatedPost, currentlyLiked)
                        return
                    }
                    onLikeClicked(currentPost, false)
                    return
                }

                val currentlyLiked = currentPost.likedBy.contains(currentUid)
                val willBeLiked = !currentlyLiked

                val updatedLikedBy = currentPost.likedBy.toMutableList().apply {
                    if (willBeLiked) {
                        if (!contains(currentUid)) add(currentUid)
                    } else {
                        remove(currentUid)
                    }
                }
                val updatedLikesCount = if (willBeLiked) currentPost.likesCount + 1 else maxOf(0L, currentPost.likesCount - 1)
                val updatedPost = currentPost.copy(
                    likedBy = updatedLikedBy,
                    likesCount = updatedLikesCount
                )

                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos < postsList.size) {
                    postsList[pos] = updatedPost
                }

                updateLikeUI(willBeLiked, updatedLikesCount)

                // أنيميشن نبض القلب الصغير في الشريط
                btnLike.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(120)
                    .withEndAction {
                        btnLike.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    }.start()

                onLikeClicked(updatedPost, currentlyLiked)
            }

            val likeClickListener = View.OnClickListener { performLikeToggle() }
            btnLikeContainer.setOnClickListener(likeClickListener)
            btnLike.setOnClickListener(likeClickListener)
            tvLikesCount.setOnClickListener(likeClickListener)

            // 2. النقر المزدوج على بطاقة المنشور (Double-Tap to Like مع اهتزاز وأنيميشن انفجار القلب)
            layoutPostCard.setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastCardClickTime < 350) {
                    // اهتزاز تكتيكي لمسي Haptic Feedback
                    itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                    // عرض أنيميشن انفجار القلب في وسط البطاقة
                    showDoubleTapHeartBurst()

                    // إذا لم يكن معجباً بالمنشور مسبقاً، نجعله معجباً تلقائياً
                    val currentP = getCurrentPost()
                    if (!currentP.likedBy.contains(getEffectiveUserId())) {
                        performLikeToggle()
                    }
                    lastCardClickTime = 0L
                } else {
                    lastCardClickTime = now
                    if (isVideo) {
                        layoutPostCard.postDelayed({
                            if (lastCardClickTime != 0L) {
                                openVideoStudio(itemView.context, getCurrentPost())
                            }
                        }, 360)
                    }
                }
            }



            // 3. التعليقات
            val commentClickListener = View.OnClickListener {
                onCommentClicked(getCurrentPost())
            }
            btnCommentContainer.setOnClickListener(commentClickListener)
            btnComment.setOnClickListener(commentClickListener)

            layoutAuthorProfile.setOnClickListener {
                onAuthorClicked(post.authorId, post.authorName)
            }

            // 4. زر تحميل صورة البطاقة في المعرض
            btnDownloadPostContainer.setOnClickListener {
                StoryShareHelper.savePostImageToGallery(itemView.context, post)
            }

            // 5. زر مشاركة كستوري 9:16 (صورة فقط) ونقرة مطولة لمشاركة واتساب مباشرة
            btnShareStoryContainer.setOnClickListener {
                StoryShareHelper.sharePostAsStory(itemView.context, post)
            }
            btnShareStoryContainer.setOnLongClickListener {
                StoryShareHelper.sharePostToWhatsApp(itemView.context, post)
                true
            }

            // 5. زر حفظ المنشور في المحفوظات (Bookmarks)
            val context = itemView.context
            var isSaved = BookmarkManager.isBookmarked(context, post.id)
            updateBookmarkUI(isSaved)

            btnBookmarkContainer.setOnClickListener {
                val newSavedState = BookmarkManager.toggleBookmark(context, post.id)
                updateBookmarkUI(newSavedState)
                ivBookmark.animate()
                    .scaleX(1.35f)
                    .scaleY(1.35f)
                    .setDuration(120)
                    .withEndAction {
                        ivBookmark.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    }.start()

                val msg = if (newSavedState) context.getString(R.string.post_action_bookmark) else context.getString(R.string.post_action_remove_bookmark)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }

            // 6. زر الخيارات (الثلاث نقاط الموحد الشامل)
            val isMyPost = effectiveUid.isNotEmpty() && post.authorId == effectiveUid
            btnPostMenu.setOnClickListener { view ->
                val ctx = view.context
                val popup = PopupMenu(ctx, view)
                if (isVideo) {
                    popup.menu.add(0, 15, 0, "فتح في الاستوديو")
                }
                popup.menu.add(0, 16, 1, "تحويل إلى فيديو / ستوري")
                popup.menu.add(0, 14, 2, "مشاركة في حالة واتساب")
                popup.menu.add(0, 13, 1, ctx.getString(R.string.download_post_image))
                popup.menu.add(0, 10, 2, ctx.getString(R.string.post_action_share_story))
                popup.menu.add(0, 11, 3, if (BookmarkManager.isBookmarked(ctx, post.id)) ctx.getString(R.string.post_action_remove_bookmark) else ctx.getString(R.string.post_action_bookmark))
                popup.menu.add(0, 12, 4, ctx.getString(R.string.post_action_copy))

                if (isMyPost) {
                    popup.menu.add(0, 1, 5, ctx.getString(R.string.post_action_edit))
                    popup.menu.add(0, 2, 6, ctx.getString(R.string.post_action_delete))
                } else {
                    popup.menu.add(0, 3, 7, ctx.getString(R.string.post_action_report))
                }

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        15 -> openVideoStudio(view.context, post)
                        16 -> {
                            val vIntent = Intent(view.context, VideoStudioActivity::class.java).apply {
                                putExtra("EXTRA_CONTENT", post.content)
                                putExtra("EXTRA_AUTHOR_NAME", post.authorName)
                                putExtra("EXTRA_AUTHOR_USERNAME", post.authorUsername)
                                putExtra("EXTRA_AUTHOR_BADGE", post.authorBadge)
                                putExtra("EXTRA_TAG", post.tag)
                                putExtra("EXTRA_FONT_ID", post.fontId)
                                putExtra("EXTRA_TEXT_COLOR", post.textColor)
                                if (post.postType == "VIDEO") {
                                    putExtra("EXTRA_BG_MOTION_ID", post.bgMotionId)
                                    putExtra("EXTRA_AUDIO_PRESET", post.audioPreset)
                                    putExtra("EXTRA_ANIM_STYLE", post.animStyle)
                                    putExtra("EXTRA_VIDEO_RATIO", post.videoRatio)
                                }
                            }
                            view.context.startActivity(vIntent)
                        }
                        14 -> StoryShareHelper.sharePostToWhatsApp(view.context, post)
                        13 -> StoryShareHelper.savePostImageToGallery(view.context, post)
                        10 -> StoryShareHelper.sharePostAsStory(view.context, post)
                        11 -> {
                            val saved = BookmarkManager.toggleBookmark(view.context, post.id)
                            updateBookmarkUI(saved)
                            val toastMsg = if (saved) ctx.getString(R.string.post_action_bookmark) else ctx.getString(R.string.post_action_remove_bookmark)
                            Toast.makeText(view.context, toastMsg, Toast.LENGTH_SHORT).show()
                        }
                        12 -> {
                            val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(ctx.getString(R.string.app_name), post.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(view.context, ctx.getString(R.string.post_copied_toast), Toast.LENGTH_SHORT).show()
                        }
                        1 -> onEditClicked(post)
                        2 -> onDeleteClicked(post)
                        3 -> onReportClicked(post)
                    }
                    true
                }
                popup.show()
            }
        }

        private fun bindAuthorBadge(badge: UserBadge, post: CommunityPost) {
            // فحص خصوصية المستخدم: هل اختار إظهار رتبته الأدبية؟
            if (!BadgeHelper.isBadgeVisibleForUser(post.authorId)) {
                tvAuthorBadge.visibility = View.GONE
                return
            }

            // إخفاء أي شارة لمنشورات تطبيق منشوراتي الرسمية
            if (post.authorId == "manchorati_curated" ||
                post.authorBadge == "official" ||
                post.authorName == "منشوراتي" ||
                post.authorUsername.equals("manchorati", ignoreCase = true) ||
                badge.id == "official") {
                tvAuthorBadge.visibility = View.GONE
                return
            }

            val emoji = badge.iconEmoji
            if (emoji.isNotEmpty()) {
                tvAuthorBadge.setCompoundDrawables(null, null, null, null)
                tvAuthorBadge.text = emoji
                tvAuthorBadge.visibility = View.VISIBLE
            } else {
                tvAuthorBadge.visibility = View.GONE
            }

            tvAuthorBadge.setOnClickListener {
                tvAuthorBadge.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                Toast.makeText(
                    itemView.context,
                    "${badge.iconEmoji} ${badge.getLocalizedTitle(itemView.context)}\n${badge.getLocalizedDescription(itemView.context)}",
                    Toast.LENGTH_SHORT
                ).show()
                onAuthorClicked(post.authorId, post.authorName)
            }
        }

        private fun bindAuthorStreak(post: CommunityPost) {
            val streak = post.authorStreak
            if (streak >= 1 && tvAuthorStreak != null) {
                val emoji = when {
                    streak >= 30 -> "🏆"
                    streak >= 15 -> "🌟"
                    streak >= 7 -> "🔥"
                    streak >= 3 -> "⚡"
                    else -> "✨"
                }
                tvAuthorStreak.text = "$emoji $streak"
                tvAuthorStreak.visibility = View.VISIBLE
                tvAuthorStreak.setOnClickListener {
                    tvAuthorStreak.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    Toast.makeText(
                        itemView.context,
                        "سلسلة التفاعل والإلهام: $streak يوم متواصل! $emoji",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                tvAuthorStreak?.visibility = View.GONE
                tvAuthorStreak?.setOnClickListener(null)
            }
        }

        private fun updateLikeUI(isLiked: Boolean, count: Long) {
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(Color.parseColor("#EF4444"))
                tvLikesCount.setTextColor(Color.parseColor("#EF4444"))
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(Color.parseColor("#64748B"))
                tvLikesCount.setTextColor(Color.parseColor("#64748B"))
            }
            tvLikesCount.text = count.formatCount()
        }

        private fun updateBookmarkUI(isBookmarked: Boolean) {
            if (isBookmarked) {
                ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                ivBookmark.setColorFilter(Color.parseColor("#F59E0B"))
            } else {
                ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                ivBookmark.setColorFilter(Color.parseColor("#64748B"))
            }
        }


        private fun openVideoStudio(context: Context, post: CommunityPost) {
            val intent = Intent(context, VideoStudioActivity::class.java).apply {
                putExtra("EXTRA_POST_ID", post.id)
                putExtra("EXTRA_CONTENT", post.content)
                putExtra("EXTRA_AUTHOR_NAME", post.authorName)
                putExtra("EXTRA_AUTHOR_USERNAME", post.authorUsername)
                putExtra("EXTRA_AUTHOR_BADGE", post.authorBadge)
                putExtra("EXTRA_AUTHOR_PHOTO_URL", post.authorPhotoUrl)
                putExtra("EXTRA_AUTHOR_ID", post.authorId)
                putExtra("EXTRA_TAG", post.tag)
                putExtra("EXTRA_BG_MOTION_ID", post.bgMotionId.ifEmpty { "STARS" })
                putExtra("EXTRA_AUDIO_PRESET", post.audioPreset.ifEmpty { "piano" })
                putExtra("EXTRA_ANIM_STYLE", post.animStyle.ifEmpty { "typewriter" })
                putExtra("EXTRA_VIDEO_RATIO", post.videoRatio.ifEmpty { "9:16" })
                putExtra("EXTRA_FONT_ID", post.fontId)
                putExtra("EXTRA_TEXT_COLOR", post.textColor)
                putExtra("EXTRA_LIKES_COUNT", post.likesCount)
                putStringArrayListExtra("EXTRA_LIKED_BY", ArrayList(post.likedBy))
            }
            context.startActivity(intent)
        }

        private fun showDoubleTapHeartBurst() {
            ivDoubleTapHeart.visibility = View.VISIBLE
            ivDoubleTapHeart.alpha = 1f
            ivDoubleTapHeart.scaleX = 0.2f
            ivDoubleTapHeart.scaleY = 0.2f
            ivDoubleTapHeart.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .setDuration(160)
                .withEndAction {
                    ivDoubleTapHeart.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(90)
                        .withEndAction {
                            ivDoubleTapHeart.animate()
                                .alpha(0f)
                                .scaleX(0.7f)
                                .scaleY(0.7f)
                                .setDuration(220)
                                .setStartDelay(200)
                                .withEndAction {
                                    ivDoubleTapHeart.visibility = View.GONE
                                }.start()
                        }.start()
                }.start()
        }
    }
}

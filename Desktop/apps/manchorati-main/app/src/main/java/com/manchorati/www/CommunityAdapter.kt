package com.manchorati.www

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
    private val onReportClicked: (post: CommunityPost) -> Unit
) : RecyclerView.Adapter<CommunityAdapter.PostViewHolder>() {

    private var postsList: MutableList<CommunityPost> = posts.toMutableList()

    fun getEffectiveUserId(): String {
        return if (currentUserId.isNotEmpty()) currentUserId else (AuthManager.currentUser?.uid ?: "")
    }

    fun updateData(newPosts: List<CommunityPost>) {
        postsList = newPosts.toMutableList()
        notifyDataSetChanged()
    }

    fun updateCurrentUserId(newUid: String) {
        currentUserId = newUid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(postsList[position])
    }

    override fun getItemCount(): Int = postsList.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutAuthorProfile: LinearLayout = itemView.findViewById(R.id.layoutAuthorProfile)
        private val ivAuthorAvatar: ImageView = itemView.findViewById(R.id.ivAuthorAvatar)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvAuthorBadge: TextView = itemView.findViewById(R.id.tvAuthorBadge)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvPostUsername)
        private val tvPostTag: TextView = itemView.findViewById(R.id.tvPostTag)
        private val btnPostMenu: ImageView = itemView.findViewById(R.id.btnPostMenu)

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

        fun bind(post: CommunityPost) {
            itemView.tag = post.id
            tvAuthor.text = post.authorName
            tvUsername.text = "@${post.authorUsername}"
            tvContent.text = post.content

            // 1. شارة تصنيف ورتبة الكاتب (تظهر دائماً بجانب اسمه)
            val initialBadge = BadgeHelper.getBadgeForPost(post)
            bindAuthorBadge(initialBadge, post)

            // جلب وتحديث رتبة الكاتب الحية من قاعدة البيانات في الخلفية
            BadgeHelper.fetchAndCacheAuthorBadge(post.authorId) { liveBadge ->
                if (itemView.tag == post.id) {
                    bindAuthorBadge(liveBadge, post)
                }
            }

            // تحميل الصورة الشخصية أو الأفاتار
            ivAuthorAvatar.loadUserAvatar(post.authorPhotoUrl)

            // شارة التصنيف / الوسم
            if (!post.tag.isNullOrEmpty()) {
                tvPostTag.visibility = View.VISIBLE
                val cleanTag = post.tag.replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "").trim()
                tvPostTag.text = cleanTag.ifEmpty { post.tag }
            } else {
                tvPostTag.visibility = View.GONE
            }

            // تلوين بطاقة المنشور والخط
            ThemeHelper.applyBackground(layoutPostCard, post.bgId, 12f)
            tvContent.typeface = ThemeHelper.getFontTypeface(itemView.context, post.fontId)
            if (!post.textColor.isNullOrEmpty()) {
                try {
                    tvContent.setTextColor(Color.parseColor(post.textColor))
                } catch (e: Exception) {
                    tvContent.setTextColor(ThemeHelper.getTextColor(post.bgId))
                }
            } else {
                tvContent.setTextColor(ThemeHelper.getTextColor(post.bgId))
            }

            // العدادات السفلية بصيغة K / M
            tvLikesCount.text = post.likesCount.formatCount()
            tvCommentsCount.text = post.commentsCount.formatCount()

            val effectiveUid = getEffectiveUserId()

            // حالة الإعجاب
            val isLiked = effectiveUid.isNotEmpty() && post.likedBy.contains(effectiveUid)
            updateLikeUI(isLiked, post.likesCount)

            // دالة الإعجاب الموحدة
            fun performLikeToggle() {
                val currentUid = getEffectiveUserId()
                if (currentUid.isEmpty()) {
                    onLikeClicked(post, false)
                    return
                }

                val currentlyLiked = post.likedBy.contains(currentUid)
                val willBeLiked = !currentlyLiked

                val updatedLikedBy = post.likedBy.toMutableList().apply {
                    if (willBeLiked) {
                        if (!contains(currentUid)) add(currentUid)
                    } else {
                        remove(currentUid)
                    }
                }
                val updatedLikesCount = if (willBeLiked) post.likesCount + 1 else maxOf(0L, post.likesCount - 1)
                val updatedPost = post.copy(
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

                onLikeClicked(post, currentlyLiked)
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
                    if (!post.likedBy.contains(getEffectiveUserId())) {
                        performLikeToggle()
                    }
                    lastCardClickTime = 0L
                } else {
                    lastCardClickTime = now
                }
            }

            // 3. التعليقات
            val commentClickListener = View.OnClickListener {
                onCommentClicked(post)
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
                popup.menu.add(0, 14, 0, "مشاركة في حالة واتساب 🟢")
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
            // إخفاء أي شارة أو علامة ذهبية لمنشورات تطبيق "منشوراتي" الرسمية بناءً على طلب المستخدم
            if (post.authorId == "manchorati_curated" ||
                post.authorBadge == "official" ||
                post.authorName == "منشوراتي" ||
                post.authorUsername.equals("manchorati", ignoreCase = true) ||
                badge.id == "official" ||
                badge.iconEmoji.isEmpty() ||
                badge.id == BadgeHelper.BADGE_BEGINNER.id) {
                tvAuthorBadge.visibility = View.GONE
                return
            }

            tvAuthorBadge.visibility = View.VISIBLE
            tvAuthorBadge.text = badge.iconEmoji
            tvAuthorBadge.background = null
            tvAuthorBadge.setPadding(0, 0, 0, 0)
            tvAuthorBadge.setOnClickListener {
                Toast.makeText(
                    itemView.context,
                    "${badge.iconEmoji} ${badge.title}\n${badge.description}",
                    Toast.LENGTH_SHORT
                ).show()
                onAuthorClicked(post.authorId, post.authorName)
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

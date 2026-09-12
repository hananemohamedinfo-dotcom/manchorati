package com.manchorati.www

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
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

    fun updateData(newPosts: List<CommunityPost>) {
        posts = newPosts
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
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutAuthorProfile: LinearLayout = itemView.findViewById(R.id.layoutAuthorProfile)
        private val ivAuthorAvatar: ImageView = itemView.findViewById(R.id.ivAuthorAvatar) // تم تغييرها إلى ImageView لدعم صور جوجل والأفاتارات
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvPostUsername)
        private val btnPostMenu: ImageView = itemView.findViewById(R.id.btnPostMenu)

        private val layoutPostCard: FrameLayout = itemView.findViewById(R.id.layoutPostCard)
        private val tvContent: TextView = itemView.findViewById(R.id.tvContent)

        private val btnLikeContainer: LinearLayout = itemView.findViewById(R.id.btnLikeContainer)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tvLikesCount)

        private val btnCommentContainer: LinearLayout = itemView.findViewById(R.id.btnCommentContainer)
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val tvCommentsCount: TextView = itemView.findViewById(R.id.tvCommentsCount)

        fun bind(post: CommunityPost) {
            tvAuthor.text = post.authorName
            tvUsername.text = "@${post.authorUsername}"
            tvContent.text = post.content

            // تحميل الصورة الشخصية (سواء كانت صورة جوجل أو أحد الأفاتارات الـ 8)
            ivAuthorAvatar.loadUserAvatar(post.authorPhotoUrl)

            // تلوين بطاقة المنشور المركزية فقط
            ThemeHelper.applyBackground(layoutPostCard, post.bgId, 12f)
            tvContent.typeface = ThemeHelper.getFontTypeface(itemView.context, post.fontId)
            tvContent.setTextColor(ThemeHelper.getTextColor(post.bgId))

            // العدادات السفلية
            tvLikesCount.text = post.likesCount.toString()
            tvCommentsCount.text = post.commentsCount.toString()

            // حالة الإعجاب
            val isLiked = currentUserId.isNotEmpty() && post.likedBy.contains(currentUserId)
            if (isLiked) {
                btnLike.setImageResource(R.drawable.ic_heart_filled)
                btnLike.setColorFilter(Color.parseColor("#EF4444"))
                tvLikesCount.setTextColor(Color.parseColor("#EF4444"))
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline)
                btnLike.setColorFilter(Color.parseColor("#64748B"))
                tvLikesCount.setTextColor(Color.parseColor("#64748B"))
            }

            // أحداث الضغط
            val likeClickListener = View.OnClickListener {
                onLikeClicked(post, isLiked)
            }
            btnLikeContainer.setOnClickListener(likeClickListener)
            btnLike.setOnClickListener(likeClickListener)

            val commentClickListener = View.OnClickListener {
                onCommentClicked(post)
            }
            btnCommentContainer.setOnClickListener(commentClickListener)
            btnComment.setOnClickListener(commentClickListener)

            layoutAuthorProfile.setOnClickListener {
                onAuthorClicked(post.authorId, post.authorName)
            }

            // زر الخيارات (الثلاث نقاط الموحد)
            val isMyPost = currentUserId.isNotEmpty() && post.authorId == currentUserId
            btnPostMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                if (isMyPost) {
                    popup.menu.add(0, 1, 0, "تعديل")
                    popup.menu.add(0, 2, 1, "حذف")
                } else {
                    popup.menu.add(0, 3, 0, "إبلاغ عن المنشور")
                }

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1 -> onEditClicked(post)
                        2 -> onDeleteClicked(post)
                        3 -> onReportClicked(post)
                    }
                    true
                }
                popup.show()
            }
        }
    }
}
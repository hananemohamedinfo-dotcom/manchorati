package com.manchorati.www

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommentsAdapter(
    private var comments: List<Comment>,
    private val currentUserId: String,
    private val postAuthorId: String,
    private val onUserClicked: (userId: String, userName: String) -> Unit = { _, _ -> },
    private val onReplyClicked: (Comment) -> Unit = {},
    private val onEditClicked: (Comment) -> Unit = {},
    private val onDeleteClicked: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewReplyIndent: View = itemView.findViewById(R.id.viewReplyIndent)
        private val layoutAvatar: FrameLayout = itemView.findViewById(R.id.layoutUserAvatar)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
        private val layoutReplyHeader: View = itemView.findViewById(R.id.layoutReplyHeader)
        private val tvReplyToAuthor: TextView = itemView.findViewById(R.id.tvReplyToAuthor)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val tvEditedBadge: TextView? = itemView.findViewById(R.id.tvEditedBadge)
        private val btnReply: TextView = itemView.findViewById(R.id.btnReplyComment)
        private val tvEditSeparator: View? = itemView.findViewById(R.id.tvEditSeparator)
        private val btnEdit: TextView? = itemView.findViewById(R.id.btnEditComment)
        private val tvDeleteSeparator: View = itemView.findViewById(R.id.tvDeleteSeparator)
        private val btnDelete: View = itemView.findViewById(R.id.btnDeleteComment)

        private fun formatTimeAgo(timeMillis: Long): String {
            if (timeMillis <= 0L) return ""
            val diff = System.currentTimeMillis() - timeMillis
            val justNow = itemView.context.getString(R.string.time_just_now)
            if (diff < 0L) return justNow
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            val isRtl = LocaleHelper.isRtl(itemView.context)
            return when {
                seconds < 60 -> justNow
                minutes < 60 -> if (isRtl) "منذ $minutes د" else "${minutes}m"
                hours < 24 -> if (isRtl) "منذ $hours س" else "${hours}h"
                days < 7 -> if (isRtl) "منذ $days يوم" else "${days}d"
                else -> {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timeMillis))
                }
            }
        }

        fun bind(comment: Comment) {
            val isReply = comment.replyToAuthorName.isNotEmpty() || comment.replyToCommentId.isNotEmpty()
            val density = itemView.context.resources.displayMetrics.density

            // تدرج وإزاحة الردود على طريقة فيسبوك (Facebook-style indent & nested avatar)
            if (isReply) {
                viewReplyIndent.visibility = View.VISIBLE
                val avatarSize = (28 * density).toInt()
                layoutAvatar.layoutParams = layoutAvatar.layoutParams.apply {
                    width = avatarSize
                    height = avatarSize
                }
                layoutReplyHeader.visibility = View.VISIBLE
                tvReplyToAuthor.text = "@${comment.replyToAuthorName}"
            } else {
                viewReplyIndent.visibility = View.GONE
                val avatarSize = (36 * density).toInt()
                layoutAvatar.layoutParams = layoutAvatar.layoutParams.apply {
                    width = avatarSize
                    height = avatarSize
                }
                layoutReplyHeader.visibility = View.GONE
            }

            tvAuthor.text = comment.authorName
            tvContent.text = comment.content

            if (comment.timestamp > 0L) {
                tvTime.text = formatTimeAgo(comment.timestamp)
                tvTime.visibility = View.VISIBLE
            } else {
                tvTime.visibility = View.GONE
            }

            // شارة (معدّل)
            if (comment.isEdited) {
                tvEditedBadge?.visibility = View.VISIBLE
            } else {
                tvEditedBadge?.visibility = View.GONE
            }

            // زر الرد
            btnReply.setOnClickListener {
                onReplyClicked(comment)
            }

            // إمكانية التعديل لصاحب التعليق فقط
            if (comment.authorId.isNotEmpty() && comment.authorId == currentUserId) {
                tvEditSeparator?.visibility = View.VISIBLE
                btnEdit?.visibility = View.VISIBLE
                btnEdit?.setOnClickListener { onEditClicked(comment) }
            } else {
                tvEditSeparator?.visibility = View.GONE
                btnEdit?.visibility = View.GONE
            }

            // تحميل الصورة الشخصية
            try {
                ivAvatar.loadUserAvatar(comment.authorPhotoUrl)
            } catch (e: Exception) {
                ivAvatar.setImageResource(R.drawable.ic_profile)
            }

            val openProfileListener = View.OnClickListener {
                if (comment.authorId.isNotEmpty()) {
                    onUserClicked(comment.authorId, comment.authorName)
                }
            }
            tvAuthor.setOnClickListener(openProfileListener)
            layoutAvatar.setOnClickListener(openProfileListener)

            // إمكانية الحذف لصاحب التعليق أو صاحب المنشور
            if (comment.authorId == currentUserId || currentUserId == postAuthorId) {
                tvDeleteSeparator.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDeleteClicked(comment) }
            } else {
                tvDeleteSeparator.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }
        }
    }
}

package com.manchorati.www

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommentsAdapter(
    private var comments: List<Comment>,
    private val currentUserId: String,
    private val postAuthorId: String,
    private val onUserClicked: (userId: String, userName: String) -> Unit = { _, _ -> },
    private val onReplyClicked: (Comment) -> Unit = {},
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
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
        private val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val btnReply: TextView = itemView.findViewById(R.id.btnReplyComment)
        private val layoutReplyHeader: View = itemView.findViewById(R.id.layoutReplyHeader)
        private val tvReplyToAuthor: TextView = itemView.findViewById(R.id.tvReplyToAuthor)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteComment)
        private val layoutAvatar: View = itemView.findViewById(R.id.layoutUserAvatar)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)

        private fun formatTimeAgo(timeMillis: Long): String {
            if (timeMillis <= 0L) return ""
            val diff = System.currentTimeMillis() - timeMillis
            if (diff < 0L) return "الآن"
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "الآن"
                minutes < 60 -> "منذ $minutes د"
                hours < 24 -> "منذ $hours س"
                days < 7 -> "منذ $days يوم"
                else -> {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timeMillis))
                }
            }
        }

        fun bind(comment: Comment) {
            tvAuthor.text = comment.authorName
            tvContent.text = comment.content

            if (comment.timestamp > 0L) {
                tvTime.text = formatTimeAgo(comment.timestamp)
                tvTime.visibility = View.VISIBLE
            } else {
                tvTime.visibility = View.GONE
            }

            // وسم الرد على تعليق
            if (comment.replyToAuthorName.isNotEmpty()) {
                layoutReplyHeader.visibility = View.VISIBLE
                tvReplyToAuthor.text = comment.replyToAuthorName
            } else {
                layoutReplyHeader.visibility = View.GONE
            }

            // زر الرد
            btnReply.setOnClickListener {
                onReplyClicked(comment)
            }

            // تحميل الصورة الشخصية أو الأفاتار
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

            if (comment.authorId == currentUserId || currentUserId == postAuthorId) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener { onDeleteClicked(comment) }
            } else {
                btnDelete.visibility = View.GONE
            }
        }
    }
}

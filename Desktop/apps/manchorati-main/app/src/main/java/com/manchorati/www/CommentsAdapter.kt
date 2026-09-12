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
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteComment)
        private val layoutAvatar: View = itemView.findViewById(R.id.layoutUserAvatar)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)

        fun bind(comment: Comment) {
            tvAuthor.text = comment.authorName
            tvContent.text = comment.content

            // استخدام الدالة الموحدة لتحميل صورة البروفايل أو الأفاتار المحلي بسلاسة
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
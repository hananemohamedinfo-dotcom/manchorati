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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CommentsAdapter(
    private var comments: List<Comment>,
    private val currentUserId: String,
    private val postAuthorId: String,
    private val postId: String = "",
    private val onUserClicked: (userId: String, userName: String) -> Unit = { _, _ -> },
    private val onReplyClicked: (Comment) -> Unit = {},
    private val onEditClicked: (Comment) -> Unit = {},
    private val onDeleteClicked: (Comment) -> Unit,
    private val onLikeCommentClicked: (Comment, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    fun removeComment(commentId: String) {
        val index = comments.indexOfFirst { it.id == commentId }
        if (index != -1) {
            val mutable = comments.toMutableList()
            mutable.removeAt(index)
            comments = mutable
            notifyItemRemoved(index)
        }
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
        private val layoutCommentBubble: View = itemView.findViewById(R.id.layoutCommentBubble)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvCommentAuthor)
        private val layoutReplyHeader: View = itemView.findViewById(R.id.layoutReplyHeader)
        private val tvReplyToAuthor: TextView = itemView.findViewById(R.id.tvReplyToAuthor)
        private val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
        private val tvEditedBadge: TextView? = itemView.findViewById(R.id.tvEditedBadge)
        private val btnLike: View = itemView.findViewById(R.id.btnLikeComment)
        private val ivLikeIcon: ImageView = itemView.findViewById(R.id.ivCommentLikeIcon)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tvCommentLikesCount)
        private val btnReply: TextView = itemView.findViewById(R.id.btnReplyComment)

        private fun formatTimeAgo(timeMillis: Long): String {
            return TimeAgoUtils.formatTimeAgo(itemView.context, timeMillis)
        }

        private fun showCommentActionBottomSheet(comment: Comment) {
            val context = itemView.context
            val dialog = BottomSheetDialog(context)
            val sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_comment_actions, null)
            dialog.setContentView(sheetView)

            val ivActionAvatar = sheetView.findViewById<ImageView>(R.id.ivActionCommentAvatar)
            val tvActionAuthor = sheetView.findViewById<TextView>(R.id.tvActionCommentAuthor)
            val tvActionPreview = sheetView.findViewById<TextView>(R.id.tvActionCommentPreview)
            val actionCopy = sheetView.findViewById<View>(R.id.actionCopyComment)
            val actionReply = sheetView.findViewById<View>(R.id.actionReplyComment)
            val actionEdit = sheetView.findViewById<View>(R.id.actionEditComment)
            val actionReport = sheetView.findViewById<View>(R.id.actionReportComment)
            val actionDelete = sheetView.findViewById<View>(R.id.actionDeleteComment)

            val isCurrentAuthor = currentUserId.isNotEmpty() && comment.authorId == currentUserId
            val effectiveAuthorName = if (isCurrentAuthor) {
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("user_name", "")?.takeIf { it.isNotBlank() } ?: comment.authorName
            } else {
                comment.authorName
            }

            val effectiveAvatar = if (isCurrentAuthor) {
                context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("user_photo", "")?.takeIf { it.isNotEmpty() } ?: comment.authorPhotoUrl
            } else {
                comment.authorPhotoUrl
            }

            ivActionAvatar.loadUserAvatar(effectiveAvatar)
            tvActionAuthor.text = effectiveAuthorName
            tvActionPreview.text = comment.content

            // 1. نسخ التعليق
            actionCopy.setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("Comment", comment.content)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.comment_copied_toast), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

            // 2. الرد على التعليق
            actionReply.setOnClickListener {
                dialog.dismiss()
                onReplyClicked(comment)
            }

            // 3. تعديل التعليق (لصاحب التعليق فقط)
            val isOwnComment = comment.authorId.isNotEmpty() && comment.authorId == currentUserId
            if (isOwnComment) {
                actionEdit.visibility = View.VISIBLE
                actionEdit.setOnClickListener {
                    dialog.dismiss()
                    onEditClicked(comment)
                }
            } else {
                actionEdit.visibility = View.GONE
            }

            // 4. إبلاغ عن التعليق (متاح دائماً بجانب الحذف والتعديل)
            if (currentUserId.isNotEmpty()) {
                actionReport.visibility = View.VISIBLE
                actionReport.setOnClickListener {
                    dialog.dismiss()
                    MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.comment_report_confirm_title))
                        .setMessage(context.getString(R.string.comment_report_confirm_msg))
                        .setPositiveButton(context.getString(R.string.comment_action_report)) { _, _ ->
                            FirestoreManager.reportComment(
                                postId = postId,
                                commentId = comment.id,
                                reporterId = currentUserId,
                                reason = "Inappropriate comment"
                            ) { success ->
                                if (success) {
                                    Toast.makeText(context, context.getString(R.string.comment_report_success), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            } else {
                actionReport.visibility = View.GONE
            }

            // 5. حذف التعليق (لصاحب التعليق أو صاحب المنشور)
            val canDelete = isOwnComment || (currentUserId.isNotEmpty() && currentUserId == postAuthorId)
            if (canDelete) {
                actionDelete.visibility = View.VISIBLE
                actionDelete.setOnClickListener {
                    dialog.dismiss()
                    onDeleteClicked(comment)
                }
            } else {
                actionDelete.visibility = View.GONE
            }

            dialog.show()
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

            val isCurrentAuthor = currentUserId.isNotEmpty() && comment.authorId == currentUserId
            val effectiveAuthorName = if (isCurrentAuthor) {
                itemView.context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("user_name", "")?.takeIf { it.isNotBlank() } ?: comment.authorName
            } else {
                comment.authorName
            }

            val effectiveAvatar = if (isCurrentAuthor) {
                itemView.context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getString("user_photo", "")?.takeIf { it.isNotEmpty() } ?: comment.authorPhotoUrl
            } else {
                comment.authorPhotoUrl
            }

            tvAuthor.text = effectiveAuthorName
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

            // حالة زر الإعجاب بالتعليق (Like comment)
            var currentlyLiked = currentUserId.isNotEmpty() && comment.likedBy.contains(currentUserId)
            var currentLikesCount = comment.likesCount
            val redColor = Color.parseColor("#EF4444")
            val secondaryColor = ContextCompat.getColor(itemView.context, R.color.text_secondary)

            val renderLikeState = {
                if (currentlyLiked) {
                    ivLikeIcon.setImageResource(R.drawable.ic_heart_filled)
                    ivLikeIcon.setColorFilter(redColor)
                    tvLikesCount.setTextColor(redColor)
                    if (currentLikesCount > 1) {
                        tvLikesCount.text = "$currentLikesCount"
                    } else {
                        tvLikesCount.text = itemView.context.getString(R.string.comment_liked_by_you)
                    }
                } else {
                    ivLikeIcon.setImageResource(R.drawable.ic_heart_outline)
                    ivLikeIcon.setColorFilter(secondaryColor)
                    tvLikesCount.setTextColor(secondaryColor)
                    if (currentLikesCount > 0) {
                        tvLikesCount.text = "$currentLikesCount"
                    } else {
                        tvLikesCount.text = itemView.context.getString(R.string.post_action_like)
                    }
                }
            }

            renderLikeState()

            btnLike.setOnClickListener {
                if (currentUserId.isEmpty()) {
                    return@setOnClickListener
                }
                currentlyLiked = !currentlyLiked
                if (currentlyLiked) {
                    currentLikesCount++
                } else {
                    currentLikesCount = maxOf(0L, currentLikesCount - 1)
                }

                btnLike.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                renderLikeState()

                ivLikeIcon.scaleX = 0.7f
                ivLikeIcon.scaleY = 0.7f
                ivLikeIcon.animate().scaleX(1.15f).scaleY(1.15f).setDuration(120).withEndAction {
                    ivLikeIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()

                onLikeCommentClicked(comment, currentlyLiked)
            }

            // زر الرد
            btnReply.setOnClickListener {
                onReplyClicked(comment)
            }

            // تحميل الصورة الشخصية
            try {
                ivAvatar.loadUserAvatar(effectiveAvatar)
            } catch (e: Exception) {
                ivAvatar.setImageResource(R.drawable.ic_profile)
            }

            val openProfileListener = View.OnClickListener {
                if (comment.authorId.isNotEmpty()) {
                    onUserClicked(comment.authorId, effectiveAuthorName)
                }
            }
            tvAuthor.setOnClickListener(openProfileListener)
            layoutAvatar.setOnClickListener(openProfileListener)

            // الضغط المطول على فقاعة التعليق يفتح قائمة الإجراءات العصرية (مثل فيسبوك)
            val onCommentLongClickListener = View.OnLongClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showCommentActionBottomSheet(comment)
                true
            }
            layoutCommentBubble.setOnLongClickListener(onCommentLongClickListener)
            itemView.setOnLongClickListener(onCommentLongClickListener)
        }
    }
}

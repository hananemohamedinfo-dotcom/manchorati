package com.manchorati.www

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

enum class WritersTabMode {
    WEEKLY,
    FOLLOWERS,
    ALL
}

class WritersAdapter(
    private var writers: List<WriterItem>,
    private var currentUserId: String,
    private var tabMode: WritersTabMode = WritersTabMode.WEEKLY,
    private val onUserClicked: (userId: String, userName: String) -> Unit,
    private val onFollowToggle: (writer: WriterItem, position: Int) -> Unit
) : RecyclerView.Adapter<WritersAdapter.WriterViewHolder>() {

    fun updateData(newWriters: List<WriterItem>, newMode: WritersTabMode = tabMode) {
        this.writers = newWriters
        this.tabMode = newMode
        notifyDataSetChanged()
    }

    fun updateCurrentUserId(newUid: String) {
        this.currentUserId = newUid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WriterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_writer_leaderboard, parent, false)
        return WriterViewHolder(view)
    }

    override fun onBindViewHolder(holder: WriterViewHolder, position: Int) {
        holder.bind(writers[position], position)
    }

    override fun getItemCount(): Int = writers.size

    inner class WriterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutRankBadge: FrameLayout = itemView.findViewById(R.id.layoutRankBadge)
        private val tvWriterRank: TextView = itemView.findViewById(R.id.tvWriterRank)
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivWriterAvatar)
        private val ivCrownSmall: ImageView = itemView.findViewById(R.id.ivWriterCrownSmall)
        private val tvName: TextView = itemView.findViewById(R.id.tvWriterName)
        private val tvBadge: TextView = itemView.findViewById(R.id.tvWriterBadge)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvWriterUsername)
        private val tvStats: TextView = itemView.findViewById(R.id.tvWriterStats)
        private val btnFollow: TextView = itemView.findViewById(R.id.btnWriterFollow)

        fun bind(writer: WriterItem, position: Int) {
            val context = itemView.context
            val rankNumber = writer.rank.takeIf { it > 0 } ?: (position + 1)

            // شارة الترتيب الملونة حسب المركز
            when (rankNumber) {
                1 -> {
                    layoutRankBadge.setBackgroundResource(R.drawable.bg_gold_crown_badge)
                    tvWriterRank.text = "1"
                    tvWriterRank.setTextColor(Color.WHITE)
                    ivCrownSmall.visibility = View.VISIBLE
                }
                2 -> {
                    layoutRankBadge.setBackgroundResource(R.drawable.bg_silver_badge)
                    tvWriterRank.text = "2"
                    tvWriterRank.setTextColor(Color.WHITE)
                    ivCrownSmall.visibility = View.GONE
                }
                3 -> {
                    layoutRankBadge.setBackgroundResource(R.drawable.bg_bronze_badge)
                    tvWriterRank.text = "3"
                    tvWriterRank.setTextColor(Color.WHITE)
                    ivCrownSmall.visibility = View.GONE
                }
                else -> {
                    layoutRankBadge.setBackgroundResource(R.drawable.bg_rank_normal)
                    tvWriterRank.text = context.getString(R.string.leaderboard_rank_format, rankNumber)
                    tvWriterRank.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    ivCrownSmall.visibility = View.GONE
                }
            }

            // تحميل الصورة
            ivAvatar.loadUserAvatar(writer.photoUrl)

            // الاسم واسم المستخدم
            tvName.text = if (writer.displayName.isNotEmpty()) writer.displayName else context.getString(R.string.anonymous_author)
            val cleanHandle = writer.username.removePrefix("@").trim()
            tvUsername.text = if (cleanHandle.isNotEmpty()) "@$cleanHandle" else context.getString(R.string.user_default_handle)

            // رتبة الكاتب Badge
            val resolvedBadge = BadgeHelper.getBadgeForProfile(
                explicitBadge = writer.badge,
                email = null,
                totalLikes = writer.totalLikes,
                followersCount = writer.followersCount
            )

            if (writer.userId != "manchorati_curated" &&
                resolvedBadge.id != "official" &&
                resolvedBadge.id != BadgeHelper.BADGE_BEGINNER.id) {
                tvBadge.visibility = View.VISIBLE
                val title = resolvedBadge.getLocalizedTitle(context)
                tvBadge.text = title
                try {
                    tvBadge.setTextColor(Color.parseColor(resolvedBadge.textColorHex))
                } catch (e: Exception) {
                    tvBadge.setTextColor(Color.parseColor("#F59E0B"))
                }
                if (resolvedBadge.bgResId != 0) {
                    tvBadge.setBackgroundResource(resolvedBadge.bgResId)
                }
            } else {
                tvBadge.visibility = View.GONE
            }

            // الإحصائيات بحسب التبويب النشط
            when (tabMode) {
                WritersTabMode.WEEKLY -> {
                    tvStats.text = "❤️ " + context.getString(R.string.writer_weekly_likes, writer.weeklyLikes)
                }
                WritersTabMode.FOLLOWERS -> {
                    tvStats.text = "👥 " + context.getString(R.string.writer_followers_count, writer.followersCount.formatCount())
                }
                WritersTabMode.ALL -> {
                    tvStats.text = "✍️ " + context.getString(R.string.writer_posts_count, writer.postsCount)
                }
            }

            // زر المتابعة
            if (currentUserId.isNotEmpty() && writer.userId == currentUserId) {
                btnFollow.visibility = View.GONE
            } else {
                btnFollow.visibility = View.VISIBLE
                if (writer.isFollowing) {
                    btnFollow.text = context.getString(R.string.following)
                    btnFollow.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                    btnFollow.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                } else {
                    btnFollow.text = context.getString(R.string.follow)
                    btnFollow.setBackgroundResource(R.drawable.bg_tool_chip_active)
                    btnFollow.setTextColor(Color.WHITE)
                }
                btnFollow.setOnClickListener {
                    onFollowToggle(writer, position)
                }
            }

            // النقر على السطر بالكامل يفتح البروفايل
            itemView.setOnClickListener {
                onUserClicked(writer.userId, writer.displayName)
            }
        }
    }
}

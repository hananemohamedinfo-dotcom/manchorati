package com.manchorati.www

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationsAdapter(
    private var items: List<AppNotification>,
    private val onItemClicked: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    fun updateData(newItems: List<AppNotification>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvNotificationText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        private val dot: View = itemView.findViewById(R.id.viewUnreadDot)

        fun bind(notification: AppNotification) {
            tvText.text = notification.message

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                notification.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            tvTime.text = relativeTime

            dot.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            itemView.setBackgroundColor(
                if (notification.isRead) Color.TRANSPARENT else Color.parseColor("#F1F5F9")
            )

            // تخصيص الأيقونة حسب نوع الإشعار
            when (notification.type) {
                "LIKE" -> ivIcon.setColorFilter(Color.parseColor("#EF4444"))
                "COMMENT" -> ivIcon.setColorFilter(Color.parseColor("#2563EB"))
                "FOLLOW" -> ivIcon.setColorFilter(Color.parseColor("#10B981"))
                else -> ivIcon.setColorFilter(Color.parseColor("#64748B"))
            }

            itemView.setOnClickListener {
                onItemClicked(notification)
            }
        }
    }
}
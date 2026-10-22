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
    initialItems: List<AppNotification>,
    private val onItemClicked: (AppNotification) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    private val items: MutableList<AppNotification> = initialItems.toMutableList()

    fun updateData(newItems: List<AppNotification>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int): AppNotification? {
        if (position in 0 until items.size) {
            val removed = items.removeAt(position)
            notifyItemRemoved(position)
            return removed
        }
        return null
    }

    fun getItems(): List<AppNotification> = items

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

            // تخصيص الأيقونة واللون حسب نوع الإشعار
            when (notification.type) {
                "LIKE" -> {
                    ivIcon.setImageResource(R.drawable.ic_heart_filled)
                    ivIcon.setColorFilter(Color.parseColor("#EF4444"))
                }
                "COMMENT" -> {
                    ivIcon.setImageResource(android.R.drawable.stat_notify_chat)
                    ivIcon.setColorFilter(Color.parseColor("#2563EB"))
                }
                "FOLLOW" -> {
                    ivIcon.setImageResource(R.drawable.ic_profile)
                    ivIcon.setColorFilter(Color.parseColor("#10B981"))
                }
                "NEW_POST" -> {
                    ivIcon.setImageResource(R.drawable.ic_edit)
                    ivIcon.setColorFilter(Color.parseColor("#F59E0B"))
                }
                else -> {
                    ivIcon.setImageResource(android.R.drawable.ic_popup_reminder)
                    ivIcon.setColorFilter(Color.parseColor("#64748B"))
                }
            }

            itemView.setOnClickListener {
                onItemClicked(notification)
            }
        }
    }
}

package com.manchorati.www

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class UserFollowItem(
    val userId: String,
    val displayName: String,
    val username: String,
    val photoUrl: String,
    var isFollowing: Boolean
)

class UserFollowAdapter(
    private var users: List<UserFollowItem>,
    private val currentUserId: String,
    private val onUserClicked: (userId: String, userName: String) -> Unit,
    private val onFollowToggle: (user: UserFollowItem, position: Int) -> Unit
) : RecyclerView.Adapter<UserFollowAdapter.UserViewHolder>() {

    fun updateData(newUsers: List<UserFollowItem>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_follow, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], position)
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivUserRowAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvUserRowName)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUserRowUsername)
        private val btnFollow: TextView = itemView.findViewById(R.id.btnUserRowFollow)

        fun bind(user: UserFollowItem, position: Int) {
            tvName.text = if (user.displayName.isNotEmpty()) user.displayName else "مستخدم"
            tvUsername.text = if (user.username.isNotEmpty()) "@${user.username}" else ""
            ivAvatar.loadUserAvatar(user.photoUrl)

            // لا يظهر زر المتابعة لنفس المستخدم المتصل
            if (currentUserId.isNotEmpty() && user.userId == currentUserId) {
                btnFollow.visibility = View.GONE
            } else {
                btnFollow.visibility = View.VISIBLE
                updateFollowButtonUI(user.isFollowing)

                btnFollow.setOnClickListener {
                    onFollowToggle(user, position)
                }
            }

            itemView.setOnClickListener {
                onUserClicked(user.userId, user.displayName)
            }
        }

        private fun updateFollowButtonUI(isFollowing: Boolean) {
            val context = itemView.context
            if (isFollowing) {
                btnFollow.text = "متابع ✓"
                btnFollow.setBackgroundResource(R.drawable.bg_tool_chip_idle)
                btnFollow.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            } else {
                btnFollow.text = "+ متابعة"
                btnFollow.setBackgroundResource(R.drawable.bg_tool_chip_active)
                btnFollow.setTextColor(Color.WHITE)
            }
        }
    }
}

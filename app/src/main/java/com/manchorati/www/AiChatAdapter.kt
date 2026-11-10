package com.manchorati.www

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AiChatAdapter(
    private val messages: MutableList<AiChatMessage>,
    private val onPublishClicked: (quote: String, topic: String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_LOADING = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is AiChatMessage.User -> VIEW_TYPE_USER
            is AiChatMessage.Bot -> VIEW_TYPE_BOT
            is AiChatMessage.Loading -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val v = inflater.inflate(R.layout.item_ai_chat_user, parent, false)
                UserViewHolder(v)
            }
            VIEW_TYPE_BOT -> {
                val v = inflater.inflate(R.layout.item_ai_chat_bot, parent, false)
                BotViewHolder(v)
            }
            else -> {
                val v = inflater.inflate(R.layout.item_ai_chat_loading, parent, false)
                LoadingViewHolder(v)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = messages[position]) {
            is AiChatMessage.User -> (holder as UserViewHolder).bind(item)
            is AiChatMessage.Bot -> (holder as BotViewHolder).bind(item)
            is AiChatMessage.Loading -> {}
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContent: TextView = itemView.findViewById(R.id.tvUserMsgContent)
        private val tvTime: TextView = itemView.findViewById(R.id.tvUserMsgTime)

        fun bind(item: AiChatMessage.User) {
            tvContent.text = item.text
            tvTime.text = item.timestamp
        }
    }

    inner class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuote: TextView = itemView.findViewById(R.id.tvBotQuoteContent)
        private val tvModelTag: TextView = itemView.findViewById(R.id.tvBotModelTag)
        private val btnCopy: MaterialButton = itemView.findViewById(R.id.btnMsgCopy)
        private val btnImage: MaterialButton = itemView.findViewById(R.id.btnMsgImage)
        private val btnVideo: MaterialButton = itemView.findViewById(R.id.btnMsgVideo)
        private val btnPublish: MaterialButton = itemView.findViewById(R.id.btnMsgPublish)

        fun bind(item: AiChatMessage.Bot) {
            tvQuote.text = item.quote
            tvModelTag.text = item.modelTag

            val context = itemView.context

            btnCopy.setOnClickListener {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Quote", item.quote)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.ai_copied_toast), Toast.LENGTH_SHORT).show()
            }

            btnImage.setOnClickListener {
                if (context is AppCompatActivity) {
                    ImageUtils.showImageEditorDialog(context, item.topic, item.quote)
                }
            }

            btnVideo.setOnClickListener {
                val intent = Intent(context, VideoStudioActivity::class.java).apply {
                    putExtra("EXTRA_CONTENT", item.quote)
                    putExtra("EXTRA_TAG", item.topic)
                }
                context.startActivity(intent)
            }

            btnPublish.setOnClickListener {
                onPublishClicked(item.quote, item.topic)
            }
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

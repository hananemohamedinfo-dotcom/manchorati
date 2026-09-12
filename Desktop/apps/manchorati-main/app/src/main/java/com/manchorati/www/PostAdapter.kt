package com.manchorati.www

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class PostAdapter(
    private val context: Context,
    private var posts: List<Post>,
    private var fontSizeSp: Float = 16f,
    private val onPickImageRequest: (() -> Unit)? = null
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val dbHelper = DatabaseHelper(context)

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContent: TextView = itemView.findViewById(R.id.tvPostContent)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategoryName)
        val btnShareGeneral: ImageView = itemView.findViewById(R.id.btnShareGeneral)
        val btnShareWhatsapp: ImageView = itemView.findViewById(R.id.btnShareWhatsapp)
        val btnShareAsImage: ImageView = itemView.findViewById(R.id.btnShareAsImage)
        val btnCopy: ImageView = itemView.findViewById(R.id.btnCopyPost)
        val btnFavorite: ImageView = itemView.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.tvContent.text = post.content
        holder.tvContent.textSize = fontSizeSp
        holder.tvCategory.text = post.categoryName

        updateFavoriteIcon(holder.btnFavorite, post.isFavorite)

        // زر المفضلة (إضافة / إزالة)
        holder.btnFavorite.setOnClickListener {
            val newStatus = dbHelper.toggleFavorite(post.id, post.isFavorite)
            post.isFavorite = newStatus
            updateFavoriteIcon(holder.btnFavorite, newStatus)
            val msg = if (newStatus == 1) "تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        // فتح نافذة المعاينة ومحرر البطاقة واختيار الصورة
        holder.btnShareAsImage.setOnClickListener {
            ImageUtils.showImageEditorDialog(context, post.categoryName, post.content, onPickImageRequest)
        }

        // زر نسخ النص
        holder.btnCopy.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Post Content", post.content)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تم نسخ النص", Toast.LENGTH_SHORT).show()
        }

        // زر مشاركة واتساب مباشرة
        holder.btnShareWhatsapp.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, post.content)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                shareGeneralText(post.content)
            }
        }

        // زر المشاركة العامة
        holder.btnShareGeneral.setOnClickListener {
            shareGeneralText(post.content)
        }
    }

    fun updateFontSize(newSize: Float) {
        this.fontSizeSp = newSize
        notifyDataSetChanged()
    }

    fun updateData(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

   private fun updateFavoriteIcon(imageView: ImageView, isFav: Int) {
        if (isFav == 1) {
            imageView.setImageResource(R.drawable.ic_heart_filled)
            imageView.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EF4444"))
        } else {
            imageView.setImageResource(R.drawable.ic_heart_outline)
            imageView.imageTintList = androidx.core.content.ContextCompat.getColorStateList(context, R.color.text_secondary)
        }
    }

    private fun shareGeneralText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة المنشور عبر"))
    }

    override fun getItemCount(): Int = posts.size
}
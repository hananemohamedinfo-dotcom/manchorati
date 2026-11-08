package com.manchorati.www

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvCategoryTitle)
        val tvCount: TextView = itemView.findViewById(R.id.tvCategoryCount)
        val badgeNew: TextView = itemView.findViewById(R.id.tvBadgeNew)
        val tvEmoji: TextView? = itemView.findViewById(R.id.tvCategoryEmoji)
        val ivIcon: ImageView? = itemView.findViewById(R.id.ivCategoryIcon)
        val layoutEmojiContainer: FrameLayout = itemView.findViewById(R.id.layoutEmojiContainer)
    }

    private data class CategoryVisual(val iconRes: Int, val tintColor: Int, val bgColor: Int)

    private fun getCategoryVisual(name: String): CategoryVisual {
        val clean = name.trim()
        val (iconRes, hexTint, hexBg) = when {
            clean.contains("إسلامية") || clean.contains("اسلام") -> Triple(R.drawable.ic_bookmark_filled, "#10B981", "#152E25")
            clean.contains("حب") || clean.contains("عشق") || clean.contains("غرام") -> Triple(R.drawable.ic_heart_filled, "#F43F5E", "#331821")
            clean.contains("شوق") || clean.contains("حنين") -> Triple(R.drawable.ic_heart_outline, "#EC4899", "#331627")
            clean.contains("مدح") || clean.contains("رمضان") || clean.contains("عيد") -> Triple(R.drawable.ic_crown, "#F59E0B", "#332612")
            clean.contains("حكم") || clean.contains("أمثال") || clean.contains("خواطر") -> Triple(R.drawable.ic_sparkle_ai, "#38BDF8", "#10293B")
            else -> Triple(R.drawable.ic_nav_categories, "#38BDF8", "#10283B")
        }
        return CategoryVisual(iconRes, Color.parseColor(hexTint), Color.parseColor(hexBg))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvTitle.text = category.name.trim()

        val visual = getCategoryVisual(category.name)
        holder.ivIcon?.setImageResource(visual.iconRes)
        holder.ivIcon?.setColorFilter(visual.tintColor)
        holder.tvEmoji?.visibility = View.GONE
        holder.layoutEmojiContainer.visibility = View.VISIBLE

        // Clean vector well background
        val density = holder.itemView.context.resources.displayMetrics.density
        val wellBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12 * density
            setColor(visual.bgColor)
            setStroke((1 * density).toInt(), visual.tintColor and 0x44FFFFFF)
        }
        holder.layoutEmojiContainer.background = wellBg

        // Post counter (clean text, zero emojis)
        if (category.postsCount > 0) {
            holder.tvCount.text = "${category.postsCount} منشور"
            holder.tvCount.visibility = View.VISIBLE
        } else {
            holder.tvCount.text = "منشورات مميزة"
            holder.tvCount.visibility = View.VISIBLE
        }

        holder.badgeNew.visibility = if (category.hasNew) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            holder.itemView.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(70)
                .withEndAction {
                    holder.itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(70)
                        .withEndAction {
                            onCategoryClick(category)
                        }
                        .start()
                }
                .start()
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<Category>) {
        this.categories = newCategories
        notifyDataSetChanged()
    }
}

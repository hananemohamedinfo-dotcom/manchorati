package com.manchorati.www

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvCategoryTitle)
        val tvCount: TextView = itemView.findViewById(R.id.tvCategoryCount)
        val badgeNew: TextView = itemView.findViewById(R.id.tvBadgeNew)
        val tvEmoji: TextView = itemView.findViewById(R.id.tvCategoryEmoji)
        val layoutEmojiContainer: FrameLayout = itemView.findViewById(R.id.layoutEmojiContainer)
    }

    private data class CategoryVisual(val emoji: String, val tintColor: Int, val bgColor: Int)

    private fun getCategoryVisual(name: String): CategoryVisual {
        val clean = name.trim()
        val (emoji, hexTint, hexBg) = when {
            clean.contains("إسلامية") || clean.contains("اسلام") -> Triple("🕌", "#10B981", "#152E25")
            clean.contains("حب") || clean.contains("عشق") || clean.contains("غرام") -> Triple("❤️", "#F43F5E", "#331821")
            clean.contains("شوق") || clean.contains("حنين") -> Triple("🕊️", "#EC4899", "#331627")
            clean.contains("فراق") || clean.contains("وداع") -> Triple("🥀", "#94A3B8", "#1E293B")
            clean.contains("مدح") -> Triple("👑", "#F59E0B", "#332612")
            clean.contains("عيد") -> Triple("🎉", "#A855F7", "#28173B")
            clean.contains("نكت") || clean.contains("ضحك") -> Triple("😂", "#EAB308", "#312813")
            clean.contains("مقالب") -> Triple("😜", "#F97316", "#331E12")
            clean.contains("رمضان") -> Triple("🌙", "#F59E0B", "#332612")
            clean.contains("حكم") || clean.contains("أمثال") -> Triple("💡", "#38BDF8", "#10293B")
            clean.contains("عتاب") -> Triple("💔", "#CBD5E1", "#1E293B")
            clean.contains("صباح") -> Triple("☀️", "#FBBF24", "#332812")
            clean.contains("مساء") -> Triple("🌌", "#818CF8", "#1C1F3B")
            clean.contains("حزن") || clean.contains("ألم") -> Triple("🌧️", "#64748B", "#18202F")
            clean.contains("اعتذار") || clean.contains("إعتذار") -> Triple("💐", "#14B8A6", "#112F2C")
            clean.contains("جمعة") -> Triple("🌿", "#10B981", "#122E23")
            clean.contains("مزخرفة") -> Triple("✒️", "#C084FC", "#281938")
            clean.contains("خواطر") -> Triple("💭", "#38BDF8", "#10283B")
            clean.contains("صداقة") -> Triple("🤝", "#F59E0B", "#332512")
            clean.contains("english", ignoreCase = true) -> Triple("🌐", "#60A5FA", "#14253B")
            else -> Triple("✨", "#38BDF8", "#10283B")
        }
        return CategoryVisual(emoji, Color.parseColor(hexTint), Color.parseColor(hexBg))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvTitle.text = category.name.trim()

        val visual = getCategoryVisual(category.name)
        holder.tvEmoji.text = visual.emoji
        holder.layoutEmojiContainer.visibility = View.VISIBLE

        // Custom well background with category tint
        val density = holder.itemView.context.resources.displayMetrics.density
        val wellBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12 * density
            setColor(visual.bgColor)
            setStroke((1 * density).toInt(), visual.tintColor and 0x44FFFFFF)
        }
        holder.layoutEmojiContainer.background = wellBg

        // Post counter
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

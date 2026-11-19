package com.manchorati.www

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import java.util.Calendar

object WriterOfTheWeekHelper {

    private const val PREFS_NAME = "writer_of_the_week_prefs"
    private const val KEY_CHAMPION_ID = "champion_user_id"
    private const val KEY_CHAMPION_NAME = "champion_display_name"
    private const val KEY_CHAMPION_2_ID = "champion_2_user_id"
    private const val KEY_CHAMPION_2_NAME = "champion_2_display_name"
    private const val KEY_CHAMPION_3_ID = "champion_3_user_id"
    private const val KEY_CHAMPION_3_NAME = "champion_3_display_name"
    private const val KEY_EXPIRES_AT = "champion_expires_at"

    // Default featured champion if nothing stored yet
    private const val DEFAULT_CHAMPION_ID = "persona_amina_k"
    private const val DEFAULT_CHAMPION_NAME = "أمينة القادري"

    @Volatile
    private var cachedChampionId: String? = null
    @Volatile
    private var cachedChampion2Id: String? = null
    @Volatile
    private var cachedChampion3Id: String? = null

    fun getChampionId(context: Context): String {
        cachedChampionId?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_CHAMPION_ID, null)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (!id.isNullOrEmpty() && (expiresAt == 0L || System.currentTimeMillis() < expiresAt)) {
            cachedChampionId = id
            return id
        }
        cachedChampionId = DEFAULT_CHAMPION_ID
        return DEFAULT_CHAMPION_ID
    }

    fun getChampionRank(context: Context, userId: String?): Int {
        if (userId.isNullOrEmpty() || userId == "manchorati_curated" || userId == "local_guest") return 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt != 0L && System.currentTimeMillis() >= expiresAt) return 0

        val id1 = cachedChampionId ?: prefs.getString(KEY_CHAMPION_ID, DEFAULT_CHAMPION_ID)
        val id2 = cachedChampion2Id ?: prefs.getString(KEY_CHAMPION_2_ID, null)
        val id3 = cachedChampion3Id ?: prefs.getString(KEY_CHAMPION_3_ID, null)

        return when (userId) {
            id1 -> 1
            id2 -> 2
            id3 -> 3
            else -> 0
        }
    }

    fun isWriterOfTheWeek(context: Context, userId: String?): Boolean {
        return getChampionRank(context, userId) == 1
    }

    fun setTop3Champions(context: Context, top3: List<Pair<String, String>>) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val expiresAt = cal.timeInMillis

        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putLong(KEY_EXPIRES_AT, expiresAt)

        if (top3.isNotEmpty()) {
            val (u1, n1) = top3[0]
            cachedChampionId = u1
            editor.putString(KEY_CHAMPION_ID, u1).putString(KEY_CHAMPION_NAME, n1)
        }
        if (top3.size > 1) {
            val (u2, n2) = top3[1]
            cachedChampion2Id = u2
            editor.putString(KEY_CHAMPION_2_ID, u2).putString(KEY_CHAMPION_2_NAME, n2)
        }
        if (top3.size > 2) {
            val (u3, n3) = top3[2]
            cachedChampion3Id = u3
            editor.putString(KEY_CHAMPION_3_ID, u3).putString(KEY_CHAMPION_3_NAME, n3)
        }
        editor.apply()
    }

    fun setChampion(context: Context, userId: String, displayName: String) {
        setTop3Champions(context, listOf(userId to displayName))
    }

    fun getChampionDisplayName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CHAMPION_NAME, null) ?: DEFAULT_CHAMPION_NAME
    }

    fun getRankBadgeTitle(context: Context? = null, rank: Int): String {
        return when (rank) {
            1 -> context?.getString(R.string.writer_of_week_rank1) ?: "كاتب الأسبوع 🥇"
            2 -> context?.getString(R.string.writer_of_week_rank2) ?: "وصيف الأسبوع 🥈"
            3 -> context?.getString(R.string.writer_of_week_rank3) ?: "متميز الأسبوع 🥉"
            else -> ""
        }
    }

    fun getRankBadgeTitle(rank: Int): String = getRankBadgeTitle(null, rank)

    fun getRankBadgeBgRes(rank: Int): Int {
        return when (rank) {
            1 -> R.drawable.bg_gold_crown_badge
            2 -> R.drawable.bg_silver_badge
            3 -> R.drawable.bg_bronze_badge
            else -> 0
        }
    }

    fun getRankColor(rank: Int): Int {
        return when (rank) {
            1 -> Color.parseColor("#FFD700") // Glowing Royal Gold
            2 -> Color.parseColor("#F8FAFC") // Pure Radiant Silver White
            3 -> Color.parseColor("#EA580C") // Vivid Copper Bronze
            else -> Color.TRANSPARENT
        }
    }

    /**
     * Shows an elegant dialogue celebrating the Writer rank (Gold, Silver, Bronze for Top 3 only)
     */
    fun showRankCelebrationDialog(
        context: Context,
        rank: Int,
        authorName: String,
        authorPhotoUrl: String? = null,
        isSelf: Boolean = false
    ) {
        if (rank !in 1..3) return

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_writer_of_week_celebration, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val cardDialog = view.findViewById<MaterialCardView>(R.id.cardCelebrationDialog)
        val ivAvatar = view.findViewById<ImageView>(R.id.ivCelebrationAvatar)
        val ivLaurel = view.findViewById<ImageView>(R.id.ivCelebrationLaurel)
        val ivCrown = view.findViewById<ImageView>(R.id.ivCelebrationCrown)
        val tvBadgeTitle = view.findViewById<TextView>(R.id.tvCelebrationBadgeTitle)
        val tvName = view.findViewById<TextView>(R.id.tvChampionName)
        val tvDesc = view.findViewById<TextView>(R.id.tvChampionDescription)
        val btnClose = view.findViewById<TextView>(R.id.btnCloseCelebration)

        tvName.text = authorName.ifEmpty { getChampionDisplayName(context) }
        ivAvatar?.loadUserAvatar(authorPhotoUrl)

        when (rank) {
            1 -> {
                val goldColor = Color.parseColor("#FFD700")
                val amberGold = Color.parseColor("#F59E0B")
                cardDialog?.strokeColor = amberGold
                tvBadgeTitle.text = context.getString(R.string.celebration_rank1_title)
                tvBadgeTitle.setTextColor(amberGold)
                ivLaurel.setColorFilter(goldColor)
                ivCrown.visibility = View.VISIBLE
                ivCrown.setColorFilter(goldColor)
                btnClose.setBackgroundResource(R.drawable.bg_gold_crown_badge)
                tvDesc.text = if (isSelf) {
                    context.getString(R.string.celebration_rank1_self)
                } else {
                    context.getString(R.string.celebration_rank1_other, tvName.text)
                }
            }
            2 -> {
                val silverColor = Color.parseColor("#F8FAFC")
                val silverBorder = Color.parseColor("#CBD5E1")
                cardDialog?.strokeColor = silverBorder
                tvBadgeTitle.text = context.getString(R.string.celebration_rank2_title)
                tvBadgeTitle.setTextColor(silverBorder)
                ivLaurel.setColorFilter(silverColor)
                ivCrown.visibility = View.VISIBLE
                ivCrown.setColorFilter(silverColor)
                btnClose.setBackgroundResource(R.drawable.bg_silver_badge)
                tvDesc.text = if (isSelf) {
                    context.getString(R.string.celebration_rank2_self)
                } else {
                    context.getString(R.string.celebration_rank2_other, tvName.text)
                }
            }
            3 -> {
                val bronzeColor = Color.parseColor("#EA580C")
                val bronzeBorder = Color.parseColor("#FB923C")
                cardDialog?.strokeColor = bronzeBorder
                tvBadgeTitle.text = context.getString(R.string.celebration_rank3_title)
                tvBadgeTitle.setTextColor(bronzeColor)
                ivLaurel.setColorFilter(bronzeColor)
                ivCrown.visibility = View.VISIBLE
                ivCrown.setColorFilter(bronzeColor)
                btnClose.setBackgroundResource(R.drawable.bg_bronze_badge)
                tvDesc.text = if (isSelf) {
                    context.getString(R.string.celebration_rank3_self)
                } else {
                    context.getString(R.string.celebration_rank3_other, tvName.text)
                }
            }
        }

        btnClose.setOnClickListener {
            btnClose.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showWriterOfWeekDialog(context: Context, authorName: String, authorPhotoUrl: String? = null) {
        showRankCelebrationDialog(context, 1, authorName, authorPhotoUrl, isSelf = false)
    }
}

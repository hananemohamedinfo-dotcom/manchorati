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

    /**
     * Shows an elegant dialogue celebrating the Writer rank (Gold, Silver, Bronze, Top 10)
     */
    fun showRankCelebrationDialog(context: Context, rank: Int, authorName: String, isSelf: Boolean = false) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_writer_of_week_celebration, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val ivLaurel = view.findViewById<ImageView>(R.id.ivCelebrationLaurel)
        val ivCrown = view.findViewById<ImageView>(R.id.ivCelebrationCrown)
        val tvBadgeTitle = view.findViewById<TextView>(R.id.tvCelebrationBadgeTitle)
        val tvName = view.findViewById<TextView>(R.id.tvChampionName)
        val tvDesc = view.findViewById<TextView>(R.id.tvChampionDescription)
        val btnClose = view.findViewById<TextView>(R.id.btnCloseCelebration)

        tvName.text = authorName.ifEmpty { getChampionDisplayName(context) }

        when (rank) {
            1 -> {
                val goldColor = Color.parseColor("#F59E0B")
                tvBadgeTitle.text = "كاتب الأسبوع الذهبي 🥇"
                tvBadgeTitle.setTextColor(goldColor)
                ivLaurel.setColorFilter(goldColor)
                ivCrown.visibility = View.VISIBLE
                ivCrown.setColorFilter(goldColor)
                btnClose.setBackgroundResource(R.drawable.bg_gold_crown_badge)
                tvDesc.text = if (isSelf) {
                    "تهانينا يا بطل! أنت كاتب الأسبوع المتوج بالمركز الأول عالمياً 🥇 وحصلت على إكليل الغار الذهبي وتاج الصدارة الملكي!"
                } else {
                    "تُوّج الكاتب ${tvName.text} بلقب كاتب الأسبوع بفضل تفاعل القراء وجمال الكلمات، وحصل على إكليل الغار الذهبي طيلة هذا الأسبوع!"
                }
            }
            2 -> {
                val silverColor = Color.parseColor("#CBD5E1")
                tvBadgeTitle.text = "وصيف الأسبوع الفضي 🥈"
                tvBadgeTitle.setTextColor(silverColor)
                ivLaurel.setColorFilter(silverColor)
                ivCrown.visibility = View.VISIBLE
                ivCrown.setColorFilter(silverColor)
                btnClose.setBackgroundResource(R.drawable.bg_silver_badge)
                tvDesc.text = if (isSelf) {
                    "إنجاز استثنائي! حققت المركز الثاني هذا الأسبوع وتوجت رسمياً بإكليل الغار الفضي 🥈!"
                } else {
                    "تألق الكاتب ${tvName.text} وحقق المركز الثاني هذا الأسبوع، ونال إكليل الغار الفضي بجدارة!"
                }
            }
            3 -> {
                val bronzeColor = Color.parseColor("#CD7F32")
                tvBadgeTitle.text = "الكاتب المتميز البرونزي 🥉"
                tvBadgeTitle.setTextColor(bronzeColor)
                ivLaurel.setColorFilter(bronzeColor)
                ivCrown.visibility = View.VISIBLE
                ivCrown.setColorFilter(bronzeColor)
                btnClose.setBackgroundResource(R.drawable.bg_bronze_badge)
                tvDesc.text = if (isSelf) {
                    "مبارك تألقك! أنت في المركز الثالث هذا الأسبوع وحصدت إكليل الغار البرونزي 🥉!"
                } else {
                    "حصد الكاتب ${tvName.text} المركز الثالث هذا الأسبوع وتوّج بإكليل الغار البرونزي!"
                }
            }
            else -> {
                val cyanColor = Color.parseColor("#38BDF8")
                tvBadgeTitle.text = "نخبة كُتّاب الأسبوع (Top 10) 🌟"
                tvBadgeTitle.setTextColor(cyanColor)
                ivLaurel.setColorFilter(cyanColor)
                ivCrown.visibility = View.GONE
                btnClose.setBackgroundResource(R.drawable.bg_button_gradient_primary)
                tvDesc.text = if (isSelf) {
                    "برافو! لقد دخلت رسمياً قائمة أفضل 10 كُتّاب لهذا الأسبوع (المركز #$rank)! 👏 واصل النشر لتصعد إلى منصة التتويج الثلاثية!"
                } else {
                    "أحد أبرز كُتّاب الأسبوع ضمن قائمة أفضل 10 متصدرين الأكثر إلهاماً وتفاعلاً!"
                }
            }
        }

        btnClose.setOnClickListener {
            btnClose.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
        }

        dialog.show()
    }

    fun showWriterOfWeekDialog(context: Context, authorName: String) {
        showRankCelebrationDialog(context, 1, authorName, isSelf = false)
    }
}

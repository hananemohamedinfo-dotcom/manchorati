package com.manchorati.www

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import java.util.Calendar

object WriterOfTheWeekHelper {

    private const val PREFS_NAME = "writer_of_the_week_prefs"
    private const val KEY_CHAMPION_ID = "champion_user_id"
    private const val KEY_CHAMPION_NAME = "champion_display_name"
    private const val KEY_EXPIRES_AT = "champion_expires_at"

    // Default featured champion if nothing stored yet
    private const val DEFAULT_CHAMPION_ID = "persona_amina_k"
    private const val DEFAULT_CHAMPION_NAME = "أمينة القادري"

    @Volatile
    private var cachedChampionId: String? = null

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

    fun isWriterOfTheWeek(context: Context, userId: String?): Boolean {
        if (userId.isNullOrEmpty() || userId == "manchorati_curated" || userId == "local_guest") return false
        val champId = getChampionId(context)
        return champId == userId
    }

    fun setChampion(context: Context, userId: String, displayName: String) {
        if (userId.isEmpty() || userId == "manchorati_curated") return
        cachedChampionId = userId
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val expiresAt = cal.timeInMillis

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CHAMPION_ID, userId)
            .putString(KEY_CHAMPION_NAME, displayName)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun getChampionDisplayName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CHAMPION_NAME, null) ?: DEFAULT_CHAMPION_NAME
    }

    /**
     * Shows an elegant dialogue celebrating the Writer of the Week when someone taps on the laurel wreath.
     */
    fun showWriterOfWeekDialog(context: Context, authorName: String) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_writer_of_week_celebration, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvName = view.findViewById<TextView>(R.id.tvChampionName)
        val tvDesc = view.findViewById<TextView>(R.id.tvChampionDescription)
        val btnClose = view.findViewById<TextView>(R.id.btnCloseCelebration)

        tvName.text = authorName.ifEmpty { getChampionDisplayName(context) }
        tvDesc.text = context.getString(
            R.string.writer_of_the_week_celebration_desc,
            authorName.ifEmpty { getChampionDisplayName(context) }
        )

        btnClose.setOnClickListener {
            btnClose.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            dialog.dismiss()
        }

        dialog.show()
    }
}

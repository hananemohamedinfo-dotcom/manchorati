package com.manchorati.www

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val isNewDay: Boolean,
    val lastActiveDate: String,
    val totalActiveDays: Int,
    val weeklyStatus: List<Boolean> // 7 days of current week (Sat to Fri)
)

object StreakManager {

    private const val PREFS_NAME = "manchorati_streak_prefs"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_LAST_DATE = "last_active_date"
    private const val KEY_TOTAL_DAYS = "total_active_days"
    private const val KEY_ACTIVE_DATES_SET = "active_dates_set"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun getStreakInfo(context: Context): StreakInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENT_STREAK, 1)
        val longest = prefs.getInt(KEY_LONGEST_STREAK, 1)
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val totalDays = prefs.getInt(KEY_TOTAL_DAYS, 1)
        val datesSet = prefs.getStringSet(KEY_ACTIVE_DATES_SET, emptySet()) ?: emptySet()

        val weeklyStatus = computeWeeklyStatus(datesSet)

        return StreakInfo(
            currentStreak = current,
            longestStreak = longest,
            isNewDay = false,
            lastActiveDate = lastDate,
            totalActiveDays = totalDays,
            weeklyStatus = weeklyStatus
        )
    }

    /**
     * يتم استدعاؤها عند فتح التطبيق لحساب السلسلة وتحديثها إن كان يوماً جديداً
     */
    fun checkAndUpdateStreak(context: Context, onNewDayCelebration: ((Int) -> Unit)? = null): StreakInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = dateFormat.format(Date())

        val lastDateStr = prefs.getString(KEY_LAST_DATE, "") ?: ""
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
        var totalDays = prefs.getInt(KEY_TOTAL_DAYS, 0)
        val datesSet = (prefs.getStringSet(KEY_ACTIVE_DATES_SET, emptySet()) ?: emptySet()).toMutableSet()

        if (lastDateStr == todayStr) {
            // مسجل بالفعل لهذا اليوم
            val weeklyStatus = computeWeeklyStatus(datesSet)
            return StreakInfo(
                currentStreak = if (currentStreak <= 0) 1 else currentStreak,
                longestStreak = if (longestStreak <= 0) 1 else longestStreak,
                isNewDay = false,
                lastActiveDate = lastDateStr,
                totalActiveDays = if (totalDays <= 0) 1 else totalDays,
                weeklyStatus = weeklyStatus
            )
        }

        // يوم جديد!
        var isConsecutive = false
        if (lastDateStr.isNotEmpty()) {
            try {
                val lastDate = dateFormat.parse(lastDateStr)
                if (lastDate != null) {
                    val calYesterday = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -1)
                    }
                    val yesterdayStr = dateFormat.format(calYesterday.time)
                    if (lastDateStr == yesterdayStr) {
                        isConsecutive = true
                    }
                }
            } catch (e: Exception) {
                isConsecutive = false
            }
        }

        if (isConsecutive) {
            currentStreak += 1
        } else {
            // انقطعت السلسلة أو أول مرة
            currentStreak = 1
        }

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
        }

        totalDays += 1
        datesSet.add(todayStr)

        // حفظ محلياً
        prefs.edit()
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_LONGEST_STREAK, longestStreak)
            .putString(KEY_LAST_DATE, todayStr)
            .putInt(KEY_TOTAL_DAYS, totalDays)
            .putStringSet(KEY_ACTIVE_DATES_SET, datesSet)
            .apply()

        // مزامنة مع فايرستور إن كان المستخدم مسجل الدخول
        syncStreakToFirestore(currentStreak, longestStreak, todayStr)

        val weeklyStatus = computeWeeklyStatus(datesSet)

        Handler(Looper.getMainLooper()).post {
            onNewDayCelebration?.invoke(currentStreak)
        }

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isNewDay = true,
            lastActiveDate = todayStr,
            totalActiveDays = totalDays,
            weeklyStatus = weeklyStatus
        )
    }

    private fun computeWeeklyStatus(datesSet: Set<String>): List<Boolean> {
        val result = mutableListOf<Boolean>()
        val cal = Calendar.getInstance()
        // البدء من يوم السبت للأسبوع الحالي
        cal.firstDayOfWeek = Calendar.SATURDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)

        for (i in 0 until 7) {
            val dateStr = dateFormat.format(cal.time)
            result.add(datesSet.contains(dateStr))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun syncStreakToFirestore(current: Int, longest: Int, lastDate: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val updates = mapOf(
                "streakCount" to current,
                "longestStreak" to longest,
                "lastActiveDate" to lastDate
            )
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(updates, SetOptions.merge())
        } catch (e: Exception) {
            // Silent fail
        }
    }

    /**
     * نافذة سفلية احتفالية تشرح سلسلة الأيام وتظهر التحديات والأوسمة
     */
    fun showStreakDialog(context: Context) {
        val info = getStreakInfo(context)
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_daily_streak, null)
        dialog.setContentView(view)

        val tvStreakNumber = view.findViewById<TextView>(R.id.tvStreakNumber)
        val tvStreakSubtitle = view.findViewById<TextView>(R.id.tvStreakSubtitle)
        val tvLongestStreak = view.findViewById<TextView>(R.id.tvLongestStreak)
        val tvTotalActiveDays = view.findViewById<TextView>(R.id.tvTotalActiveDays)
        val btnShareStreak = view.findViewById<View>(R.id.btnShareStreak)
        val btnCloseStreak = view.findViewById<View>(R.id.btnCloseStreak)

        tvStreakNumber.text = "${info.currentStreak}"
        tvLongestStreak.text = "${info.longestStreak} يوم"
        tvTotalActiveDays.text = "${info.totalActiveDays} يوم"

        tvStreakSubtitle.text = when {
            info.currentStreak >= 30 -> "أسطورة حقيقية! لقد أكملت شهراً كاملاً من الإلهام المستمر 👑"
            info.currentStreak >= 14 -> "رائع ومبهر! أسبوعان من العادة الإيجابية اليومية 💎"
            info.currentStreak >= 7 -> "ممتاز! أتممت أسبوعاً كاملاً من الإلهام والقراءة ⭐"
            info.currentStreak >= 3 -> "بداية قوية! حافظ على اشتعال شعلتك اليومية 🔥"
            else -> "بداية موفقة! عد غداً لمتابعة سلسلتك وإشعال الشعلة 🚀"
        }

        // دوائر أيام الأسبوع (السبت، الأحد، الإثنين، الثلاثاء، الأربعاء، الخميس، الجمعة)
        val weekDayCircleIds = listOf(
            R.id.circleDaySat, R.id.circleDaySun, R.id.circleDayMon,
            R.id.circleDayTue, R.id.circleDayWed, R.id.circleDayThu, R.id.circleDayFri
        )
        val weekDayIconIds = listOf(
            R.id.iconDaySat, R.id.iconDaySun, R.id.iconDayMon,
            R.id.iconDayTue, R.id.iconDayWed, R.id.iconDayThu, R.id.iconDayFri
        )

        for (i in 0 until 7) {
            val circle = view.findViewById<View>(weekDayCircleIds[i])
            val icon = view.findViewById<ImageView>(weekDayIconIds[i])
            val isActive = if (i < info.weeklyStatus.size) info.weeklyStatus[i] else false

            if (isActive) {
                circle.setBackgroundResource(R.drawable.bg_streak_day_active)
                icon.visibility = View.VISIBLE
            } else {
                circle.setBackgroundResource(R.drawable.bg_streak_day_inactive)
                icon.visibility = View.INVISIBLE
            }
        }

        // أوسمة الإنجازات المتتالية
        updateMilestoneUI(view, R.id.layoutMilestone3, R.id.pbMilestone3, R.id.tvMilestone3Status, info.currentStreak, 3, "وسام البداية القوية (3 أيام)")
        updateMilestoneUI(view, R.id.layoutMilestone7, R.id.pbMilestone7, R.id.tvMilestone7Status, info.currentStreak, 7, "وسام الأسبوع الذهبي (7 أيام)")
        updateMilestoneUI(view, R.id.layoutMilestone14, R.id.pbMilestone14, R.id.tvMilestone14Status, info.currentStreak, 14, "وسام المثابرة (14 يوماً)")
        updateMilestoneUI(view, R.id.layoutMilestone30, R.id.pbMilestone30, R.id.tvMilestone30Status, info.currentStreak, 30, "درع أسطورة الإلهام (30 يوماً)")

        btnShareStreak.setOnClickListener {
            shareStreakText(context, info.currentStreak)
        }

        btnCloseStreak.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateMilestoneUI(
        parent: View,
        layoutId: Int,
        pbId: Int,
        tvStatusId: Int,
        currentStreak: Int,
        target: Int,
        title: String
    ) {
        val layout = parent.findViewById<View>(layoutId) ?: return
        val pb = parent.findViewById<ProgressBar>(pbId) ?: return
        val tvStatus = parent.findViewById<TextView>(tvStatusId) ?: return

        pb.max = target
        pb.progress = currentStreak.coerceAtMost(target)

        if (currentStreak >= target) {
            tvStatus.text = "✓ تم الفتح"
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            layout.alpha = 1.0f
        } else {
            val remaining = target - currentStreak
            tvStatus.text = "باقي $remaining أيام"
            tvStatus.setTextColor(Color.parseColor("#94A3B8"))
            layout.alpha = 0.85f
        }
    }

    private fun shareStreakText(context: Context, streak: Int) {
        val message = "🔥 أنا في اليوم $streak على التوالي من قراءة ونشر الإلهام في تطبيق #منشوراتي!\nحمل التطبيق وابدأ سلسلتك اليومية الآن ✨"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "شارك إنجازك اليومي"))
    }
}

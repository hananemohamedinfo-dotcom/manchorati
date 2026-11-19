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

data class DayProgress(
    val dayName: String,
    val dateStr: String,
    val isActive: Boolean,
    val isToday: Boolean
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val isNewDay: Boolean,
    val lastActiveDate: String,
    val totalActiveDays: Int,
    val weeklyDays: List<DayProgress>,
    val weeklyStatus: List<Boolean> = weeklyDays.map { it.isActive }
)

object StreakManager {

    private const val PREFS_NAME = "manchorati_streak_prefs"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_LAST_DATE = "last_active_date"
    private const val KEY_START_DATE = "streak_cycle_start_date"
    private const val KEY_TOTAL_DAYS = "total_active_days"
    private const val KEY_ACTIVE_DATES_SET = "active_dates_set"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

    fun getStreakInfo(context: Context): StreakInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_CURRENT_STREAK, 1)
        val longest = prefs.getInt(KEY_LONGEST_STREAK, 1)
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val startDate = prefs.getString(KEY_START_DATE, "") ?: ""
        val totalDays = prefs.getInt(KEY_TOTAL_DAYS, 1)
        val datesSet = prefs.getStringSet(KEY_ACTIVE_DATES_SET, emptySet()) ?: emptySet()

        val weeklyDays = computeWeeklyDays(context, startDate, current, datesSet)

        return StreakInfo(
            currentStreak = current,
            longestStreak = longest,
            isNewDay = false,
            lastActiveDate = lastDate,
            totalActiveDays = totalDays,
            weeklyDays = weeklyDays
        )
    }

    /**
     * يتم استدعاؤها عند فتح التطبيق لحساب السلسلة وتحديثها إن كان يوماً جديداً
     */
    fun checkAndUpdateStreak(context: Context, onNewDayCelebration: ((Int) -> Unit)? = null): StreakInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = dateFormat.format(Date())

        val lastDateStr = prefs.getString(KEY_LAST_DATE, "") ?: ""
        var startDateStr = prefs.getString(KEY_START_DATE, "") ?: ""
        var currentStreak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        var longestStreak = prefs.getInt(KEY_LONGEST_STREAK, 0)
        var totalDays = prefs.getInt(KEY_TOTAL_DAYS, 0)
        val datesSet = (prefs.getStringSet(KEY_ACTIVE_DATES_SET, emptySet()) ?: emptySet()).toMutableSet()

        if (lastDateStr == todayStr) {
            // مسجل بالفعل لهذا اليوم
            val weeklyDays = computeWeeklyDays(context, startDateStr, currentStreak, datesSet)
            return StreakInfo(
                currentStreak = if (currentStreak <= 0) 1 else currentStreak,
                longestStreak = if (longestStreak <= 0) 1 else longestStreak,
                isNewDay = false,
                lastActiveDate = lastDateStr,
                totalActiveDays = if (totalDays <= 0) 1 else totalDays,
                weeklyDays = weeklyDays
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
            if (startDateStr.isEmpty()) {
                startDateStr = todayStr
            }
        } else {
            // انقطعت السلسلة أو أول مرة: يبدأ الحساب من هذا اليوم ليكون هو الأول في القائمة
            currentStreak = 1
            startDateStr = todayStr
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
            .putString(KEY_START_DATE, startDateStr)
            .putInt(KEY_TOTAL_DAYS, totalDays)
            .putStringSet(KEY_ACTIVE_DATES_SET, datesSet)
            .apply()

        // مزامنة مع فايرستور إن كان المستخدم مسجل الدخول
        syncStreakToFirestore(currentStreak, longestStreak, todayStr)

        val weeklyDays = computeWeeklyDays(context, startDateStr, currentStreak, datesSet)

        Handler(Looper.getMainLooper()).post {
            onNewDayCelebration?.invoke(currentStreak)
        }

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isNewDay = true,
            lastActiveDate = todayStr,
            totalActiveDays = totalDays,
            weeklyDays = weeklyDays
        )
    }

    /**
     * حساب الأيام السبعة بحيث تبدأ القائمة من اليوم الذي بدأ فيه المستخدم ليكون هو الأول في القائمة
     */
    private fun computeWeeklyDays(context: Context, startDateStr: String, currentStreak: Int, datesSet: Set<String>): List<DayProgress> {
        val result = mutableListOf<DayProgress>()
        val todayStr = dateFormat.format(Date())

        val cal = Calendar.getInstance()
        var baseDate: Date? = null
        if (startDateStr.isNotEmpty()) {
            try {
                baseDate = dateFormat.parse(startDateStr)
            } catch (_: Exception) {}
        }
        if (baseDate == null) {
            baseDate = Date()
        }

        cal.time = baseDate

        // إذا تخطى المستخدم 7 أيام، ينتقل العرض إلى حزمة الأسبوع الحالية مع الحفاظ على يوم البداية كأول يوم
        if (currentStreak > 7) {
            val cycleOffset = ((currentStreak - 1) / 7) * 7
            cal.add(Calendar.DAY_OF_YEAR, cycleOffset)
        }

        for (i in 0 until 7) {
            val dateStr = dateFormat.format(cal.time)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayName = getLocalizedDayName(context, dayOfWeek)
            val isToday = (dateStr == todayStr)
            val isActive = datesSet.contains(dateStr)

            result.add(DayProgress(
                dayName = dayName,
                dateStr = dateStr,
                isActive = isActive,
                isToday = isToday
            ))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun getLocalizedDayName(context: Context, dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SATURDAY -> context.getString(R.string.day_sat)
            Calendar.SUNDAY -> context.getString(R.string.day_sun)
            Calendar.MONDAY -> context.getString(R.string.day_mon)
            Calendar.TUESDAY -> context.getString(R.string.day_tue)
            Calendar.WEDNESDAY -> context.getString(R.string.day_wed)
            Calendar.THURSDAY -> context.getString(R.string.day_thu)
            Calendar.FRIDAY -> context.getString(R.string.day_fri)
            else -> ""
        }
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

    fun isStreakBadgeVisible(context: Context): Boolean {
        val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("show_streak_in_posts", true)
    }

    fun setStreakBadgeVisible(context: Context, visible: Boolean) {
        val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("show_streak_in_posts", visible).apply()
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            try {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("showStreakBadge", visible)
            } catch (_: Exception) {}
        }
    }

    fun formatDaysCount(context: Context, days: Int): String {
        val lang = LocaleHelper.getSelectedLanguage(context)
        return when (lang) {
            LocaleHelper.LANG_FR -> if (days <= 1) "$days jour" else "$days jours"
            LocaleHelper.LANG_EN -> if (days <= 1) "$days day" else "$days days"
            else -> when {
                days == 1 -> "يوم واحد"
                days == 2 -> "يومان"
                days in 3..10 -> "$days أيام"
                else -> "$days يوماً"
            }
        }
    }

    fun formatDaysCount(days: Int): String {
        return when {
            days == 1 -> "يوم واحد"
            days == 2 -> "يومان"
            days in 3..10 -> "$days أيام"
            else -> "$days يوماً"
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

        // فتح الـ BottomSheet بالكامل لمنع قص الأزرار السفلية
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true

        val tvStreakNumber = view.findViewById<TextView>(R.id.tvStreakNumber)
        val tvStreakSubtitle = view.findViewById<TextView>(R.id.tvStreakSubtitle)
        val tvLongestStreak = view.findViewById<TextView>(R.id.tvLongestStreak)
        val tvTotalActiveDays = view.findViewById<TextView>(R.id.tvTotalActiveDays)
        val btnShareStreak = view.findViewById<View>(R.id.btnShareStreak)
        val btnCloseStreak = view.findViewById<View>(R.id.btnCloseStreak)
        val switchStreak = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchShowStreakBadge)

        tvStreakNumber.text = "${info.currentStreak}"
        tvLongestStreak.text = formatDaysCount(context, info.longestStreak)
        tvTotalActiveDays.text = formatDaysCount(context, info.totalActiveDays)

        // إعداد زر التبديل لتفعيل/إخفاء الشعلة في المنشورات
        switchStreak?.isChecked = isStreakBadgeVisible(context)
        switchStreak?.setOnCheckedChangeListener { _, isChecked ->
            setStreakBadgeVisible(context, isChecked)
        }

        tvStreakSubtitle.text = when {
            info.currentStreak >= 100 -> context.getString(R.string.streak_subtitle_100)
            info.currentStreak >= 60 -> context.getString(R.string.streak_subtitle_60)
            info.currentStreak >= 30 -> context.getString(R.string.streak_subtitle_30)
            info.currentStreak >= 7 -> context.getString(R.string.streak_subtitle_7)
            else -> context.getString(R.string.streak_subtitle_default)
        }

        // دوائر أيام الأسبوع تبدأ من اليوم الذي بدأ فيه المستخدم ليكون هو الأول في القائمة
        val weekDayCircleIds = listOf(
            R.id.circleDay1, R.id.circleDay2, R.id.circleDay3,
            R.id.circleDay4, R.id.circleDay5, R.id.circleDay6, R.id.circleDay7
        )
        val weekDayIconIds = listOf(
            R.id.iconDay1, R.id.iconDay2, R.id.iconDay3,
            R.id.iconDay4, R.id.iconDay5, R.id.iconDay6, R.id.iconDay7
        )
        val weekDayNameIds = listOf(
            R.id.tvDayName1, R.id.tvDayName2, R.id.tvDayName3,
            R.id.tvDayName4, R.id.tvDayName5, R.id.tvDayName6, R.id.tvDayName7
        )

        for (i in 0 until 7) {
            val circle = view.findViewById<View>(weekDayCircleIds[i])
            val icon = view.findViewById<ImageView>(weekDayIconIds[i])
            val tvName = view.findViewById<TextView>(weekDayNameIds[i])
            val dayProg = if (i < info.weeklyDays.size) info.weeklyDays[i] else null

            if (dayProg != null) {
                tvName?.text = dayProg.dayName
                if (dayProg.isToday) {
                    tvName?.setTextColor(Color.parseColor("#F59E0B"))
                    tvName?.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    tvName?.setTextColor(view.context.getColor(R.color.text_secondary))
                    tvName?.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
                if (dayProg.isActive) {
                    circle?.setBackgroundResource(R.drawable.bg_streak_day_active)
                    icon?.visibility = View.VISIBLE
                } else {
                    circle?.setBackgroundResource(R.drawable.bg_streak_day_inactive)
                    icon?.visibility = View.INVISIBLE
                }
            }
        }

        // أوسمة الإنجازات المتتالية الموسعة (7، 30، 60، 100 يوم)
        updateMilestoneUI(view, R.id.layoutMilestone7, R.id.pbMilestone7, R.id.tvMilestone7Status, info.currentStreak, 7, context.getString(R.string.streak_milestone_7))
        updateMilestoneUI(view, R.id.layoutMilestone30, R.id.pbMilestone30, R.id.tvMilestone30Status, info.currentStreak, 30, context.getString(R.string.streak_milestone_30))
        updateMilestoneUI(view, R.id.layoutMilestone60, R.id.pbMilestone60, R.id.tvMilestone60Status, info.currentStreak, 60, context.getString(R.string.streak_milestone_60))
        updateMilestoneUI(view, R.id.layoutMilestone100, R.id.pbMilestone100, R.id.tvMilestone100Status, info.currentStreak, 100, context.getString(R.string.streak_milestone_100))

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
            tvStatus.text = parent.context.getString(R.string.streak_unlocked)
            tvStatus.setTextColor(Color.parseColor("#10B981"))
            layout.alpha = 1.0f
        } else {
            val remaining = target - currentStreak
            val lang = LocaleHelper.getSelectedLanguage(parent.context)
            val remainingText = when (lang) {
                LocaleHelper.LANG_FR -> if (remaining <= 1) "Plus que 1 jour" else "Plus que $remaining jours"
                LocaleHelper.LANG_EN -> if (remaining <= 1) "1 day left" else "$remaining days left"
                else -> when {
                    remaining == 1 -> "باقٍ يوم واحد"
                    remaining == 2 -> "باقٍ يومان"
                    remaining in 3..10 -> "باقي $remaining أيام"
                    else -> "باقي $remaining يوماً"
                }
            }
            tvStatus.text = remainingText
            tvStatus.setTextColor(Color.parseColor("#CBD5E1"))
            layout.alpha = 0.95f
        }
    }

    private fun shareStreakText(context: Context, streak: Int) {
        val lang = LocaleHelper.getSelectedLanguage(context)
        val message = when (lang) {
            LocaleHelper.LANG_FR -> "Je suis à mon ${streak}e jour consécutif d’inspiration et de lecture sur l’application #Manchorati !\nTéléchargez l’application et commencez votre série quotidienne maintenant"
            LocaleHelper.LANG_EN -> "I am on day $streak of consecutive daily inspiration and reading on #Manchorati app!\nDownload the app and start your streak now"
            else -> "أنا في اليوم $streak على التوالي من قراءة ونشر الإلهام في تطبيق #منشوراتي!\nحمل التطبيق وابدأ سلسلتك اليومية الآن"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.streak_share_btn)))
    }
}

package com.manchorati.www

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var switchMaster: SwitchMaterial
    private lateinit var layoutOptionsContainer: LinearLayout

    private lateinit var switchMorning: SwitchMaterial
    private lateinit var layoutMorningDetails: LinearLayout
    private lateinit var tvMorningTime: TextView
    private lateinit var spinnerMorningCategory: Spinner

    private lateinit var switchEvening: SwitchMaterial
    private lateinit var layoutEveningDetails: LinearLayout
    private lateinit var tvEveningTime: TextView
    private lateinit var spinnerEveningCategory: Spinner

    private lateinit var switchRandom: SwitchMaterial
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    private var morningHour = 8
    private var morningMinute = 30
    private var eveningHour = 20
    private var eveningMinute = 30

    private var categories: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        prefs = getSharedPreferences("app_notification_settings", Context.MODE_PRIVATE)

        initViews()
        setupSpinners()
        loadSavedSettings()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        switchMaster = findViewById(R.id.switchMasterNotifications)
        layoutOptionsContainer = findViewById(R.id.layoutOptionsContainer)

        switchMorning = findViewById(R.id.switchMorning)
        layoutMorningDetails = findViewById(R.id.layoutMorningDetails)
        tvMorningTime = findViewById(R.id.tvMorningTime)
        spinnerMorningCategory = findViewById(R.id.spinnerMorningCategory)

        switchEvening = findViewById(R.id.switchEvening)
        layoutEveningDetails = findViewById(R.id.layoutEveningDetails)
        tvEveningTime = findViewById(R.id.tvEveningTime)
        spinnerEveningCategory = findViewById(R.id.spinnerEveningCategory)

        switchRandom = findViewById(R.id.switchRandom)
        btnSave = findViewById(R.id.btnSaveSettings)
    }

    private fun setupSpinners() {
        val dbHelper = DatabaseHelper(this)
        val dbCategories = dbHelper.getAllCategoryNames()
        categories = listOf("افتراضي (الكل)") + dbCategories

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerMorningCategory.adapter = adapter
        spinnerEveningCategory.adapter = adapter
    }

    private fun loadSavedSettings() {
        val isMasterEnabled = prefs.getBoolean("master_enabled", true)
        switchMaster.isChecked = isMasterEnabled
        layoutOptionsContainer.visibility = if (isMasterEnabled) View.VISIBLE else View.GONE

        // صباح
        val isMorningEnabled = prefs.getBoolean("morning_enabled", true)
        morningHour = prefs.getInt("morning_hour", 8)
        morningMinute = prefs.getInt("morning_minute", 30)
        switchMorning.isChecked = isMorningEnabled
        layoutMorningDetails.visibility = if (isMorningEnabled) View.VISIBLE else View.GONE
        tvMorningTime.text = formatTime(morningHour, morningMinute)
        val morningPos = prefs.getInt("morning_category_pos", 0)
        if (morningPos < categories.size) {
            spinnerMorningCategory.setSelection(morningPos)
        }

        // مساء
        val isEveningEnabled = prefs.getBoolean("evening_enabled", true)
        eveningHour = prefs.getInt("evening_hour", 20)
        eveningMinute = prefs.getInt("evening_minute", 30)
        switchEvening.isChecked = isEveningEnabled
        layoutEveningDetails.visibility = if (isEveningEnabled) View.VISIBLE else View.GONE
        tvEveningTime.text = formatTime(eveningHour, eveningMinute)
        val eveningPos = prefs.getInt("evening_category_pos", 0)
        if (eveningPos < categories.size) {
            spinnerEveningCategory.setSelection(eveningPos)
        }

        // عشوائي
        switchRandom.isChecked = prefs.getBoolean("random_enabled", false)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            layoutOptionsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        switchMorning.setOnCheckedChangeListener { _, isChecked ->
            layoutMorningDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        switchEvening.setOnCheckedChangeListener { _, isChecked ->
            layoutEveningDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        findViewById<View>(R.id.btnMorningTime).setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                morningHour = h
                morningMinute = m
                tvMorningTime.text = formatTime(h, m)
            }, morningHour, morningMinute, false).show()
        }

        findViewById<View>(R.id.btnEveningTime).setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                eveningHour = h
                eveningMinute = m
                tvEveningTime.text = formatTime(h, m)
            }, eveningHour, eveningMinute, false).show()
        }

        btnSave.setOnClickListener {
            saveSettingsAndSchedule()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "م" else "ص"
        val h = if (hour % 12 == 0) 12 else hour % 12
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
    }

    private fun saveSettingsAndSchedule() {
        val morningSelectedPos = spinnerMorningCategory.selectedItemPosition
        val eveningSelectedPos = spinnerEveningCategory.selectedItemPosition

        val morningCategoryName = if (morningSelectedPos in categories.indices) categories[morningSelectedPos] else "افتراضي (الكل)"
        val eveningCategoryName = if (eveningSelectedPos in categories.indices) categories[eveningSelectedPos] else "افتراضي (الكل)"

        prefs.edit().apply {
            putBoolean("master_enabled", switchMaster.isChecked)

            putBoolean("morning_enabled", switchMorning.isChecked)
            putInt("morning_hour", morningHour)
            putInt("morning_minute", morningMinute)
            putInt("morning_category_pos", morningSelectedPos)
            putString("morning_category_name", morningCategoryName)

            putBoolean("evening_enabled", switchEvening.isChecked)
            putInt("evening_hour", eveningHour)
            putInt("evening_minute", eveningMinute)
            putInt("evening_category_pos", eveningSelectedPos)
            putString("evening_category_name", eveningCategoryName)

            putBoolean("random_enabled", switchRandom.isChecked)
            apply()
        }

        DailyNotificationHelper.rescheduleFromPreferences(this)
        Toast.makeText(this, "تم حفظ وتفعيل إعدادات الإشعارات بنجاح", Toast.LENGTH_SHORT).show()
        finish()
    }
}
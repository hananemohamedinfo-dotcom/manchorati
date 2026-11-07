package com.manchorati.www

import android.content.Context

data class VideoTheme(
    val id: String,
    val name: String,
    val subtitle: String,
    val emoji: String,
    val bgMotionId: String,
    val fontId: String,
    val textColor: String,
    val audioPreset: String,
    val animStyle: String,
    val animSpeed: Float = 1.0f
) {
    companion object {
        val PRESET_THEMES = listOf(
            VideoTheme(
                id = "gold_luxury",
                name = "فخامة ذهبية",
                subtitle = "خط أميري • بريق ذهبي • عود أصيل",
                emoji = "",
                bgMotionId = "GOLD",
                fontId = "amiri",
                textColor = "#FEF08A",
                audioPreset = "oud",
                animStyle = "word_by_word",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "aurora_magic",
                name = "شفق ساحر",
                subtitle = "خط كايرو • نعناع قطبي • أصوات طبيعة",
                emoji = "",
                bgMotionId = "AURORA",
                fontId = "cairo",
                textColor = "#D1FAE5",
                audioPreset = "nature",
                animStyle = "fade",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "night_stars",
                name = "سماء النجوم",
                subtitle = "خط تجوال • أبيض ناصع • بيانو ناعم",
                emoji = "",
                bgMotionId = "STARS",
                fontId = "tajawal",
                textColor = "#FFFFFF",
                audioPreset = "piano",
                animStyle = "typewriter",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "dreamy_rain",
                name = "قطرات المطر",
                subtitle = "خط رقعة • فضي هادئ • بيانو عذب",
                emoji = "",
                bgMotionId = "RAIN",
                fontId = "ruqaa",
                textColor = "#F1F5F9",
                audioPreset = "piano",
                animStyle = "typewriter",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "ocean_calm",
                name = "سكون البحر",
                subtitle = "خط تجوال • زرقة سماوية • أمواج هادئة",
                emoji = "",
                bgMotionId = "OCEAN",
                fontId = "tajawal",
                textColor = "#BAE6FD",
                audioPreset = "nature",
                animStyle = "slide_up",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "warm_sunset",
                name = "غروب دافئ",
                subtitle = "خط أميري • برتقالي غروبي • عود شجي",
                emoji = "",
                bgMotionId = "SUNSET",
                fontId = "amiri",
                textColor = "#FED7AA",
                audioPreset = "oud",
                animStyle = "glow",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "neon_synth",
                name = "أضواء النيون",
                subtitle = "خط تشانغا • وردي ونيون • نبض سينمائي",
                emoji = "",
                bgMotionId = "NEON",
                fontId = "changa",
                textColor = "#FDF4FF",
                audioPreset = "piano",
                animStyle = "zoom",
                animSpeed = 1.5f
            ),
            VideoTheme(
                id = "emerald_zen",
                name = "واحة السلام",
                subtitle = "خط أميري • زمرد ونور • طبيعة ساحرة",
                emoji = "",
                bgMotionId = "EMERALD",
                fontId = "amiri",
                textColor = "#ECFDF5",
                audioPreset = "nature",
                animStyle = "word_by_word",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "autumn_nostalgia",
                name = "أوراق الخريف",
                subtitle = "خط المسيري • كهرمان عتيق • عود هادئ",
                emoji = "",
                bgMotionId = "GOLD",
                fontId = "el_messiri",
                textColor = "#FDE68A",
                audioPreset = "oud",
                animStyle = "fade",
                animSpeed = 1.0f
            ),
            VideoTheme(
                id = "deep_cosmos",
                name = "شهب السماء",
                subtitle = "خط ريم كوفي • زرقة جليدية • إلهام كوني",
                emoji = "",
                bgMotionId = "STARS",
                fontId = "reem_kufi",
                textColor = "#BAE6FD",
                audioPreset = "piano",
                animStyle = "blur_in",
                animSpeed = 1.0f
            )
        )

        /**
         * Returns a random theme guaranteed to be different from the last applied one.
         */
        fun getRandomTheme(context: Context): VideoTheme {
            val prefs = context.getSharedPreferences("video_themes_prefs", Context.MODE_PRIVATE)
            val lastIndex = prefs.getInt("last_theme_index", -1)

            val availableIndices = PRESET_THEMES.indices.filter { it != lastIndex }
            val newIndex = if (availableIndices.isNotEmpty()) {
                availableIndices.random()
            } else {
                PRESET_THEMES.indices.random()
            }

            prefs.edit().putInt("last_theme_index", newIndex).apply()
            return PRESET_THEMES[newIndex]
        }

        fun getThemeById(id: String): VideoTheme? {
            return PRESET_THEMES.find { it.id == id }
        }
    }
}

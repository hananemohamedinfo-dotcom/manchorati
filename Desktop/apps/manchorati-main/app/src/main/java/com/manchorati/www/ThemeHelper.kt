package com.manchorati.www

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.res.ResourcesCompat

data class CardTemplate(
    val id: String,
    val nameRes: Int,
    val startColor: Int,
    val endColor: Int,
    val textColor: Int,
    val defaultFont: String = "tajawal",
    val borderColor: Int? = null,
    val borderWidthDp: Float = 0f,
    val isLight: Boolean = false
)

object ThemeHelper {

    val CARD_TEMPLATES = listOf(
        CardTemplate(
            id = "card_crimson_brand",
            nameRes = R.string.template_crimson_brand,
            startColor = Color.parseColor("#991B1B"),
            endColor = Color.parseColor("#DC2626"),
            textColor = Color.WHITE,
            defaultFont = "tajawal",
            borderColor = Color.parseColor("#EF4444"),
            borderWidthDp = 1.5f
        ),
        CardTemplate(
            id = "card_luxury",
            nameRes = R.string.template_dark_luxury,
            startColor = Color.parseColor("#0B0F19"),
            endColor = Color.parseColor("#1E293B"),
            textColor = Color.parseColor("#F8FAFC"),
            defaultFont = "tajawal",
            borderColor = Color.parseColor("#EAB308"),
            borderWidthDp = 1.5f
        ),
        CardTemplate(
            id = "card_classic_paper",
            nameRes = R.string.template_classic_paper,
            startColor = Color.parseColor("#F6F0EA"),
            endColor = Color.parseColor("#DFD2C0"),
            textColor = Color.parseColor("#1E293B"),
            defaultFont = "arefruqaa",
            borderColor = Color.parseColor("#D6C4AE"),
            borderWidthDp = 1.5f,
            isLight = true
        ),
        CardTemplate(
            id = "card_neon_cyber",
            nameRes = R.string.template_neon_cyber,
            startColor = Color.parseColor("#091A28"),
            endColor = Color.parseColor("#022C43"),
            textColor = Color.parseColor("#38BDF8"),
            defaultFont = "blakahollow",
            borderColor = Color.parseColor("#0284C7"),
            borderWidthDp = 2f
        ),
        CardTemplate(
            id = "card_sunset",
            nameRes = R.string.template_calm_sunset,
            startColor = Color.parseColor("#4A0E2E"),
            endColor = Color.parseColor("#9A3412"),
            textColor = Color.WHITE,
            defaultFont = "cairoplay",
            borderColor = Color.parseColor("#F59E0B"),
            borderWidthDp = 1.2f
        ),
        CardTemplate(
            id = "card_glass_royal",
            nameRes = R.string.template_glass,
            startColor = Color.parseColor("#1E1B4B"),
            endColor = Color.parseColor("#312E81"),
            textColor = Color.WHITE,
            defaultFont = "kufam",
            borderColor = Color.parseColor("#6366F1"),
            borderWidthDp = 1.5f
        ),
        CardTemplate(
            id = "card_emerald",
            nameRes = R.string.template_emerald,
            startColor = Color.parseColor("#064E3B"),
            endColor = Color.parseColor("#047857"),
            textColor = Color.WHITE,
            defaultFont = "tajawal",
            borderColor = Color.parseColor("#10B981"),
            borderWidthDp = 1.2f
        ),
        CardTemplate(
            id = "card_ocean_breeze",
            nameRes = R.string.template_ocean_breeze,
            startColor = Color.parseColor("#0F172A"),
            endColor = Color.parseColor("#0369A1"),
            textColor = Color.WHITE,
            defaultFont = "marhey",
            borderColor = Color.parseColor("#38BDF8"),
            borderWidthDp = 1.5f
        ),
        CardTemplate(
            id = "card_pure_minimal",
            nameRes = R.string.template_pure_minimal,
            startColor = Color.parseColor("#FFFFFF"),
            endColor = Color.parseColor("#F8FAFC"),
            textColor = Color.parseColor("#0F172A"),
            defaultFont = "tajawal",
            borderColor = Color.parseColor("#E2E8F0"),
            borderWidthDp = 1.5f,
            isLight = true
        )
    )

    fun getCardTemplate(id: String): CardTemplate? {
        return CARD_TEMPLATES.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getFontTypeface(context: Context, fontId: String): Typeface? {
        val fontRes = when (fontId) {
            "arefruqaa" -> R.font.arefruqaa
            "blakahollow" -> R.font.blakahollow
            "cairoplay" -> R.font.cairoplay
            "kufam" -> R.font.kufam
            "marhey" -> R.font.marhey
            "notonastaliqurdu" -> R.font.notonastaliqurdu
            "oi" -> R.font.oi
            "tajawal" -> R.font.tajawal
            else -> R.font.tajawal
        }
        return try {
            ResourcesCompat.getFont(context, fontRes)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * يدعم:
     * 1. معرف قالب بطاقة مثل "card_luxury" أو "card_classic_paper"
     * 2. تدرج بلونين مفصولين بفاصلة مثل "#FF512F,#DD2476"
     * 3. لون عادي مثل "#0F766E" أو "#FFFFFF"
     */
    fun applyBackground(view: View, bgCode: String, radiusDp: Float = 16f) {
        val density = view.context.resources.displayMetrics.density
        val radiusPx = radiusDp * density

        val template = getCardTemplate(bgCode)
        if (template != null) {
            val drawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(template.startColor, template.endColor)
            ).apply {
                cornerRadius = radiusPx
                if (template.borderColor != null && template.borderWidthDp > 0) {
                    setStroke((template.borderWidthDp * density).toInt(), template.borderColor)
                }
            }
            view.background = drawable
            return
        }

        val drawable = if (bgCode.contains(",")) {
            val parts = bgCode.split(",")
            val startColor = Color.parseColor(parts[0].trim())
            val endColor = Color.parseColor(parts[1].trim())
            GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(startColor, endColor)).apply {
                cornerRadius = radiusPx
            }
        } else {
            val solidColor = try {
                Color.parseColor(bgCode)
            } catch (e: Exception) {
                Color.WHITE
            }
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusPx
                setColor(solidColor)
                if (solidColor == Color.WHITE) {
                    setStroke((1 * density).toInt(), Color.parseColor("#E2E8F0"))
                }
            }
        }
        view.background = drawable
    }

    fun getTextColor(bgCode: String): Int {
        val template = getCardTemplate(bgCode)
        if (template != null) {
            return template.textColor
        }

        if (bgCode.equals("#FFFFFF", ignoreCase = true) || bgCode.equals("white", ignoreCase = true)) {
            return Color.parseColor("#1E293B")
        }
        return try {
            val color = if (bgCode.contains(",")) {
                Color.parseColor(bgCode.split(",")[0].trim())
            } else {
                Color.parseColor(bgCode)
            }
            val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(color)
            if (luminance > 0.55) Color.parseColor("#0F172A") else Color.WHITE
        } catch (e: Exception) {
            Color.WHITE
        }
    }
}

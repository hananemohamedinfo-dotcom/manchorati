package com.manchorati.www

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.res.ResourcesCompat

object ThemeHelper {

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
     * يدعم لون عادي مثل "#0F766E" أو تدرج بلونين مفصولين بفاصلة مثل "#FF512F,#DD2476"
     */
    fun applyBackground(view: View, bgCode: String, radiusDp: Float = 16f) {
        val density = view.context.resources.displayMetrics.density
        val radiusPx = radiusDp * density

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
        return if (bgCode.equals("#FFFFFF", ignoreCase = true) || bgCode.equals("white", ignoreCase = true)) {
            Color.parseColor("#1E293B")
        } else {
            Color.WHITE
        }
    }
}
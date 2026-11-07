package com.manchorati.www

import android.widget.ImageView
import coil.load
import coil.transform.CircleCropTransformation

fun ImageView.loadUserAvatar(photoUrl: String?) {
    // السطر السحري: إزالة الفلتر اللوني (Tint) الذي يجعل الصورة بيضاء
    this.imageTintList = null 

    if (photoUrl.isNullOrEmpty()) {
        setImageResource(R.drawable.ic_profile)
        return
    }
    if (photoUrl.startsWith("avatar_")) {
        val resId = context.resources.getIdentifier(photoUrl, "drawable", context.packageName)
        if (resId != 0) {
            load(resId) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        } else {
            setImageResource(R.drawable.ic_profile)
        }
    } else {
        load(photoUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
            transformations(CircleCropTransformation())
        }
    }
}

/**
 * دالة عالمية لتنسيق الأرقام والعدادات في التطبيق بالكامل
 * < 1,000 -> الرقم كاملاً (مثل: 950)
 * >= 1,000 -> بصيغة K (مثل: 1.5k, 12k)
 * >= 1,000,000 -> بصيغة M (مثل: 1.2M, 5M)
 */
fun Long.formatCount(): String {
    return when {
        this >= 1_000_000L -> {
            val formatted = String.format(java.util.Locale.US, "%.1f", this / 1_000_000.0).removeSuffix(".0")
            "${formatted}M"
        }
        this >= 1_000L -> {
            val formatted = String.format(java.util.Locale.US, "%.1f", this / 1_000.0).removeSuffix(".0")
            "${formatted}k"
        }
        else -> this.toString()
    }
}

fun Int.formatCount(): String = this.toLong().formatCount()

package com.manchorati.www

import android.content.Context

data class Category(
    val id: Int,
    val name: String,
    val postsCount: Int = 0,
    var hasNew: Boolean = false
) {
    fun getLocalizedName(context: Context): String {
        return name
    }

    companion object {
        fun getLocalizedCategoryName(context: Context, rawName: String): String {
            return rawName.trim()
        }
    }
}

package com.manchorati.www

import android.content.Context
import android.content.SharedPreferences

object LocalLikesManager {
    private const val PREFS_NAME = "local_curated_likes"

    fun isLiked(context: Context, postId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(postId, false)
    }

    fun toggleLike(context: Context, postId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getBoolean(postId, false)
        val newState = !current
        prefs.edit().putBoolean(postId, newState).apply()
        return newState
    }
}

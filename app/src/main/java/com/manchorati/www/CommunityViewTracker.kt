package com.manchorati.www

import android.content.Context

object CommunityViewTracker {
    private const val PREFS_NAME = "seen_community_posts_prefs"
    private const val KEY_SEEN_IDS = "seen_post_ids"

    fun isPostSeen(context: Context, postId: String): Boolean {
        if (postId.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenSet = prefs.getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()
        return seenSet.contains(postId)
    }

    fun markPostSeen(context: Context, postId: String) {
        if (postId.isEmpty() || postId.startsWith("vpost_") || postId.startsWith("curated_")) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()).toMutableSet()
        if (current.add(postId)) {
            if (current.size > 1000) {
                val trimmed = current.toList().takeLast(800).toSet()
                prefs.edit().putStringSet(KEY_SEEN_IDS, trimmed).apply()
            } else {
                prefs.edit().putStringSet(KEY_SEEN_IDS, current).apply()
            }
        }
    }
}

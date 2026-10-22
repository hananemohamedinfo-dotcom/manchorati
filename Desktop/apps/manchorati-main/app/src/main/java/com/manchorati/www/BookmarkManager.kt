package com.manchorati.www

import android.content.Context
import android.content.SharedPreferences

object BookmarkManager {

    private const val PREFS_NAME = "community_bookmarks_prefs"
    private const val KEY_SAVED_IDS = "saved_community_post_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        val uid = AuthManager.currentUser?.uid ?: "local_guest"
        return context.getSharedPreferences("${PREFS_NAME}_$uid", Context.MODE_PRIVATE)
    }

    fun isBookmarked(context: Context, postId: String): Boolean {
        if (postId.isEmpty()) return false
        val set = getPrefs(context).getStringSet(KEY_SAVED_IDS, emptySet()) ?: emptySet()
        return set.contains(postId)
    }

    fun toggleBookmark(context: Context, postId: String): Boolean {
        if (postId.isEmpty()) return false
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_SAVED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val willBeBookmarked = !currentSet.contains(postId)

        if (willBeBookmarked) {
            currentSet.add(postId)
        } else {
            currentSet.remove(postId)
        }

        prefs.edit().putStringSet(KEY_SAVED_IDS, currentSet).apply()

        // Sync with Firestore if logged in
        val uid = AuthManager.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(uid)
            val updateOp = if (willBeBookmarked) {
                com.google.firebase.firestore.FieldValue.arrayUnion(postId)
            } else {
                com.google.firebase.firestore.FieldValue.arrayRemove(postId)
            }
            userRef.update("savedCommunityPosts", updateOp).addOnFailureListener {
                userRef.set(mapOf("savedCommunityPosts" to listOf(postId)), com.google.firebase.firestore.SetOptions.merge())
            }
        }

        return willBeBookmarked
    }

    fun getBookmarkedPostIds(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_SAVED_IDS, emptySet()) ?: emptySet()
    }
}

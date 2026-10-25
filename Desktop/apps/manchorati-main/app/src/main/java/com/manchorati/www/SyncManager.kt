package com.manchorati.www

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context, private val dbHelper: DatabaseHelper) {

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val act = cm.getNetworkCapabilities(net) ?: return false
        return act.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun checkAndUpdatePosts(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext false

        try {
            val prefs = context.getSharedPreferences("category_sync_prefs", Context.MODE_PRIVATE)
            val lastSyncTime = prefs.getLong("last_sync_timestamp", 0L)

            val query = if (lastSyncTime == 0L) {
                FirebaseFirestore.getInstance().collection("category_updates")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
            } else {
                FirebaseFirestore.getInstance().collection("category_updates")
                    .whereGreaterThan("timestamp", lastSyncTime)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
            }

            val snapshot = query.get().await()
            if (snapshot.isEmpty) return@withContext false

            var addedAny = false
            var maxTimestamp = lastSyncTime

            for (doc in snapshot.documents) {
                val catName = doc.getString("category_name")?.trim() ?: ""
                val content = doc.getString("content")?.trim() ?: ""
                val ts = doc.getLong("timestamp") ?: 0L

                if (catName.isNotEmpty() && content.isNotEmpty()) {
                    val added = dbHelper.insertNewPostFromRemote(catName, content)
                    if (added) addedAny = true
                }
                if (ts > maxTimestamp) {
                    maxTimestamp = ts
                }
            }

            if (maxTimestamp > lastSyncTime) {
                prefs.edit().putLong("last_sync_timestamp", maxTimestamp).apply()
            }

            return@withContext addedAny
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}

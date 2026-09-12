package com.manchorati.www

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context, private val dbHelper: DatabaseHelper) {

    // الرابط المباشر من JSONBin
    private val jsonUrl = "https://api.jsonbin.io/v3/b/6a9caa7ff5f4af5e296fab6d?meta=false"

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val act = cm.getNetworkCapabilities(net) ?: return false
        return act.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun checkAndUpdatePosts(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext false

        try {
            val request = Request.Builder()
                .url(jsonUrl)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val jsonData = response.body?.string() ?: return@withContext false
            val rootObj = JSONObject(jsonData)

            if (!rootObj.has("new_posts")) return@withContext false

            val postsArray = rootObj.getJSONArray("new_posts")
            if (postsArray.length() == 0) return@withContext false

            var addedAny = false
            for (i in 0 until postsArray.length()) {
                val item = postsArray.getJSONObject(i)
                val catName = item.optString("category_name", "").trim()
                val content = item.optString("content", "").trim()

                if (catName.isNotEmpty() && content.isNotEmpty()) {
                    val added = dbHelper.insertNewPostFromRemote(catName, content)
                    if (added) addedAny = true
                }
            }
            return@withContext addedAny
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
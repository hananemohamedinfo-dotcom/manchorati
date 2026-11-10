package com.manchorati.www

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AiChatHistoryManager {

    private const val PREFS_NAME = "manchorati_ai_chat_history"
    private const val KEY_PREFIX = "chat_history_"

    fun saveHistory(context: Context, userId: String, messages: List<AiChatMessage>) {
        if (userId.isEmpty()) return
        val effectiveId = if (userId.isNotBlank()) userId else "local_guest"
        try {
            val jsonArray = JSONArray()
            for (msg in messages) {
                val obj = JSONObject()
                when (msg) {
                    is AiChatMessage.User -> {
                        obj.put("type", "user")
                        obj.put("text", msg.text)
                        obj.put("timestamp", msg.timestamp)
                        obj.put("id", msg.id)
                        jsonArray.put(obj)
                    }
                    is AiChatMessage.Bot -> {
                        obj.put("type", "bot")
                        obj.put("quote", msg.quote)
                        obj.put("topic", msg.topic)
                        obj.put("isRealAi", msg.isRealAi)
                        obj.put("modelTag", msg.modelTag)
                        obj.put("id", msg.id)
                        jsonArray.put(obj)
                    }
                    is AiChatMessage.Loading -> {
                        // لا نحفظ حالة التحميل المؤقتة
                    }
                }
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PREFIX + effectiveId, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadHistory(context: Context, userId: String): List<AiChatMessage> {
        val effectiveId = if (userId.isNotBlank()) userId else "local_guest"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_PREFIX + effectiveId, null) ?: return emptyList()

        val result = mutableListOf<AiChatMessage>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                when (obj.optString("type")) {
                    "user" -> {
                        val text = obj.optString("text", "")
                        val timestamp = obj.optString("timestamp", "")
                        result.add(AiChatMessage.User(text = text, timestamp = timestamp))
                    }
                    "bot" -> {
                        val quote = obj.optString("quote", "")
                        val topic = obj.optString("topic", "حكم وأمثال")
                        val isRealAi = obj.optBoolean("isRealAi", true)
                        val modelTag = obj.optString("modelTag", "Google Gemini")
                        result.add(
                            AiChatMessage.Bot(
                                quote = quote,
                                topic = topic,
                                isRealAi = isRealAi,
                                modelTag = modelTag
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun clearHistory(context: Context, userId: String) {
        val effectiveId = if (userId.isNotBlank()) userId else "local_guest"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PREFIX + effectiveId)
            .apply()
    }
}

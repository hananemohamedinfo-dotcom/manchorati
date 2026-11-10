package com.manchorati.www

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class AiChatMessage(val id: String = UUID.randomUUID().toString()) {

    data class User(
        val text: String,
        val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    ) : AiChatMessage()

    data class Bot(
        val quote: String,
        val topic: String = "حكم وأمثال",
        val isRealAi: Boolean = true,
        val modelTag: String = "Google Gemini"
    ) : AiChatMessage()

    object Loading : AiChatMessage()
}

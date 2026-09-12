package com.manchorati.www

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val toolbar = findViewById<Toolbar>(R.id.toolbarContact)
        toolbar.setNavigationOnClickListener { finish() }

        val spType = findViewById<Spinner>(R.id.spFeedbackType)
        val etMessage = findViewById<EditText>(R.id.etFeedbackMessage)
        val btnSend = findViewById<Button>(R.id.btnSendFeedback)

        val options = arrayOf(
            "✍️ اقتراح منشور أو حكمة جديدة",
            "💡 اقتراح فكرة أو تحسين للتطبيق",
            "⚠️ الإبلاغ عن مشكلة فنية",
            "📩 رسالة عامة للإدارة"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spType.adapter = adapter

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isEmpty()) {
                etMessage.error = "الرجاء كتابة نص الرسالة"
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            val user = FirebaseAuth.getInstance().currentUser
            val feedbackData = hashMapOf(
                "userId" to (user?.uid ?: "anonymous"),
                "userName" to (user?.displayName ?: "فاعل خير"),
                "userEmail" to (user?.email ?: ""),
                "type" to spType.selectedItem.toString(),
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance().collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener {
                    Toast.makeText(this, "شكراً لك! تم إرسال رسالتك بنجاح 🌟", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSend.isEnabled = true
                    Toast.makeText(this, "تعذر الإرسال، حاول لاحقاً: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
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

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applyLayoutDirection(this)
        setContentView(R.layout.activity_contact)

        val toolbar = findViewById<Toolbar>(R.id.toolbarContact)
        toolbar.setNavigationOnClickListener { finish() }

        val spType = findViewById<Spinner>(R.id.spFeedbackType)
        val etMessage = findViewById<EditText>(R.id.etFeedbackMessage)
        val btnSend = findViewById<Button>(R.id.btnSendFeedback)

        val options = arrayOf(
            getString(R.string.feedback_type_suggest_quote),
            getString(R.string.feedback_type_suggest_feature),
            getString(R.string.feedback_type_report_bug),
            getString(R.string.feedback_type_general)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spType.adapter = adapter

        btnSend.setOnClickListener {
            val message = etMessage.text.toString().trim()
            if (message.isEmpty()) {
                etMessage.error = getString(R.string.feedback_empty_error)
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            val user = FirebaseAuth.getInstance().currentUser
            val feedbackData = hashMapOf(
                "userId" to (user?.uid ?: "anonymous"),
                "userName" to (user?.displayName ?: "User"),
                "userEmail" to (user?.email ?: ""),
                "type" to spType.selectedItem.toString(),
                "message" to message,
                "timestamp" to System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance().collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.feedback_sent_success), Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    btnSend.isEnabled = true
                    Toast.makeText(this, "${getString(R.string.feedback_send_failed)}: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
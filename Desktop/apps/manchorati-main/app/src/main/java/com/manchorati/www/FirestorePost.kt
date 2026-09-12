package com.manchorati.www

import com.google.firebase.Timestamp

data class FirestorePost(
    var id: String = "",
    val content: String = "",
    val category: String = "",
    val categoryId: Int = 1,
    val isFeatured: Int = 0,
    val createdAt: Timestamp? = null
)
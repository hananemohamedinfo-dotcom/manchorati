package com.manchorati.www

data class Post(
    val id: Int = 0,
    val categoryId: Int = 0,
    val categoryName: String = "",
    val content: String = "",
    var isFavorite: Int = 0,
    var isFeatured: Int = 0
)
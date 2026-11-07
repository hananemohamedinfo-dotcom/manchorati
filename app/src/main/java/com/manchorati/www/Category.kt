package com.manchorati.www

data class Category(
    val id: Int,
    val name: String,
    val postsCount: Int = 0,
    var hasNew: Boolean = false
)
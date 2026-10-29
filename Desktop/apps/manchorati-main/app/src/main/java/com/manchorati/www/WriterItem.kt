package com.manchorati.www

data class WriterItem(
    val userId: String,
    var displayName: String,
    var username: String,
    var photoUrl: String,
    var bio: String = "",
    var badge: String = "",
    var followersCount: Long = 0L,
    var weeklyLikes: Long = 0L,
    var totalLikes: Long = 0L,
    var postsCount: Int = 0,
    var isFollowing: Boolean = false,
    var rank: Int = 0
)

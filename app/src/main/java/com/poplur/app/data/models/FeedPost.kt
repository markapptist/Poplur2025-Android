package com.poplur.app.data.models

data class FeedPost(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val mediaUrl: String,
    val radius: Double,
    val viewCount: Int,
    val userId: String
)

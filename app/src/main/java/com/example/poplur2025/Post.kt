package com.example.poplur2025

// Post.kt
import android.os.Parcelable
import kotlinx.parcelize.Parcelize // For Parcelable implementation

// Enum for media type
enum class MediaType {
    NONE, IMAGE, VIDEO
}

@Parcelize // If you need to pass Post objects via Intents or Navigation Component arguments
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String? = null,
    val textContent: String? = null,

    // New Media Fields
    val mediaType: MediaType = MediaType.NONE,
    val mediaUrl: String? = null, // URL to the image or video in Firebase Storage or other CDN

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String? = null,
    val imageUrl: String? = null, // Make sure this line exists and is spelled correctly

    // New Radius Field
    val radius: Double = 0.0, // Radius in meters

    val timestamp: Long = 0L
) : Parcelable { // Implement Parcelable
    // Add a no-argument constructor if using Firebase Firestore
    constructor() : this(
        postId = "",
        userId = "",
        username = "",
        userAvatarUrl = null,
        textContent = null,
        mediaType = MediaType.NONE,
        mediaUrl = null,
        latitude = 0.0,
        longitude = 0.0,
        locationName = null,
        imageUrl = null, // Changed from 0.0 to null
        radius = 0.0,    // Also ensure this is correct, it was 0L before, now 0.0 for Double
        timestamp = 0L
    )
}

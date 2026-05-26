package com.apptists.poplur2025.data

import com.google.firebase.Timestamp

enum class AppTab(val label: String) { Home("Home"), Map("Map"), Camera("Camera"), Feed("Feed"), Messages("Messages"), Profile("Profile"), Settings("Settings") }

data class UserProfile(
    val id: String = "demo-user",
    val username: String = "mark",
    val displayName: String = "Mark",
    val bio: String = "Building Poplur.",
    val avatarUrl: String? = null,
    val radius: Int = 1250,
    val followers: Int = 1200,
    val posts: Int = 12
)

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val caption: String = "",
    val mediaUrl: String? = null,
    val videoUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Timestamp? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val detectedObjects: List<String> = emptyList()
)

data class ChatUser(val id: String, val name: String, val avatarUrl: String? = null)
data class ChatMessage(val id: String, val senderId: String, val text: String, val createdAtMillis: Long = System.currentTimeMillis())
data class ConversationThread(val id: String, val title: String, val lastMessage: String, val participants: List<ChatUser>)
data class CityDominance(val city: String, val leader: String, val score: Int, val lat: Double, val lng: Double)
data class ARFilter(val id: String, val name: String, val type: String)
data class DetectedObject(val label: String, val confidence: Float)

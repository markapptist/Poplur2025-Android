package com.poplur.app.data.models

import java.util.UUID

data class DMUser(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val handle: String,
    val avatar: String = "person"
)

data class DMMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

data class DMThread(
    val id: String = UUID.randomUUID().toString(),
    val participantIds: List<String>, // includes current user id
    val messages: List<DMMessage> = emptyList()
) {
    val lastMessage: DMMessage? get() = messages.lastOrNull()
}

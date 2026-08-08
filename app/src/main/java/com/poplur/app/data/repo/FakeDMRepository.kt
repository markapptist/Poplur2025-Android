package com.poplur.app.data.repo

import com.poplur.app.data.models.DMMessage
import com.poplur.app.data.models.DMThread
import com.poplur.app.data.models.DMUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class FakeDMRepository {

    private val me = DMUser(id = UUID.randomUUID().toString(), displayName = "You", handle = "you", avatar = "person")
    private val users = listOf(
        DMUser(displayName = "Mia", handle = "mia", avatar = "person"),
        DMUser(displayName = "Jay", handle = "jay", avatar = "person"),
        DMUser(displayName = "Alex Johnson", handle = "alexj", avatar = "person"),
        DMUser(displayName = "Samira Khan", handle = "samk", avatar = "person"),
        DMUser(displayName = "Diego Morales", handle = "diegom", avatar = "person"),
        DMUser(displayName = "Mina Park", handle = "minap", avatar = "person")
    )

    private val _threads = MutableStateFlow<List<DMThread>>(emptyList())
    val threads: StateFlow<List<DMThread>> = _threads

    init {
        seedThread("Alex Johnson", "Hey! This is a demo DM thread.", "Reply to keep the conversation going.")
        seedThread("Jay", "The map animation is ready")
        seedThread("Mia", "Let’s post at the food market")
    }

    fun currentUser(): DMUser = me
    fun allUsers(): List<DMUser> = users

    private fun seedThread(displayName: String, vararg messages: String) {
        val other = users.firstOrNull { it.displayName == displayName } ?: return
        val thread = createThread(listOf(other))
        messages.forEach { message ->
            send(thread.id, other.id, message)
        }
    }

    fun createThread(with: List<DMUser>): DMThread {
        val ids = listOf(me.id) + with.map { it.id }
        val thread = DMThread(id = UUID.randomUUID().toString(), participantIds = ids, messages = emptyList())
        _threads.value = listOf(thread) + _threads.value
        return thread
    }

    fun thread(threadId: String): DMThread? = _threads.value.firstOrNull { it.id == threadId }

    fun displayNameForParticipants(participantIds: List<String>): String {
        val others = participantIds.filter { it != me.id }
        val names = others.mapNotNull { id -> users.firstOrNull { it.id == id }?.displayName }
        return names.joinToString(", ")
    }

    fun user(id: String): DMUser? = if (id == me.id) me else users.firstOrNull { it.id == id }

    fun send(threadId: String, fromUserId: String = me.id, text: String) {
        val threads = _threads.value.toMutableList()
        val idx = threads.indexOfFirst { it.id == threadId }
        if (idx == -1) return

        val msg = DMMessage(
            senderId = fromUserId,
            text = text,
            isRead = (fromUserId == me.id)
        )

        val thread = threads[idx]
        val updated = thread.copy(messages = thread.messages + msg)
        threads.removeAt(idx)
        threads.add(0, updated) // move to top
        _threads.value = threads
    }

    fun markRead(threadId: String) {
        val threads = _threads.value.toMutableList()
        val idx = threads.indexOfFirst { it.id == threadId }
        if (idx == -1) return
        val t = threads[idx]
        val updated = t.copy(messages = t.messages.map { it.copy(isRead = true) })
        threads[idx] = updated
        _threads.value = threads
    }

    fun hasUnread(thread: DMThread): Boolean {
        return thread.messages.any { !it.isRead && it.senderId != me.id }
    }
}

package com.poplur.app.ui.vm

import androidx.lifecycle.ViewModel
import com.poplur.app.data.repo.FakeDMRepository

class MessagesViewModel(
    private val repo: FakeDMRepository = FakeDMRepository()
) : ViewModel() {
    val threads = repo.threads
    val currentUser = repo.currentUser()
    val users = repo.allUsers()

    fun createThread(userIds: List<String>): String {
        val chosen = users.filter { userIds.contains(it.id) }
        val t = repo.createThread(with = chosen)
        return t.id
    }

    fun displayName(participantIds: List<String>) = repo.displayNameForParticipants(participantIds)
    fun hasUnread(threadId: String): Boolean = repo.thread(threadId)?.let { repo.hasUnread(it) } ?: false
    fun thread(threadId: String) = repo.thread(threadId)
    fun user(id: String) = repo.user(id)

    fun send(threadId: String, text: String) = repo.send(threadId = threadId, text = text)
    fun sendFrom(threadId: String, fromUserId: String, text: String) = repo.send(threadId = threadId, fromUserId = fromUserId, text = text)
    fun markRead(threadId: String) = repo.markRead(threadId)
}

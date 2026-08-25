package com.apptists.poplur2025

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real direct messages, for the web chat screen. Port of the iOS
 * `PoplurDirectMessages` so both phones read and write the same threads.
 *
 * The chat lives in the web bundle and had a hardcoded array of two canned
 * messages behind it. The bundle has no Firebase at all — every piece of real
 * data crosses this bridge — so DMs are read and written here and pushed to the
 * page, the same way profile data already is.
 *
 * Thread ids are the two uids sorted and joined, so opening a chat with the
 * same person twice can never fork into two threads and no lookup is needed to
 * find an existing one. The rules enforce that shape too, or a client could
 * create a thread at an id a later pair would also resolve to.
 */
object PoplurDirectMessages {

    private const val TAG = "PoplurDM"

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * One listener per open thread, torn down when the page leaves it. Without
     * this a listener would survive every thread switch and the page would keep
     * receiving messages for conversations it had already closed.
     */
    private var threadListener: ListenerRegistration? = null
    private var threadStateListener: ListenerRegistration? = null
    private var listListener: ListenerRegistration? = null
    private var openThreadId: String? = null

    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    fun threadId(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"

    // ---- Reading -----------------------------------------------------------

    /**
     * Live list of this account's threads, newest first. [onChange] fires again
     * on every update, so the page never has to poll.
     */
    fun observeThreads(onChange: (JSONArray) -> Unit) {
        val me = uid
        if (me == null) { onChange(JSONArray()); return }
        listListener?.remove()
        listListener = db.collection("dmThreads")
            .whereArrayContains("participants", me)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // A failed index or rule is worth saying out loud: the page
                    // would otherwise just show an empty inbox forever.
                    Log.w(TAG, "thread list failed", error)
                    onChange(JSONArray())
                    return@addSnapshotListener
                }
                val threads = JSONArray()
                snapshot?.documents?.forEach { document ->
                    @Suppress("UNCHECKED_CAST")
                    val participants = document.get("participants") as? List<String> ?: emptyList()
                    val other = participants.firstOrNull { it != me } ?: ""
                    val title = displayName(other, document.get("participantNames"))
                    val updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    @Suppress("UNCHECKED_CAST")
                    val readAtBy = document.get("readAtBy") as? Map<String, Timestamp>
                    val readAt = readAtBy?.get(me)?.toDate()?.time ?: 0L
                    val unread = if (document.getString("lastSenderId") != me && updatedAt > readAt) 1 else 0
                    threads.put(JSONObject().apply {
                        put("id", document.id)
                        put("otherId", other)
                        put("title", title)
                        put("initials", initials(title))
                        put(
                            "preview",
                            document.getString("lastMessage")?.takeIf { it.isNotBlank() }
                                ?: "No messages yet"
                        )
                        put("unread", unread)
                        put("updatedAtMillis", updatedAt)
                    })
                }
                onChange(threads)
            }
    }

    /** Live messages for one thread, oldest first (the order a chat reads in). */
    fun observeThread(
        threadId: String,
        onChange: (JSONArray) -> Unit,
        onState: (JSONObject) -> Unit
    ) {
        val me = uid
        if (me == null) { onChange(JSONArray()); return }
        threadListener?.remove()
        threadStateListener?.remove()
        openThreadId = threadId
        val threadRef = db.collection("dmThreads").document(threadId)
        threadListener = threadRef
            .collection("messages")
            .orderBy("createdAt")
            .limit(200)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "thread failed", error)
                    onChange(JSONArray())
                    return@addSnapshotListener
                }
                val messages = JSONArray()
                snapshot?.documents?.forEach { document ->
                    messages.put(JSONObject().apply {
                        put("id", document.id)
                        // The page only needs to know which side to draw it on.
                        put("from", if (document.getString("senderId") == me) "me" else "them")
                        put("text", document.getString("text").orEmpty())
                        put("createdAtMillis", document.getTimestamp("createdAt")?.toDate()?.time ?: 0L)
                        put("pending", document.metadata.hasPendingWrites())
                    })
                }
                onChange(messages)
            }
        threadStateListener = threadRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "thread state failed", error)
                return@addSnapshotListener
            }
            val document = snapshot ?: return@addSnapshotListener
            @Suppress("UNCHECKED_CAST")
            val participants = document.get("participants") as? List<String> ?: return@addSnapshotListener
            if (!participants.contains(me)) return@addSnapshotListener
            val other = participants.firstOrNull { it != me }
            @Suppress("UNCHECKED_CAST")
            val typingBy = document.get("typingBy") as? Map<String, Timestamp>
            @Suppress("UNCHECKED_CAST")
            val readAtBy = document.get("readAtBy") as? Map<String, Timestamp>
            onState(JSONObject().apply {
                put("threadId", threadId)
                other?.let { otherUid ->
                    typingBy?.get(otherUid)?.toDate()?.time?.let { put("typingAtMillis", it) }
                    readAtBy?.get(otherUid)?.toDate()?.time?.let { put("otherReadAtMillis", it) }
                }
            })

            val updatedAt = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            val ownReadAt = readAtBy?.get(me)?.toDate()?.time ?: 0L
            if (document.getString("lastSenderId") != me && updatedAt > ownReadAt) {
                scope.launch { markRead(threadId) }
            }
        }
    }

    fun closeThread() {
        val closingThreadId = openThreadId
        val me = uid
        if (closingThreadId != null && me != null) {
            db.collection("dmThreads").document(closingThreadId)
                .update(FieldPath.of("typingBy", me), FieldValue.delete())
                .addOnFailureListener { Log.w(TAG, "typing cleanup failed", it) }
        }
        threadListener?.remove()
        threadListener = null
        threadStateListener?.remove()
        threadStateListener = null
        openThreadId = null
    }

    fun stop() {
        closeThread()
        listListener?.remove()
        listListener = null
    }

    // ---- Writing -----------------------------------------------------------

    /**
     * Sends into a thread, creating it if this is the first message.
     *
     * The parent and message are one atomic batch. Its deterministic message
     * id makes a retry safe: an uncertain network result cannot create a
     * duplicate bubble, and rules can inspect the post-batch parent.
     */
    suspend fun send(otherUid: String, text: String, clientMessageId: String): Boolean {
        val me = uid ?: return false
        if (otherUid.isEmpty() || otherUid == me || !validMessageId(clientMessageId)) return false
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 2000) return false

        val threadRef = db.collection("dmThreads").document(threadId(me, otherUid))
        val messageRef = threadRef.collection("messages").document(clientMessageId)
        return try {
            if (messageRef.get().await().exists()) return true
            val batch = db.batch()
            batch.set(
                threadRef,
                mapOf(
                    "participants" to listOf(me, otherUid).sorted(),
                    "lastMessage" to trimmed,
                    "lastSenderId" to me,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.set(
                messageRef,
                mapOf(
                    "senderId" to me,
                    "text" to trimmed,
                    "clientMessageId" to clientMessageId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            true
        } catch (error: Exception) {
            Log.w(TAG, "send failed", error)
            false
        }
    }

    suspend fun setTyping(threadId: String, typing: Boolean) {
        val me = uid ?: return
        if (threadId != openThreadId) return
        val value: Any = if (typing) FieldValue.serverTimestamp() else FieldValue.delete()
        try {
            db.collection("dmThreads").document(threadId)
                .update(FieldPath.of("typingBy", me), value)
                .await()
        } catch (error: Exception) {
            Log.w(TAG, "typing update failed", error)
        }
    }

    private suspend fun markRead(threadId: String) {
        val me = uid ?: return
        if (threadId != openThreadId) return
        try {
            db.collection("dmThreads").document(threadId)
                .update(FieldPath.of("readAtBy", me), FieldValue.serverTimestamp())
                .await()
        } catch (error: Exception) {
            Log.w(TAG, "read receipt failed", error)
        }
    }

    private fun validMessageId(value: String): Boolean =
        value.length in 20..100 && value.matches(Regex("^msg-[A-Za-z0-9-]{16,96}$"))

    // ---- Names -------------------------------------------------------------

    /**
     * Cached so a thread list of 50 does not become 50 profile reads on every
     * snapshot — the list listener fires on every new message.
     */
    private val nameCache = mutableMapOf<String, String>()

    private fun displayName(otherUid: String, participantNames: Any?): String {
        if (otherUid.isEmpty()) return "Poplur"
        nameCache[otherUid]?.let { return it }
        // A denormalised name on the thread avoids a read entirely when present.
        @Suppress("UNCHECKED_CAST")
        val names = participantNames as? Map<String, String>
        val denormalised = names?.get(otherUid)
        if (!denormalised.isNullOrEmpty()) {
            nameCache[otherUid] = denormalised
            return denormalised
        }
        scope.launch {
            val profile = runCatching {
                db.collection("users").document(otherUid).get().await()
            }.getOrNull()
            val name = profile?.getString("username")?.takeIf { it.isNotBlank() }
                ?: profile?.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: otherUid.take(8)
            nameCache[otherUid] = name
        }
        return otherUid.take(8)
    }

    private fun initials(name: String): String {
        val joined = name
            .replace("@", "")
            .split(" ")
            .mapNotNull { it.firstOrNull() }
            .take(2)
            .joinToString("")
            .uppercase()
        return joined.ifEmpty { "P" }
    }
}

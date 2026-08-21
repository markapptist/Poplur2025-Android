package com.apptists.poplur2025

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private var listListener: ListenerRegistration? = null

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
                        put("unread", 0)
                    })
                }
                onChange(threads)
            }
    }

    /** Live messages for one thread, oldest first (the order a chat reads in). */
    fun observeThread(threadId: String, onChange: (JSONArray) -> Unit) {
        val me = uid
        if (me == null) { onChange(JSONArray()); return }
        threadListener?.remove()
        threadListener = db.collection("dmThreads").document(threadId)
            .collection("messages")
            .orderBy("createdAt")
            .limit(200)
            .addSnapshotListener { snapshot, error ->
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
                    })
                }
                onChange(messages)
            }
    }

    fun closeThread() {
        threadListener?.remove()
        threadListener = null
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
     * The thread doc is written with merge BEFORE the message so the rules can
     * see `participants` when the message create is evaluated — a message into
     * a thread that does not exist yet is denied, because the rule reads the
     * parent.
     */
    suspend fun send(otherUid: String, text: String): Boolean {
        val me = uid ?: return false
        if (otherUid.isEmpty()) return false
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 2000) return false

        val threadRef = db.collection("dmThreads").document(threadId(me, otherUid))
        return try {
            threadRef.set(
                mapOf(
                    "participants" to listOf(me, otherUid).sorted(),
                    "lastMessage" to trimmed,
                    "lastSenderId" to me,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            threadRef.collection("messages").add(
                mapOf(
                    "senderId" to me,
                    "text" to trimmed,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (error: Exception) {
            Log.w(TAG, "send failed", error)
            false
        }
    }

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

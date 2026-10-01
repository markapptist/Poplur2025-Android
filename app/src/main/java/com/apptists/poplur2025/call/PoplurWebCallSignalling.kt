package com.apptists.poplur2025.call

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import org.json.JSONObject

/**
 * Firestore relay for the post-login WebView's WebRTC calls.
 *
 * WebRTC media stays inside the clay-map page. This class only writes the
 * rendezvous document and returns descriptions/candidates to that page. It is
 * deliberately separate from [CallSessionManager]: that manager owns the
 * older Agora voice-call surface, while this relay owns only
 * `provider: "webrtc"` documents so the two cannot answer each other's calls.
 */
object PoplurWebCallSignalling {
    private const val WEBRTC_PROVIDER = "webrtc"

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var callListener: ListenerRegistration? = null
    private var candidateListener: ListenerRegistration? = null
    private var incomingListener: ListenerRegistration? = null

    /** Delivers the same payload shape used by the iOS WebView bridge. */
    var onEvent: ((JSONObject) -> Unit)? = null

    /** A call answered on the phone's ringing screen while the page loads. The page
     *  joins it from `poplurCallAnswer` (PostLoginWebActivity), so its ringing
     *  document must not also arrive as a second, popup-raising incoming call. */
    @Volatile
    var answeringCallId: String? = null

    /** Open a call. `video` is an intent flag only; media remains WebRTC-in-page. */
    fun startCall(
        otherUid: String,
        callerName: String?,
        video: Boolean,
        context: String? = null,
        completion: (String?) -> Unit,
    ) {
        val authenticatedUid = auth.currentUser?.uid
        if (authenticatedUid == null || authenticatedUid.isEmpty()) {
            completion(null)
            return
        }
        if (otherUid.isBlank() || otherUid == authenticatedUid) {
            completion(null)
            return
        }
        val ref = db.collection("calls").document()
        val displayName = callerName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName
            ?: "Unknown"
        val payload = hashMapOf<String, Any>(
            "participants" to listOf(authenticatedUid, otherUid).sorted(),
            "callerId" to authenticatedUid,
            "callerName" to displayName,
            "calleeId" to otherUid,
            "status" to "ringing",
            // The callee receives this before it sees an SDP offer, which is
            // what lets an incoming FaceTime request camera media up front.
            "video" to video,
            "createdAt" to FieldValue.serverTimestamp(),
            "provider" to WEBRTC_PROVIDER,
        )
        // Keep legacy call documents unchanged; only the known House owner is
        // persisted so the recipient can suppress duplicate global controls.
        if (context == "house") payload["context"] = "house"
        ref.set(payload)
            .addOnSuccessListener {
                observe(ref.id)
                completion(ref.id)
            }
            .addOnFailureListener { completion(null) }
    }

    fun publishDescription(callId: String, type: String, sdp: String) {
        if (callId.isBlank()) return
        val key = if (type == "offer") "offer" else "answer"
        val payload = hashMapOf<String, Any>(
            key to mapOf("type" to type, "sdp" to sdp),
        )
        // startCall already writes ringing. A late offer must not regress a
        // rendezvous the callee has already advanced to connected.
        if (key == "answer") payload["status"] = "connected"
        db.collection("calls").document(callId)
            .set(
                payload,
                SetOptions.merge(),
            )
    }

    fun publishCandidate(callId: String, candidate: Map<String, Any?>) {
        val uid = auth.currentUser?.uid ?: return
        if (callId.isBlank()) return
        val payload = HashMap<String, Any>()
        candidate.forEach { (key, value) -> if (value != null) payload[key] = value }
        payload["senderId"] = uid
        payload["createdAt"] = FieldValue.serverTimestamp()
        db.collection("calls").document(callId).collection("candidates").add(payload)
    }

    fun endCall(callId: String) {
        if (callId.isBlank()) return
        stopObserving()
        val ref = db.collection("calls").document(callId)
        // Mark first so the far-side snapshot resolves cleanly before cleanup.
        ref.set(mapOf("status" to "ended"), SetOptions.merge())
        ref.collection("candidates").get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach { document -> document.reference.delete() }
            ref.delete()
        }
    }

    fun declineCall(callId: String) {
        if (callId.isBlank()) return
        db.collection("calls").document(callId)
            .update("status", "declined")
    }

    /** Whether a call is still ringing, for an Answer that took a while to open. */
    fun isRinging(callId: String, result: (Boolean) -> Unit) {
        if (callId.isBlank()) { result(false); return }
        db.collection("calls").document(callId).get()
            .addOnSuccessListener { snapshot -> result(snapshot.getString("status") == "ringing") }
            .addOnFailureListener { result(false) }
    }

    /** Decline from the ringing notification. Only a call that is still ringing:
     *  a late tap must not end a call answered elsewhere or already hung up. */
    fun declineIfRinging(callId: String, done: () -> Unit = {}) {
        if (callId.isBlank()) { done(); return }
        val ref = db.collection("calls").document(callId)
        db.runTransaction { transaction ->
            if (transaction.get(ref).getString("status") == "ringing") {
                transaction.update(ref, "status", "declined")
            }
        }.addOnCompleteListener { done() }
    }

    fun acceptCall(callId: String) {
        if (callId.isBlank()) return
        // Start both relays before the page publishes its answer; otherwise
        // very early caller ICE candidates can be lost.
        observe(callId)
        db.collection("calls").document(callId)
            .update("status", "connected")
    }

    /** Relay the remote SDP, candidates and terminal call state to the WebView. */
    fun observe(callId: String) {
        stopObserving()
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("calls").document(callId)

        callListener = ref.addSnapshotListener { snapshot, _ ->
            val data = snapshot?.data
            if (data == null) {
                emit("poplurCallEnded") { put("callId", callId) }
                stopObserving()
                return@addSnapshotListener
            }
            val status = data["status"] as? String ?: ""
            if (status == "ended" || status == "declined") {
                emit("poplurCallEnded") {
                    put("callId", callId)
                    put("reason", status)
                }
                stopObserving()
                return@addSnapshotListener
            }
            val wanted = if (data["callerId"] as? String == uid) "answer" else "offer"
            @Suppress("UNCHECKED_CAST")
            val description = data[wanted] as? Map<String, Any?>
            val sdp = description?.get("sdp") as? String ?: return@addSnapshotListener
            emit("poplurCallDescription") {
                put("callId", callId)
                put("sdpType", description["type"] as? String ?: wanted)
                put("sdp", sdp)
            }
        }

        candidateListener = ref.collection("candidates")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges
                    ?.filter { it.type == DocumentChange.Type.ADDED }
                    ?.forEach { change ->
                        val candidate = HashMap(change.document.data)
                        if (candidate["senderId"] == uid) return@forEach
                        candidate.remove("senderId")
                        candidate.remove("createdAt")
                        emit("poplurCallCandidate") {
                            put("callId", callId)
                            put("candidate", JSONObject(candidate))
                        }
                    }
            }
    }

    /** Watch only WebRTC calls; the existing Agora listener owns all others. */
    fun observeIncoming() {
        val uid = auth.currentUser?.uid ?: return
        incomingListener?.remove()
        incomingListener = db.collection("calls")
            // participants, not calleeId. The read rule is
            // `uid in resource.data.participants`, and Firestore must prove a
            // query satisfies it STATICALLY — a calleeId filter cannot, so this
            // listener only worked because a duplicate `match /calls` allowed
            // any signed-in read and nullified the participant scoping.
            .whereArrayContains("participants", uid)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documentChanges
                    ?.forEach { change ->
                        val data = change.document.data
                        // The query matches every ringing call this account is IN,
                        // including the ones it placed. Only the callee rings: from
                        // 4 Sep the caller's own page received its outgoing call as
                        // incoming and declined it as busy.
                        if (!IncomingCalls.isIncomingFor(data, uid)) return@forEach
                        val callId = change.document.id
                        if (change.type == DocumentChange.Type.REMOVED) {
                            if (callId == answeringCallId) answeringCallId = null
                            emit("poplurCallIncomingUnavailable") {
                                put("callId", callId)
                            }
                            return@forEach
                        }
                        if (change.type != DocumentChange.Type.ADDED) return@forEach
                        if (callId == answeringCallId) return@forEach
                        if (!IncomingCalls.isFresh(data, System.currentTimeMillis())) return@forEach
                        emit("poplurCallIncoming") {
                            put("callId", callId)
                            put("callerId", data["callerId"] as? String ?: "")
                            put("callerName", data["callerName"] as? String ?: "Unknown")
                            put("video", data["video"] as? Boolean ?: false)
                            put("context", data["context"] as? String ?: "default")
                        }
                    }
            }
    }

    fun stopObserving() {
        callListener?.remove(); callListener = null
        candidateListener?.remove(); candidateListener = null
    }

    fun stopAll() {
        stopObserving()
        incomingListener?.remove(); incomingListener = null
        onEvent = null
    }

    private fun emit(type: String, populate: JSONObject.() -> Unit = {}) {
        onEvent?.invoke(JSONObject().apply {
            put("type", type)
            populate()
        })
    }
}

/** Which ringing calls are this account's to answer. Free of Firebase so it can be
 *  tested on its own. */
internal object IncomingCalls {
    private const val WEBRTC_PROVIDER = "webrtc"

    /** A WebRTC rendezvous on which this account is the callee, not the caller.
     *  The older Agora calls (CallSessionManager) share the collection and query. */
    fun isIncomingFor(data: Map<String, Any?>, uid: String): Boolean =
        uid.isNotEmpty() &&
            data["provider"] as? String == WEBRTC_PROVIDER &&
            data["calleeId"] as? String == uid &&
            data["callerId"] as? String != uid

    /** Nothing ages a call out on the server, so a caller killed mid-ring leaves a
     *  document that would ring again on every launch. The caller gives up after
     *  45 s; anything this old is stale. A document still waiting for its server
     *  timestamp is new by definition. */
    const val MAX_RING_AGE_MS = 90_000L

    fun isFresh(data: Map<String, Any?>, nowMillis: Long): Boolean {
        val created = data["createdAt"] as? com.google.firebase.Timestamp ?: return true
        return nowMillis - created.toDate().time <= MAX_RING_AGE_MS
    }
}

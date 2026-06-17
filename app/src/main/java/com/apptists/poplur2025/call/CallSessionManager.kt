package com.apptists.poplur2025.call

import com.apptists.poplur2025.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// Signaling collection: calls/{documentId}
// Fields: callerId, callerName, calleeId, calleeName, channelId, status, createdAt
// Status flow: ringing → connected | declined | ended
//
// TODO: replace UserProfile.id with the callee's real Firebase Auth UID once
// real user profiles store their uid in Firestore.
object CallSessionManager {

    sealed class CallState {
        object Idle : CallState()
        data class Outgoing(val calleeName: String) : CallState()
        data class Incoming(val callerName: String, val channelId: String, val documentId: String) : CallState()
        data class Connected(val calleeName: String, val channelId: String) : CallState()
        data class Ended(val reason: EndReason) : CallState()
    }

    enum class EndReason { HangUp, Declined, NoAnswer }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private var callDocumentId: String? = null
    private var documentListener: ListenerRegistration? = null
    private var incomingListener: ListenerRegistration? = null
    private var timerJob: Job? = null
    private var timeoutJob: Job? = null

    // Call once after sign-in is confirmed (e.g., in PoplurRoot LaunchedEffect).
    fun startListeningForIncomingCalls() {
        val myId = auth.currentUser?.uid ?: return
        incomingListener?.remove()
        incomingListener = db.collection("calls")
            .whereEqualTo("calleeId", myId)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, _ ->
                if (_callState.value !is CallState.Idle) return@addSnapshotListener
                val doc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                val data = doc.data ?: return@addSnapshotListener
                val callerName = data["callerName"] as? String ?: "Unknown"
                val channelId = data["channelId"] as? String ?: doc.id
                _callState.value = CallState.Incoming(
                    callerName = callerName,
                    channelId = channelId,
                    documentId = doc.id
                )
            }
    }

    fun placeCall(callee: UserProfile) {
        if (_callState.value !is CallState.Idle) return
        scope.launch {
            val myUid = auth.currentUser?.uid ?: "anonymous"
            val myName = auth.currentUser?.displayName ?: "Poplur User"
            val channelId = UUID.randomUUID().toString()
            val payload = hashMapOf(
                "callerId" to myUid,
                "callerName" to myName,
                "calleeId" to callee.id,
                "calleeName" to callee.displayName,
                "channelId" to channelId,
                "status" to "ringing",
                "createdAt" to FieldValue.serverTimestamp()
            )
            val ref = runCatching { db.collection("calls").add(payload).await() }.getOrNull() ?: return@launch
            callDocumentId = ref.id
            _callState.value = CallState.Outgoing(calleeName = callee.displayName)
            listenToDocument(ref.id, calleeName = callee.displayName)
            startNoAnswerTimeout()

            AgoraCallEngine.joinChannel(channelId)
        }
    }

    fun acceptCall(channelId: String, documentId: String, callerName: String) {
        scope.launch {
            callDocumentId = documentId
            setStatus("connected", documentId)
            _callState.value = CallState.Connected(calleeName = callerName, channelId = channelId)
            startElapsedTimer()

            AgoraCallEngine.joinChannel(channelId)
        }
    }

    fun declineCall(documentId: String) {
        scope.launch {
            setStatus("declined", documentId)
            reset(CallState.Idle)
        }
    }

    fun endCall() {
        scope.launch {
            timeoutJob?.cancel()
            timerJob?.cancel()
            documentListener?.remove()
            documentListener = null
            val id = callDocumentId
            reset(CallState.Ended(EndReason.HangUp))
            if (id != null) setStatus("ended", id)

            AgoraCallEngine.leaveChannel()
            delay(1_800)
            if (_callState.value is CallState.Ended) _callState.value = CallState.Idle
        }
    }

    // Simulates a local-only outgoing call (no Firestore signaling) — used for DM calls.
    fun simulateLocalCall(calleeName: String) {
        if (_callState.value !is CallState.Idle) return
        _callState.value = CallState.Outgoing(calleeName = calleeName)
        timeoutJob = scope.launch {
            delay(3_000)
            if (_callState.value is CallState.Outgoing) {
                _callState.value = CallState.Connected(calleeName = calleeName, channelId = "local")
                startElapsedTimer()
            }
        }
    }

    fun cancelSimulatedCall() {
        timeoutJob?.cancel()
        timerJob?.cancel()
        _callState.value = CallState.Idle
        _elapsedSeconds.value = 0
    }

    private fun listenToDocument(id: String, calleeName: String) {
        documentListener?.remove()
        documentListener = db.collection("calls").document(id)
            .addSnapshotListener { snap, _ ->
                val data = snap?.data ?: return@addSnapshotListener
                val status = data["status"] as? String ?: return@addSnapshotListener
                scope.launch {
                    when (status) {
                        "connected" -> {
                            if (_callState.value !is CallState.Outgoing) return@launch
                            val channelId = data["channelId"] as? String ?: id
                            _callState.value = CallState.Connected(calleeName = calleeName, channelId = channelId)
                            startElapsedTimer()
                        }
                        "declined" -> {
                            reset(CallState.Ended(EndReason.Declined))
                            delay(1_800)
                            if (_callState.value is CallState.Ended) _callState.value = CallState.Idle
                        }
                        "ended" -> {
                            if (_callState.value !is CallState.Connected) return@launch
                            reset(CallState.Ended(EndReason.HangUp))
                            delay(1_800)
                            if (_callState.value is CallState.Ended) _callState.value = CallState.Idle
                        }
                    }
                }
            }
    }

    private fun startNoAnswerTimeout() {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(30_000)
            if (_callState.value !is CallState.Outgoing) return@launch
            callDocumentId?.let { setStatus("ended", it) }
            reset(CallState.Ended(EndReason.NoAnswer))
            delay(1_800)
            if (_callState.value is CallState.Ended) _callState.value = CallState.Idle
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        _elapsedSeconds.value = 0
        timerJob = scope.launch {
            while (true) {
                delay(1_000)
                _elapsedSeconds.value++
            }
        }
    }

    private suspend fun setStatus(status: String, id: String? = null) {
        val docId = id ?: callDocumentId ?: return
        runCatching { db.collection("calls").document(docId).update("status", status).await() }
    }

    private fun reset(state: CallState) {
        timeoutJob?.cancel()
        timerJob?.cancel()
        documentListener?.remove()
        documentListener = null
        callDocumentId = null
        _elapsedSeconds.value = 0
        _callState.value = state
    }

    fun formattedDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}

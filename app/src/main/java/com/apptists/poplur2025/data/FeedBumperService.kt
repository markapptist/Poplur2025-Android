package com.apptists.poplur2025.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.apptists.poplur2025.util.PoplurEQSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

// Feed commercial announcer: Claude API script + Android TTS.
// Mirror of iOS BumperService, adapted for Android TextToSpeech.

object FeedBumperService {

    private const val TAG = "FeedBumperService"

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingScript: String? = null
    private var lastSpokenSlotId: String = ""
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val engine = tts ?: return@TextToSpeech
                engine.language = Locale.US
                engine.voices
                    ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                    ?.maxByOrNull { it.quality }
                    ?.let { engine.voice = it }
                engine.setSpeechRate(0.90f)
                engine.setPitch(1.02f)
                pendingScript?.let { speak(it) }
                pendingScript = null
            } else {
                setActive(false)
                Log.w(TAG, "TTS init failed: $status")
            }
        }
    }

    suspend fun prefetch(post: Post, channelName: String, slotId: String, context: Context) {
        if (slotId == lastSpokenSlotId) return
        lastSpokenSlotId = slotId
        init(context)

        // Show overlay + speak fallback immediately (before any network call)
        val immediate = fallbackScript(post, channelName)
        setActive(true)
        if (ttsReady) speak(immediate) else pendingScript = immediate

        // Upgrade with Claude's script in background — queued after fallback finishes
        try {
            val aiScript = generateScript(post, channelName)
            if (lastSpokenSlotId == slotId) {
                tts?.speak(aiScript, TextToSpeech.QUEUE_ADD, null, "bumper_ai")
                val ms = ((aiScript.split(" ").size / 2.5) * 1000).toLong().coerceIn(3_000L, 12_000L)
                mainHandler.removeCallbacksAndMessages(null)
                mainHandler.postDelayed({ setActive(false) }, ms)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Claude unavailable, fallback already playing")
        }
    }

    fun dismiss() {
        tts?.stop()
        setActive(false)
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun speak(script: String) {
        tts?.speak(script, TextToSpeech.QUEUE_FLUSH, null, "bumper")
        // Auto-hide overlay after estimated speech duration
        val words = script.split(" ").size
        val ms = ((words / 2.5) * 1000).toLong().coerceIn(3_000L, 12_000L)
        mainHandler.postDelayed({ setActive(false) }, ms)
    }

    private fun setActive(active: Boolean) {
        _isActive.value = active
        if (active) {
            PoplurEQSync.begin(PoplurEQSync.Lane.FeedBumper)
        } else {
            PoplurEQSync.end(PoplurEQSync.Lane.FeedBumper)
        }
    }

    // The host-sentence prompt lives on the backend (POST /bumper/script);
    // the device only sends the structured context.
    private suspend fun generateScript(post: Post, channelName: String): String {
        var ctx = "Channel: $channelName"
        if (post.locationName.isNotBlank()) ctx += "\nLocation: ${post.locationName}"
        if (post.isLiveStream) ctx += "\nThis is a LIVE broadcast."
        if (post.likes > 100) ctx += "\nTrending: ${post.likes} likes."
        if (post.radius > 0) {
            val r = if (post.radius >= 1000) "${post.radius / 1000} km" else "${post.radius} m"
            ctx += "\nRadius reach: $r"
        }

        return try {
            PoplurAIBackendClient().bumperScript(context = ctx).trim()
                .ifBlank { fallbackScript(post, channelName) }
        } catch (e: AIStackException.BackendError) {
            fallbackScript(post, channelName)
        }
    }

    private fun fallbackScript(post: Post, channelName: String): String {
        val radiusPart = if (post.radius >= 1000) ", reaching ${post.radius / 1000} km"
                         else if (post.radius > 0) ", reaching ${post.radius} m"
                         else ""
        return if (post.locationName.isNotBlank()) {
            if (post.isLiveStream)
                "Live right now from ${post.locationName} on $channelName$radiusPart — stay tuned."
            else
                "Up next on $channelName — fresh content from ${post.locationName}$radiusPart."
        } else {
            "Coming up on $channelName — the next video is on its way$radiusPart."
        }
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        tts = null
        ttsReady = false
        _isActive.value = false
        lastSpokenSlotId = ""
    }
}

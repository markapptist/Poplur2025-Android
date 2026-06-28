package com.apptists.poplur2025.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// Feed commercial announcer: Claude API script + Android TTS.
// Mirror of iOS BumperService, adapted for Android TextToSpeech.

object FeedBumperService {

    private const val TAG = "FeedBumperService"
    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5-20251001"

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
        _isActive.value = true
        if (ttsReady) speak(immediate) else pendingScript = immediate

        // Upgrade with Claude's script in background — queued after fallback finishes
        try {
            val aiScript = generateScript(post, channelName)
            if (lastSpokenSlotId == slotId) {
                tts?.speak(aiScript, TextToSpeech.QUEUE_ADD, null, "bumper_ai")
                val ms = ((aiScript.split(" ").size / 2.5) * 1000).toLong().coerceIn(3_000L, 12_000L)
                mainHandler.removeCallbacksAndMessages(null)
                mainHandler.postDelayed({ _isActive.value = false }, ms)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Claude unavailable, fallback already playing")
        }
    }

    fun dismiss() {
        tts?.stop()
        _isActive.value = false
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun speak(script: String) {
        tts?.speak(script, TextToSpeech.QUEUE_FLUSH, null, "bumper")
        // Auto-hide overlay after estimated speech duration
        val words = script.split(" ").size
        val ms = ((words / 2.5) * 1000).toLong().coerceIn(3_000L, 12_000L)
        mainHandler.postDelayed({ _isActive.value = false }, ms)
    }

    private suspend fun generateScript(post: Post, channelName: String): String =
        withContext(Dispatchers.IO) {
            val key = Secrets.claudeApiKey
            if (key.isEmpty()) return@withContext fallbackScript(post, channelName)

            var ctx = "Channel: $channelName"
            if (post.locationName.isNotBlank()) ctx += "\nLocation: ${post.locationName}"
            if (post.isLiveStream) ctx += "\nThis is a LIVE broadcast."
            if (post.likes > 100) ctx += "\nTrending: ${post.likes} likes."
            if (post.radius > 0) {
                val r = if (post.radius >= 1000) "${post.radius / 1000} km" else "${post.radius} m"
                ctx += "\nRadius reach: $r"
            }

            val prompt = """
You are a charismatic TV host for Poplur, a social video app.
Using the details below, write ONE natural spoken sentence (12–22 words) that a host would say live on air to introduce the next video.
Reference the channel, location, and radius reach naturally — don't just list them.
Be warm, specific, and engaging. Return ONLY the spoken sentence — no quotes, no labels.

$ctx
""".trimIndent()

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 80)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                }))
            }

            val url = URL(ENDPOINT)
            val conn = url.openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("x-api-key", key)
                conn.setRequestProperty("anthropic-version", "2023-06-01")
                conn.setRequestProperty("content-type", "application/json")
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

                if (conn.responseCode != 200) return@withContext fallbackScript(post, channelName)

                val text = JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONArray("content").getJSONObject(0).getString("text").trim()
                text.ifBlank { fallbackScript(post, channelName) }
            } finally {
                conn.disconnect()
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

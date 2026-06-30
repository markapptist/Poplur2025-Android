package com.apptists.poplur2025.data

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

enum class AIVideoStyle(
    val label: String,
    val emoji: String,
    val promptSuffix: String
) {
    PoplurTV(
        "Poplur TV", "📺",
        "broadcast television quality, professional studio lighting, clean news anchor aesthetic, 4K crisp"
    ),
    Movie90s(
        "90s Movie", "🎬",
        "1990s live-action film, 35mm grain, practical creature effects, fog machine, dramatic colored lights, handheld camera, no CGI"
    ),
    MiamiNightclub(
        "Miami Nightclub", "🌴",
        "Miami Vice nightlife, neon pink and teal lights, palm trees, luxury rooftop, cinematic slow motion, 80s aesthetic"
    ),
    MonsterMovie(
        "Monster Movie", "👾",
        "creature feature, practical rubber suit, horror lighting, fog, abandoned warehouse, retro B-movie"
    ),
    NewsReport(
        "News Report", "📰",
        "live TV news broadcast, lower third graphics, serious anchor, breaking news chyron, studio set"
    ),
    MusicVideo(
        "Music Video", "🎵",
        "cinematic music video, high production value, dramatic lighting, concept visual storytelling, vertical format"
    ),
    MapStory(
        "Map Story", "🗺️",
        "documentary location coverage, aerial establishing shots, street-level journalism, authentic"
    ),
    VHS(
        "VHS Tape", "📼",
        "vintage VHS camcorder recording, scanlines, color bleeding, tape distortion, 1980s home video"
    ),
    FoodAd(
        "Food Ad", "🍔",
        "professional food photography commercial, macro close-ups, steam rising, golden hour lighting, appetizing"
    )
}

sealed class AIVideoJobStatus {
    object Idle : AIVideoJobStatus()
    object Storyboarding : AIVideoJobStatus()
    data class GeneratingClip(val current: Int, val total: Int) : AIVideoJobStatus()
    object Complete : AIVideoJobStatus()
    data class Failed(val reason: String) : AIVideoJobStatus()
}

data class AIVideoStoryboardShot(
    val shot: Int,
    val type: String,
    val prompt: String,
    val duration: Int = 5
)

data class AIVideoStoryboard(
    val title: String,
    val style: String,
    val shots: List<AIVideoStoryboardShot>
)

object AIVideoService {

    private const val TAG = "AIVideoService"

    private val _status = MutableStateFlow<AIVideoJobStatus>(AIVideoJobStatus.Idle)
    val status: StateFlow<AIVideoJobStatus> = _status

    private val _storyboard = MutableStateFlow<AIVideoStoryboard?>(null)
    val storyboard: StateFlow<AIVideoStoryboard?> = _storyboard

    private val _clipUrls = MutableStateFlow<List<String>>(emptyList())
    val clipUrls: StateFlow<List<String>> = _clipUrls

    fun reset() {
        _status.value = AIVideoJobStatus.Idle
        _storyboard.value = null
        _clipUrls.value = emptyList()
    }

    suspend fun generate(
        prompt: String,
        style: AIVideoStyle,
        locationName: String
    ) = withContext(Dispatchers.IO) {
        _status.value = AIVideoJobStatus.Idle
        _clipUrls.value = emptyList()

        try {
            _status.value = AIVideoJobStatus.Storyboarding
            val board = buildStoryboard(prompt, style, locationName)
            _storyboard.value = board
            Log.d(TAG, "Storyboard: ${board.title}, ${board.shots.size} shots")

            val urls = mutableListOf<String>()
            board.shots.forEachIndexed { idx, shot ->
                _status.value = AIVideoJobStatus.GeneratingClip(idx + 1, board.shots.size)
                val videoPrompt = "${shot.prompt}. ${style.promptSuffix}. Vertical 9:16, cinematic, no text, no watermark."
                val url = generateClip(videoPrompt)
                urls.add(url)
                _clipUrls.value = urls.toList()
            }

            _status.value = AIVideoJobStatus.Complete
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            _status.value = AIVideoJobStatus.Failed(e.message ?: "Unknown error")
        }
    }

    // ── Storyboard via Claude ────────────────────────────────────────────────

    private fun buildStoryboard(
        prompt: String,
        style: AIVideoStyle,
        locationName: String
    ): AIVideoStoryboard {
        val key = Secrets.claudeApiKey
        if (key.isBlank()) return fallbackStoryboard(prompt, style)

        val locationCtx = if (locationName.isNotBlank()) "Location: $locationName." else ""
        val styleDesc = "${style.label} — ${style.promptSuffix}"

        val systemPrompt = """
You are a creative director for Poplur, a social video app where content is tied to real-world locations.
Generate a 3-shot storyboard for a short vertical social video based on the user's prompt.
Style: $styleDesc
$locationCtx

Return ONLY valid JSON in this exact structure — no markdown, no explanation:
{
  "title": "short catchy title",
  "style": "brief style description",
  "shots": [
    {"shot": 1, "type": "opening shot type", "prompt": "detailed visual description of what happens in this 5-second clip, no text or watermarks", "duration": 5},
    {"shot": 2, "type": "main action type", "prompt": "detailed visual description", "duration": 5},
    {"shot": 3, "type": "closing shot type", "prompt": "detailed visual description", "duration": 5}
  ]
}

IMPORTANT: Each prompt must describe ORIGINAL characters only — no copyrighted IP, no named celebrities or brands.
Make the prompts rich, cinematic, and specific to the ${style.label} aesthetic.
""".trimIndent()

        val body = JSONObject().apply {
            put("model", "claude-haiku-4-5-20251001")
            put("max_tokens", 600)
            put("system", systemPrompt)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
        }

        val conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("x-api-key", key)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.setRequestProperty("content-type", "application/json")
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

            if (conn.responseCode != 200) return fallbackStoryboard(prompt, style)

            val responseText = conn.inputStream.bufferedReader().readText()
            val content = JSONObject(responseText)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            val json = JSONObject(content)
            val shots = json.getJSONArray("shots").let { arr ->
                (0 until arr.length()).map { i ->
                    val s = arr.getJSONObject(i)
                    AIVideoStoryboardShot(
                        shot = s.getInt("shot"),
                        type = s.getString("type"),
                        prompt = s.getString("prompt"),
                        duration = s.optInt("duration", 5)
                    )
                }
            }
            return AIVideoStoryboard(
                title = json.getString("title"),
                style = json.getString("style"),
                shots = shots
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun fallbackStoryboard(prompt: String, style: AIVideoStyle): AIVideoStoryboard {
        val base = prompt.take(80)
        return AIVideoStoryboard(
            title = "Poplur ${style.label} Scene",
            style = style.label,
            shots = listOf(
                AIVideoStoryboardShot(1, "opening", "$base — establishing wide shot, ${style.promptSuffix}"),
                AIVideoStoryboardShot(2, "main action", "$base — close-up action, dramatic lighting, ${style.promptSuffix}"),
                AIVideoStoryboardShot(3, "closing", "$base — cinematic outro, slow motion, ${style.promptSuffix}")
            )
        )
    }

    // ── Video clip via Replicate WAN 2.1 ────────────────────────────────────

    private fun generateClip(videoPrompt: String): String {
        val key = Secrets.replicateApiKey
        if (key.isBlank()) throw AIVideoError.NoAPIKey

        val body = JSONObject().apply {
            put("input", JSONObject().apply {
                put("prompt", videoPrompt)
                put("num_frames", 81)
                put("size", "480*832") // 9:16 vertical
            })
        }

        val conn = (URL("https://api.replicate.com/v1/models/wan-ai/wan2.1-t2v-480p/predictions")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..201) throw AIVideoError.GenerationFailed("Replicate HTTP $code")
            val predictionId = JSONObject(conn.inputStream.bufferedReader().readText()).getString("id")
            return pollClip(predictionId, key)
        } finally {
            conn.disconnect()
        }
    }

    private fun pollClip(id: String, key: String): String {
        val url = URL("https://api.replicate.com/v1/predictions/$id")
        repeat(120) {
            Thread.sleep(3_000)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $key")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                if (conn.responseCode != 200) return@repeat
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                when (json.optString("status")) {
                    "succeeded" -> {
                        val out = json.opt("output")
                        return when {
                            out is JSONArray -> out.getString(0)
                            out is String -> out
                            else -> throw AIVideoError.InvalidResponse
                        }
                    }
                    "failed", "canceled" -> throw AIVideoError.GenerationFailed("Replicate status: ${json.optString("status")}")
                }
            } finally {
                conn.disconnect()
            }
        }
        throw AIVideoError.Timeout
    }
}

sealed class AIVideoError(message: String) : Exception(message) {
    object NoAPIKey : AIVideoError("API key missing")
    object InvalidResponse : AIVideoError("Unexpected API response")
    data class GenerationFailed(val detail: String) : AIVideoError("Generation failed: $detail")
    object Timeout : AIVideoError("Generation timed out after 6 minutes")
}

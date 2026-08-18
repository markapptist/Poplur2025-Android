package com.apptists.poplur2025.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

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

    // ── Storyboard via Poplur backend (Claude runs server-side) ─────────────

    private suspend fun buildStoryboard(
        prompt: String,
        style: AIVideoStyle,
        locationName: String
    ): AIVideoStoryboard {
        val json = try {
            PoplurAIBackendClient().videoStoryboard(
                prompt = prompt,
                styleLabel = style.label,
                styleSuffix = style.promptSuffix,
                locationName = locationName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Storyboard backend unavailable, using fallback: ${e.message}")
            return fallbackStoryboard(prompt, style)
        }

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

    // ── Video clip via Poplur backend / Google AI Studio ───────────────────

    private suspend fun generateClip(videoPrompt: String): String {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?: throw AIVideoError.ProviderMustRunOnBackend
        val response = PoplurAIBackendClient().generate(
            AIGenerationRequest(
                userId = uid,
                plan = PoplurPlan.Creator,
                contentType = PoplurAIContentType.Video,
                prompt = videoPrompt,
                metadata = mapOf("surface" to "ai_video_studio")
            )
        )
        return response.outputUrl ?: throw AIVideoError.InvalidResponse
    }
}

sealed class AIVideoError(message: String) : Exception(message) {
    object ProviderMustRunOnBackend : AIVideoError("Premium AI video must run through Poplur backend using Google AI Studio.")
    object InvalidResponse : AIVideoError("Unexpected API response")
    data class GenerationFailed(val detail: String) : AIVideoError("Generation failed: $detail")
    object Timeout : AIVideoError("Generation timed out after 6 minutes")
}

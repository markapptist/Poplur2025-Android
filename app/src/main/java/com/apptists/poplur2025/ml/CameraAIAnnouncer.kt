package com.apptists.poplur2025.ml

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.apptists.poplur2025.data.DetectedObject
import com.apptists.poplur2025.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.NumberFormat
import java.util.Locale

object CameraAIAnnouncer {

    private const val TAG = "CameraAIAnnouncer"
    private const val ML_COOLDOWN_MS = 2_200L
    private const val LABEL_CLEAR_DELAY_MS = 3_500L

    private var tts: TextToSpeech? = null
    private var ready = false
    private var lastFaceLabel: String? = null
    private var lastRecognizedSignature: String? = null
    private var lastMLSignature: String? = null
    private var lastMLTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    // Visible label shown on camera screen while speaking
    private val _announcement = MutableStateFlow<String?>(null)
    val announcement: StateFlow<String?> = _announcement

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val engine = tts ?: return@TextToSpeech
                engine.language = Locale.US
                engine.voices
                    ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                    ?.maxByOrNull { it.quality }
                    ?.let { engine.voice = it }
                engine.setSpeechRate(0.95f)
                engine.setPitch(1.05f)
                Log.d(TAG, "Camera AI announcer ready")
            }
        }
    }

    fun announceFaceDetected(faceCount: Int, profile: UserProfile? = null) {
        if (faceCount <= 0) {
            clearLastFace()
            return
        }
        if (profile != null) {
            announceRecognizedUser(profile)
            return
        }
        val label = if (faceCount == 1) "🎙 Face detected" else "🎙 $faceCount faces detected"
        if (label == lastFaceLabel) return
        lastFaceLabel = label
        speak(label)
    }

    fun announceRecognizedUser(profile: UserProfile) {
        val username = profile.username.trim().removePrefix("@").ifEmpty { "unknown" }
        val signature = listOf(username, profile.displayName, profile.radius, profile.posts).joinToString("|")
        if (signature == lastRecognizedSignature) return
        lastRecognizedSignature = signature

        val name = profile.nameForSpeech()
        val posts = if (profile.posts == 1) "1 post" else "${profile.posts} posts"
        val radius = formatRadiusForSpeech(profile.radius)
        val label = "🎙 $name. Username @$username. Current radius $radius. $posts."

        Log.d(
            TAG,
            "Recognized user name=$name username=@$username radius=${profile.radius}m posts=${profile.posts}"
        )
        speak(label)
    }

    fun announceMLObjects(objects: List<DetectedObject>) {
        val labels = objects
            .asSequence()
            .sortedByDescending { it.confidence }
            .map { it.label.cleanDetectionLabel() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
            .toList()
        announceMLLabels(labels)
    }

    fun announceMLLabel(label: String) {
        announceMLLabels(listOf(label.cleanDetectionLabel()))
    }

    fun clearLastFace() {
        lastFaceLabel = null
        lastRecognizedSignature = null
    }

    fun resetSession() {
        mainHandler.removeCallbacksAndMessages(null)
        tts?.stop()
        lastFaceLabel = null
        lastRecognizedSignature = null
        lastMLSignature = null
        lastMLTime = 0L
        _announcement.value = null
    }

    private fun announceMLLabels(labels: List<String>) {
        val cleanLabels = labels.filter { it.isNotBlank() }.distinct().take(3)
        if (cleanLabels.isEmpty()) {
            lastMLSignature = null
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastMLTime < ML_COOLDOWN_MS) return
        val signature = cleanLabels.joinToString("|") { it.lowercase(Locale.US) }
        if (signature == lastMLSignature) return
        lastMLSignature = signature
        lastMLTime = now

        val seenText = when (cleanLabels.size) {
            1 -> cleanLabels.first()
            2 -> "${cleanLabels[0]} and ${cleanLabels[1]}"
            else -> "${cleanLabels[0]}, ${cleanLabels[1]}, and ${cleanLabels[2]}"
        }
        speak("🎙 I see $seenText.")
    }

    private fun String.cleanDetectionLabel(): String =
        trim()
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.US)

    private fun speak(text: String) {
        if (!ready) return
        tts?.speak(text.removePrefix("🎙 "), TextToSpeech.QUEUE_FLUSH, null, "cam_ai")
        mainHandler.removeCallbacksAndMessages(null)
        _announcement.value = text
        mainHandler.postDelayed({ _announcement.value = null }, LABEL_CLEAR_DELAY_MS)
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        tts?.shutdown()
        tts = null
        ready = false
        _announcement.value = null
    }

    private fun UserProfile.nameForSpeech(): String =
        displayName.trim()
            .ifEmpty { username.trim().removePrefix("@") }
            .ifEmpty { "Poplur user" }

    private fun formatRadiusForSpeech(radiusMeters: Int): String {
        val radius = radiusMeters.coerceAtLeast(0)
        return when {
            radius >= 1_000 -> "${formatKilometersForSpeech(radius / 1_000f)} kilometers"
            else -> "${NumberFormat.getNumberInstance(Locale.US).format(radius)} meters"
        }
    }

    private fun formatKilometersForSpeech(kilometers: Float): String =
        if (kilometers >= 100f) {
            NumberFormat.getNumberInstance(Locale.US).format(kilometers.toInt())
        } else {
            String.format(Locale.US, "%.1f", kilometers).removeSuffix(".0")
        }
}

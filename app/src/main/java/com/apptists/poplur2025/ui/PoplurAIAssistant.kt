package com.apptists.poplur2025.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.util.PoplurEQSync
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class PoplurAIProfileSortOrder {
    MostRadius,
    LeastRadius
}

enum class PoplurAICameraMode {
    Open,
    Photo,
    Video
}

sealed interface PoplurAICommand {
    data class OpenTab(val tab: AppTab) : PoplurAICommand
    data class OpenPage(val page: String) : PoplurAICommand
    data class GoToChannel(val query: String) : PoplurAICommand
    data class PostToLocation(val location: String?) : PoplurAICommand
    data class Camera(val mode: PoplurAICameraMode) : PoplurAICommand
    data class SetTourActive(val active: Boolean) : PoplurAICommand
    data object Logout : PoplurAICommand
    data class SortPosts(val order: PoplurAIProfileSortOrder) : PoplurAICommand
    data object ShowAnalytics : PoplurAICommand
    data class Unknown(val text: String) : PoplurAICommand
}

@Composable
fun PoplurAIAssistantOverlay(
    enabled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    tutorialPulseToken: Int = 0,
    explicitRequestToken: Int = 0,
    onCommand: (PoplurAICommand) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnCommand by rememberUpdatedState(onCommand)
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingExplicitStart by remember { mutableStateOf(false) }
    var isIlluminated by remember { mutableStateOf(false) }
    var isWaitingForCommand by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Say Hey Poplur") }
    var wakeToken by remember { mutableIntStateOf(0) }
    var lastHandledText by remember { mutableStateOf("") }
    var lastHandledAt by remember { mutableStateOf(0L) }
    var ttsReady by remember { mutableStateOf(false) }
    val ttsState = remember { mutableStateOf<TextToSpeech?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            pendingExplicitStart = false
            statusText = "Enable microphone access"
        }
    }

    val wakeChime = remember { runCatching { ToneGenerator(AudioManager.STREAM_RING, 45) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { runCatching { wakeChime?.release() } } }

    DisposableEffect(context) {
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsState.value?.apply {
                    setLanguage(Locale.US)
                    setSpeechRate(0.94f)
                    setPitch(1.03f)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                }
                ttsReady = true
            }
        }
        ttsState.value = engine
        onDispose {
            ttsReady = false
            ttsState.value = null
            runCatching { engine.stop() }
            runCatching { engine.shutdown() }
        }
    }

    fun speakFeedback(text: String) {
        if (ttsReady) {
            ttsState.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "poplur_ai_feedback")
        }
    }

    fun activateWake() {
        if (enabled) {
            runCatching { wakeChime?.startTone(ToneGenerator.TONE_PROP_PROMPT, 90) }
        }
        wakeToken += 1
        val token = wakeToken
        isIlluminated = true
        isWaitingForCommand = true
        statusText = "Listening"
        scope.launch {
            delay(6_000)
            if (wakeToken == token) {
                isWaitingForCommand = false
                isIlluminated = false
                lastHandledText = ""
                lastHandledAt = 0L
                statusText = "Say Hey Poplur"
            }
        }
    }

    fun emitCommand(command: PoplurAICommand) {
        wakeToken += 1
        val token = wakeToken
        isIlluminated = true
        isWaitingForCommand = false
        val feedback = PoplurAIParser.feedback(command)
        statusText = feedback
        // The host forwards feedback to the WebView first so it can silence
        // Feed/Map before native TTS enters the device audio session.
        currentOnCommand(command)
        speakFeedback(feedback)
        scope.launch {
            delay(2_400)
            if (wakeToken == token) {
                isIlluminated = false
                lastHandledText = ""
                lastHandledAt = 0L
                statusText = "Say Hey Poplur"
            }
        }
    }

    fun handleRecognized(text: String, isFinalResult: Boolean) {
        val normalized = PoplurAIParser.normalize(text)
        val now = System.currentTimeMillis()
        if (normalized.isBlank()) return
        if (normalized == lastHandledText && now - lastHandledAt < 800L) return
        if (!isFinalResult) return

        val wakeMatch = PoplurAIParser.wakePhraseMatch(normalized)
        if (wakeMatch != null) {
            lastHandledText = normalized
            lastHandledAt = now
            val commandText = normalized.substring(wakeMatch.last + 1).trim()
            activateWake()
            if (commandText.isNotBlank()) {
                emitCommand(PoplurAIParser.parse(commandText))
            }
            return
        }

        if (isWaitingForCommand) {
            lastHandledText = normalized
            lastHandledAt = now
            emitCommand(PoplurAIParser.parse(normalized))
        }
    }

    LaunchedEffect(tutorialPulseToken) {
        if (tutorialPulseToken > 0) {
            // The profile tutorial may glow, but it must never trigger an OS
            // sheet. Recognition starts only if access already exists.
            if (hasAudioPermission) activateWake()
        }
    }

    LaunchedEffect(enabled) {
        if (enabled) {
            // Re-check in case access was granted in Settings. Do not request
            // here: Home launch is never an OS permission-prompt trigger.
            hasAudioPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(explicitRequestToken) {
        if (explicitRequestToken > 0) {
            if (hasAudioPermission) {
                activateWake()
            } else {
                pendingExplicitStart = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(hasAudioPermission, pendingExplicitStart) {
        if (hasAudioPermission && pendingExplicitStart) {
            pendingExplicitStart = false
            activateWake()
        }
    }

    DisposableEffect(enabled, hasAudioPermission) {
        if (!enabled || !hasAudioPermission || !SpeechRecognizer.isRecognitionAvailable(context)) {
            onDispose { }
        } else {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
            var savedSystemVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
            var systemMuted = false

            var savedRingVol = 0
            var savedNotifVol = 0

            fun silenceStartSound() {
                savedSystemVol = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
                savedRingVol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                savedNotifVol = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                if (savedSystemVol > 0) audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
                if (savedRingVol > 0) audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
                if (savedNotifVol > 0) audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
                systemMuted = true
            }

            fun restoreSystemSound() {
                if (systemMuted) {
                    if (savedSystemVol > 0) audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystemVol, 0)
                    if (savedRingVol > 0) audioManager.setStreamVolume(AudioManager.STREAM_RING, savedRingVol, 0)
                    if (savedNotifVol > 0) audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotifVol, 0)
                    systemMuted = false
                }
            }

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4_500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 650L)
            }
            var restartJob: Job? = null
            var watchdogJob: Job? = null
            var lastRecognizerEventAt = System.currentTimeMillis()

            fun restartListening(delayMillis: Long = 240L) {
                restartJob?.cancel()
                restartJob = scope.launch {
                    delay(delayMillis)
                    if (!PoplurEQSync.isAppVisible) return@launch
                    lastRecognizerEventAt = System.currentTimeMillis()
                    runCatching {
                        recognizer.cancel()
                        silenceStartSound()
                        recognizer.startListening(recognizerIntent)
                    }
                }
            }

            watchdogJob = scope.launch {
                while (PoplurEQSync.isAppVisible) {
                    delay(12_000L)
                    if (!PoplurEQSync.isAppVisible) break
                    if (System.currentTimeMillis() - lastRecognizerEventAt > 18_000L) {
                        restartListening(0L)
                    }
                }
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    lastRecognizerEventAt = System.currentTimeMillis()
                    restoreSystemSound()
                    statusText = if (isWaitingForCommand) "Listening" else "Say Hey Poplur"
                }

                override fun onBeginningOfSpeech() {
                    lastRecognizerEventAt = System.currentTimeMillis()
                }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    lastRecognizerEventAt = System.currentTimeMillis()
                    restartListening(1_200L)
                }
                override fun onError(error: Int) {
                    lastRecognizerEventAt = System.currentTimeMillis()
                    restoreSystemSound()
                    val delayMillis = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 180L
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 700L
                        else -> 500L
                    }
                    restartListening(delayMillis)
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) {
                    lastRecognizerEventAt = System.currentTimeMillis()
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) handleRecognized(text, isFinalResult = false)
                }

                override fun onResults(results: Bundle?) {
                    lastRecognizerEventAt = System.currentTimeMillis()
                    restoreSystemSound()
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) handleRecognized(text, isFinalResult = true)
                    restartListening()
                }
            })

            silenceStartSound()
            runCatching { recognizer.startListening(recognizerIntent) }

            onDispose {
                restartJob?.cancel()
                watchdogJob.cancel()
                restoreSystemSound()
                runCatching { recognizer.cancel() }
                runCatching { recognizer.destroy() }
            }
        }
    }

    PoplurAIEdgeGlow(
        visible = isIlluminated,
        accent = accent,
        statusText = statusText,
        modifier = modifier
    )
}

@Composable
private fun PoplurAIEdgeGlow(
    visible: Boolean,
    accent: Color,
    statusText: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)),
        exit = fadeOut(tween(180)),
        modifier = modifier
    ) {
        val pulse = rememberInfiniteTransition(label = "poplur-ai-pulse")
        val glow by pulse.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 820, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "edge-glow"
        )

        Box(Modifier.fillMaxSize()) {
            AssistantEdges(accent = accent, glow = glow)
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 124.dp)
                    .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun BoxScope.AssistantEdges(accent: Color, glow: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val line = 12.dp.toPx() + glow * 10.dp.toPx()
        val alpha = 0.58f + glow * 0.34f
        val edgeColor = accent.copy(alpha = alpha)
        val haloColor = accent.copy(alpha = alpha * 0.32f)

        drawRect(edgeColor, topLeft = Offset.Zero, size = Size(size.width, line))
        drawRect(edgeColor, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
        drawRect(edgeColor, topLeft = Offset.Zero, size = Size(line, size.height))
        drawRect(edgeColor, topLeft = Offset(size.width - line, 0f), size = Size(line, size.height))

        drawRect(
            haloColor,
            topLeft = Offset(line / 2f, line / 2f),
            size = Size(size.width - line, size.height - line),
            style = Stroke(width = line * 1.8f)
        )
    }
}

internal object PoplurAIParser {
    fun parse(commandText: String): PoplurAICommand {
        val text = normalize(commandText)

        if ("logout" in text || "log out" in text || "sign out" in text) {
            return PoplurAICommand.Logout
        }
        if ("analytics" in text || "stats" in text || "statistics" in text) {
            return PoplurAICommand.ShowAnalytics
        }
        if ("most radius" in text || "highest radius" in text || "largest radius" in text || "biggest radius" in text) {
            return PoplurAICommand.SortPosts(PoplurAIProfileSortOrder.MostRadius)
        }
        if ("least radius" in text || "lowest radius" in text || "smallest radius" in text) {
            return PoplurAICommand.SortPosts(PoplurAIProfileSortOrder.LeastRadius)
        }
        if ("start tour" in text || "begin tour" in text) {
            return PoplurAICommand.SetTourActive(true)
        }
        if ("stop tour" in text || "pause tour" in text || "end tour" in text) {
            return PoplurAICommand.SetTourActive(false)
        }
        if ("take a photo" in text || "take photo" in text || "take picture" in text || "capture photo" in text) {
            return PoplurAICommand.Camera(PoplurAICameraMode.Photo)
        }
        if ("take a video" in text || "record video" in text || "start video" in text) {
            return PoplurAICommand.Camera(PoplurAICameraMode.Video)
        }
        if ("open camera" in text || "go to camera" in text || "open record" in text) {
            return PoplurAICommand.Camera(PoplurAICameraMode.Open)
        }
        if ("post" in text && ("location" in text || "map" in text || "somewhere" in text || "here" in text || " to " in text)) {
            return PoplurAICommand.PostToLocation(extractLocation(text))
        }
        extractChannel(text)?.let { channel ->
            return PoplurAICommand.GoToChannel(channel)
        }
        if ("tv guide" in text) return PoplurAICommand.OpenPage("tv guide")
        if ("messages" in text || "mail" in text || "dm" in text) return PoplurAICommand.OpenPage("messages")
        if ("settings" in text) return PoplurAICommand.OpenPage("settings")
        if ("profile" in text) return PoplurAICommand.OpenTab(AppTab.Profile)
        if ("feed" in text) return PoplurAICommand.OpenTab(AppTab.Feed)
        if ("map" in text) return PoplurAICommand.OpenTab(AppTab.Map)
        return PoplurAICommand.Unknown(commandText)
    }

    fun normalize(text: String): String {
        var normalized = text
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        listOf(
            "pop blur" to "poplur",
            "pop blurr" to "poplur",
            "pop lure" to "poplur",
            "pop lur" to "poplur",
            "pop lour" to "poplur",
            "popler" to "poplur",
            "poppler" to "poplur",
            "popular" to "poplur",
            "poplar" to "poplur"
        ).forEach { (from, to) ->
            normalized = normalized.replace(from, to)
        }

        return normalized
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun wakePhraseMatch(text: String): IntRange? {
        val tokens = text.split(" ").filter { it.isNotBlank() }
        if (tokens.size < 2) return null

        fun isHeyToken(token: String): Boolean =
            token in setOf("hey", "hay")

        fun isPoplurToken(token: String): Boolean =
            token in setOf("poplur", "populr")

        var cursor = 0
        for (index in 0 until tokens.lastIndex) {
            val token = tokens[index]
            val tokenStart = text.indexOf(token, startIndex = cursor).coerceAtLeast(cursor)
            val tokenEnd = tokenStart + token.length
            cursor = tokenEnd

            if (isHeyToken(token) && isPoplurToken(tokens[index + 1])) {
                val next = tokens[index + 1]
                val nextStart = text.indexOf(next, startIndex = tokenEnd).coerceAtLeast(tokenEnd)
                return tokenStart until (nextStart + next.length)
            }
        }

        return null
    }

    fun feedback(command: PoplurAICommand): String {
        return when (command) {
            is PoplurAICommand.OpenTab -> "Opening ${command.tab.label}"
            is PoplurAICommand.OpenPage -> "Opening ${command.page}"
            is PoplurAICommand.GoToChannel -> "Tuning to ${command.query}"
            is PoplurAICommand.PostToLocation -> "Opening map upload"
            is PoplurAICommand.Camera -> when (command.mode) {
                PoplurAICameraMode.Open -> "Opening camera"
                PoplurAICameraMode.Photo -> "Opening camera for photo"
                PoplurAICameraMode.Video -> "Opening camera for video"
            }
            is PoplurAICommand.SetTourActive -> if (command.active) "Starting tour" else "Stopping tour"
            PoplurAICommand.Logout -> "Logging out"
            is PoplurAICommand.SortPosts -> if (command.order == PoplurAIProfileSortOrder.MostRadius) {
                "Sorting by most radius"
            } else {
                "Sorting by least radius"
            }
            PoplurAICommand.ShowAnalytics -> "Opening analytics"
            is PoplurAICommand.Unknown -> "I did not catch that"
        }
    }

    private fun extractChannel(text: String): String? {
        val channels = listOf("food", "beauty", "sports", "modelling", "modeling", "fashion", "music", "tech", "news", "french", "spanish", "live")
        if ("channel" in text || "feed" in text || "go to" in text || "open" in text) {
            val match = channels.firstOrNull { it in text }
            if (match != null) return if (match == "modeling") "modelling" else match
            (101..111).firstOrNull { text.contains(it.toString()) }?.let { return it.toString() }
        }
        return null
    }

    private fun extractLocation(text: String): String? {
        val marker = listOf(" to ", " in ", " at ")
            .mapNotNull { token ->
                val index = text.indexOf(token)
                if (index >= 0) index to token else null
            }
            .minByOrNull { it.first }
            ?: return null
        val location = text
            .substring(marker.first + marker.second.length)
            .replace("location", "")
            .trim()
        return location.ifBlank { null }
    }
}

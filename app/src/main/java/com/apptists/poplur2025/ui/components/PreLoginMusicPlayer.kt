package com.apptists.poplur2025.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.apptists.poplur2025.data.PoplurAIBackendClient
import java.io.File
import java.util.Locale
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps the pre-login flow on the same audio treatment as Map: a local radio stream mixed
 * with the Poplur AI host. Mute preference persists via SharedPreferences.
 */
class PreLoginMusicPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("poplur_prelogin_music", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val backendClient = PoplurAIBackendClient()

    var isMuted: Boolean by mutableStateOf(prefs.getBoolean(KEY_MUTED, false))
        private set
    var currentChannelTitle: String by mutableStateOf(CHANNEL_TITLE)
        private set
    var currentAIScript: String by mutableStateOf(AI_STATUS_TEXT)
        private set

    private var radioPlayer: MediaPlayer? = null
    private var voicePlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var voiceJob: Job? = null
    private var tempVoiceFile: File? = null
    private var hasRequestedVoice = false

    init {
        textToSpeech = runCatching {
            TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.setLanguage(Locale.US)
                    textToSpeech?.setSpeechRate(0.88f)
                    textToSpeech?.setPitch(1.01f)
                    textToSpeech?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                }
            }
        }.getOrNull()
    }

    fun toggleMute() {
        isMuted = !isMuted
        prefs.edit().putBoolean(KEY_MUTED, isMuted).apply()
        applyMuteState()
    }

    /** Starts (or resumes) the local radio stream. Safe to call repeatedly. */
    fun start() {
        if (isMuted) return
        if (radioPlayer != null) {
            applyMuteState()
            return
        }
        val newPlayer = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(RADIO_URL)
                isLooping = true
                setOnPreparedListener { prepared ->
                    if (!isMuted) prepared.start()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Local radio stream failed ($what/$extra)")
                    true
                }
                prepareAsync()
            }
        }.getOrElse {
            Log.w(TAG, "Failed to create local radio stream", it)
            return
        }
        radioPlayer = newPlayer
        applyMuteState()
        requestAIWelcome()
    }

    fun stop() {
        voiceJob?.cancel()
        voiceJob = null
        voicePlayer?.stopCatching()
        voicePlayer?.release()
        voicePlayer = null
        tempVoiceFile?.delete()
        tempVoiceFile = null
        textToSpeech?.stop()
        radioPlayer?.stopCatching()
        radioPlayer?.release()
        radioPlayer = null
        hasRequestedVoice = false
    }

    fun dispose() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        scope.cancel()
    }

    private fun applyMuteState() {
        if (isMuted) { stop(); return }
        val radio = radioPlayer ?: run { start(); return }
        if (!isMuted) {
            if (!radio.isPlayingCatching()) radio.startCatching()
            if (!hasRequestedVoice) requestAIWelcome()
        }
    }

    private fun requestAIWelcome() {
        if (isMuted || hasRequestedVoice) return
        hasRequestedVoice = true
        voiceJob?.cancel()
        voiceJob = scope.launch {
            try {
                val hosted = backendClient.radioHostAudio(
                    script = OPENING_SCRIPT,
                    cityName = "your area",
                    stopTitle = "Welcome",
                    stationName = CHANNEL_TITLE
                )
                playHostedVoice(hosted.audioData)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                speakWithSystemVoice(OPENING_SCRIPT)
            }
        }
    }

    private suspend fun playHostedVoice(data: ByteArray) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("poplur-prelogin-radio", ".wav", appContext.cacheDir).also {
                it.writeBytes(data)
            }
        }
        withContext(Dispatchers.Main.immediate) {
            if (isMuted) {
                file.delete()
                return@withContext
            }
            tempVoiceFile?.delete()
            tempVoiceFile = file
            voicePlayer?.stopCatching()
            voicePlayer?.release()
            voicePlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    release()
                    voicePlayer = null
                    file.delete()
                }
                prepare()
                start()
            }
        }
    }

    private fun speakWithSystemVoice(text: String) {
        if (isMuted) return
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private companion object {
        const val TAG = "PreLoginMusicPlayer"
        const val KEY_MUTED = "muted"
        const val RADIO_URL = "https://playerservices.streamtheworld.com/api/livestream-redirect/CBC_R1_TOR_H.mp3"
        const val CHANNEL_TITLE = "CBC Radio One Toronto"
        const val AI_STATUS_TEXT = "Poplur AI is mixing your local radio."
        const val OPENING_SCRIPT = "Welcome to Poplur Radio, live from your area. Poplur AI is mixing the local radio with what is happening around you. Stay with us."
        const val UTTERANCE_ID = "poplur-prelogin-welcome"
    }
}

private fun MediaPlayer.stopCatching() {
    runCatching { stop() }
}

private fun MediaPlayer.startCatching(): Boolean = runCatching {
    start()
}.isSuccess

private fun MediaPlayer.pauseCatching() {
    runCatching { pause() }
}

private fun MediaPlayer.isPlayingCatching(): Boolean = runCatching { isPlaying }.getOrDefault(false)

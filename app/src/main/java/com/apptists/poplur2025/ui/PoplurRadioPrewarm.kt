package com.apptists.poplur2025.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Warms up the Android TTS engine during the loading screen so the first map radio
 * utterance starts without the usual 300-600ms engine-bind delay.
 *
 * Call [start] once (idempotent) from PoplurRoot while the launch cover is visible.
 * The warmed engine stays bound; subsequent TextToSpeech() calls in MapScreen complete
 * much faster because the underlying TTS service is already running.
 */
object PoplurRadioPrewarm {

    private const val TAG = "PoplurRadioPrewarm"

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var started = false

    fun start(context: Context) {
        if (started) return
        started = true

        val appCtx = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            try {
                tts = TextToSpeech(appCtx) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        setupVoice()
                        Log.d(TAG, "TTS engine warmed up")
                    } else {
                        Log.w(TAG, "TTS init failed: $status")
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "TTS prewarm exception", e)
            }
        }
    }

    private fun setupVoice() {
        val engine = tts ?: return
        val langResult = engine.setLanguage(Locale.US)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.getDefault())
        }
        // Pick the highest-quality offline voice for the radio feel
        engine.voices
            ?.filter { it.locale.language == Locale.US.language && !it.isNetworkConnectionRequired }
            ?.maxByOrNull { it.quality }
            ?.let { engine.voice = it }
        engine.setSpeechRate(0.88f)
        engine.setPitch(1.01f)
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        )
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        started = false
    }
}

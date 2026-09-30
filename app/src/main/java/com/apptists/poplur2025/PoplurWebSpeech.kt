package com.apptists.poplur2025

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Native text-to-speech for the page's tour narration and radio hosts: the
 * Android twin of iOS PoplurWebSpeech. The page sends SpeechSynthesis-style rate
 * and pitch, where about 1.0 is normal, and TextToSpeech takes them as they are.
 * A new line replaces the one playing, as on iOS. The page keeps its own timer
 * for when a line ends, so nothing is reported back.
 */
object PoplurWebSpeech {
    private const val TAG = "PoplurWebSpeech"
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Line? = null

    private class Line(val text: String, val rate: Float, val pitch: Float)

    fun speak(context: Context, text: String, rate: Float, pitch: Float) {
        val line = text.trim()
        if (line.isEmpty()) return
        if (tts == null) {
            pending = Line(line, rate, pitch)
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (!ready) {
                    Log.w(TAG, "text-to-speech unavailable (status $status)")
                    pending = null
                    tts?.shutdown()
                    tts = null
                    return@TextToSpeech
                }
                tts?.let(::configure)
                pending?.let(::say)
                pending = null
            }
            return
        }
        if (!ready) {
            pending = Line(line, rate, pitch)
            return
        }
        say(Line(line, rate, pitch))
    }

    fun cancel() {
        pending = null
        if (ready) tts?.stop()
    }

    private fun configure(engine: TextToSpeech) {
        val canada = engine.isLanguageAvailable(Locale.CANADA) >= TextToSpeech.LANG_AVAILABLE
        engine.setLanguage(if (canada) Locale.CANADA else Locale.US)
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    private fun say(line: Line) {
        val engine = tts ?: return
        engine.setSpeechRate(line.rate.coerceIn(0.5f, 2f))
        engine.setPitch(line.pitch.coerceIn(0.5f, 2f))
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.92f) }
        engine.speak(line.text.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_FLUSH, params, "poplur-web-speech")
    }
}

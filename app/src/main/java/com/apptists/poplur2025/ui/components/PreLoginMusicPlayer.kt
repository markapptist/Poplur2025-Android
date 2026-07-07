package com.apptists.poplur2025.ui.components

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Loops a single bundled ambient track behind the whole pre-login flow (Welcome, Login,
 * Sign Up). Mirrors iOS's PreLoginMusicPlayer. Mute preference persists via SharedPreferences.
 *
 * NOTE: expects a bundled asset at `assets/prelogin_ambient.mp3` — add it to the app module.
 * This player logs a warning and silently no-ops without it.
 */
class PreLoginMusicPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("poplur_prelogin_music", Context.MODE_PRIVATE)

    var isMuted: Boolean by mutableStateOf(prefs.getBoolean(KEY_MUTED, false))
        private set

    private var player: MediaPlayer? = null

    fun toggleMute() {
        isMuted = !isMuted
        prefs.edit().putBoolean(KEY_MUTED, isMuted).apply()
        applyMuteState()
    }

    /** Starts (or resumes) looping playback. Safe to call repeatedly. */
    fun start() {
        if (player != null) {
            applyMuteState()
            return
        }
        val descriptor: AssetFileDescriptor = try {
            appContext.assets.openFd(ASSET_NAME)
        } catch (error: Exception) {
            Log.w(TAG, "Missing bundled asset $ASSET_NAME — background music disabled.", error)
            return
        }
        try {
            val newPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                isLooping = true
                setVolume(PLAYBACK_VOLUME, PLAYBACK_VOLUME)
                prepare()
            }
            player = newPlayer
            applyMuteState()
        } catch (error: Exception) {
            Log.w(TAG, "Failed to start pre-login music", error)
        } finally {
            descriptor.close()
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }

    private fun applyMuteState() {
        val current = player ?: return
        if (isMuted) {
            if (current.isPlaying) current.pause()
        } else if (!current.isPlaying) {
            current.start()
        }
    }

    private companion object {
        const val TAG = "PreLoginMusicPlayer"
        const val ASSET_NAME = "prelogin_ambient.mp3"
        const val KEY_MUTED = "muted"
        const val PLAYBACK_VOLUME = 0.35f
    }
}


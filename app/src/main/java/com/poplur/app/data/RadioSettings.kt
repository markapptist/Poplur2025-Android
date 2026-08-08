package com.poplur.app.data

import android.content.Context

enum class RadioPlaybackMode(val title: String, val subtitle: String) {
    SCREEN_ONLY("Radio screen only", "Stops when you leave radio."),
    IN_APP("Across app", "Keeps playing while you use Poplur."),
    OUTSIDE_APP("Outside app", "Runs as background audio after leaving Poplur.")
}

object RadioSettings {
    private const val PREFS = "poplur_radio_settings"
    private const val KEY_MODE = "radio_playback_mode"

    fun getMode(context: Context): RadioPlaybackMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MODE, null)
        return raw?.let { runCatching { RadioPlaybackMode.valueOf(it) }.getOrNull() } ?: RadioPlaybackMode.SCREEN_ONLY
    }

    fun setMode(context: Context, mode: RadioPlaybackMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}

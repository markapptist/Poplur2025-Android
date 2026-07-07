package com.apptists.poplur2025.map

import android.content.Context
import androidx.core.content.edit

/**
 * Persists the user's map theme selection across app restarts using SharedPreferences.
 */
class ThemeRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(theme: MapTheme) {
        prefs.edit { putString(KEY_THEME_ID, theme.id) }
    }

    fun load(): MapTheme {
        val id = prefs.getString(KEY_THEME_ID, MapTheme.DEFAULT.id) ?: MapTheme.DEFAULT.id
        return MapTheme.fromId(id)
    }

    companion object {
        private const val PREFS_NAME  = "poplur_map_theme_v1"
        private const val KEY_THEME_ID = "theme_id"
    }
}

package com.apptists.poplur2025.ui.profile

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.apptists.poplur2025.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProfileThemeAvailability {
    Included,
    Premium
}

enum class ProfileStatusBarIcons {
    Light,
    Dark
}

/**
 * Stable, manifest-backed profile appearance. The ID is the only persisted value;
 * resource identifiers and display metadata may safely change between app builds.
 */
data class ProfileTheme(
    val id: String,
    val displayName: String,
    @DrawableRes val backgroundResId: Int?,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val foreground: Color,
    val overlayColor: Color,
    val overlayOpacity: Float,
    val statusBarIcons: ProfileStatusBarIcons,
    val availability: ProfileThemeAvailability = ProfileThemeAvailability.Included
) {
    val isDefault: Boolean get() = backgroundResId == null
}

object ProfileThemes {
    val Default = ProfileTheme(
        id = "default",
        displayName = "Default",
        backgroundResId = null,
        primaryAccent = Color(0xFFFF4FA3),
        secondaryAccent = Color(0xFFFF79B9),
        foreground = Color.Black,
        overlayColor = Color.Transparent,
        overlayOpacity = 0f,
        statusBarIcons = ProfileStatusBarIcons.Dark
    )

    val Chocolate = ProfileTheme(
        id = "chocolate",
        displayName = "Chocolate",
        backgroundResId = R.drawable.profile_theme_chocolate_background,
        primaryAccent = Color(0xFFFF3B8D),
        secondaryAccent = Color(0xFF8B4A2B),
        foreground = Color(0xFFFFF4EA),
        overlayColor = Color(0xFF180A05),
        overlayOpacity = 0.16f,
        statusBarIcons = ProfileStatusBarIcons.Light
    )

    val RainbowNeon = ProfileTheme(
        id = "rainbowNeon",
        displayName = "Rainbow Neon",
        backgroundResId = R.drawable.profile_theme_rainbow_neon_background,
        primaryAccent = Color(0xFF00E7FF),
        secondaryAccent = Color(0xFFFF3B9D),
        foreground = Color.White,
        overlayColor = Color(0xFF100A34),
        overlayOpacity = 0.12f,
        statusBarIcons = ProfileStatusBarIcons.Light
    )

    val Camouflage = ProfileTheme(
        id = "camouflage",
        displayName = "Camouflage",
        backgroundResId = R.drawable.profile_theme_camouflage_background,
        primaryAccent = Color(0xFFF43F8E),
        secondaryAccent = Color(0xFFA68B5B),
        foreground = Color(0xFFF7F0DE),
        overlayColor = Color(0xFF0B0E09),
        overlayOpacity = 0.20f,
        statusBarIcons = ProfileStatusBarIcons.Light
    )

    val Toy = ProfileTheme(
        id = "toy",
        displayName = "Toy",
        backgroundResId = R.drawable.profile_theme_toy_background,
        primaryAccent = Color(0xFF1677FF),
        secondaryAccent = Color(0xFFFF335F),
        foreground = Color(0xFF222222),
        overlayColor = Color.White,
        overlayOpacity = 0.05f,
        statusBarIcons = ProfileStatusBarIcons.Dark
    )

    val RubberPink = ProfileTheme(
        id = "rubberPink",
        displayName = "Rubber Pink",
        backgroundResId = R.drawable.profile_theme_rubber_pink_background,
        primaryAccent = Color(0xFFFF2E88),
        secondaryAccent = Color(0xFF2EE8FF),
        foreground = Color.White,
        overlayColor = Color(0xFF08080B),
        overlayOpacity = 0.12f,
        statusBarIcons = ProfileStatusBarIcons.Light
    )

    val IcyCrystal = ProfileTheme(
        id = "icyCrystal",
        displayName = "Icy Crystal",
        backgroundResId = R.drawable.profile_theme_icy_crystal_background,
        primaryAccent = Color(0xFF19BDF2),
        secondaryAccent = Color(0xFFEC4FB4),
        foreground = Color(0xFF17324D),
        overlayColor = Color.White,
        overlayOpacity = 0.08f,
        statusBarIcons = ProfileStatusBarIcons.Dark
    )

    val LuxeGold = ProfileTheme(
        id = "luxeGold",
        displayName = "Luxe Gold",
        backgroundResId = R.drawable.profile_theme_luxe_gold_background,
        primaryAccent = Color(0xFFF5C452),
        secondaryAccent = Color(0xFFE9378F),
        foreground = Color(0xFFFFF7D6),
        overlayColor = Color(0xFF090705),
        overlayOpacity = 0.15f,
        statusBarIcons = ProfileStatusBarIcons.Light
    )

    val GlamPink = ProfileTheme(
        id = "glamPink",
        displayName = "Glam Pink",
        backgroundResId = R.drawable.profile_theme_glam_pink_background,
        primaryAccent = Color(0xFFFF2D9B),
        secondaryAccent = Color(0xFF49DDF7),
        foreground = Color.White,
        overlayColor = Color(0xFF0A0209),
        overlayOpacity = 0.14f,
        statusBarIcons = ProfileStatusBarIcons.Light
    )

    val All: List<ProfileTheme> = listOf(
        Default,
        Chocolate,
        RainbowNeon,
        Camouflage,
        Toy,
        RubberPink,
        IcyCrystal,
        LuxeGold,
        GlamPink
    )

    private val byId = All.associateBy(ProfileTheme::id)

    fun fromId(id: String?): ProfileTheme = byId[id] ?: Default

    fun findById(id: String?): ProfileTheme? = id?.let(byId::get)
}

/**
 * Local-first theme source. A future account-sync implementation can observe
 * [selectedTheme] and mirror only [ProfileTheme.id] without affecting rendering.
 */
class ProfileThemeRepository private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val _selectedTheme = MutableStateFlow(
        ProfileThemes.fromId(preferences.getString(KEY_SELECTED_THEME_ID, null))
    )
    val selectedTheme: StateFlow<ProfileTheme> = _selectedTheme.asStateFlow()

    fun select(theme: ProfileTheme) {
        if (_selectedTheme.value.id == theme.id) return
        preferences.edit().putString(KEY_SELECTED_THEME_ID, theme.id).apply()
        _selectedTheme.value = theme
    }

    /** Returns false for unknown IDs so bridges never silently persist bad data. */
    fun selectById(id: String?): Boolean {
        val theme = ProfileThemes.findById(id) ?: return false
        select(theme)
        return true
    }

    companion object {
        private const val PREFERENCES_NAME = "poplur_profile_themes"
        private const val KEY_SELECTED_THEME_ID = "selected_profile_theme_id"

        @Volatile private var instance: ProfileThemeRepository? = null

        fun getInstance(context: Context): ProfileThemeRepository =
            instance ?: synchronized(this) {
                instance ?: ProfileThemeRepository(context.applicationContext).also { instance = it }
            }
    }
}

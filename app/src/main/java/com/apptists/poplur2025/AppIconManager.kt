package com.apptists.poplur2025

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes

data class PoplurAppIconOption(
    val id: String,
    val displayName: String,
    @DrawableRes val previewRes: Int,
    val aliasSuffix: String
)

object AppIconManager {
    private const val PREFS_NAME = "poplur_app_icon"
    private const val KEY_SELECTED_ID = "selected_id_v1"

    val options = listOf(
        PoplurAppIconOption("default", "Default Clay", R.mipmap.app_icon_default, "DefaultIcon"),
        PoplurAppIconOption("pastel", "Pastel Clay", R.mipmap.app_icon_pastel, "PastelIcon"),
        PoplurAppIconOption("neonBlue", "Neon Blue", R.mipmap.app_icon_neon_blue, "NeonBlueIcon"),
        PoplurAppIconOption("roseGold", "Rose Gold", R.mipmap.app_icon_rose_gold, "RoseGoldIcon")
    )

    /**
     * Repairs the launcher entry for anyone updating from a build that still had the
     * retired icons. Their alias was explicitly disabled via PackageManager, and that
     * override outlives the update — so if the icon they had selected is gone, the app
     * would be left with no enabled launcher alias at all.
     */
    fun ensureValidSelection(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SELECTED_ID, "default")
        if (options.any { it.id == saved }) return

        prefs.edit().putString(KEY_SELECTED_ID, "default").commit()
        val packageManager = context.packageManager
        options.forEachIndexed { index, option ->
            packageManager.setComponentEnabledSetting(
                componentName(context, option),
                if (index == 0) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun currentId(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ID, "default")
        return options.firstOrNull { it.id == saved }?.id ?: "default"
    }

    fun select(context: Context, id: String): Boolean {
        val selected = options.firstOrNull { it.id == id } ?: return false
        if (currentId(context) == selected.id) return true

        // Commit before touching launcher components: some OEM launchers may
        // recreate the app process as soon as an alias changes.
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_ID, selected.id)
            .commit()

        val packageManager = context.packageManager
        val selectedComponent = componentName(context, selected)
        packageManager.setComponentEnabledSetting(
            selectedComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        options.asSequence()
            .filter { it.id != selected.id }
            .forEach { option ->
                packageManager.setComponentEnabledSetting(
                    componentName(context, option),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        return true
    }

    private fun componentName(context: Context, option: PoplurAppIconOption) =
        ComponentName(context.packageName, "${context.packageName}.launcher.${option.aliasSuffix}")
}

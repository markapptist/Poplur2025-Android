package com.apptists.poplur2025.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.apptists.poplur2025.R

val CubeSkin.appIconAliasName: String
    get() = when (this) {
        CubeSkin.Default -> "IconDefaultAlias"
        CubeSkin.Gold -> "IconGoldAlias"
        CubeSkin.Toy -> "IconToyAlias"
        CubeSkin.Rainbow -> "IconRainbowAlias"
        CubeSkin.Carbon -> "IconSparklePinkAlias"
        CubeSkin.Ice -> "IconIceAlias"
        CubeSkin.Chocolate -> "IconChocolateAlias"
        CubeSkin.Space -> "IconSpaceAlias"
        CubeSkin.Camo -> "IconCamoAlias"
    }

@get:DrawableRes
val CubeSkin.appIconResId: Int
    get() = when (this) {
        CubeSkin.Default -> R.mipmap.ic_launcher
        CubeSkin.Gold -> R.mipmap.ic_launcher_gold
        CubeSkin.Toy -> R.mipmap.ic_launcher_toy
        CubeSkin.Rainbow -> R.mipmap.ic_launcher_rainbow
        CubeSkin.Carbon -> R.mipmap.ic_launcher_sparkle_pink
        CubeSkin.Ice -> R.mipmap.ic_launcher_ice
        CubeSkin.Chocolate -> R.mipmap.ic_launcher_chocolate
        CubeSkin.Space -> R.mipmap.ic_launcher_space
        CubeSkin.Camo -> R.mipmap.ic_launcher_camo
    }

object AppIconThemeManager {
    private val allAliasNames = CubeSkin.entries.map { it.appIconAliasName }

    fun apply(context: Context, skin: CubeSkin) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        allAliasNames.forEach { alias ->
            val state = if (alias == skin.appIconAliasName) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(packageName, "$packageName.$alias"),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

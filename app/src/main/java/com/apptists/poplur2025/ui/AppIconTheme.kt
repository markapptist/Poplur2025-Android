package com.apptists.poplur2025.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.apptists.poplur2025.R

val CubeSkin.appIconAliasName: String
    get() = when (this) {
        CubeSkin.Default -> "IconDefaultLauncherAlias"
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
    private const val EXPLICIT_LAUNCH_ALIAS = "IconDefaultAlias"
    private val allAliasNames = CubeSkin.entries.map { it.appIconAliasName }
    private var lastAppliedSkinId: String? = null

    fun ensureExplicitLaunchAliasEnabled(context: Context) {
        val appContext = context.applicationContext
        val componentName = ComponentName(appContext.packageName, "${appContext.packageName}.$EXPLICIT_LAUNCH_ALIAS")
        runCatching {
            appContext.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    fun apply(context: Context, skin: CubeSkin) {
        if (lastAppliedSkinId == skin.id) return
        lastAppliedSkinId = skin.id

        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName
        val selectedAlias = skin.appIconAliasName

        fun setAliasState(alias: String, state: Int) {
            val componentName = ComponentName(packageName, "$packageName.$alias")
            val currentState = runCatching {
                packageManager.getComponentEnabledSetting(componentName)
            }.getOrElse {
                return
            }

            val isDefaultAlias = alias == "IconDefaultLauncherAlias"
            
            // Determine if the component is effectively correct based on its manifest default
            val isCorrectIfDefault = if (isDefaultAlias) {
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            val needsUpdate = if (currentState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                !isCorrectIfDefault
            } else {
                currentState != state
            }

            if (needsUpdate) {
                runCatching {
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        state,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }

        setAliasState(EXPLICIT_LAUNCH_ALIAS, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        setAliasState(selectedAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)

        allAliasNames
            .filterNot { it == selectedAlias }
            .forEach { alias ->
                setAliasState(
                    alias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                )
            }
    }
}

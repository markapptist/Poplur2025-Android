package com.apptists.poplur2025.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.badlogic.gdx.graphics.Color

/** Normalized [r, g, b] in 0..1 for menu tinting, or null for the Default skin. */
val CubeSkin.themeColorRGB: FloatArray?
    get() = gdxColor?.let { floatArrayOf(it.r, it.g, it.b) }

/** True when each hold-menu tile should glow in its own item.accent instead of the shared skin colour. */
val CubeSkin.usePerItemTint: Boolean
    get() = this == CubeSkin.Default || this == CubeSkin.Toy

/** Required asset path for the hold-menu panel/tile background texture. */
val CubeSkin.holdMenuAssetPath: String
    get() = when (this) {
        CubeSkin.Ice       -> "holdmenu/ice.png"
        CubeSkin.Camo      -> "holdmenu/camo.png"
        CubeSkin.Chocolate -> "holdmenu/chocolate.png"
        CubeSkin.Default   -> "holdmenu/classic.png"
        CubeSkin.Gold      -> "holdmenu/gold.png"
        CubeSkin.Carbon, CubeSkin.Clay -> "holdmenu/sparkle.png"
        CubeSkin.Rainbow   -> "holdmenu/rainbow.png"
        CubeSkin.Toy       -> "holdmenu/toy.png"
        CubeSkin.Space     -> "holdmenu/sparkle.png"
    }

/** Required full-screen banner art used by theme-aware app surfaces. */
val CubeSkin.bannerAssetPath: String
    get() = when (this) {
        CubeSkin.Default   -> "theme_banners/theme_banner_default.png"
        CubeSkin.Gold      -> "theme_banners/theme_banner_gold.png"
        CubeSkin.Toy       -> "theme_banners/theme_banner_toy.png"
        CubeSkin.Rainbow   -> "theme_banners/theme_banner_rainbow.png"
        CubeSkin.Carbon, CubeSkin.Clay -> "theme_banners/theme_banner_sparkle_pink.png"
        CubeSkin.Ice       -> "theme_banners/theme_banner_ice.png"
        CubeSkin.Chocolate -> "theme_banners/theme_banner_chocolate.png"
        CubeSkin.Space     -> "theme_banners/theme_banner_sparkle_pink.png"
        CubeSkin.Camo      -> "theme_banners/theme_banner_camo.png"
    }

enum class CubeSkin(
    val id: String,
    val label: String,
    val assetPath: String?,
    val modelAssetPath: String,
    val gdxColor: Color?,
    val logoAssetPath: String
) {
    Default(
        "default",
        "Classic",
        null,
        "cube_models/dark_rubber_neon_icon_cube.glb",
        null,
        "logo.png"
    ),
    Clay(
        "clay",
        "Poplur Clay",
        null, // use the GLB's own baked clay texture (like Default)
        "cube_models/clay_icon_cube.glb",
        Color(0.93f, 0.09f, 0.55f, 1f),
        "logo/logo_clay.png"
    ),
    Gold(
        "gold",
        "24K Gold",
        "cube/skin_gold.png",
        "cube_models/gold_metallic_icon_cube.glb",
        Color(0.88f, 0.68f, 0.10f, 1f),
        "logo/logo_gold.png"
    ),
    Toy(
        "toy",
        "Toy Box",
        "cube/skin_toy.png",
        "cube_models/circus_carnival_icon_cube.glb",
        Color(0.95f, 0.20f, 0.20f, 1f),
        "logo/logo_toy.png"
    ),
    Rainbow(
        "rainbow",
        "Neon Rainbow",
        "cube/skin_rainbow.png",
        "cube_models/rainbow_neon_icon_cube.glb",
        Color(0.55f, 0.10f, 0.95f, 1f),
        "logo/logo_rainbow.png"
    ),
    Carbon(
        "carbon",
        "Carbon Pink",
        "cube/skin_carbon.png",
        "cube_models/purple_sparkle_icon_cube.glb",
        Color(1.00f, 0.40f, 0.70f, 1f),
        "logo/logo_sparkle_pink.png"
    ),
    Ice(
        "ice",
        "Arctic Ice",
        "cube/skin_ice.png",
        "cube_models/frozen_ice_icon_cube.glb",
        Color(0.75f, 0.92f, 1.00f, 1f),
        "logo/logo_ice.png"
    ),
    Chocolate(
        "chocolate",
        "Rich Cocoa",
        "cube/skin_chocolate.png",
        "cube_models/dark_chocolate_icon_cube.glb",
        Color(0.45f, 0.25f, 0.08f, 1f),
        "logo/logo_chocolate.png"
    ),
    Space(
        "space",
        "Deep Space",
        "cube/skin_space.png",
        "cube_models/purple_sparkle_icon_cube.glb",
        Color(0.08f, 0.05f, 0.20f, 1f),
        "logo/logo_sparkle_pink.png"
    ),
    Camo(
        "camo",
        "Stealth Camo",
        "cube/skin_camo.png",
        "cube_models/military_camo_icon_cube.glb",
        Color(0.30f, 0.48f, 0.18f, 1f),
        "logo/logo_camo.png"
    )
}

object GdxState {
    var isSceneActive by mutableStateOf(false)
    var currentScene by mutableStateOf("radius")

    var selectedCubeSkin by mutableStateOf(CubeSkin.Gold)

    var sceneBoundsX by mutableStateOf(0f)
    var sceneBoundsY by mutableStateOf(0f)
    var sceneBoundsWidth by mutableStateOf(0f)
    var sceneBoundsHeight by mutableStateOf(0f)

    // Written from sensor thread, read in LibGDX render thread
    @Volatile @JvmField var tiltYawOffset: Float = 0f
}

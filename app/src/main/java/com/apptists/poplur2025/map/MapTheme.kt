package com.apptists.poplur2025.map

import com.apptists.poplur2025.ui.CubeSkin

// ─── Layer / Image IDs shared between MapScreen and the theme engine ────────
// These must match the private constants declared in MapScreen.kt exactly.
internal const val BASEMAP_IMPORT_ID          = "basemap"
internal const val POST_LAYER_ID              = "poplur-post-markers"
internal const val CURRENT_PIN_LAYER_ID       = "poplur-current-location-pin-layer"
internal const val LANDMARK_PIN_LAYER_ID      = "poplur-landmark-pin-layer"
internal const val RADIUS_FILL_LAYER_ID       = "poplur-radius-circle-fill-layer"
internal const val RADIUS_LINE_LAYER_ID       = "poplur-radius-circle-line-layer"
internal const val BLUE_PIN_IMAGE_ID          = "poplur-gdx-blue-location-pin"
internal const val PINK_PIN_IMAGE_ID          = "poplur-gdx-pink-landmark-pin"

// Custom layers owned exclusively by the theme engine
internal const val THEME_LAND_TINT_LAYER_ID  = "poplur-theme-land-tint"
internal const val THEME_BUILDINGS_LAYER_ID  = "poplur-theme-buildings"
internal const val THEME_FADE_LAYER_ID       = "poplur-theme-fade"

// ─── Theme definitions ───────────────────────────────────────────────────────

enum class MapTheme(val id: String, val displayName: String) {

    DEFAULT("default", "Default"),
    DARK("dark", "Dark"),
    ICE("ice", "Ice"),
    GOLD("gold", "Gold"),
    GLITTER("glitter", "Glitter"),
    RAINBOW("rainbow", "Rainbow"),
    CHOCOLATE("chocolate", "Chocolate"),
    CARNIVAL("carnival", "Carnival"),
    CAMO("camo", "Camo"),
    TOY_BOX("toy_box", "Toy Box");

    // ── Per-layer color tokens ───────────────────────────────────────────────

    data class Colors(
        // Post circle markers
        val postCircle: String,
        val postStroke: String,
        val postOpacity: Double,
        val postEmissive: Double,
        // Location / landmark pins
        val locationPin: String,
        val landmarkPin: String,
        // Radius circle (fill + stroke)
        val radiusFill: String,
        val radiusStroke: String,
        val radiusFillOpacity: Double,
        val radiusStrokeOpacity: Double,
        // Building extrusion
        val buildingFill: String,
        val buildingEmissive: Double,
        val buildingOpacity: Double,
        // Land tint overlay (semi-transparent, shifts the base map hue)
        val landTint: String,
        val landTintOpacity: Double,
        // UI accent (exposed so Compose can use it for overlaid UI elements)
        val accent: String,
    )

    data class StyleConfig(
        // Mapbox Standard light preset: "dawn" | "day" | "dusk" | "night"
        val lightPreset: String,
        val show3dObjects: Boolean,
        val showRoadLabels: Boolean,
        val showPoiLabels: Boolean,
        val showPlaceLabels: Boolean,
        // Whether to add a custom building extrusion layer
        val customBuildings: Boolean,
        // Whether the animation controller should run special effects
        val animationType: AnimationType,
    )

    enum class AnimationType { NONE, PULSE, RAINBOW_CYCLE, GLITTER_SPARKLE }

    val colors: Colors
        get() = when (this) {
            DEFAULT -> Colors(
                postCircle = "#ff2d8a", postStroke = "#ffffff",
                postOpacity = 0.46, postEmissive = 1.0,
                locationPin = "#2680ff", landmarkPin = "#ba2cff",
                radiusFill = "#ff2d8a", radiusStroke = "#ff44d6",
                radiusFillOpacity = 0.10, radiusStrokeOpacity = 0.70,
                buildingFill = "#1a1a2e", buildingEmissive = 0.25, buildingOpacity = 1.0,
                landTint = "#0a0a1a", landTintOpacity = 0.0,
                accent = "#ff2d8a",
            )
            DARK -> Colors(
                postCircle = "#4488ff", postStroke = "#88bbff",
                postOpacity = 0.55, postEmissive = 1.2,
                locationPin = "#0066ff", landmarkPin = "#6644dd",
                radiusFill = "#2255cc", radiusStroke = "#4488ff",
                radiusFillOpacity = 0.12, radiusStrokeOpacity = 0.70,
                buildingFill = "#0d1117", buildingEmissive = 0.20, buildingOpacity = 1.0,
                landTint = "#050a14", landTintOpacity = 0.18,
                accent = "#4488ff",
            )
            ICE -> Colors(
                postCircle = "#55ddff", postStroke = "#ffffff",
                postOpacity = 0.55, postEmissive = 0.9,
                locationPin = "#44aaff", landmarkPin = "#88eeff",
                radiusFill = "#88eeff", radiusStroke = "#55ddff",
                radiusFillOpacity = 0.14, radiusStrokeOpacity = 0.65,
                buildingFill = "#d0ecf8", buildingEmissive = 0.10, buildingOpacity = 1.0,
                landTint = "#c8eaf8", landTintOpacity = 0.13,
                accent = "#55ddff",
            )
            GOLD -> Colors(
                postCircle = "#fac72e", postStroke = "#fff0a0",
                postOpacity = 0.60, postEmissive = 1.1,
                locationPin = "#e8960a", landmarkPin = "#fac72e",
                radiusFill = "#fac72e", radiusStroke = "#e8960a",
                radiusFillOpacity = 0.12, radiusStrokeOpacity = 0.70,
                buildingFill = "#3d2a0a", buildingEmissive = 0.30, buildingOpacity = 1.0,
                landTint = "#1a1000", landTintOpacity = 0.12,
                accent = "#fac72e",
            )
            GLITTER -> Colors(
                postCircle = "#ff44dd", postStroke = "#cc88ff",
                postOpacity = 0.60, postEmissive = 1.4,
                locationPin = "#00eeff", landmarkPin = "#ff44dd",
                radiusFill = "#cc00ff", radiusStroke = "#ff44dd",
                radiusFillOpacity = 0.15, radiusStrokeOpacity = 0.75,
                buildingFill = "#1a0020", buildingEmissive = 0.45, buildingOpacity = 1.0,
                landTint = "#0d0020", landTintOpacity = 0.22,
                accent = "#ff44dd",
            )
            RAINBOW -> Colors(
                postCircle = "#ff0080", postStroke = "#ffffff",
                postOpacity = 0.65, postEmissive = 1.3,
                locationPin = "#0099ff", landmarkPin = "#ff6600",
                radiusFill = "#7700ff", radiusStroke = "#ff0080",
                radiusFillOpacity = 0.14, radiusStrokeOpacity = 0.70,
                buildingFill = "#0d0d1a", buildingEmissive = 0.35, buildingOpacity = 1.0,
                landTint = "#0d0010", landTintOpacity = 0.20,
                accent = "#ff0080",
            )
            CHOCOLATE -> Colors(
                postCircle = "#d4874a", postStroke = "#f5deb3",
                postOpacity = 0.55, postEmissive = 0.8,
                locationPin = "#8b4513", landmarkPin = "#d4874a",
                radiusFill = "#cd853f", radiusStroke = "#d4874a",
                radiusFillOpacity = 0.12, radiusStrokeOpacity = 0.65,
                buildingFill = "#3d1a00", buildingEmissive = 0.20, buildingOpacity = 1.0,
                landTint = "#1a0800", landTintOpacity = 0.13,
                accent = "#d4874a",
            )
            CARNIVAL -> Colors(
                postCircle = "#ff4466", postStroke = "#ffee00",
                postOpacity = 0.65, postEmissive = 1.2,
                locationPin = "#ff4466", landmarkPin = "#44ddff",
                radiusFill = "#ff0055", radiusStroke = "#ffcc00",
                radiusFillOpacity = 0.15, radiusStrokeOpacity = 0.80,
                buildingFill = "#220011", buildingEmissive = 0.30, buildingOpacity = 1.0,
                landTint = "#000000", landTintOpacity = 0.0,
                accent = "#ff4466",
            )
            CAMO -> Colors(
                postCircle = "#6b8c42", postStroke = "#4a6a28",
                postOpacity = 0.55, postEmissive = 0.6,
                locationPin = "#3d5c1e", landmarkPin = "#8aaa55",
                radiusFill = "#4a6a28", radiusStroke = "#6b8c42",
                radiusFillOpacity = 0.12, radiusStrokeOpacity = 0.65,
                buildingFill = "#1a2410", buildingEmissive = 0.15, buildingOpacity = 1.0,
                landTint = "#0a1400", landTintOpacity = 0.16,
                accent = "#6b8c42",
            )
            TOY_BOX -> Colors(
                postCircle = "#ff2200", postStroke = "#ffff00",
                postOpacity = 0.70, postEmissive = 1.2,
                locationPin = "#0066ff", landmarkPin = "#ff2200",
                radiusFill = "#ff0000", radiusStroke = "#ffff00",
                radiusFillOpacity = 0.15, radiusStrokeOpacity = 0.80,
                buildingFill = "#001133", buildingEmissive = 0.25, buildingOpacity = 1.0,
                landTint = "#000000", landTintOpacity = 0.0,
                accent = "#ff2200",
            )
        }

    val styleConfig: StyleConfig
        get() = when (this) {
            DEFAULT    -> StyleConfig("dusk",  show3dObjects = true,  showRoadLabels = true,  showPoiLabels = true,  showPlaceLabels = true,  customBuildings = false, animationType = AnimationType.PULSE)
            DARK       -> StyleConfig("night", show3dObjects = true,  showRoadLabels = true,  showPoiLabels = false, showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.PULSE)
            ICE        -> StyleConfig("dawn",  show3dObjects = true,  showRoadLabels = true,  showPoiLabels = true,  showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.PULSE)
            GOLD       -> StyleConfig("day",   show3dObjects = true,  showRoadLabels = true,  showPoiLabels = true,  showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.PULSE)
            GLITTER    -> StyleConfig("night", show3dObjects = true,  showRoadLabels = false, showPoiLabels = false, showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.GLITTER_SPARKLE)
            RAINBOW    -> StyleConfig("night", show3dObjects = true,  showRoadLabels = false, showPoiLabels = false, showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.RAINBOW_CYCLE)
            CHOCOLATE  -> StyleConfig("dusk",  show3dObjects = true,  showRoadLabels = true,  showPoiLabels = true,  showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.PULSE)
            CARNIVAL   -> StyleConfig("day",   show3dObjects = false, showRoadLabels = true,  showPoiLabels = true,  showPlaceLabels = true,  customBuildings = false, animationType = AnimationType.PULSE)
            CAMO       -> StyleConfig("dusk",  show3dObjects = true,  showRoadLabels = true,  showPoiLabels = false, showPlaceLabels = true,  customBuildings = true,  animationType = AnimationType.NONE)
            TOY_BOX    -> StyleConfig("day",   show3dObjects = false, showRoadLabels = true,  showPoiLabels = true,  showPlaceLabels = true,  customBuildings = false, animationType = AnimationType.PULSE)
        }

    // ── Pin theme used by MarkerRenderer ────────────────────────────────────

    data class PinTheme(
        val base: Int,
        val mid: Int,
        val highlight: Int,
        val glow: Int,
    )

    companion object {
        fun fromCubeSkin(skin: CubeSkin): MapTheme = when (skin) {
            CubeSkin.Default   -> DEFAULT
            CubeSkin.Gold      -> GOLD
            CubeSkin.Toy       -> TOY_BOX
            CubeSkin.Rainbow   -> RAINBOW
            CubeSkin.Carbon, CubeSkin.Clay -> GLITTER
            CubeSkin.Ice       -> ICE
            CubeSkin.Chocolate -> CHOCOLATE
            CubeSkin.Space     -> DARK
            CubeSkin.Camo      -> CAMO
        }

        fun fromId(id: String): MapTheme =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

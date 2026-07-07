package com.apptists.poplur2025.map

import android.util.Log
import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.all
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.gt
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer

/**
 * Adds and maintains a theme-colored building extrusion layer.
 *
 * When [MapTheme.StyleConfig.customBuildings] is true:
 *   • The Standard style's built-in 3D objects are disabled (show3dObjects = false)
 *     to avoid double-rendering.
 *   • A custom FillExtrusionLayer is inserted from the "composite" Mapbox source,
 *     giving us full control over color, opacity, emissive strength, and AO.
 *
 * When false:
 *   • The Standard style's own 3D objects are used (show3dObjects follows the
 *     theme's preference) and our custom layer is removed if present.
 *
 * [sync] is idempotent — safe to call on every theme change.
 */
class BuildingRenderer {

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Synchronises the building layer state with [theme].
     * Call from the main thread after [PoplurMapThemeManager.applyCurrentTheme].
     */
    fun sync(style: Style, theme: MapTheme) {
        if (theme.styleConfig.customBuildings) {
            addOrUpdate(style, theme)
        } else {
            remove(style)
        }
    }

    // ── Layer management ─────────────────────────────────────────────────────

    private fun addOrUpdate(style: Style, theme: MapTheme) = style.safe("buildings-sync") {
        val c = theme.colors
        if (styleLayerExists(THEME_BUILDINGS_LAYER_ID)) {
            setStyleLayerProperty(THEME_BUILDINGS_LAYER_ID, "fill-extrusion-color",            Value.valueOf(c.buildingFill))
            setStyleLayerProperty(THEME_BUILDINGS_LAYER_ID, "fill-extrusion-opacity",           Value.valueOf(c.buildingOpacity))
            setStyleLayerProperty(THEME_BUILDINGS_LAYER_ID, "fill-extrusion-emissive-strength", Value.valueOf(c.buildingEmissive))
        } else {
            val layer = fillExtrusionLayer(THEME_BUILDINGS_LAYER_ID, VECTOR_SRC) {
                sourceLayer(SOURCE_LAYER)
                filter(
                    all {
                        eq {
                            get("extrude")
                            literal("true")
                        }
                        gt {
                            get("height")
                            literal(0.0)
                        }
                    }
                )
                fillExtrusionHeight(get("height"))
                fillExtrusionBase(get("min_height"))
                fillExtrusionColor(c.buildingFill)
                fillExtrusionOpacity(c.buildingOpacity)
                fillExtrusionEmissiveStrength(c.buildingEmissive)
                fillExtrusionAmbientOcclusionIntensity(0.30)
                fillExtrusionAmbientOcclusionRadius(3.0)
                fillExtrusionVerticalScale(1.0)
            }
            if (styleLayerExists(POST_LAYER_ID)) addLayerAbove(layer, POST_LAYER_ID)
            else addLayer(layer)
        }
    }

    private fun remove(style: Style) = style.safe("buildings-remove") {
        if (styleLayerExists(THEME_BUILDINGS_LAYER_ID))
            removeStyleLayer(THEME_BUILDINGS_LAYER_ID)
    }

    private inline fun Style.safe(label: String, block: Style.() -> Unit) {
        runCatching { block() }.onFailure {
            Log.w(TAG, "[$label] ${it.message}")
        }
    }

    companion object {
        private const val TAG          = "BuildingRenderer"
        private const val VECTOR_SRC   = "composite"
        private const val SOURCE_LAYER = "building"
    }
}

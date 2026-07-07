package com.apptists.poplur2025.map

import android.util.Log
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Manages the themed radius circle rendered on the map.
 *
 * The radius circle consists of:
 *   • A fill layer with configurable color and opacity
 *   • A stroke (line) layer with a glow-style width
 *   • Optional pulse animation that breathes the opacity over time
 *
 * This renderer does NOT decide when to show the radius — that logic stays in
 * MapScreen.kt. Call [addLayers] once after the style loads, then call
 * [updateCenter] whenever the location or radius value changes.
 *
 * NOTE: The radius was hidden "as per request" in the current build. This class
 * is ready to use when it is re-enabled; nothing in MapScreen's existing logic
 * needs to change, just call [addLayers] in preparePoplurMapStyle.
 */
class RadiusRenderer {

    private var pulseJob: Job? = null
    private var currentOpacity = 0.10
    private var strokeOpacity  = 0.70

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Adds the radius fill and stroke layers to [style].
     * Must be called from the Mapbox style-loaded callback (main thread).
     */
    fun addLayers(style: Style, theme: MapTheme) = style.safe("radius-add-layers") {
        val c = theme.colors

        // Sources (GeoJSON, start empty — populated by updateCenter)
        if (!sourceExists(style, SOURCE_FILL))
            addSource(geoJsonSource(SOURCE_FILL) {})
        if (!sourceExists(style, SOURCE_LINE))
            addSource(geoJsonSource(SOURCE_LINE) {})

        // Fill layer
        if (!styleLayerExists(RADIUS_FILL_LAYER_ID)) {
            val fill = fillLayer(RADIUS_FILL_LAYER_ID, SOURCE_FILL) {
                fillColor(c.radiusFill)
                fillOpacity(c.radiusFillOpacity)
                fillEmissiveStrength(0.4)
                fillAntialias(true)
            }
            if (styleLayerExists(POST_LAYER_ID)) addLayerAbove(fill, POST_LAYER_ID)
            else addLayer(fill)
        }

        // Stroke (line) layer — slightly wider for a glow effect
        if (!styleLayerExists(RADIUS_LINE_LAYER_ID)) {
            val line = lineLayer(RADIUS_LINE_LAYER_ID, SOURCE_LINE) {
                lineColor(c.radiusStroke)
                lineOpacity(c.radiusStrokeOpacity)
                lineWidth(2.5)
                lineBlur(1.8)
                lineEmissiveStrength(1.0)
            }
            if (styleLayerExists(RADIUS_FILL_LAYER_ID)) addLayerAbove(line, RADIUS_FILL_LAYER_ID)
            else addLayer(line)
        }
    }

    /**
     * Updates the circle's center and radius.
     * Call this every time the selected coordinate or radius value changes.
     */
    fun updateCenter(style: Style, center: Point, radiusKm: Int) = style.safe("radius-update") {
        val polygon = buildCirclePolygon(center, radiusKm)
        getSourceAs<GeoJsonSource>(SOURCE_FILL)?.geometry(polygon)
        getSourceAs<GeoJsonSource>(SOURCE_LINE)?.geometry(polygon)
    }

    /**
     * Updates only the theme colors without touching geometry.
     */
    fun applyTheme(style: Style, theme: MapTheme) {
        val c = theme.colors
        currentOpacity = c.radiusFillOpacity
        strokeOpacity  = c.radiusStrokeOpacity
        style.safe("radius-theme") {
            if (styleLayerExists(RADIUS_FILL_LAYER_ID)) {
                setStyleLayerProperty(RADIUS_FILL_LAYER_ID, "fill-color",   Value.valueOf(c.radiusFill))
                setStyleLayerProperty(RADIUS_FILL_LAYER_ID, "fill-opacity", Value.valueOf(c.radiusFillOpacity))
            }
            if (styleLayerExists(RADIUS_LINE_LAYER_ID)) {
                setStyleLayerProperty(RADIUS_LINE_LAYER_ID, "line-color",   Value.valueOf(c.radiusStroke))
                setStyleLayerProperty(RADIUS_LINE_LAYER_ID, "line-opacity", Value.valueOf(c.radiusStrokeOpacity))
            }
        }
    }

    /**
     * Starts a smooth opacity pulse animation on the radius fill layer.
     * Animations pause automatically when [pausePulse] is called.
     */
    fun startPulse(style: Style, scope: CoroutineScope) {
        stopPulse()
        pulseJob = scope.launch {
            var frame = 0
            while (isActive) {
                val pulse = (sin(frame * 0.05) * 0.5 + 0.5).toFloat()   // 0..1 at ~0.5 Hz
                val opacity = currentOpacity * (0.65 + 0.35 * pulse)
                style.safe("radius-pulse") {
                    if (styleLayerExists(RADIUS_FILL_LAYER_ID))
                        setStyleLayerProperty(RADIUS_FILL_LAYER_ID, "fill-opacity", Value.valueOf(opacity))
                }
                frame++
                delay(33)  // ~30 fps — efficient for a slow pulse
            }
        }
    }

    fun stopPulse() {
        pulseJob?.cancel()
        pulseJob = null
    }

    fun removeLayers(style: Style) = style.safe("radius-remove") {
        if (styleLayerExists(RADIUS_LINE_LAYER_ID)) removeStyleLayer(RADIUS_LINE_LAYER_ID)
        if (styleLayerExists(RADIUS_FILL_LAYER_ID)) removeStyleLayer(RADIUS_FILL_LAYER_ID)
    }

    // ── Geometry ─────────────────────────────────────────────────────────────

    private fun buildCirclePolygon(center: Point, radiusKm: Int): Polygon {
        val clampedKm   = radiusKm.coerceAtLeast(1)
        val angularDist = clampedKm / EARTH_KM
        val centerLat   = Math.toRadians(center.latitude())
        val centerLng   = Math.toRadians(center.longitude())

        val ring = (0..SEGMENTS).map { index ->
            val bearing = 2.0 * PI * index / SEGMENTS
            val lat = asin(
                sin(centerLat) * cos(angularDist) +
                    cos(centerLat) * sin(angularDist) * cos(bearing)
            )
            val lng = centerLng + atan2(
                sin(bearing) * sin(angularDist) * cos(centerLat),
                cos(angularDist) - sin(centerLat) * sin(lat)
            )
            Point.fromLngLat(Math.toDegrees(lng), Math.toDegrees(lat))
        }
        return Polygon.fromLngLats(listOf(ring))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sourceExists(style: Style, id: String): Boolean =
        runCatching { style.getSourceAs<GeoJsonSource>(id) != null }.getOrElse { false }

    private inline fun Style.safe(label: String, block: Style.() -> Unit) {
        runCatching { block() }.onFailure {
            Log.w(TAG, "[$label] ${it.message}")
        }
    }

    companion object {
        private const val TAG = "RadiusRenderer"
        private const val SOURCE_FILL = "poplur-radius-circle-source"
        private const val SOURCE_LINE = "poplur-radius-circle-line-source"
        private const val SEGMENTS = 128      // Points used to approximate the circle
        private const val EARTH_KM = 6_371.0  // Earth mean radius in km
    }
}

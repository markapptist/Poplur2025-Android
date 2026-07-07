package com.apptists.poplur2025.ui.screens

import com.mapbox.maps.extension.style.expressions.dsl.generated.all
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.gt
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.extension.style.layers.generated.backgroundLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.rasterLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.sources.generated.rasterSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.extension.style.StyleExtensionImpl

/**
 * "Default Poplur" 2D map style — the first fully-realized theme from the Poplur Map Style
 * Generator spec. Built from Mapbox's public `mapbox-streets-v8` vector tileset with fully
 * custom paint properties, rather than customizing Mapbox Standard (whose runtime config slots
 * don't allow this level of per-road-class recoloring).
 *
 * Covers what a Mapbox style can actually express: dark land, neon/glow roads by class, emerald
 * parks, deep turquoise water, white rounded labels, and colorful circular POI markers. Things
 * the spec asks for that no map style (Mapbox or otherwise) can render — animated water shaders,
 * individually modeled trees, reflective glass/steel building materials, weather particles,
 * bloom, on-map fireworks/confetti — are NOT part of this style; those belong in an app-level
 * overlay layered on top of the MapView, the same way this app already renders confetti/radius
 * rings, not in the Mapbox style itself.
 */
private const val POPLUR_STREETS_SOURCE_ID = "poplur-streets"
private const val POPLUR_STREETS_URL = "mapbox://mapbox.mapbox-streets-v8"

private object PoplurMapColors {
    const val LAND = "#1a1a1f"
    const val WATER = "#08383d"
    const val PARK = "#0f5c47"
    const val HIGHWAY = "#00e5ff"
    const val PRIMARY_ROAD = "#ff2d8a"
    const val SECONDARY_ROAD = "#9b6bff"
    const val MINOR_ROAD = "#3a3a45"
    const val LABEL_TEXT = "#ffffff"
    const val LABEL_HALO = "#000000"

    const val POI_FOOD = "#ffd700"
    const val POI_MUSIC = "#ff2d8a"
    const val POI_SHOPPING = "#3ddccf"
    const val POI_COFFEE = "#2ecc71"
    const val POI_HOTEL = "#3aa0ff"
    const val POI_DEFAULT = "#b98bff"
}

/** Rounded, highly legible label font already bundled with Mapbox's hosted font sets. */
private val LABEL_FONT_STACK = listOf("DIN Pro Medium", "Arial Unicode MS Regular")

/**
 * A minimal, blank Style Spec document — no sources, no layers — used as the base so the map
 * starts from a clean canvas instead of inheriting Mapbox Streets/Standard's own layers. Glyphs
 * (label fonts) must be declared here: the Kotlin style builder DSL has no `glyphs` setter of its
 * own, so it can only come from the base style document.
 */
const val POPLUR_BLANK_BASE_STYLE = """
{
  "version": 8,
  "sources": {},
  "layers": [],
  "glyphs": "mapbox://fonts/mapbox/{fontstack}/{range}.pbf"
}
"""

fun poplurDefaultMapStyle(): StyleContract.StyleExtension = style(POPLUR_BLANK_BASE_STYLE) {
    addPoplurDefaultMapLayers()
}

/**
 * Adds every "Default Poplur" layer to an in-progress style builder. Exposed separately (rather
 * than only via [poplurDefaultMapStyle]) so a caller that also needs its own sources/layers in
 * the same style (e.g. MapScreen's post/pin overlays) can compose them into one `style { }` call
 * — Mapbox's `loadStyle` takes a single style extension, so two independently-built ones can't
 * be merged after the fact.
 */
fun StyleExtensionImpl.Builder.addPoplurDefaultMapLayers() {
    +vectorSource(POPLUR_STREETS_SOURCE_ID) {
        url(POPLUR_STREETS_URL)
    }

    +backgroundLayer("poplur-bg") {
        backgroundColor(PoplurMapColors.LAND)
    }

    +fillLayer("poplur-water", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("water")
        fillColor(PoplurMapColors.WATER)
    }

    +fillLayer("poplur-parks", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("landuse")
        filter(eq { get { literal("class") }; literal("park") })
        fillColor(PoplurMapColors.PARK)
        fillOpacity(1.0)
    }

    // ── Highways (motorway/trunk) — cyan, strongest glow ──
    +lineLayer("poplur-highway-glow", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("motorway"); literal(true) }
                stop { literal("trunk"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurMapColors.HIGHWAY)
        lineWidth(9.0)
        lineOpacity(0.35)
        lineBlur(6.0)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }
    +lineLayer("poplur-highway-crisp", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("motorway"); literal(true) }
                stop { literal("trunk"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurMapColors.HIGHWAY)
        lineWidth(2.2)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    // ── Primary roads — neon pink ──
    +lineLayer("poplur-primary-glow", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(eq { get { literal("class") }; literal("primary") })
        lineColor(PoplurMapColors.PRIMARY_ROAD)
        lineWidth(7.0)
        lineOpacity(0.30)
        lineBlur(5.0)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }
    +lineLayer("poplur-primary-crisp", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(eq { get { literal("class") }; literal("primary") })
        lineColor(PoplurMapColors.PRIMARY_ROAD)
        lineWidth(1.8)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    // ── Secondary/tertiary roads — soft purple ──
    +lineLayer("poplur-secondary-glow", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("secondary"); literal(true) }
                stop { literal("tertiary"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurMapColors.SECONDARY_ROAD)
        lineWidth(4.5)
        lineOpacity(0.22)
        lineBlur(4.0)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }
    +lineLayer("poplur-secondary-crisp", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("secondary"); literal(true) }
                stop { literal("tertiary"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurMapColors.SECONDARY_ROAD)
        lineWidth(1.2)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    // Minor streets stay dim and glow-free — "minimal clutter" per the spec.
    +lineLayer("poplur-minor-roads", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("street"); literal(true) }
                stop { literal("path"); literal(true) }
                stop { literal("service"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurMapColors.MINOR_ROAD)
        lineWidth(0.75)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    +symbolLayer("poplur-place-labels", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("place_label")
        textField("{name}")
        textFont(LABEL_FONT_STACK)
        textColor(PoplurMapColors.LABEL_TEXT)
        textHaloColor(PoplurMapColors.LABEL_HALO)
        textHaloWidth(1.2)
        textSize(
            interpolate {
                linear()
                zoom()
                stop { literal(4.0); literal(10.0) }
                stop { literal(12.0); literal(16.0) }
            }
        )
        textAnchor(TextAnchor.CENTER)
    }

    // Generic map POIs (restaurants, cafés, shops, hotels) as colorful circular markers —
    // Poplur's own pin/post/event overlays are separate GeoJSON layers added by the caller.
    +circleLayer("poplur-poi", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("poi_label")
        minZoom(14.0)
        circleRadius(5.0)
        circleColor(
            match {
                get { literal("maki") }
                stop { literal("restaurant"); literal(PoplurMapColors.POI_FOOD) }
                stop { literal("fast-food"); literal(PoplurMapColors.POI_FOOD) }
                stop { literal("music"); literal(PoplurMapColors.POI_MUSIC) }
                stop { literal("shop"); literal(PoplurMapColors.POI_SHOPPING) }
                stop { literal("clothing-store"); literal(PoplurMapColors.POI_SHOPPING) }
                stop { literal("cafe"); literal(PoplurMapColors.POI_COFFEE) }
                stop { literal("lodging"); literal(PoplurMapColors.POI_HOTEL) }
                literal(PoplurMapColors.POI_DEFAULT)
            }
        )
        circleStrokeColor(PoplurMapColors.LABEL_TEXT)
        circleStrokeWidth(1.4)
        circleOpacity(0.92)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "Poplur Hybrid" — satellite imagery + roads/labels/3D buildings, matching
// iOS's MKMapType.hybrid(elevation: .realistic). Reuses the same
// mapbox-streets-v8 vector source as the default style for roads, place
// labels, POIs, and buildings, but swaps the flat vector land/water/park
// fills for real satellite raster tiles plus terrain elevation, and restyles
// roads/labels for legibility over imagery instead of neon-on-dark.
// ─────────────────────────────────────────────────────────────────────────────

private const val POPLUR_SATELLITE_SOURCE_ID = "poplur-satellite"
private const val POPLUR_SATELLITE_URL = "mapbox://mapbox.satellite"
private const val POPLUR_TERRAIN_DEM_SOURCE_ID = "poplur-terrain-dem"
private const val POPLUR_TERRAIN_DEM_URL = "mapbox://mapbox.mapbox-terrain-dem-v1"

private object PoplurHybridColors {
    const val ROAD_HIGHWAY = "#ffffff"
    const val ROAD_PRIMARY = "#f2f2f0"
    const val ROAD_SECONDARY = "#e2e0d8"
    const val ROAD_MINOR = "#c9c7bd"
    const val ROAD_CASING = "#0c0c0c"
    const val LABEL_TEXT = "#ffffff"
    const val LABEL_HALO = "#000000"
    const val BUILDING_FILL = "#d8d6cc"
}

fun poplurHybridMapStyle(): StyleContract.StyleExtension = style(POPLUR_BLANK_BASE_STYLE) {
    addPoplurHybridMapLayers()
}

/**
 * Adds every "Poplur Hybrid" layer to an in-progress style builder — same composition
 * contract as [addPoplurDefaultMapLayers], for callers that need to add their own
 * sources/layers (post pins, music waves, etc.) into the same `style { }` call.
 */
fun StyleExtensionImpl.Builder.addPoplurHybridMapLayers() {
    +vectorSource(POPLUR_STREETS_SOURCE_ID) {
        url(POPLUR_STREETS_URL)
    }
    +rasterSource(POPLUR_SATELLITE_SOURCE_ID) {
        url(POPLUR_SATELLITE_URL)
        tileSize(512)
    }
    +rasterDemSource(POPLUR_TERRAIN_DEM_SOURCE_ID) {
        url(POPLUR_TERRAIN_DEM_URL)
        tileSize(514)
    }
    +terrain(POPLUR_TERRAIN_DEM_SOURCE_ID) {
        exaggeration(1.35)
    }

    +rasterLayer("poplur-satellite-layer", POPLUR_SATELLITE_SOURCE_ID) {
        rasterOpacity(1.0)
    }

    // ── Roads — white/light-gray with a dark casing, no neon glow, over imagery ──
    +lineLayer("poplur-highway-casing", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("motorway"); literal(true) }
                stop { literal("trunk"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurHybridColors.ROAD_CASING)
        lineWidth(5.0)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }
    +lineLayer("poplur-highway-fill", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("motorway"); literal(true) }
                stop { literal("trunk"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurHybridColors.ROAD_HIGHWAY)
        lineWidth(2.6)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    +lineLayer("poplur-primary-casing", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(eq { get { literal("class") }; literal("primary") })
        lineColor(PoplurHybridColors.ROAD_CASING)
        lineWidth(3.6)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }
    +lineLayer("poplur-primary-fill", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(eq { get { literal("class") }; literal("primary") })
        lineColor(PoplurHybridColors.ROAD_PRIMARY)
        lineWidth(1.8)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    +lineLayer("poplur-secondary-casing", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("secondary"); literal(true) }
                stop { literal("tertiary"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurHybridColors.ROAD_CASING)
        lineWidth(2.4)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }
    +lineLayer("poplur-secondary-fill", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("secondary"); literal(true) }
                stop { literal("tertiary"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurHybridColors.ROAD_SECONDARY)
        lineWidth(1.1)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    +lineLayer("poplur-minor-roads-hybrid", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("road")
        filter(
            match {
                get { literal("class") }
                stop { literal("street"); literal(true) }
                stop { literal("path"); literal(true) }
                stop { literal("service"); literal(true) }
                literal(false)
            }
        )
        lineColor(PoplurHybridColors.ROAD_MINOR)
        lineWidth(0.75)
        lineOpacity(0.85)
        lineJoin(LineJoin.ROUND)
        lineCap(LineCap.ROUND)
    }

    // ── 3D buildings, extruded from the same vector tileset ──
    +fillExtrusionLayer("poplur-hybrid-buildings", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("building")
        filter(
            all {
                eq { get("extrude"); literal("true") }
                gt { get("height"); literal(0.0) }
            }
        )
        fillExtrusionHeight(get("height"))
        fillExtrusionBase(get("min_height"))
        fillExtrusionColor(PoplurHybridColors.BUILDING_FILL)
        fillExtrusionOpacity(1.0)
    }

    +symbolLayer("poplur-place-labels-hybrid", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("place_label")
        textField("{name}")
        textFont(LABEL_FONT_STACK)
        textColor(PoplurHybridColors.LABEL_TEXT)
        textHaloColor(PoplurHybridColors.LABEL_HALO)
        textHaloWidth(1.4)
        textSize(
            interpolate {
                linear()
                zoom()
                stop { literal(4.0); literal(10.0) }
                stop { literal(12.0); literal(16.0) }
            }
        )
        textAnchor(TextAnchor.CENTER)
    }

    // Generic map POIs — same colorful circle-marker treatment as the default style.
    +circleLayer("poplur-poi-hybrid", POPLUR_STREETS_SOURCE_ID) {
        sourceLayer("poi_label")
        minZoom(14.0)
        circleRadius(5.0)
        circleColor(
            match {
                get { literal("maki") }
                stop { literal("restaurant"); literal(PoplurMapColors.POI_FOOD) }
                stop { literal("fast-food"); literal(PoplurMapColors.POI_FOOD) }
                stop { literal("music"); literal(PoplurMapColors.POI_MUSIC) }
                stop { literal("shop"); literal(PoplurMapColors.POI_SHOPPING) }
                stop { literal("clothing-store"); literal(PoplurMapColors.POI_SHOPPING) }
                stop { literal("cafe"); literal(PoplurMapColors.POI_COFFEE) }
                stop { literal("lodging"); literal(PoplurMapColors.POI_HOTEL) }
                literal(PoplurMapColors.POI_DEFAULT)
            }
        )
        circleStrokeColor(PoplurHybridColors.LABEL_TEXT)
        circleStrokeWidth(1.4)
        circleOpacity(0.92)
    }
}

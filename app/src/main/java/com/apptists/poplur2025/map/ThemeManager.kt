package com.apptists.poplur2025.map

import android.content.Context
import android.util.Log
import com.apptists.poplur2025.ui.CubeSkin
import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.backgroundLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central manager for Poplur's dynamic map theme system.
 *
 * Usage in MapScreen.kt
 * ─────────────────────
 * 1. One-time init (e.g. in Application.onCreate or before MapView creation):
 *        PoplurMapThemeManager.initialize(context)
 *
 * 2. After style loads (inside loadStyle callback):
 *        PoplurMapThemeManager.applyCurrentTheme(style, scope)
 *
 * 3. When CubeSkin changes (LaunchedEffect in MapScreen):
 *        LaunchedEffect(selectedCubeSkin) {
 *            val style = mapView?.mapboxMap?.style ?: return@LaunchedEffect
 *            PoplurMapThemeManager.setThemeFromSkin(selectedCubeSkin, style, scope)
 *        }
 *
 * 4. Optional Compose fade overlay (place inside the MapView's parent Box):
 *        val transitioning by PoplurMapThemeManager.isTransitioning.collectAsState()
 *        if (transitioning) {
 *            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
 *        }
 */
object PoplurMapThemeManager {

    private const val TAG = "PoplurMapTheme"
    private const val TRANSITION_STEP_MS  = 12L   // ~83 FPS ramp
    private const val TRANSITION_STEPS    = 20    // 240 ms total fade

    private lateinit var repository: ThemeRepository
    private val animationController = MapAnimationController()
    private val buildingRenderer    = BuildingRenderer()

    private val _currentTheme    = MutableStateFlow(MapTheme.DEFAULT)
    val currentTheme: StateFlow<MapTheme> = _currentTheme.asStateFlow()

    private val _isTransitioning = MutableStateFlow(false)
    val isTransitioning: StateFlow<Boolean> = _isTransitioning.asStateFlow()

    // Live style reference; cleared when the map is destroyed
    @Volatile private var liveStyle: Style? = null
    private var transitionJob: Job? = null

    // ── Lifecycle ────────────────────────────────────────────────────────────

    fun initialize(context: Context) {
        repository = ThemeRepository(context.applicationContext)
        animationController.initialize(context.applicationContext)
        _currentTheme.value = repository.load()
    }

    /**
     * Called once by MapScreen after the Mapbox style finishes loading.
     * Applies the persisted theme immediately (no transition animation).
     */
    fun applyCurrentTheme(style: Style, scope: CoroutineScope) {
        liveStyle = style
        val theme = _currentTheme.value
        applyThemeImmediate(style, theme)
        buildingRenderer.sync(style, theme)
        animationController.start(theme, style, scope)
    }

    /** Called when the map is destroyed so we don't hold a stale reference. */
    fun onMapDestroyed() {
        liveStyle = null
        animationController.stop()
    }

    /** Notify the animation controller that the map is idle (saves battery). */
    fun onMapIdle()    { animationController.pause() }
    /** Notify the animation controller that the user is interacting with the map. */
    fun onMapMoving()  { animationController.resume() }

    /** Stops all theme coroutine wakeups when the MapView is hidden or pre-empted. */
    fun setRenderingActive(active: Boolean) {
        animationController.setRenderingActive(active)
    }

    // ── Theme switching ──────────────────────────────────────────────────────

    /** Switch to a theme derived from the active CubeSkin with a fade transition. */
    fun setThemeFromSkin(skin: CubeSkin, style: Style, scope: CoroutineScope) {
        setTheme(MapTheme.fromCubeSkin(skin), style, scope)
    }

    /** Switch to any MapTheme with a smooth animated fade. */
    fun setTheme(theme: MapTheme, style: Style, scope: CoroutineScope) {
        if (theme == _currentTheme.value) return
        liveStyle = style
        transitionJob?.cancel()
        transitionJob = scope.launch {
            performTransition(style, theme, scope)
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Fades the map out (via the Compose overlay flag), applies the new theme,
     * then signals the overlay to fade back in.
     */
    private suspend fun performTransition(style: Style, newTheme: MapTheme, scope: CoroutineScope) {
        _isTransitioning.value = true
        delay(TRANSITION_STEPS * TRANSITION_STEP_MS)   // Give the Compose overlay time to appear

        withContext(Dispatchers.Main) {
            applyThemeImmediate(style, newTheme)
            buildingRenderer.sync(style, newTheme)
            animationController.start(newTheme, style, scope)
        }

        delay(TRANSITION_STEPS * TRANSITION_STEP_MS)   // Hold briefly so the style settles
        _isTransitioning.value = false

        _currentTheme.value = newTheme
        repository.save(newTheme)
    }

    /**
     * Applies every theme property to the live style without animation.
     * Safe to call from the main thread inside a Mapbox style callback.
     */
    private fun applyThemeImmediate(style: Style, theme: MapTheme) {
        val c = theme.colors
        val s = theme.styleConfig

        style.safe("import-config") {
            setStyleImportConfigProperty(BASEMAP_IMPORT_ID, "lightPreset",          Value.valueOf(s.lightPreset))
            setStyleImportConfigProperty(BASEMAP_IMPORT_ID, "show3dObjects",         Value.valueOf(!s.customBuildings && s.show3dObjects))
            setStyleImportConfigProperty(BASEMAP_IMPORT_ID, "showRoadLabels",        Value.valueOf(s.showRoadLabels))
            setStyleImportConfigProperty(BASEMAP_IMPORT_ID, "showPointOfInterestLabels", Value.valueOf(s.showPoiLabels))
            setStyleImportConfigProperty(BASEMAP_IMPORT_ID, "showPlaceLabels",       Value.valueOf(s.showPlaceLabels))
        }

        style.safe("post-layer") {
            if (styleLayerExists(POST_LAYER_ID)) {
                setStyleLayerProperty(POST_LAYER_ID, "circle-color",            Value.valueOf(c.postCircle))
                setStyleLayerProperty(POST_LAYER_ID, "circle-stroke-color",     Value.valueOf(c.postStroke))
                setStyleLayerProperty(POST_LAYER_ID, "circle-opacity",          Value.valueOf(c.postOpacity))
                setStyleLayerProperty(POST_LAYER_ID, "circle-emissive-strength",Value.valueOf(c.postEmissive))
            }
        }

        style.safe("radius-layer") {
            if (styleLayerExists(RADIUS_FILL_LAYER_ID)) {
                setStyleLayerProperty(RADIUS_FILL_LAYER_ID, "fill-color",   Value.valueOf(c.radiusFill))
                setStyleLayerProperty(RADIUS_FILL_LAYER_ID, "fill-opacity", Value.valueOf(c.radiusFillOpacity))
            }
            if (styleLayerExists(RADIUS_LINE_LAYER_ID)) {
                setStyleLayerProperty(RADIUS_LINE_LAYER_ID, "line-color",   Value.valueOf(c.radiusStroke))
                setStyleLayerProperty(RADIUS_LINE_LAYER_ID, "line-opacity", Value.valueOf(c.radiusStrokeOpacity))
            }
        }

        // Land tint overlay — replace or create as needed
        applyLandTint(style, c)
    }

    private fun applyLandTint(style: Style, c: MapTheme.Colors) {
        style.safe("land-tint") {
            if (c.landTintOpacity <= 0.0) {
                if (styleLayerExists(THEME_LAND_TINT_LAYER_ID))
                    removeStyleLayer(THEME_LAND_TINT_LAYER_ID)
                return@safe
            }
            if (styleLayerExists(THEME_LAND_TINT_LAYER_ID)) {
                setStyleLayerProperty(THEME_LAND_TINT_LAYER_ID, "background-color",   Value.valueOf(c.landTint))
                setStyleLayerProperty(THEME_LAND_TINT_LAYER_ID, "background-opacity", Value.valueOf(c.landTintOpacity))
            } else {
                // Insert tint below the post marker layer so pins stay on top
                val layer = com.mapbox.maps.extension.style.layers.generated.backgroundLayer(THEME_LAND_TINT_LAYER_ID) {
                    backgroundColor(c.landTint)
                    backgroundOpacity(c.landTintOpacity)
                }
                if (styleLayerExists(POST_LAYER_ID)) addLayerBelow(layer, POST_LAYER_ID)
                else addLayer(layer)
            }
        }
    }

    // Mirrors the safeMapStyleUpdate pattern from MapScreen.kt
    private inline fun Style.safe(label: String, block: Style.() -> Unit) {
        runCatching { block() }.onFailure {
            Log.w(TAG, "Stale style update [$label]: ${it.message}")
        }
    }
}

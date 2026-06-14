package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.AudioAttributes
import androidx.compose.ui.platform.LocalInspectionMode
import com.apptists.poplur2025.ui.MapViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.weather.WeatherBadge
import com.apptists.poplur2025.location.LocationProvider
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Image
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    isVisible: Boolean = true,
    mapViewModel: MapViewModel = viewModel()
) {
    MapScreenContent(
        isVisible = isVisible,
        centerOnUserTrigger = mapViewModel.centerOnUserTrigger,
        postCoordinateTrigger = mapViewModel.postCoordinateTrigger
    )
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreenContent(
    isVisible: Boolean = true,
    centerOnUserTrigger: Flow<Unit> = emptyFlow(),
    postCoordinateTrigger: Flow<Point> = emptyFlow()
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    // Start on the full globe, then fly into the user's location.
    val globeView = Point.fromLngLat(-98.0, 18.0)
    val fallbackLocation = Point.fromLngLat(-73.985130, 40.758896)
    var selectedCoordinate by remember { mutableStateOf<Point?>(null) }
    var activeCityName by remember { mutableStateOf("Your Location") }
    var userCityName by remember { mutableStateOf<String?>(null) }
    var userCountryCode by remember { mutableStateOf<String?>(null) }
    var activeRadius by remember { mutableIntStateOf(DEFAULT_RADIUS_KM) }
    var activeWeatherPoint by remember { mutableStateOf<Point?>(fallbackLocation) }
    var activeWeather by remember { mutableStateOf(WeatherBadge.Loading) }
    var activeAiInsight by remember { mutableStateOf<AiTourInsight?>(null) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var userLocation by remember { mutableStateOf<Point?>(null) }
    var idleGeneration by remember { mutableIntStateOf(0) }
    var showLocationSearch by remember { mutableStateOf(false) }
    var locationSearchQuery by remember { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { }
    val mapView = remember { if (isPreview) null else MapView(context) }
    val locationProvider = remember { LocationProvider(context) }
    val popTone = remember { if (isPreview) null else RealPopTonePlayer() }
    val postFeatures = remember {
        DemoPostStore.posts.mapNotNull { post ->
            val lat = post.latitude
            val lng = post.longitude
            if (lat != null && lng != null) Feature.fromGeometry(Point.fromLngLat(lng, lat)) else null
        }
    }
    val landmarkFeatures = remember {
        FeatureCollection.fromFeatures(TourStops.map { Feature.fromGeometry(it.coordinate) })
    }
    var mapReady by remember { mutableStateOf(false) }
    var hasZoomedToLocation by remember { mutableStateOf(false) }
    var zoomSliderValue by remember { mutableFloatStateOf(16.9f) }
    val userTourStop = remember(userLocation, userCityName, userCountryCode) {
        userLocation?.let { location ->
            TourStop(
                cityName = userCityName ?: "Your Location",
                storefront = userCountryCode?.lowercase(java.util.Locale.US) ?: "us",
                coordinate = location,
                bearing = 28.0,
                radiusKm = DEFAULT_RADIUS_KM,
                zoom = 16.9
            )
        }
    }
    val activeMusicStop = remember(activeCityName, isVisible, userTourStop, selectedCoordinate, activeRadius, userCountryCode) {
        if (!isVisible) return@remember null
        val stop = TourStops.find { it.cityName == activeCityName }
        if (stop != null) return@remember stop
        if (activeCityName == "Your Location" || (userCityName != null && activeCityName == userCityName)) return@remember userTourStop
        if (activeCityName == "Selected Location" && selectedCoordinate != null) {
            return@remember TourStop(
                cityName = "Selected Location",
                storefront = userCountryCode?.lowercase(java.util.Locale.US) ?: "us",
                coordinate = selectedCoordinate!!,
                bearing = 28.0,
                radiusKm = activeRadius,
                zoom = 17.6
            )
        }
        null
    }
    var tourSubtextMode by remember { mutableStateOf(TourSubtextMode.NowPlaying) }
    val tourSubtext = remember(activeMusicStop, activeAiInsight, tourSubtextMode, userCityName, activeCityName) {
        activeMusicStop?.let { stop ->
            when (tourSubtextMode) {
                TourSubtextMode.NowPlaying -> nowPlayingSubtext(stop, activeAiInsight)
                TourSubtextMode.Popular -> activeAiInsight?.subtitle ?: popularSubtext(stop)
            }
        } ?: if (activeCityName == "Your Location" || (userCityName != null && activeCityName == userCityName)) {
            "It's popping nearby - street style, food spots, and music clips are moving now in ${userCityName ?: "your area"}."
        } else {
            "Tap the 3D map to choose exactly where a post should live."
        }
    }
    val titlePulse by animateFloatAsState(
        targetValue = if (activeMusicStop != null) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "tourTitlePulse"
    )
    val locationSearchStops = remember(locationSearchQuery, userTourStop) {
        val query = locationSearchQuery.trim().lowercase(Locale.US)
        val stops = buildList {
            userTourStop?.let { add(it) }
            addAll(TourStops)
        }
        if (query.isEmpty()) {
            stops.take(10)
        } else {
            stops.filter { stop ->
                stop.cityName.lowercase(Locale.US).contains(query) ||
                    stop.storefront.lowercase(Locale.US).contains(query)
            }.take(10)
        }
    }
    val openSearchStop: (TourStop) -> Unit = { stop ->
        showLocationSearch = false
        locationSearchQuery = ""
        selectedCoordinate = null
        activeCityName = stop.cityName
        activeRadius = stop.radiusKm
        activeWeatherPoint = stop.coordinate
        val insight = spoofAiTourInsight(stop)
        activeAiInsight = insight
        idleGeneration++
        mapView?.mapboxMap?.flyTo(
            CameraOptions.Builder()
                .center(stop.coordinate)
                .zoom(stop.zoom)
                .pitch(66.0)
                .bearing(stop.bearing)
                .build(),
            mapAnimationOptions { duration(1600) }
        )
        if (mapReady) {
            mapView?.mapboxMap?.getStyle { style ->
                style.setMusicWaveSource(stop.coordinate)
                style.setTrendPopIcon(insight)
            }
        }
    }

    CityTourMusicPlayer(
        tourStop = activeMusicStop,
        isVisible = isVisible,
        aiInsight = activeAiInsight
    )
    RainWeatherSound(
        weather = activeWeather,
        isVisible = isVisible
    )

    DisposableEffect(popTone) {
        onDispose { popTone?.release() }
    }

    LaunchedEffect(activeMusicStop?.cityName, activeAiInsight?.id, isVisible) {
        tourSubtextMode = TourSubtextMode.NowPlaying
        if (!isVisible || activeMusicStop == null) return@LaunchedEffect
        while (true) {
            delay(TOUR_SUBTEXT_SWITCH_MS)
            tourSubtextMode = when (tourSubtextMode) {
                TourSubtextMode.NowPlaying -> TourSubtextMode.Popular
                TourSubtextMode.Popular -> TourSubtextMode.NowPlaying
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible && !hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(mapReady, hasLocationPermission) {
        if (mapReady) {
            mapView?.location?.updateSettings {
                enabled = hasLocationPermission
                pulsingEnabled = true
            }
            mapView?.mapboxMap?.addOnMapClickListener { point ->
                val current = selectedCoordinate
                if (current != null) {
                    // If user taps very close to the current selection or we just want to clear on any tap:
                    // The user said "should be closed when i tap the map again". 
                    // Let's make any tap close it, but if it's a different enough point, maybe select that?
                    // Actually, "tap again" usually implies a toggle or dismiss.
                    // To be safe and satisfy "tap again", we'll check distance.
                    val dist = android.location.Location("").apply {
                        latitude = current.latitude(); longitude = current.longitude()
                    }.distanceTo(android.location.Location("").apply {
                        latitude = point.latitude(); longitude = point.longitude()
                    })
                    
                    if (dist < 50f) { // 50 meters
                        selectedCoordinate = null
                        activeCityName = userCityName ?: "Your Location"
                        activeRadius = DEFAULT_RADIUS_KM
                        activeWeatherPoint = userLocation
                        return@addOnMapClickListener true
                    }
                }
                
                // Select new point
                selectedCoordinate = point
                activeCityName = "Selected Location"
                activeRadius = SELECTED_LOCATION_RADIUS_KM
                activeWeatherPoint = point
                activeAiInsight = null
                idleGeneration++
                mapView?.mapboxMap?.getStyle { style ->
                    style.clearTrendPopSource()
                }
                mapView?.mapboxMap?.flyTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(17.6)
                        .pitch(66.0)
                        .bearing(28.0)
                        .build(),
                    mapAnimationOptions { duration(1600) }
                )
                true
            }
        }
    }

    LaunchedEffect(activeWeatherPoint) {
        val point = activeWeatherPoint
        if (point == null) {
            activeWeather = WeatherBadge("🌎", "World")
            return@LaunchedEffect
        }
        activeWeather = WeatherBadge.Loading
        activeWeather = fetchCurrentWeather(point).getOrElse {
            fallbackWeatherBadge(point)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationProvider.lastLocation { lat, lng ->
                val finalLat = lat ?: fallbackLocation.latitude()
                val finalLng = lng ?: fallbackLocation.longitude()
                userLocation = Point.fromLngLat(finalLng, finalLat)
            }
        } else {
            userLocation = fallbackLocation
        }
    }

    LaunchedEffect(userLocation) {
        val location = userLocation ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude(), location.longitude(), 1)
                val address = addresses?.firstOrNull()
                userCityName = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                userCountryCode = address?.countryCode
            }
        }
    }

    LaunchedEffect(userCityName) {
        if (activeCityName == "Your Location" && userCityName != null) {
            activeCityName = userCityName!!
        }
    }

    LaunchedEffect(mapReady, userLocation) {
        val location = userLocation ?: return@LaunchedEffect
        if (mapReady) {
            mapView?.mapboxMap?.getStyle { style ->
                style.getSourceAs<GeoJsonSource>(CURRENT_PIN_SOURCE_ID)?.geometry(location)
            }
        }
    }

    LaunchedEffect(mapReady, userLocation) {
        val location = userLocation ?: return@LaunchedEffect
        if (mapReady) {
            // Radius circle removed as per request
        }
    }

    LaunchedEffect(mapReady, userLocation, userTourStop) {
        centerOnUserTrigger.collect {
            val location = userLocation ?: return@collect
            selectedCoordinate = null
            activeCityName = userCityName ?: "Your Location"
            activeRadius = DEFAULT_RADIUS_KM
            activeWeatherPoint = location
            val localStop = userTourStop ?: TourStop(
                cityName = userCityName ?: "Your Location",
                storefront = userCountryCode?.lowercase(java.util.Locale.US) ?: "us",
                coordinate = location,
                bearing = 28.0,
                radiusKm = DEFAULT_RADIUS_KM,
                zoom = 16.9
            )
            val localInsight = spoofAiTourInsight(localStop)
            if (mapReady) {
                mapView?.mapboxMap?.flyTo(
                    CameraOptions.Builder()
                        .center(location)
                        .zoom(16.9)
                        .pitch(66.0)
                        .bearing(28.0)
                        .build(),
                    mapAnimationOptions { duration(1600) }
                )
            }
            delay(520L)
            activeAiInsight = localInsight
            repeat(LOCATION_POP_BURSTS) { index ->
                delay(if (index == 0) 180L else LOCATION_POP_BURST_GAP_MS)
                popTone?.play(105 + index * 12)
                confettiTrigger++
            }
        }
    }

    LaunchedEffect(mapReady, activeMusicStop) {
        if (mapReady) {
            mapView?.mapboxMap?.getStyle { style ->
                style.setMusicWaveSource(activeMusicStop?.coordinate)
            }
        }
    }

    LaunchedEffect(mapReady, activeAiInsight, activeMusicStop) {
        if (!mapReady) return@LaunchedEffect
        val insight = activeAiInsight
        if (insight == null) {
            mapView?.mapboxMap?.getStyle { style -> style.clearTrendPopSource() }
            return@LaunchedEffect
        }
        mapView?.mapboxMap?.getStyle { style ->
            style.setTrendPopIcon(insight)
            style.clearTrendPopSource()
        }
    }

    LaunchedEffect(mapReady, activeMusicStop) {
        if (!mapReady || activeMusicStop == null) return@LaunchedEffect
        var frame = 0
        while (true) {
            mapView?.mapboxMap?.getStyle { style ->
                style.setStyleLayerProperty(
                    MUSIC_WAVE_LAYER_ID,
                    "icon-image",
                    Value.valueOf(MUSIC_WAVE_IMAGE_IDS[frame % MUSIC_WAVE_IMAGE_IDS.size])
                )
            }
            frame++
            delay(MUSIC_WAVE_FRAME_MS)
        }
    }

    // Radius pulse removed as per request

    LaunchedEffect(isVisible, mapReady, userLocation, userTourStop) {
        val location = userLocation
        if (isVisible && mapReady && location != null && !hasZoomedToLocation) {
            activeCityName = "Your Location"
            activeRadius = DEFAULT_RADIUS_KM
            activeWeatherPoint = location
            activeAiInsight = null
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(globeView)
                    .zoom(0.45)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build()
            )
            delay(1150)
            mapView?.mapboxMap?.flyTo(
                CameraOptions.Builder()
                    .center(location)
                    .zoom(7.0)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build(),
                mapAnimationOptions { duration(1900) }
            )
            delay(2050)
            mapView?.mapboxMap?.flyTo(
                CameraOptions.Builder()
                    .center(location)
                    .zoom(16.9)
                    .pitch(66.0)
                    .bearing(28.0)
                    .build(),
                mapAnimationOptions { duration(3600) }
            )
            if (userCityName != null) {
                activeCityName = userCityName!!
            }
            val localStop = userTourStop ?: TourStop(
                cityName = userCityName ?: "Your Location",
                storefront = userCountryCode?.lowercase(java.util.Locale.US) ?: "us",
                coordinate = location,
                bearing = 28.0,
                radiusKm = DEFAULT_RADIUS_KM,
                zoom = 16.9
            )
            val localInsight = spoofAiTourInsight(localStop)
            delay(760L)
            activeAiInsight = localInsight
            repeat(LOCATION_POP_BURSTS + 1) { index ->
                delay(if (index == 0) 260L else LOCATION_POP_BURST_GAP_MS)
                popTone?.play(110 + index * 10)
                confettiTrigger++
            }
            hasZoomedToLocation = true
        }
    }



    LaunchedEffect(isVisible, mapReady, idleGeneration) {
        if (!isVisible || !mapReady) return@LaunchedEffect
        delay(IDLE_TOUR_DELAY_MS)
        while (isVisible && mapReady) {
            TourStops.shuffled().take(TOUR_STOPS_PER_CYCLE).forEach { stop ->
                activeCityName = "World"
                activeRadius = EARTH_RADIUS_KM
                activeWeatherPoint = null
                mapView?.mapboxMap?.flyTo(
                    CameraOptions.Builder()
                        .center(globeView)
                        .zoom(0.55)
                        .pitch(0.0)
                        .bearing(0.0)
                        .build(),
                    mapAnimationOptions { duration(2600) }
                )
                delay(3300)
                activeCityName = stop.cityName
                activeRadius = stop.radiusKm
                activeWeatherPoint = stop.coordinate
                val insight = spoofAiTourInsight(stop)
                activeAiInsight = insight
                mapView?.mapboxMap?.flyTo(
                    CameraOptions.Builder()
                        .center(stop.coordinate)
                        .zoom(stop.zoom)
                        .pitch(66.0)
                        .bearing(stop.bearing)
                        .build(),
                    mapAnimationOptions { duration(4200) }
                )
                delay(TREND_POP_START_DELAY_MS)
                mapView?.mapboxMap?.getStyle { style ->
                    style.setTrendPopIcon(insight)
                    style.clearTrendPopSource()
                }
                repeat(3) { index ->
                    popTone?.play(125 + index * 12)
                    confettiTrigger++
                    delay(180L)
                }
                delay((TOUR_STOP_DWELL_MS - TREND_POP_START_DELAY_MS).coerceAtLeast(0L))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!isPreview) {
            AndroidView(
                factory = {
                    MapWeatherTouchContainer(context, mapView!!).apply {
                        setWeather(activeWeather)
                        mapView.apply {
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(globeView)
                                    .zoom(0.45)
                                    .pitch(0.0)
                                    .bearing(0.0)
                                    .build()
                            )
                            mapboxMap.loadStyle(
                                style(Style.STANDARD) {
                                    +atmosphere { }
                                    +projection(ProjectionName.GLOBE)
                                    +geoJsonSource(POST_SOURCE_ID) {
                                        featureCollection(FeatureCollection.fromFeatures(postFeatures))
                                    }
                                    +geoJsonSource(CURRENT_PIN_SOURCE_ID) {
                                        geometry(fallbackLocation)
                                    }
                                    +geoJsonSource(LANDMARK_PIN_SOURCE_ID) {
                                        featureCollection(landmarkFeatures)
                                    }
                                    +geoJsonSource(MUSIC_WAVE_SOURCE_ID) {
                                        featureCollection(emptyFeatureCollection())
                                    }
                                    +geoJsonSource(TREND_POP_SOURCE_ID) {
                                        featureCollection(emptyFeatureCollection())
                                    }
                                    +circleLayer(POST_LAYER_ID, POST_SOURCE_ID) {
                                        circleRadius(18.0)
                                        circleColor("#ff2d8a")
                                        circleOpacity(0.46)
                                        circleStrokeColor("#ffffff")
                                        circleStrokeOpacity(0.9)
                                        circleStrokeWidth(2.5)
                                        circleEmissiveStrength(1.0)
                                    }
                                    +symbolLayer(LANDMARK_PIN_LAYER_ID, LANDMARK_PIN_SOURCE_ID) {
                                        iconImage(PINK_PIN_IMAGE_ID)
                                        iconAnchor(IconAnchor.BOTTOM)
                                        iconAllowOverlap(true)
                                        iconIgnorePlacement(true)
                                        iconSize(0.28)
                                        iconOffset(listOf(0.0, -4.0))
                                    }
                                    +symbolLayer(MUSIC_WAVE_LAYER_ID, MUSIC_WAVE_SOURCE_ID) {
                                        iconImage(MUSIC_WAVE_IMAGE_IDS.first())
                                        iconAnchor(IconAnchor.BOTTOM)
                                        iconAllowOverlap(true)
                                        iconIgnorePlacement(true)
                                        iconSize(0.36)
                                        iconOffset(listOf(31.0, -67.0))
                                    }
                                    +symbolLayer(TREND_POP_LAYER_ID, TREND_POP_SOURCE_ID) {
                                        iconImage(TrendCategory.Clothes.imageId)
                                        iconAnchor(IconAnchor.CENTER)
                                        iconAllowOverlap(true)
                                        iconIgnorePlacement(true)
                                        iconPitchAlignment(IconPitchAlignment.MAP)
                                        iconRotationAlignment(IconRotationAlignment.MAP)
                                        iconSize(0.42)
                                    }
                                    +symbolLayer(CURRENT_PIN_LAYER_ID, CURRENT_PIN_SOURCE_ID) {
                                        iconImage(BLUE_PIN_IMAGE_ID)
                                        iconAnchor(IconAnchor.BOTTOM)
                                        iconAllowOverlap(true)
                                        iconIgnorePlacement(true)
                                        iconSize(0.32)
                                        iconOffset(listOf(0.0, -4.0))
                                    }
                                }
                            ) { style ->
                                style.addStyleImage(BLUE_PIN_IMAGE_ID, 1.0f, createGdxStylePinImage(BLUE_PIN_THEME), false, emptyList(), emptyList(), null)
                                style.addStyleImage(PINK_PIN_IMAGE_ID, 1.0f, createGdxStylePinImage(PINK_PIN_THEME), false, emptyList(), emptyList(), null)
                                MUSIC_WAVE_IMAGE_IDS.forEachIndexed { index, imageId ->
                                    style.addStyleImage(imageId, 1.0f, createGdxMusicWaveImage(index), false, emptyList(), emptyList(), null)
                                }
                                TrendCategory.values().forEach { category ->
                                    style.addStyleImage(category.imageId, 1.0f, createTrendPopImage(category), false, emptyList(), emptyList(), null)
                                }
                                style.setMusicWaveSource(activeMusicStop?.coordinate)
                                style.setTrendPopIcon(activeAiInsight)
                                style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "show3dObjects", Value.valueOf(true))
                                style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showPointOfInterestLabels", Value.valueOf(true))
                                style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showRoadLabels", Value.valueOf(true))
                                style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showPlaceLabels", Value.valueOf(true))
                                style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "lightPreset", Value.valueOf("dusk"))
                                mapReady = true
                            }
                        }
                    }
                },
                update = { container ->
                    container.setWeather(activeWeather)
                    container.setTrendPops(activeMusicStop, activeAiInsight) {
                        popTone?.play(80)
                    }
                    container.burstConfetti(confettiTrigger)
                },
                modifier = Modifier.fillMaxSize().zIndex(0f)
            )
        } else {
            // Placeholder for map in preview
            Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Text("Map Preview Placeholder", color = Color.White)
            }
        }

        Box(Modifier.fillMaxWidth().height(190.dp).zIndex(2f).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent))))
        Column(Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopStart).zIndex(2f)) {
            Text(
                activeCityTitle(activeCityName, activeMusicStop, userCountryCode),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    val pulse = 1f + (titlePulse * 0.018f)
                    scaleX = pulse
                    scaleY = pulse
                }
            )
            TourSubtextLine(
                text = tourSubtext,
                isTouring = activeMusicStop != null
            )
        }
        Column(
            Modifier
                .padding(end = 16.dp)
                .align(Alignment.CenterEnd)
                .zIndex(2f)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(activeWeather.icon, fontSize = 36.sp)
            Text(activeWeather.label, color = Color.White, fontWeight = FontWeight.Bold)
            activeWeather.temperatureLabel?.let { temperature ->
                Text(temperature, color = Color.White.copy(alpha = .78f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        MapZoomSlider(
            zoom = zoomSliderValue,
            onZoomChange = { requestedZoom ->
                zoomSliderValue = requestedZoom
                if (mapReady) {
                    val cameraState = mapView?.mapboxMap?.cameraState
                    cameraState?.let {
                        mapView?.mapboxMap?.setCamera(
                            CameraOptions.Builder()
                                .center(it.center)
                                .zoom(requestedZoom.toDouble())
                                .pitch(it.pitch)
                                .bearing(it.bearing)
                                .build()
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 210.dp, start = 20.dp)
                .zIndex(3f)
        )
        Column(Modifier.padding(bottom = 58.dp, start = 20.dp).align(Alignment.BottomStart).zIndex(2f)) {
            Text("TRENDING", color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            HorizontalDivider(color = Color.White, thickness = 2.dp, modifier = Modifier.width(60.dp).padding(vertical = 6.dp))
            Text(DemoPostStore.posts.size.toString(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 42.sp)
            Text("posts", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        selectedCoordinate?.let { point ->
            Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 158.dp, start = 20.dp, end = 20.dp).zIndex(2f), color = Color.Black.copy(alpha = .72f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Post here", color = Color.White, fontWeight = FontWeight.Black)
                    Text("${"%.5f".format(point.latitude())}, ${"%.5f".format(point.longitude())}", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Button(onClick = { mediaPicker.launch("image/*") }, modifier = Modifier.padding(top = 8.dp)) { Text("Choose media") }
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 56.dp, end = 20.dp)
                .zIndex(4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            MapSearchButton(
                onClick = { showLocationSearch = true }
            )
            RadiusDisplay(radius = activeRadius)
        }

        if (showLocationSearch) {
            LocationSearchDialog(
                query = locationSearchQuery,
                onQueryChange = { locationSearchQuery = it },
                stops = locationSearchStops,
                onDismiss = {
                    showLocationSearch = false
                    locationSearchQuery = ""
                },
                onSelect = openSearchStop
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TourSubtextLine(text: String, isTouring: Boolean) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(animationSpec = tween(360)) togetherWith fadeOut(animationSpec = tween(240))
        },
        label = "tourSubtext"
    ) { line ->
        Text(
            line,
            color = Color.White.copy(alpha = if (isTouring) .94f else .9f),
            fontSize = 16.sp,
            fontWeight = if (isTouring) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 20.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun MapSearchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(54.dp),
        shape = RoundedCornerShape(100.dp),
        color = Color.Transparent,
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search locations",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun LocationSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    stops: List<TourStop>,
    onDismiss: () -> Unit,
    onSelect: (TourStop) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Search locations", fontWeight = FontWeight.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = { Text("City or country") },
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(stops, key = { "${it.cityName}-${it.storefront}" }) { stop ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(stop) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Text(stop.cityName, fontWeight = FontWeight.Bold)
                            Text(
                                "${stop.radiusKm} km radius",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MapZoomSlider(
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableIntStateOf(1) }
    val minZoom = 2.0f
    val maxZoom = 18.5f
    val clampedZoom = zoom.coerceIn(minZoom, maxZoom)
    val fraction = ((clampedZoom - minZoom) / (maxZoom - minZoom)).coerceIn(0f, 1f)

    fun updateFromY(y: Float) {
        val nextFraction = (1f - (y / heightPx.coerceAtLeast(1))).coerceIn(0f, 1f)
        onZoomChange(minZoom + nextFraction * (maxZoom - minZoom))
    }

    Box(
        modifier = modifier
            .width(64.dp)
            .height(350.dp)
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> updateFromY(offset.y) },
                    onDrag = { change, _ ->
                        change.consume()
                        updateFromY(change.position.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.32f), RoundedCornerShape(100.dp))
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = ((1f - fraction) * 314f).dp)
                .size(width = 52.dp, height = 36.dp)
                .background(Color.White, RoundedCornerShape(7.dp))
        )
    }
}

private class MapWeatherTouchContainer(
    context: Context,
    private val mapView: MapView
) : FrameLayout(context) {
    private val weatherView = FullScreenWeatherView(context)
    private val trendPopView = GdxUprightTrendPopView(context, mapView)
    private val confettiView = GdxTrendConfettiView(context)
    private val sparkleView = RadiusSparkleView(context, mapView)
    private var lastConfettiTrigger = 0

    init {
        addView(mapView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(weatherView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(trendPopView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(sparkleView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(confettiView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        trendPopView.isClickable = false
        trendPopView.isFocusable = false
        confettiView.isClickable = false
        confettiView.isFocusable = false
        weatherView.isClickable = false
        weatherView.isFocusable = false
        isClickable = false
    }

    fun setWeather(weather: WeatherBadge) {
        weatherView.setWeather(weather)
    }

    fun setTrendPops(stop: TourStop?, insight: AiTourInsight?, onPop: () -> Unit) {
        trendPopView.setTrend(stop, insight, onPop)
    }

    fun burstConfetti(trigger: Int) {
        if (trigger > 0 && trigger != lastConfettiTrigger) {
            lastConfettiTrigger = trigger
            confettiView.burst()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return mapView.dispatchTouchEvent(event)
    }
}

private class GdxUprightTrendPopView(
    context: Context,
    private val mapView: MapView
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var stop: TourStop? = null
    private var insight: AiTourInsight? = null
    private var objects: List<TrendPopObject> = emptyList()
    private var startMs = 0L
    private var onObjectPop: (() -> Unit)? = null
    private val poppedIndices = mutableSetOf<Int>()
    private val frozenScreenPositions = mutableMapOf<Int, PointF>()

    private val animationTick = object : Runnable {
        override fun run() {
            if (objects.isNotEmpty()) {
                val elapsed = (SystemClock.uptimeMillis() - startMs).coerceAtLeast(0L)
                objects.forEachIndexed { index, item ->
                    if (index !in poppedIndices && elapsed >= item.delayMs) {
                        poppedIndices.add(index)
                        onObjectPop?.invoke()
                    }
                }
                invalidate()
                postOnAnimation(this)
            }
        }
    }

    fun setTrend(nextStop: TourStop?, nextInsight: AiTourInsight?, onPop: () -> Unit) {
        val changed = stop?.cityName != nextStop?.cityName || insight?.id != nextInsight?.id
        stop = nextStop
        insight = nextInsight
        onObjectPop = onPop
        if (nextStop == null || nextInsight == null) {
            objects = emptyList()
            poppedIndices.clear()
            frozenScreenPositions.clear()
            removeCallbacks(animationTick)
            invalidate()
            return
        }
        if (changed) {
            objects = buildTrendPopObjects(nextStop, nextInsight)
            poppedIndices.clear()
            frozenScreenPositions.clear()
            startMs = SystemClock.uptimeMillis()
            removeCallbacks(animationTick)
            postOnAnimation(animationTick)
        }
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val activeInsight = insight ?: return
        val elapsed = (SystemClock.uptimeMillis() - startMs).coerceAtLeast(0L)
        objects.forEachIndexed { index, item ->
            val localElapsed = elapsed - item.delayMs
            if (localElapsed < 0L) return@forEachIndexed
            val frozenPosition = frozenScreenPositions.getOrPut(index) {
                val anchor = stop?.coordinate ?: item.point
                val screen = mapView.mapboxMap.pixelForCoordinate(anchor)
                val angle = index * 2.3999632f + item.seed * 0.017f
                val ring = index % 6
                val spreadX = width * (0.24f + ring * 0.055f)
                val spreadY = height * (0.15f + ring * 0.035f)
                val quadrantBiasX = ((index % 3) - 1) * width * 0.035f
                val biasY = when (index % 4) {
                    0 -> -height * 0.085f
                    1 -> height * 0.045f
                    2 -> -height * 0.025f
                    else -> height * 0.095f
                }
                PointF(
                    (screen.x.toFloat() + cos(angle) * spreadX + quadrantBiasX).coerceIn(76f, width - 76f),
                    (screen.y.toFloat() + sin(angle) * spreadY + biasY).coerceIn(170f, height - 250f)
                )
            }
            val x = frozenPosition.x
            val y = frozenPosition.y
            if (x !in -160f..(width + 160f).toFloat() || y !in -220f..(height + 180f).toFloat()) return@forEachIndexed
            val popPhase = (localElapsed / TREND_3D_POP_MS.toFloat()).coerceIn(0f, 1f)
            val scale = overshootScale(popPhase) * item.scale
            val bob = sin((elapsed + index * 157L) / 520f) * 3f
            drawObjectConfetti(canvas, x, y + bob, localElapsed, item.seed)
            drawUprightEmoji(canvas, item.emoji, x, y + bob, scale)
        }
    }

    private fun drawUprightEmoji(
        canvas: AndroidCanvas,
        emoji: String,
        x: Float,
        y: Float,
        scale: Float
    ) {
        if (scale <= 0.01f) return
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(scale, scale)
        
        // Ground Shadow
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            0f,
            76f,
            70f,
            intArrayOf(AndroidColor.argb(95, 0, 0, 0), AndroidColor.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(-58f, 52f, 58f, 92f), paint)
        paint.shader = null

        // Emoji with clean shadow
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 78f
        paint.color = AndroidColor.WHITE
        
        // Drop shadow for the emoji itself
        paint.setShadowLayer(12f, 0f, 6f, AndroidColor.argb(140, 0, 0, 0))
        canvas.drawText(emoji, 0f, 18f, paint)
        paint.clearShadowLayer()

        canvas.restore()
    }

    private fun drawObjectConfetti(canvas: AndroidCanvas, x: Float, y: Float, elapsed: Long, seed: Int) {
        if (elapsed !in 0 until TREND_3D_CONFETTI_MS) return
        val phase = elapsed / TREND_3D_CONFETTI_MS.toFloat()
        paint.style = Paint.Style.FILL
        repeat(28) { index ->
            val angle = seed * 0.017f + index * 0.92f
            val distance = (42f + (index % 7) * 13f) * phase
            val px = x + cos(angle) * distance
            val py = y - 34f + sin(angle) * distance + phase * phase * 58f
            paint.color = when ((seed + index) % 6) {
                0 -> AndroidColor.argb((230 * (1f - phase)).toInt(), 255, 74, 174)
                1 -> AndroidColor.argb((230 * (1f - phase)).toInt(), 255, 211, 69)
                2 -> AndroidColor.argb((230 * (1f - phase)).toInt(), 88, 235, 255)
                3 -> AndroidColor.argb((230 * (1f - phase)).toInt(), 113, 255, 142)
                4 -> AndroidColor.argb((230 * (1f - phase)).toInt(), 172, 118, 255)
                else -> AndroidColor.argb((230 * (1f - phase)).toInt(), 255, 255, 255)
            }
            canvas.save()
            canvas.rotate(index * 31f + phase * 220f, px, py)
            canvas.drawRoundRect(RectF(px - 4f, py - 7f, px + 4f, py + 7f), 2f, 2f, paint)
            canvas.restore()
        }
    }
}

private class GdxTrendConfettiView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var burstStartMs = 0L
    private val animationTick = object : Runnable {
        override fun run() {
            if (SystemClock.uptimeMillis() - burstStartMs < CONFETTI_BURST_MS) {
                invalidate()
                postOnAnimation(this)
            }
        }
    }

    fun burst() {
        burstStartMs = SystemClock.uptimeMillis()
        removeCallbacks(animationTick)
        postOnAnimation(animationTick)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val elapsed = SystemClock.uptimeMillis() - burstStartMs
        if (elapsed !in 0 until CONFETTI_BURST_MS) return
        val phase = elapsed / CONFETTI_BURST_MS.toFloat()
        val centerX = width * 0.5f
        val centerY = height * 0.42f
        paint.style = Paint.Style.FILL
        repeat(120) { index ->
            val angle = index * 0.73f
            val speed = 110f + (index % 13) * 20f
            val x = centerX + cos(angle) * speed * phase + ((index % 7) - 3) * 18f
            val y = centerY + sin(angle) * speed * phase + phase * phase * 230f
            paint.color = when (index % 6) {
                0 -> AndroidColor.argb((220 * (1f - phase)).toInt(), 255, 45, 138)
                1 -> AndroidColor.argb((220 * (1f - phase)).toInt(), 95, 232, 255)
                2 -> AndroidColor.argb((220 * (1f - phase)).toInt(), 255, 214, 72)
                3 -> AndroidColor.argb((220 * (1f - phase)).toInt(), 128, 255, 128)
                4 -> AndroidColor.argb((220 * (1f - phase)).toInt(), 175, 116, 255)
                else -> AndroidColor.argb((220 * (1f - phase)).toInt(), 255, 255, 255)
            }
            canvas.save()
            canvas.rotate((phase * 260f + index * 17f) % 360f, x, y)
            canvas.drawRoundRect(RectF(x - 5f, y - 9f, x + 5f, y + 9f), 3f, 3f, paint)
            canvas.restore()
        }
    }
}

private class FullScreenWeatherView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var weather: WeatherBadge = WeatherBadge.Loading
    private var startMs = SystemClock.uptimeMillis()
    private val animationTick = object : Runnable {
        override fun run() {
            if (weatherParticleMode(weather.label) != WeatherParticleMode.None) {
                invalidate()
                postOnAnimation(this)
            }
        }
    }

    fun setWeather(nextWeather: WeatherBadge) {
        if (weather.label == nextWeather.label && weather.icon == nextWeather.icon) return
        weather = nextWeather
        startMs = SystemClock.uptimeMillis()
        removeCallbacks(animationTick)
        if (weatherParticleMode(weather.label) != WeatherParticleMode.None && isAttachedToWindow) {
            postOnAnimation(animationTick)
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (weatherParticleMode(weather.label) != WeatherParticleMode.None) {
            postOnAnimation(animationTick)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val mode = weatherParticleMode(weather.label)
        if (mode == WeatherParticleMode.None) return

        val width = width.toFloat().coerceAtLeast(1f)
        val height = height.toFloat().coerceAtLeast(1f)
        val phase = ((SystemClock.uptimeMillis() - startMs) % 3_200L) / 3_200f

        when (mode) {
            WeatherParticleMode.Rain -> drawRain(canvas, width, height, phase, storm = false)
            WeatherParticleMode.Storm -> {
                drawRain(canvas, width, height, phase, storm = true)
                if (phase < 0.08f) {
                    paint.style = Paint.Style.FILL
                    paint.color = AndroidColor.argb((46f * (1f - phase / 0.08f)).toInt(), 255, 255, 255)
                    canvas.drawRect(0f, 0f, width, height, paint)
                }
            }
            WeatherParticleMode.Snow -> drawSnow(canvas, width, height, phase)
            WeatherParticleMode.Cloud -> drawClouds(canvas, width, height, phase)
            WeatherParticleMode.None -> Unit
        }
    }

    private fun drawRain(canvas: AndroidCanvas, width: Float, height: Float, phase: Float, storm: Boolean) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = if (storm) 2.8f else 2.4f
        paint.color = if (storm) AndroidColor.argb(170, 181, 203, 255) else AndroidColor.argb(153, 168, 199, 255)
        val count = if (storm) 140 else 125
        val speed = if (storm) 2.35f else 2.1f
        repeat(count) { index ->
            val lane = ((index * 37) % 100) / 100f
            val y = ((phase * height * speed + index * 47f) % (height + 90f)) - 45f
            val x = lane * width + ((index % 7) - 3) * 5f
            canvas.drawLine(x, y, x - 8f, y + if (storm) 38f else 34f, paint)
        }
    }

    private fun drawSnow(canvas: AndroidCanvas, width: Float, height: Float, phase: Float) {
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.argb(204, 255, 255, 255)
        repeat(95) { index ->
            val lane = ((index * 41) % 100) / 100f
            val y = ((phase * height * 1.05f + index * 39f) % (height + 70f)) - 35f
            val x = lane * width + kotlin.math.sin((phase * 6.28f + index) * 0.7f) * 18f
            canvas.drawCircle(x, y, 2.4f + (index % 3), paint)
        }
    }

    private fun drawClouds(canvas: AndroidCanvas, width: Float, height: Float, phase: Float) {
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            height,
            intArrayOf(
                AndroidColor.argb(128, 214, 224, 238),
                AndroidColor.argb(104, 224, 232, 242),
                AndroidColor.argb(54, 226, 232, 238),
                AndroidColor.TRANSPARENT
            ),
            floatArrayOf(0f, 0.24f, 0.56f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        paint.maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
        repeat(34) { index ->
            val depth = 0.34f + ((index * 17) % 66) / 100f
            val radius = width * (0.10f + ((index * 13) % 38) / 260f) * depth
            val driftSpeed = 0.03f + depth * 0.045f
            val x = ((index * 97f + phase * width * driftSpeed) % (width + radius * 2f)) - radius
            val baseY = height * (0.08f + ((index * 23) % 64) / 100f)
            val y = baseY + sin(phase * 6.28318f + index * 0.41f) * height * 0.018f
            val alpha = (34 + depth * 64).toInt().coerceIn(34, 98)
            paint.shader = RadialGradient(
                x,
                y,
                radius,
                intArrayOf(
                    AndroidColor.argb(alpha, 218, 226, 234),
                    AndroidColor.argb((alpha * 0.58f).toInt(), 198, 207, 216),
                    AndroidColor.TRANSPARENT
                ),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(RectF(x - radius * 1.15f, y - radius * 0.48f, x + radius * 1.15f, y + radius * 0.48f), paint)
        }

        paint.maskFilter = null
        paint.shader = null
        repeat(24) { index ->
            val y = height * (0.16f + (index % 12) * 0.047f)
            val drift = ((phase * width * 0.16f + index * 83f) % (width + 260f)) - 130f
            val alpha = 28 + (index % 5) * 7
            paint.color = AndroidColor.argb(alpha, 235, 241, 247)
            canvas.drawRoundRect(
                drift,
                y,
                drift + width * (0.42f + (index % 3) * 0.12f),
                y + 8f + (index % 4) * 3f,
                18f,
                18f,
                paint
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun CityTourMusicPlayer(
    tourStop: TourStop?,
    isVisible: Boolean,
    aiInsight: AiTourInsight?
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val chartTrackCache = remember { mutableStateMapOf<String, List<CityMusicTrack>>() }
    val radioTrackCache = remember { mutableStateMapOf<String, List<CityMusicTrack>>() }
    val sportsRadioTrackCache = remember { mutableStateMapOf<String, List<CityMusicTrack>>() }
    var fallbackTrackForPlayer by remember { mutableStateOf<CityMusicTrack?>(null) }
    val player = remember {
        if (isPreview) null else {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 1f
                }
        }
    }

    LaunchedEffect(tourStop?.cityName, isVisible, aiInsight?.id) {
        if (!isVisible || tourStop == null || player == null) {
            player?.pause()
            player?.clearMediaItems()
            return@LaunchedEffect
        }

        val fallbackTrack = fallbackMusicTrackFor(tourStop)
        fallbackTrackForPlayer = fallbackTrack
        player.playCityMusicTrack(fallbackTrack)

        val resolvedTrack = run {
            val tracks = chartTrackCache[tourStop.cityName]
                ?: resolveTopCountryMusicTracks(tourStop).getOrElse {
                    listOf(FallbackCityMusicTracks[tourStop.cityName] ?: DefaultFallbackCityMusicTrack)
                }.also { chartTrackCache[tourStop.cityName] = it }
            val radioTracks = radioTrackCache[tourStop.cityName]
                ?: resolveTopCityRadioTracks(tourStop).getOrElse {
                    FallbackCityRadioTracks[tourStop.cityName] ?: emptyList()
                }.also { radioTrackCache[tourStop.cityName] = it }
            val sportsRadioTracks = sportsRadioTrackCache[tourStop.cityName]
                ?: resolveTopCitySportsRadioTracks(tourStop).getOrElse {
                    FallbackCitySportsRadioTracks[tourStop.cityName] ?: emptyList()
                }.also { sportsRadioTrackCache[tourStop.cityName] = it }

            when {
                aiInsight?.category == TrendCategory.Sports && sportsRadioTracks.isNotEmpty() -> sportsRadioTracks.random()
                else -> tracks.random()
            }
        }

        if (resolvedTrack.assetUri != fallbackTrack.assetUri) {
            player.playCityMusicTrack(resolvedTrack)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val fallbackTrack = fallbackTrackForPlayer ?: return
                player?.playCityMusicTrack(fallbackTrack)
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            player?.release()
        }
    }
}

private fun ExoPlayer.playCityMusicTrack(track: CityMusicTrack) {
    setMediaItem(MediaItem.fromUri(track.assetUri))
    prepare()
    playWhenReady = true
    play()
}

private fun fallbackMusicTrackFor(stop: TourStop): CityMusicTrack =
    FallbackCityMusicTracks[stop.cityName] ?: DefaultFallbackCityMusicTrack

private const val POST_SOURCE_ID = "poplur-posts"
private const val POST_LAYER_ID = "poplur-post-markers"
private const val CURRENT_PIN_SOURCE_ID = "poplur-current-location-pin-source"
private const val CURRENT_PIN_LAYER_ID = "poplur-current-location-pin-layer"
private const val RADIUS_CIRCLE_SOURCE_ID = "poplur-radius-circle-source"
private const val RADIUS_CIRCLE_FILL_LAYER_ID = "poplur-radius-circle-fill-layer"
private const val RADIUS_CIRCLE_LINE_LAYER_ID = "poplur-radius-circle-line-layer"
private const val LANDMARK_PIN_SOURCE_ID = "poplur-landmark-pin-source"
private const val LANDMARK_PIN_LAYER_ID = "poplur-landmark-pin-layer"
private const val MUSIC_WAVE_SOURCE_ID = "poplur-music-wave-source"
private const val MUSIC_WAVE_LAYER_ID = "poplur-music-wave-layer"
private const val TREND_POP_SOURCE_ID = "poplur-ai-trend-pop-source"
private const val TREND_POP_LAYER_ID = "poplur-ai-trend-pop-layer"
private const val WEATHER_RAIN_SOURCE_ID = "poplur-weather-rain-source"
private const val WEATHER_RAIN_LAYER_ID = "poplur-weather-rain-layer"
private const val WEATHER_SNOW_SOURCE_ID = "poplur-weather-snow-source"
private const val WEATHER_SNOW_LAYER_ID = "poplur-weather-snow-layer"
private const val WEATHER_CLOUD_SOURCE_ID = "poplur-weather-cloud-source"
private const val WEATHER_CLOUD_LAYER_ID = "poplur-weather-cloud-layer"
private const val BLUE_PIN_IMAGE_ID = "poplur-gdx-blue-location-pin"
private const val PINK_PIN_IMAGE_ID = "poplur-gdx-pink-landmark-pin"
private const val MUSIC_WAVE_IMAGE_ID_PREFIX = "poplur-gdx-music-wave"
private const val MAPBOX_BASEMAP_IMPORT_ID = "basemap"
private const val IDLE_TOUR_DELAY_MS = 30_000L
private const val TOUR_STOP_DWELL_MS = 30_000L
private const val TOUR_STOPS_PER_CYCLE = 12
private const val MUSIC_WAVE_FRAME_MS = 180L
private const val WEATHER_PARTICLE_FRAME_MS = 140L
private const val DEFAULT_RADIUS_KM = 5_000
private const val SELECTED_LOCATION_RADIUS_KM = 850
private const val EARTH_RADIUS_KM = 6_371
private const val WEATHER_TIMEOUT_MS = 5_000
private const val MUSIC_LOOKUP_TIMEOUT_MS = 5_000
private const val MUSIC_TOP_TRACK_CHOICES = 5
private const val TREND_POP_START_DELAY_MS = 1_350L
private const val TOUR_SUBTEXT_SWITCH_MS = 6_000L
private const val TREND_POP_ANIMATION_FRAME_MS = 360L
private const val TREND_3D_POP_MS = 620L
private const val TREND_3D_SEQUENCE_GAP_MS = 150L
private const val TREND_3D_CONFETTI_MS = 1_250L
private const val CONFETTI_BURST_MS = 1_850L
private const val LOCATION_POP_BURSTS = 4
private const val LOCATION_POP_BURST_GAP_MS = 420L
private const val RADIUS_CIRCLE_SEGMENTS = 128
private const val TREND_POP_COUNT_MIN = 18
private const val TREND_POP_COUNT_RANGE = 8
private val MUSIC_WAVE_IMAGE_IDS = List(4) { index -> "$MUSIC_WAVE_IMAGE_ID_PREFIX-$index" }

private data class TourStop(
    val cityName: String,
    val storefront: String,
    val coordinate: Point,
    val bearing: Double,
    val radiusKm: Int,
    val zoom: Double = 16.2
)

private enum class TourSubtextMode {
    NowPlaying,
    Popular
}

private fun activeCityTitle(cityName: String, stop: TourStop?, userCountryCode: String?): String {
    val code = stop?.storefront ?: (if (cityName != "World" && cityName != "Selected Location") userCountryCode else null)
    return code?.let { "$cityName ${countryFlagEmoji(it)}" } ?: cityName
}

private fun nowPlayingSubtext(stop: TourStop, insight: AiTourInsight?): String {
    val track = when {
        insight?.category == TrendCategory.Sports -> FallbackCitySportsRadioTracks[stop.cityName]?.firstOrNull()
        else -> FallbackCityRadioTracks[stop.cityName]?.firstOrNull()
            ?: FallbackCityMusicTracks[stop.cityName]
            ?: DefaultFallbackCityMusicTrack
    } ?: DefaultFallbackCityMusicTrack
    return "Now playing - ${track.artistSeed}"
}

private fun popularSubtext(stop: TourStop): String {
    return FallbackCityPopularSubtexts[stop.cityName]
        ?: "Popular here - music clips, food spots, street style, and landmark posts are moving now."
}

@Composable
private fun RainWeatherSound(
    weather: WeatherBadge,
    isVisible: Boolean
) {
    val isPreview = LocalInspectionMode.current
    val rainPlayer = remember { if (isPreview) null else ProceduralRainPlayer() }

    LaunchedEffect(weather.label, isVisible) {
        if (isVisible && isRainWeatherLabel(weather.label)) {
            rainPlayer?.play()
        } else {
            rainPlayer?.pause()
        }
    }

    DisposableEffect(rainPlayer) {
        onDispose {
            rainPlayer?.release()
        }
    }
}

private fun isRainWeatherLabel(label: String): Boolean =
    label.contains("Rain", ignoreCase = true) ||
        label.contains("Drizzle", ignoreCase = true) ||
        label.contains("Storm", ignoreCase = true)

private fun countryFlagEmoji(countryCode: String): String {
    val normalized = countryCode.uppercase(Locale.US)
    if (normalized.length != 2 || normalized.any { it !in 'A'..'Z' }) return ""
    return normalized.map { code ->
        String(Character.toChars(0x1F1E6 + (code.code - 'A'.code)))
    }.joinToString("")
}

private val TourStops = listOf(
    TourStop("Paris", "fr", Point.fromLngLat(2.2945, 48.8584), 28.0, 105),
    TourStop("New York", "us", Point.fromLngLat(-73.9857, 40.7484), -24.0, 132),
    TourStop("Los Angeles", "us", Point.fromLngLat(-118.3267, 34.1016), 22.0, 150),
    TourStop("Chicago", "us", Point.fromLngLat(-87.6233, 41.8819), -18.0, 120),
    TourStop("Miami", "us", Point.fromLngLat(-80.1918, 25.7617), 36.0, 95),
    TourStop("Las Vegas", "us", Point.fromLngLat(-115.1728, 36.1147), -34.0, 115),
    TourStop("New Orleans", "us", Point.fromLngLat(-90.0659, 29.9584), 30.0, 90),
    TourStop("San Francisco", "us", Point.fromLngLat(-122.4783, 37.8199), 18.0, 110),
    TourStop("Seattle", "us", Point.fromLngLat(-122.3493, 47.6205), -28.0, 105),
    TourStop("Washington DC", "us", Point.fromLngLat(-77.0365, 38.8977), 20.0, 80),
    TourStop("Toronto", "ca", Point.fromLngLat(-79.3871, 43.6426), 24.0, 84),
    TourStop("Vancouver", "ca", Point.fromLngLat(-123.1119, 49.2827), -22.0, 95),
    TourStop("Montreal", "ca", Point.fromLngLat(-73.5673, 45.5017), 30.0, 88),
    TourStop("Calgary", "ca", Point.fromLngLat(-114.0719, 51.0447), -18.0, 100),
    TourStop("Mexico City", "mx", Point.fromLngLat(-99.1332, 19.4326), 26.0, 130),
    TourStop("Rio de Janeiro", "br", Point.fromLngLat(-43.2105, -22.9519), -24.0, 130),
    TourStop("Sao Paulo", "br", Point.fromLngLat(-46.6333, -23.5505), 34.0, 150),
    TourStop("Tokyo", "jp", Point.fromLngLat(139.7454, 35.6586), 34.0, 148),
    TourStop("Seoul", "kr", Point.fromLngLat(126.9780, 37.5665), -20.0, 105),
    TourStop("Singapore", "sg", Point.fromLngLat(103.8518, 1.2834), 22.0, 70),
    TourStop("Bangkok", "th", Point.fromLngLat(100.5018, 13.7563), -30.0, 100),
    TourStop("Jakarta", "id", Point.fromLngLat(106.8272, -6.1754), 28.0, 135),
    TourStop("Manila", "ph", Point.fromLngLat(120.9842, 14.5995), -26.0, 110),
    TourStop("London", "gb", Point.fromLngLat(-0.1246, 51.5007), -32.0, 118),
    TourStop("Dubai", "ae", Point.fromLngLat(55.2744, 25.1972), 18.0, 96),
    TourStop("Doha", "qa", Point.fromLngLat(51.5310, 25.2854), -24.0, 80),
    TourStop("Riyadh", "sa", Point.fromLngLat(46.6753, 24.7136), 30.0, 115),
    TourStop("Istanbul", "tr", Point.fromLngLat(28.9784, 41.0082), -28.0, 125),
    TourStop("Petra", "jo", Point.fromLngLat(35.4444, 30.3285), 20.0, 65, zoom = 15.7),
    TourStop("Mumbai", "in", Point.fromLngLat(72.8777, 19.0760), 26.0, 145),
    TourStop("Delhi", "in", Point.fromLngLat(77.2295, 28.6129), -24.0, 155),
    TourStop("Jaipur", "in", Point.fromLngLat(75.8235, 26.9239), 34.0, 95),
    TourStop("Bengaluru", "in", Point.fromLngLat(77.5946, 12.9716), -18.0, 120),
    TourStop("Cairo", "eg", Point.fromLngLat(31.1342, 29.9792), 26.0, 120, zoom = 15.7),
    TourStop("Lagos", "ng", Point.fromLngLat(3.3792, 6.5244), -20.0, 165),
    TourStop("Nairobi", "ke", Point.fromLngLat(36.8219, -1.2921), 30.0, 115),
    TourStop("Cape Town", "za", Point.fromLngLat(18.4241, -33.9249), -26.0, 105),
    TourStop("Marrakech", "ma", Point.fromLngLat(-7.9811, 31.6295), 22.0, 85),
    TourStop("Accra", "gh", Point.fromLngLat(-0.1869, 5.6037), -28.0, 95),
    TourStop("Sydney", "au", Point.fromLngLat(151.2153, -33.8568), -28.0, 110)
)

private data class CityMusicTrack(
    val assetUri: String,
    val artistSeed: String
)

private data class AiTourInsight(
    val id: String,
    val subtitle: String,
    val category: TrendCategory
)

private data class TrendPopObject(
    val point: Point,
    val emoji: String,
    val scale: Float,
    val delayMs: Long,
    val seed: Int
)

private enum class TrendCategory(val imageId: String) {
    Clothes("poplur-ai-trend-clothes"),
    Sports("poplur-ai-trend-sports"),
    Food("poplur-ai-trend-food"),
    Nightlife("poplur-ai-trend-nightlife"),
    Music("poplur-ai-trend-music"),
    Travel("poplur-ai-trend-travel")
}

private enum class WeatherParticleMode {
    None,
    Rain,
    Snow,
    Cloud,
    Storm
}

private val FallbackCityMusicTracks = mapOf(
    "Paris" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "Aya Nakamura, Daft Punk, DJ Snake"),
    "New York" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "Jay-Z, Cardi B, A\$AP Rocky"),
    "Tokyo" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "YOASOBI, Hikaru Utada, Ryuichi Sakamoto"),
    "London" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "Dua Lipa, Stormzy, Adele"),
    "Dubai" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "Balqees, Hussain Al Jassmi, Wegz"),
    "Toronto" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "Drake, The Weeknd, PARTYNEXTDOOR"),
    "Sydney" to CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "The Kid LAROI, Sia, Flume")
)

private val DefaultFallbackCityMusicTrack =
    CityMusicTrack("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "Global pop and rap mix")

private val FallbackCityPopularSubtexts = mapOf(
    "Paris" to "Popular here - Eiffel Tower shots, fashion reels, cafes, and night walks are spiking.",
    "New York" to "Popular here - street style, skyline clips, slice shops, and courtside chatter are moving.",
    "Los Angeles" to "Popular here - studio lots, fit checks, beach drives, and late-night food spots are hot.",
    "Chicago" to "Popular here - architecture reels, deep dish stops, live sets, and sports talk are surging.",
    "Miami" to "Popular here - beach clips, Latin nightlife, rooftop food, and art district posts are climbing.",
    "Las Vegas" to "Popular here - casino walk-throughs, show clips, neon views, and club lines are trending.",
    "Toronto" to "Popular here - CN Tower views, Drake mentions, food halls, and Raptors chatter are rising.",
    "London" to "Popular here - pub scenes, streetwear, football talk, and West End clips are leading.",
    "Tokyo" to "Popular here - Shibuya crossings, anime shops, ramen stops, and late trains are trending.",
    "Sydney" to "Popular here - harbour views, surf clips, live radio, and brunch spots are taking off.",
    "Dubai" to "Popular here - skyline views, luxury malls, desert drives, and night markets are glowing.",
    "Rio de Janeiro" to "Popular here - beach football, samba clips, mountain views, and sunset posts are up.",
    "Mexico City" to "Popular here - taco runs, museum clips, street markets, and park walks are everywhere.",
    "Singapore" to "Popular here - hawker stalls, skyline reels, garden walks, and night food are buzzing."
)

private val FallbackCityRadioTracks = mapOf(
    "New York" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/WNYCFM.mp3", "WNYC live radio"),
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/WQXRFM.mp3", "WQXR live radio")
    ),
    "Los Angeles" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/KUSCMP256.mp3", "KUSC live radio")
    ),
    "Chicago" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/WBEZFM.mp3", "WBEZ live radio")
    ),
    "London" to listOf(
        CityMusicTrack("https://stream.live.vc.bbcmedia.co.uk/bbc_radio_fourfm", "BBC Radio 4 live"),
        CityMusicTrack("https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one", "BBC Radio 1 live")
    ),
    "Paris" to listOf(
        CityMusicTrack("https://icecast.radiofrance.fr/fip-midfi.mp3", "FIP live radio")
    ),
    "Toronto" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/CBC_R1_TOR_H.mp3", "CBC Radio One Toronto")
    ),
    "Sydney" to listOf(
        CityMusicTrack("https://live-radio01.mediahubaustralia.com/2TJW/mp3/", "triple j Sydney live")
    )
)

private val FallbackCitySportsRadioTracks = mapOf(
    "New York" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/WFANAM.mp3", "WFAN Sports Radio New York")
    ),
    "Los Angeles" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/KSPNAMAAC.aac", "ESPN LA live sports radio")
    ),
    "Chicago" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/WSCRAM.mp3", "670 The Score Chicago")
    ),
    "Toronto" to listOf(
        CityMusicTrack("https://playerservices.streamtheworld.com/api/livestream-redirect/CHUMAM.mp3", "TSN 1050 Toronto")
    ),
    "London" to listOf(
        CityMusicTrack("https://radio.talksport.com/stream", "talkSPORT London")
    )
)

private fun spoofAiTourInsight(stop: TourStop): AiTourInsight {
    val sportsCities = setOf("New York", "Los Angeles", "Chicago", "Toronto", "London", "Tokyo", "Sydney", "Miami")
    val options = listOf(
        AiTourInsight("${stop.cityName}-clothes-${SystemClock.uptimeMillis()}", "AI: most posts nearby are fit checks, sneakers, and streetwear drops.", TrendCategory.Clothes),
        AiTourInsight("${stop.cityName}-food-${SystemClock.uptimeMillis()}", "AI: food posts are spiking here: late plates, cafes, and table shots.", TrendCategory.Food),
        AiTourInsight("${stop.cityName}-nightlife-${SystemClock.uptimeMillis()}", "AI: nightlife is trending now: clubs, rooftops, and after-dark clips.", TrendCategory.Nightlife),
        AiTourInsight("${stop.cityName}-music-${SystemClock.uptimeMillis()}", "AI: music posts are leading here: concerts, street performers, and studio clips.", TrendCategory.Music),
        AiTourInsight("${stop.cityName}-travel-${SystemClock.uptimeMillis()}", "AI: travel posts are highest here: landmarks, views, and arrival moments.", TrendCategory.Travel)
    )
    val shouldSimulateGame = stop.cityName in sportsCities && abs((stop.cityName.hashCode() + SystemClock.uptimeMillis()).toInt()) % 3 == 0
    if (shouldSimulateGame) {
        return AiTourInsight(
            id = "${stop.cityName}-sports-${SystemClock.uptimeMillis()}",
            subtitle = "AI: sports posts are surging here. Tuning into a live local sports radio station.",
            category = TrendCategory.Sports
        )
    }
    return options.random()
}


private data class PinTheme(
    val base: Int,
    val mid: Int,
    val highlight: Int,
    val glow: Int
)

private val BLUE_PIN_THEME = PinTheme(
    base = AndroidColor.rgb(38, 128, 255),
    mid = AndroidColor.rgb(96, 231, 255),
    highlight = AndroidColor.WHITE,
    glow = AndroidColor.argb(110, 78, 220, 255)
)

private val PINK_PIN_THEME = PinTheme(
    base = AndroidColor.rgb(186, 44, 255),
    mid = AndroidColor.rgb(255, 74, 221),
    highlight = AndroidColor.WHITE,
    glow = AndroidColor.argb(118, 255, 70, 216)
)

private fun createGdxStylePinImage(theme: PinTheme): Image {
    val bitmap = createGdxStylePinBitmap(theme)
    return bitmap.toMapboxImage()
}

private fun createGdxMusicWaveImage(frame: Int): Image {
    val size = 172
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    val activeArc = frame % 3
    val arcs = listOf(
        WaveArc(RectF(8f, 48f, 78f, 124f), 12f, AndroidColor.argb(if (activeArc == 0) 255 else 150, 255, 255, 255)),
        WaveArc(RectF(-8f, 27f, 122f, 145f), 14f, AndroidColor.argb(if (activeArc == 1) 255 else 162, 118, 235, 255)),
        WaveArc(RectF(-24f, 6f, 168f, 166f), 16f, AndroidColor.argb(if (activeArc == 2) 255 else 170, 255, 68, 214))
    )

    arcs.forEachIndexed { index, arc ->
        paint.strokeWidth = arc.strokeWidth + if (index == activeArc) 2.5f else 0f
        paint.color = arc.color
        canvas.drawArc(arc.bounds, -54f, 108f, false, paint)
    }

    return bitmap.toMapboxImage()
}

private fun createTrendPopImage(category: TrendCategory): Image {
    val size = 144
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val accent = when (category) {
        TrendCategory.Clothes -> AndroidColor.rgb(255, 72, 180)
        TrendCategory.Sports -> AndroidColor.rgb(96, 234, 122)
        TrendCategory.Food -> AndroidColor.rgb(255, 190, 72)
        TrendCategory.Nightlife -> AndroidColor.rgb(156, 105, 255)
        TrendCategory.Music -> AndroidColor.rgb(82, 224, 255)
        TrendCategory.Travel -> AndroidColor.rgb(255, 100, 88)
    }
    val dark = AndroidColor.argb(245, 30, 22, 52)

    paint.style = Paint.Style.FILL
    paint.color = AndroidColor.argb(76, 0, 0, 0)
    canvas.drawOval(RectF(22f, 102f, 122f, 132f), paint)

    paint.shader = null
    paint.color = AndroidColor.argb(210, 24, 16, 46)
    canvas.save()
    canvas.translate(10f, 12f)
    drawTrendShape(canvas, paint, category)
    canvas.restore()
    paint.color = AndroidColor.argb(155, 72, 40, 116)
    canvas.save()
    canvas.translate(5f, 6f)
    drawTrendShape(canvas, paint, category)
    canvas.restore()

    paint.shader = LinearGradient(
        36f,
        22f,
        112f,
        112f,
        intArrayOf(AndroidColor.WHITE, accent, dark),
        floatArrayOf(0f, 0.42f, 1f),
        Shader.TileMode.CLAMP
    )

    drawTrendShape(canvas, paint, category)

    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = AndroidColor.argb(220, 255, 255, 255)
    when (category) {
        TrendCategory.Clothes -> canvas.drawPath(trendShirtPath(), paint)
        TrendCategory.Sports -> canvas.drawCircle(72f, 64f, 34f, paint)
        TrendCategory.Food -> canvas.drawRoundRect(RectF(38f, 54f, 106f, 96f), 18f, 18f, paint)
        TrendCategory.Nightlife -> canvas.drawPath(trendGlassPath(), paint)
        TrendCategory.Music -> canvas.drawPath(trendMusicPath(), paint)
        TrendCategory.Travel -> canvas.drawPath(trendPlanePath(), paint)
    }

    return bitmap.toMapboxImage()
}

private fun drawTrendShape(canvas: AndroidCanvas, paint: Paint, category: TrendCategory) {
    paint.style = Paint.Style.FILL
    when (category) {
        TrendCategory.Clothes -> drawTrendShirt(canvas, paint)
        TrendCategory.Sports -> drawTrendBall(canvas, paint)
        TrendCategory.Food -> drawTrendFood(canvas, paint)
        TrendCategory.Nightlife -> drawTrendNightlife(canvas, paint)
        TrendCategory.Music -> drawTrendMusic(canvas, paint)
        TrendCategory.Travel -> drawTrendTravel(canvas, paint)
    }
}

private fun drawTrendShirt(canvas: AndroidCanvas, paint: Paint) {
    canvas.drawPath(trendShirtPath(), paint)
}

private fun trendShirtPath(): Path = Path().apply {
    moveTo(42f, 38f)
    lineTo(57f, 24f)
    lineTo(72f, 34f)
    lineTo(87f, 24f)
    lineTo(104f, 39f)
    lineTo(118f, 60f)
    lineTo(100f, 72f)
    lineTo(94f, 116f)
    lineTo(50f, 116f)
    lineTo(44f, 72f)
    lineTo(26f, 60f)
    close()
}

private fun drawTrendBall(canvas: AndroidCanvas, paint: Paint) {
    canvas.drawCircle(72f, 64f, 34f, paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = AndroidColor.argb(190, 30, 22, 52)
    canvas.drawLine(43f, 47f, 101f, 82f, paint)
    canvas.drawArc(RectF(36f, 31f, 108f, 98f), -70f, 140f, false, paint)
    canvas.drawArc(RectF(42f, 26f, 112f, 102f), 110f, 130f, false, paint)
    paint.style = Paint.Style.FILL
}

private fun drawTrendFood(canvas: AndroidCanvas, paint: Paint) {
    canvas.drawRoundRect(RectF(38f, 54f, 106f, 96f), 18f, 18f, paint)
    canvas.drawOval(RectF(30f, 42f, 114f, 66f), paint)
    canvas.drawRect(52f, 96f, 92f, 112f, paint)
}

private fun drawTrendNightlife(canvas: AndroidCanvas, paint: Paint) {
    canvas.drawPath(trendGlassPath(), paint)
}

private fun trendGlassPath(): Path = Path().apply {
    moveTo(42f, 30f)
    lineTo(104f, 30f)
    lineTo(91f, 74f)
    lineTo(78f, 86f)
    lineTo(78f, 112f)
    lineTo(102f, 118f)
    lineTo(44f, 118f)
    lineTo(68f, 112f)
    lineTo(68f, 86f)
    lineTo(55f, 74f)
    close()
}

private fun drawTrendMusic(canvas: AndroidCanvas, paint: Paint) {
    canvas.drawPath(trendMusicPath(), paint)
}

private fun trendMusicPath(): Path = Path().apply {
    moveTo(58f, 28f)
    lineTo(98f, 20f)
    lineTo(98f, 86f)
    lineTo(88f, 86f)
    lineTo(88f, 42f)
    lineTo(66f, 46f)
    lineTo(66f, 94f)
    lineTo(56f, 94f)
    close()
    addOval(RectF(38f, 88f, 66f, 116f), Path.Direction.CW)
    addOval(RectF(78f, 80f, 106f, 108f), Path.Direction.CW)
}

private fun drawTrendTravel(canvas: AndroidCanvas, paint: Paint) {
    canvas.drawPath(trendPlanePath(), paint)
}

private fun trendPlanePath(): Path = Path().apply {
    moveTo(24f, 68f)
    lineTo(120f, 30f)
    lineTo(92f, 76f)
    lineTo(108f, 108f)
    lineTo(78f, 90f)
    lineTo(52f, 118f)
    lineTo(56f, 80f)
    close()
}

private fun createGdxStylePinBitmap(theme: PinTheme): Bitmap {
    val size = 168
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.shader = RadialGradient(
        size / 2f,
        size * 0.58f,
        size * 0.42f,
        intArrayOf(theme.glow, AndroidColor.TRANSPARENT),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(size / 2f, size * 0.58f, size * 0.42f, paint)

    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 10f
    paint.color = theme.glow
    canvas.drawCircle(size / 2f, size * 0.58f, 44f, paint)
    paint.strokeWidth = 5f
    paint.alpha = 150
    canvas.drawCircle(size / 2f, size * 0.58f, 65f, paint)
    paint.alpha = 255

    paint.style = Paint.Style.FILL
    val spire = Path().apply {
        moveTo(size / 2f, 10f)
        lineTo(size * 0.68f, 61f)
        lineTo(size * 0.56f, 128f)
        lineTo(size / 2f, 155f)
        lineTo(size * 0.44f, 128f)
        lineTo(size * 0.32f, 61f)
        close()
    }
    paint.shader = LinearGradient(
        size * 0.32f,
        18f,
        size * 0.68f,
        146f,
        intArrayOf(theme.highlight, theme.mid, theme.base),
        floatArrayOf(0f, 0.46f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(spire, paint)

    paint.shader = LinearGradient(
        size * 0.34f,
        22f,
        size * 0.54f,
        128f,
        intArrayOf(AndroidColor.argb(210, 255, 255, 255), AndroidColor.argb(40, 255, 255, 255)),
        null,
        Shader.TileMode.CLAMP
    )
    val leftFacet = Path().apply {
        moveTo(size / 2f, 12f)
        lineTo(size * 0.44f, 129f)
        lineTo(size * 0.33f, 61f)
        close()
    }
    canvas.drawPath(leftFacet, paint)

    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    paint.color = AndroidColor.argb(210, 255, 255, 255)
    canvas.drawPath(spire, paint)

    return bitmap
}

private data class WaveArc(
    val bounds: RectF,
    val strokeWidth: Float,
    val color: Int
)

private fun Bitmap.toMapboxImage(): Image {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    val buffer = ByteBuffer.allocateDirect(pixels.size * 4)
    pixels.forEach { pixel ->
        buffer.put(AndroidColor.red(pixel).toByte())
        buffer.put(AndroidColor.green(pixel).toByte())
        buffer.put(AndroidColor.blue(pixel).toByte())
        buffer.put(AndroidColor.alpha(pixel).toByte())
    }
    buffer.flip()
    return Image(width, height, DataRef(buffer))
}

private fun emptyFeatureCollection(): FeatureCollection {
    return FeatureCollection.fromFeatures(emptyList<Feature>())
}

private fun Style.setMusicWaveSource(point: Point?) {
    val features = point?.let { listOf(Feature.fromGeometry(it)) } ?: emptyList()
    getSourceAs<GeoJsonSource>(MUSIC_WAVE_SOURCE_ID)?.featureCollection(
        FeatureCollection.fromFeatures(features)
    )
}

private fun Style.setTrendPopIcon(insight: AiTourInsight?) {
    setStyleLayerProperty(
        TREND_POP_LAYER_ID,
        "icon-image",
        Value.valueOf(insight?.category?.imageId ?: TrendCategory.Clothes.imageId)
    )
}

private fun Style.setTrendPopSource(stop: TourStop, insight: AiTourInsight, frame: Int) {
    getSourceAs<GeoJsonSource>(TREND_POP_SOURCE_ID)?.featureCollection(
        trendPopFeatureCollection(stop, insight, frame)
    )
}

private fun Style.clearTrendPopSource() {
    getSourceAs<GeoJsonSource>(TREND_POP_SOURCE_ID)?.featureCollection(emptyFeatureCollection())
}

private fun Style.setRadiusCircleSource(center: Point, radiusKm: Int) {
    getSourceAs<GeoJsonSource>(RADIUS_CIRCLE_SOURCE_ID)?.geometry(
        radiusCirclePolygon(center, radiusKm)
    )
}

private fun radiusCirclePolygon(center: Point, radiusKm: Int): Polygon {
    val angularDistance = radiusKm.coerceAtLeast(1) / EARTH_RADIUS_KM.toDouble()
    val centerLat = Math.toRadians(center.latitude())
    val centerLng = Math.toRadians(center.longitude())
    val ring = (0..RADIUS_CIRCLE_SEGMENTS).map { index ->
        val bearing = 2.0 * Math.PI * index / RADIUS_CIRCLE_SEGMENTS
        val lat = asin(
            sin(centerLat) * cos(angularDistance) +
                cos(centerLat) * sin(angularDistance) * cos(bearing)
        )
        val lng = centerLng + atan2(
            sin(bearing) * sin(angularDistance) * cos(centerLat),
            cos(angularDistance) - sin(centerLat) * sin(lat)
        )
        Point.fromLngLat(Math.toDegrees(lng), Math.toDegrees(lat))
    }
    return Polygon.fromLngLats(listOf(ring))
}

private fun trendPopFeatureCollection(stop: TourStop, insight: AiTourInsight, frame: Int): FeatureCollection {
    val seed = abs((stop.cityName.hashCode() xor insight.category.name.hashCode()))
    val count = TREND_POP_COUNT_MIN + seed % TREND_POP_COUNT_RANGE
    val lngScale = 0.0074
    val latScale = 0.0058
    val features = (0 until count).map { index ->
        val angle = 2.0 * Math.PI * index / count + (seed % 360) * Math.PI / 180.0
        val radius = 0.56 + ((index * 29 + seed) % 100) / 120.0
        val wobble = sin(index * 1.7 + seed + frame * 0.38) * 0.26
        val pop = 1.0 + sin(frame * 0.72 + index) * 0.12
        Feature.fromGeometry(
            Point.fromLngLat(
                stop.coordinate.longitude() + cos(angle + wobble) * lngScale * radius * pop,
                stop.coordinate.latitude() + sin(angle + wobble * 0.35) * latScale * radius * pop
            )
        )
    }
    return FeatureCollection.fromFeatures(features)
}

private fun buildTrendPopObjects(stop: TourStop, insight: AiTourInsight): List<TrendPopObject> {
    val seed = abs(stop.cityName.hashCode() xor insight.id.hashCode() xor insight.category.name.hashCode())
    val count = TREND_POP_COUNT_MIN + seed % TREND_POP_COUNT_RANGE
    val lngScale = 0.052
    val latScale = 0.038
    return (0 until count).map { index ->
        val angle = index * 2.399963229728653 + (seed % 360) * Math.PI / 180.0
        val ring = index % 5
        val radius = 1.35 + ring * 0.48 + ((index * 29 + seed) % 100) / 170.0
        TrendPopObject(
            point = Point.fromLngLat(
                stop.coordinate.longitude() + cos(angle) * lngScale * radius,
                stop.coordinate.latitude() + sin(angle) * latScale * radius
            ),
            emoji = trendEmoji(insight.category, seed + index),
            scale = 0.78f + ((index * 17 + seed) % 24) / 100f,
            delayMs = index * TREND_3D_SEQUENCE_GAP_MS,
            seed = seed + index * 97
        )
    }
}

private fun trendEmoji(category: TrendCategory, seed: Int): String {
    val emojis = when (category) {
        TrendCategory.Clothes -> listOf("👕", "👟", "🧢", "🕶️", "👜")
        TrendCategory.Sports -> listOf("🏀", "⚽", "🏈", "⚾", "🏆")
        TrendCategory.Food -> listOf("🍔", "🍕", "🍜", "🍣", "☕")
        TrendCategory.Nightlife -> listOf("🪩", "🍸", "🎉", "🌃", "✨")
        TrendCategory.Music -> listOf("🎤", "🎧", "🎸", "🎹", "🎵")
        TrendCategory.Travel -> listOf("✈️", "🗽", "🏙️", "📍", "🌍")
    }
    return emojis[abs(seed) % emojis.size]
}

private fun overshootScale(phase: Float): Float {
    if (phase <= 0f) return 0f
    if (phase >= 1f) return 1f
    val eased = 1f - (1f - phase) * (1f - phase) * (1f - phase)
    return eased + sin(phase * Math.PI.toFloat()) * 0.18f
}

private fun lightTrendColor(category: TrendCategory): Int = when (category) {
    TrendCategory.Clothes -> AndroidColor.rgb(255, 139, 218)
    TrendCategory.Sports -> AndroidColor.rgb(161, 255, 176)
    TrendCategory.Food -> AndroidColor.rgb(255, 224, 117)
    TrendCategory.Nightlife -> AndroidColor.rgb(205, 166, 255)
    TrendCategory.Music -> AndroidColor.rgb(148, 246, 255)
    TrendCategory.Travel -> AndroidColor.rgb(255, 157, 142)
}

private fun midTrendColor(category: TrendCategory): Int = when (category) {
    TrendCategory.Clothes -> AndroidColor.rgb(255, 72, 180)
    TrendCategory.Sports -> AndroidColor.rgb(70, 226, 101)
    TrendCategory.Food -> AndroidColor.rgb(255, 176, 45)
    TrendCategory.Nightlife -> AndroidColor.rgb(145, 88, 255)
    TrendCategory.Music -> AndroidColor.rgb(69, 210, 255)
    TrendCategory.Travel -> AndroidColor.rgb(255, 88, 75)
}

private fun darkTrendColor(category: TrendCategory): Int = when (category) {
    TrendCategory.Clothes -> AndroidColor.rgb(128, 28, 96)
    TrendCategory.Sports -> AndroidColor.rgb(28, 128, 55)
    TrendCategory.Food -> AndroidColor.rgb(155, 88, 18)
    TrendCategory.Nightlife -> AndroidColor.rgb(71, 42, 143)
    TrendCategory.Music -> AndroidColor.rgb(24, 112, 142)
    TrendCategory.Travel -> AndroidColor.rgb(151, 45, 42)
}

private fun uprightShirtPath(): Path = Path().apply {
    moveTo(-46f, -62f)
    lineTo(-22f, -84f)
    lineTo(-8f, -66f)
    lineTo(8f, -66f)
    lineTo(22f, -84f)
    lineTo(46f, -62f)
    lineTo(32f, -30f)
    lineTo(22f, -38f)
    lineTo(22f, 52f)
    lineTo(-22f, 52f)
    lineTo(-22f, -38f)
    lineTo(-32f, -30f)
    close()
}

private fun uprightGlassPath(): Path = Path().apply {
    moveTo(-42f, -72f)
    lineTo(42f, -72f)
    lineTo(24f, -6f)
    cubicTo(18f, 12f, -18f, 12f, -24f, -6f)
    close()
    moveTo(0f, 8f)
    lineTo(0f, 48f)
    moveTo(-26f, 52f)
    lineTo(26f, 52f)
}

private fun uprightMusicPath(): Path = Path().apply {
    moveTo(-24f, -72f)
    lineTo(34f, -84f)
    lineTo(34f, 18f)
    cubicTo(34f, 42f, -4f, 42f, -4f, 20f)
    cubicTo(-4f, 2f, 22f, 0f, 24f, 10f)
    lineTo(24f, -50f)
    lineTo(-14f, -42f)
    lineTo(-14f, 32f)
    cubicTo(-14f, 56f, -52f, 56f, -52f, 34f)
    cubicTo(-52f, 16f, -26f, 14f, -24f, 24f)
    close()
}

private fun uprightPlanePath(): Path = Path().apply {
    moveTo(-58f, -8f)
    lineTo(58f, -66f)
    lineTo(24f, -2f)
    lineTo(46f, 46f)
    lineTo(8f, 22f)
    lineTo(-32f, 58f)
    lineTo(-22f, 8f)
    close()
}

private suspend fun resolveTopCountryMusicTracks(stop: TourStop): Result<List<CityMusicTrack>> = runCatching {
    withContext(Dispatchers.IO) {
        val chartUrl = URL("https://rss.applemarketingtools.com/api/v2/${stop.storefront}/music/most-played/50/songs.json")
        val chartResponse = chartUrl.openConnection().apply {
            connectTimeout = MUSIC_LOOKUP_TIMEOUT_MS
            readTimeout = MUSIC_LOOKUP_TIMEOUT_MS
        }.getInputStream().bufferedReader().use { it.readText() }
        val results = JSONObject(chartResponse)
            .getJSONObject("feed")
            .getJSONArray("results")

        val chartItems = (0 until results.length()).map { index ->
            results.getJSONObject(index)
        }
        val tracks = mutableListOf<CityMusicTrack>()

        for (item in chartItems.take(24)) {
            val title = item.optString("name")
            val artist = item.optString("artistName")
            val previewUrl = lookupITunesPreviewUrl(title, artist, stop.storefront)
            if (previewUrl != null) {
                tracks += CityMusicTrack(previewUrl, "$artist - $title")
                if (tracks.size == MUSIC_TOP_TRACK_CHOICES) {
                    return@withContext tracks
                }
            }
        }

        if (tracks.isNotEmpty()) tracks else error("No playable chart preview found for ${stop.cityName}")
    }
}

private suspend fun resolveTopCityRadioTracks(stop: TourStop): Result<List<CityMusicTrack>> = runCatching {
    withContext(Dispatchers.IO) {
        val cityQuery = URLEncoder.encode(stop.cityName, "UTF-8")
        val country = stop.storefront.uppercase(Locale.US)
        val stationUrl = URL(
            "https://de1.api.radio-browser.info/json/stations/search" +
                "?name=$cityQuery" +
                "&countrycode=$country" +
                "&hidebroken=true" +
                "&order=clickcount" +
                "&reverse=true" +
                "&limit=24"
        )
        val response = stationUrl.openConnection().apply {
            connectTimeout = MUSIC_LOOKUP_TIMEOUT_MS
            readTimeout = MUSIC_LOOKUP_TIMEOUT_MS
            setRequestProperty("User-Agent", "Poplur2025-Android/1.0")
        }.getInputStream().bufferedReader().use { it.readText() }
        val stations = JSONArray(response)
        val tracks = mutableListOf<CityMusicTrack>()

        for (index in 0 until stations.length()) {
            val station = stations.getJSONObject(index)
            val streamUrl = station.optString("url_resolved").ifBlank { station.optString("url") }
            val name = station.optString("name", "Live radio")
            if (streamUrl.startsWith("http", ignoreCase = true) && name.isNotBlank()) {
                tracks += CityMusicTrack(streamUrl, "$name live radio")
                if (tracks.size == MUSIC_TOP_TRACK_CHOICES) {
                    return@withContext tracks
                }
            }
        }

        if (tracks.isNotEmpty()) tracks else error("No playable radio streams found for ${stop.cityName}")
    }
}

private suspend fun resolveTopCitySportsRadioTracks(stop: TourStop): Result<List<CityMusicTrack>> = runCatching {
    val cityStations = queryRadioBrowserStations(stop, nameQuery = stop.cityName, tagQuery = "sports", limit = 36)
    val countrySportsStations = if (cityStations.size < MUSIC_TOP_TRACK_CHOICES) {
        queryRadioBrowserStations(stop, nameQuery = "", tagQuery = "sports", limit = 36)
    } else {
        emptyList()
    }
    val sportsSignals = listOf("sports", "sport", "espn", "fox sports", "tsn", "sportsnet", "talksport", "score", "fan")
    val tracks = (cityStations + countrySportsStations)
        .filter { station ->
            val searchable = "${station.name} ${station.tags}".lowercase(Locale.US)
            sportsSignals.any { signal -> searchable.contains(signal) }
        }
        .distinctBy { it.streamUrl }
        .take(MUSIC_TOP_TRACK_CHOICES)
        .map { station -> CityMusicTrack(station.streamUrl, "${station.name} live sports radio") }

    if (tracks.isNotEmpty()) tracks else error("No playable sports radio streams found for ${stop.cityName}")
}

private data class RadioStationCandidate(
    val name: String,
    val tags: String,
    val streamUrl: String
)

private suspend fun queryRadioBrowserStations(
    stop: TourStop,
    nameQuery: String,
    tagQuery: String,
    limit: Int
): List<RadioStationCandidate> {
    return withContext(Dispatchers.IO) {
        val name = URLEncoder.encode(nameQuery, "UTF-8")
        val tag = URLEncoder.encode(tagQuery, "UTF-8")
        val country = stop.storefront.uppercase(Locale.US)
        val stationUrl = URL(
            "https://de1.api.radio-browser.info/json/stations/search" +
                "?name=$name" +
                "&tag=$tag" +
                "&countrycode=$country" +
                "&hidebroken=true" +
                "&order=clickcount" +
                "&reverse=true" +
                "&limit=$limit"
        )
        val response = stationUrl.openConnection().apply {
            connectTimeout = MUSIC_LOOKUP_TIMEOUT_MS
            readTimeout = MUSIC_LOOKUP_TIMEOUT_MS
            setRequestProperty("User-Agent", "Poplur2025-Android/1.0")
        }.getInputStream().bufferedReader().use { it.readText() }
        val stations = JSONArray(response)
        (0 until stations.length()).mapNotNull { index ->
            val station = stations.getJSONObject(index)
            val streamUrl = station.optString("url_resolved").ifBlank { station.optString("url") }
            val nameValue = station.optString("name", "Live radio")
            if (streamUrl.startsWith("http", ignoreCase = true) && nameValue.isNotBlank()) {
                RadioStationCandidate(
                    name = nameValue,
                    tags = station.optString("tags"),
                    streamUrl = streamUrl
                )
            } else {
                null
            }
        }
    }
}

private fun lookupITunesPreviewUrl(title: String, artist: String, storefront: String): String? {
    val term = URLEncoder.encode("$artist $title", "UTF-8")
    val country = storefront.uppercase(Locale.US)
    val searchUrl = URL("https://itunes.apple.com/search?term=$term&country=$country&media=music&entity=song&limit=5")
    val response = searchUrl.openConnection().apply {
        connectTimeout = MUSIC_LOOKUP_TIMEOUT_MS
        readTimeout = MUSIC_LOOKUP_TIMEOUT_MS
    }.getInputStream().bufferedReader().use { it.readText() }
    val results = JSONObject(response).getJSONArray("results")
    for (index in 0 until results.length()) {
        val item = results.getJSONObject(index)
        val preview = item.optString("previewUrl")
        if (preview.isNotBlank()) return preview
    }
    return null
}

private fun Style.setWeatherParticleSources(mode: WeatherParticleMode, point: Point?, frame: Int) {
    getSourceAs<GeoJsonSource>(WEATHER_RAIN_SOURCE_ID)?.featureCollection(
        if (mode == WeatherParticleMode.Rain || mode == WeatherParticleMode.Storm) weatherParticleFeatureCollection(point, mode, frame) else emptyFeatureCollection()
    )
    getSourceAs<GeoJsonSource>(WEATHER_SNOW_SOURCE_ID)?.featureCollection(
        if (mode == WeatherParticleMode.Snow) weatherParticleFeatureCollection(point, mode, frame) else emptyFeatureCollection()
    )
    getSourceAs<GeoJsonSource>(WEATHER_CLOUD_SOURCE_ID)?.featureCollection(
        if (mode == WeatherParticleMode.Cloud || mode == WeatherParticleMode.Storm) weatherParticleFeatureCollection(point, mode, frame) else emptyFeatureCollection()
    )
}

private fun weatherParticleMode(label: String): WeatherParticleMode {
    return when {
        label.contains("Rain", ignoreCase = true) || label.contains("Drizzle", ignoreCase = true) -> WeatherParticleMode.Rain
        label.contains("Snow", ignoreCase = true) -> WeatherParticleMode.Snow
        label.contains("Storm", ignoreCase = true) -> WeatherParticleMode.Storm
        else -> WeatherParticleMode.None
    }
}

private fun weatherParticleFeatureCollection(point: Point?, mode: WeatherParticleMode, frame: Int): FeatureCollection {
    if (point == null) return emptyFeatureCollection()
    val count = when (mode) {
        WeatherParticleMode.Rain -> 90
        WeatherParticleMode.Snow -> 70
        WeatherParticleMode.Cloud -> 30
        WeatherParticleMode.Storm -> 112
        WeatherParticleMode.None -> 0
    }
    val spanLat = when (mode) {
        WeatherParticleMode.Cloud -> 0.22
        WeatherParticleMode.Storm -> 0.16
        else -> 0.12
    }
    val spanLng = when (mode) {
        WeatherParticleMode.Cloud -> 0.28
        WeatherParticleMode.Storm -> 0.2
        else -> 0.16
    }
    val phase = (frame % 120) / 120.0
    val features = (0 until count).map { index ->
        val lane = ((index * 37) % 100) / 100.0
        val cross = ((index * 53) % 100) / 100.0
        val fall = (phase * (if (mode == WeatherParticleMode.Cloud) 0.38 else 1.35) + index * 0.031) % 1.0
        val sway = kotlin.math.sin((phase * 6.28318) + index * 0.61) * when (mode) {
            WeatherParticleMode.Snow -> 0.12
            WeatherParticleMode.Cloud -> 0.18
            else -> 0.04
        }
        val lng = point.longitude() + (cross - 0.5 + sway) * spanLng
        val lat = point.latitude() + (0.5 - fall + (lane - 0.5) * 0.2) * spanLat
        Feature.fromGeometry(Point.fromLngLat(lng, lat))
    }
    return FeatureCollection.fromFeatures(features)
}

private suspend fun fetchCurrentWeather(point: Point): Result<WeatherBadge> = runCatching {
    withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${point.latitude()}" +
                "&longitude=${point.longitude()}" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=fahrenheit" +
                "&timezone=auto"
        )
        val connection = url.openConnection().apply {
            connectTimeout = WEATHER_TIMEOUT_MS
            readTimeout = WEATHER_TIMEOUT_MS
        }
        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        val current = JSONObject(response).getJSONObject("current")
        val code = current.getInt("weather_code")
        val temperature = current.getDouble("temperature_2m")
        weatherBadgeForCode(code, temperature)
    }
}

private fun weatherBadgeForCode(code: Int, temperature: Double): WeatherBadge {
    val temperatureLabel = "${String.format(Locale.US, "%.0f", temperature)}°F"
    val condition = when (code) {
        0 -> "Clear" to "☀️"
        1 -> "Mostly Clear" to "🌤️"
        2 -> "Partly Cloudy" to "⛅"
        3 -> "Cloudy" to "☁️"
        45, 48 -> "Fog" to "🌫️"
        51, 53, 55, 56, 57 -> "Drizzle" to "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain" to "🌧️"
        71, 73, 75, 77, 85, 86 -> "Snow" to "❄️"
        95, 96, 99 -> "Storm" to "⛈️"
        else -> "Weather" to "☁️"
    }
    return WeatherBadge(icon = condition.second, label = condition.first, temperatureLabel = temperatureLabel)
}

private fun fallbackWeatherBadge(point: Point): WeatherBadge {
    val seed = abs((point.latitude() * 17 + point.longitude() * 11).toInt())
    val temperature = 54 + (seed % 34)
    val condition = when (seed % 5) {
        0 -> "Clear" to "☀️"
        1 -> "Partly Cloudy" to "⛅"
        2 -> "Cloudy" to "☁️"
        3 -> "Rain" to "🌧️"
        else -> "Mostly Clear" to "🌤️"
    }
    return WeatherBadge(condition.second, condition.first, "${temperature}°F")
}

private class RadiusSparkleView(
    context: Context,
    private val mapView: MapView
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var center: Point? = null
    private var radiusKm: Int = 0
    private val sparkles = List(450) { createRandomSparkle() }
    private val startMs = SystemClock.uptimeMillis()

    private fun createRandomSparkle() = Sparkle(
        bearing = Math.random() * 360.0,
        size = (2.5f + Math.random() * 8.5f).toFloat(),
        alpha = (0.25f + Math.random() * 0.75f).toFloat(),
        radialFactor = Math.random(),
        phase = (Math.random() * Math.PI * 2.0).toFloat()
    )

    fun setRadius(center: Point?, radiusKm: Int) {
        if (this.center != center || this.radiusKm != radiusKm) {
            this.center = center
            this.radiusKm = radiusKm
            invalidate()
        }
    }

    private val animationTick = object : Runnable {
        override fun run() {
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postOnAnimation(animationTick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val center = center ?: return
        if (radiusKm <= 0) return

        val elapsed = SystemClock.uptimeMillis() - startMs
        paint.style = Paint.Style.FILL

        sparkles.forEach { sparkle ->
            val r = radiusKm.toDouble() * sqrt(sparkle.radialFactor)
            val coord = getPointOnCircle(center, r, sparkle.bearing)
            val pixel = mapView.mapboxMap.pixelForCoordinate(coord)
            
            if (pixel.x in -60.0..(width + 60).toDouble() && pixel.y in -60.0..(height + 60).toDouble()) {
                val twinkle = (sin(elapsed / 380.0 + sparkle.phase).toFloat() + 1f) / 2f
                val currentAlpha = sparkle.alpha * (0.3f + 0.7f * twinkle)
                val currentSize = sparkle.size * (0.7f + 0.3f * twinkle)
                
                // Glow
                paint.color = AndroidColor.argb((currentAlpha * 95).toInt(), 255, 225, 120)
                canvas.drawCircle(pixel.x.toFloat(), pixel.y.toFloat(), currentSize * 1.5f, paint)
                
                // Core
                paint.color = AndroidColor.argb((currentAlpha * 255).toInt(), 255, 255, 235)
                canvas.drawCircle(pixel.x.toFloat(), pixel.y.toFloat(), currentSize / 2f, paint)
            }
        }
    }

    private fun getPointOnCircle(center: Point, radiusKm: Double, bearingDegrees: Double): Point {
        val angularDistance = radiusKm / EARTH_RADIUS_KM.toDouble()
        val bearingRad = Math.toRadians(bearingDegrees)
        val centerLat = Math.toRadians(center.latitude())
        val centerLng = Math.toRadians(center.longitude())

        val lat = asin(
            sin(centerLat) * cos(angularDistance) +
                cos(centerLat) * sin(angularDistance) * cos(bearingRad)
        )
        val lng = centerLng + atan2(
            sin(bearingRad) * sin(angularDistance) * cos(centerLat),
            cos(angularDistance) - sin(centerLat) * sin(lat)
        )
        return Point.fromLngLat(Math.toDegrees(lng), Math.toDegrees(lat))
    }
    
    private data class Sparkle(
        val bearing: Double,
        val size: Float,
        val alpha: Float,
        val radialFactor: Double,
        val phase: Float
    )
}

@Composable
private fun WeatherParticleOverlay(
    weather: WeatherBadge,
    modifier: Modifier = Modifier
) {
    val label = weather.label
    if (
        label.contains("Weather", ignoreCase = true) ||
        label.contains("World", ignoreCase = true) ||
        label.contains("Cloudy", ignoreCase = true) ||
        label.contains("Fog", ignoreCase = true) ||
        label.contains("Clear", ignoreCase = true) && !label.contains("Cloudy", ignoreCase = true)
    ) {
        return
    }

    val transition = rememberInfiniteTransition(label = "map-weather-particles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "map-weather-phase"
    )

    Canvas(modifier = modifier) {
        val drawTop = 0f
        val drawHeight = size.height.coerceAtLeast(1f)

        when {
            label.contains("Rain", ignoreCase = true) || label.contains("Drizzle", ignoreCase = true) -> {
                repeat(125) { index ->
                    val lane = ((index * 37) % 100) / 100f
                    val y = drawTop + ((phase * drawHeight * 2.1f + index * 47f) % (drawHeight + 90f)) - 45f
                    val x = lane * size.width + ((index % 7) - 3) * 5f
                    drawLine(
                        color = Color(0x99A8C7FF),
                        start = Offset(x, y),
                        end = Offset(x - 8f, y + 34f),
                        strokeWidth = 2.4f
                    )
                }
            }
            label.contains("Snow", ignoreCase = true) -> {
                repeat(95) { index ->
                    val lane = ((index * 41) % 100) / 100f
                    val y = drawTop + ((phase * drawHeight * 1.05f + index * 39f) % (drawHeight + 70f)) - 35f
                    val x = lane * size.width + kotlin.math.sin((phase * 6.28f + index) * 0.7f) * 18f
                    drawCircle(
                        color = Color(0xCCFFFFFF),
                        radius = 2.4f + (index % 3),
                        center = Offset(x, y)
                    )
                }
            }
            label.contains("Storm", ignoreCase = true) -> {
                repeat(140) { index ->
                    val lane = ((index * 31) % 100) / 100f
                    val y = drawTop + ((phase * drawHeight * 2.35f + index * 43f) % (drawHeight + 90f)) - 45f
                    val x = lane * size.width
                    drawLine(
                        color = Color(0xAAB5CBFF),
                        start = Offset(x, y),
                        end = Offset(x - 10f, y + 38f),
                        strokeWidth = 2.8f
                    )
                }
                if (phase < 0.08f) {
                    drawRect(Color.White.copy(alpha = 0.18f * (1f - phase / 0.08f)))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MapScreenPreview() {
    MaterialTheme {
        MapScreenContent(
            isVisible = true,
            centerOnUserTrigger = emptyFlow(),
            postCoordinateTrigger = emptyFlow()
        )
    }
}

private class RealPopTonePlayer {
    private val generator = try {
        android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 82)
    } catch (e: Exception) {
        null
    }

    fun play(duration: Int) {
        generator?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, duration)
    }

    fun release() {
        generator?.release()
    }
}

private class ProceduralRainPlayer {
    private val audioTrack = runCatching { createRainTrack() }.getOrNull()

    fun play() {
        val track = audioTrack ?: return
        if (track.playState != android.media.AudioTrack.PLAYSTATE_PLAYING) {
            track.play()
        }
    }

    fun pause() {
        val track = audioTrack ?: return
        if (track.playState == android.media.AudioTrack.PLAYSTATE_PLAYING) {
            track.pause()
        }
    }

    fun release() {
        audioTrack?.release()
    }

    private fun createRainTrack(): android.media.AudioTrack {
        val sampleRate = 22_050
        val frameCount = sampleRate * 4
        val samples = ShortArray(frameCount)
        val random = java.util.Random(20250610L)
        var low = 0.0

        repeat(frameCount) { index ->
            val t = index.toDouble() / sampleRate
            val white = random.nextDouble() * 2.0 - 1.0
            low = low * 0.982 + white * 0.018
            val hiss = white * 0.16 + low * 1.12
            val drop = if (random.nextDouble() < 0.018) {
                random.nextDouble() * 0.34 * sin((2.0 * Math.PI * (1_300.0 + random.nextDouble() * 2_800.0) * t).toFloat())
            } else {
                0.0
            }
            val rumble = sin((2.0 * Math.PI * 78.0 * t).toFloat()) * 0.045
            val value = ((hiss * 0.58 + drop * 0.2 + rumble) * 0.34).coerceIn(-0.9, 0.9)
            samples[index] = (value * Short.MAX_VALUE).toInt().toShort()
        }

        val audioFormat = android.media.AudioFormat.Builder()
            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        return android.media.AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
            .setTransferMode(android.media.AudioTrack.MODE_STATIC)
            .build()
            .apply {
                write(samples, 0, samples.size)
                setLoopPoints(0, samples.size, -1)
                setVolume(0.34f)
            }
    }
}

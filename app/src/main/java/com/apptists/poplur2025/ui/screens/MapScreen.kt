package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.location.LocationProvider
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ProjectionName
import com.mapbox.maps.extension.style.projection.generated.projection
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(isVisible: Boolean = true) {
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
    var activeRadius by remember { mutableIntStateOf(DEFAULT_RADIUS_KM) }
    var idleGeneration by remember { mutableIntStateOf(0) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { }
    val mapView = remember { MapView(context) }
    val locationProvider = remember { LocationProvider(context) }
    val postFeatures = remember {
        DemoPostStore.posts.mapNotNull { post ->
            val lat = post.latitude
            val lng = post.longitude
            if (lat != null && lng != null) Feature.fromGeometry(Point.fromLngLat(lng, lat)) else null
        }
    }
    var mapReady by remember { mutableStateOf(false) }
    var hasZoomedToLocation by remember { mutableStateOf(false) }

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
            mapView.location.updateSettings {
                enabled = hasLocationPermission
                pulsingEnabled = true
            }
            mapView.mapboxMap.addOnMapClickListener { point ->
                selectedCoordinate = point
                activeCityName = "Selected Location"
                activeRadius = SELECTED_LOCATION_RADIUS_KM
                idleGeneration++
                mapView.mapboxMap.flyTo(
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

    var userLocation by remember { mutableStateOf<Point?>(null) }

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

    LaunchedEffect(isVisible, mapReady, userLocation) {
        val location = userLocation
        if (isVisible && mapReady && location != null && !hasZoomedToLocation) {
            activeCityName = "Your Location"
            activeRadius = DEFAULT_RADIUS_KM
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(globeView)
                    .zoom(0.45)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build()
            )
            delay(1150)
            mapView.mapboxMap.flyTo(
                CameraOptions.Builder()
                    .center(location)
                    .zoom(7.0)
                    .pitch(0.0)
                    .bearing(0.0)
                    .build(),
                mapAnimationOptions { duration(1900) }
            )
            delay(2050)
            mapView.mapboxMap.flyTo(
                CameraOptions.Builder()
                    .center(location)
                    .zoom(16.9)
                    .pitch(66.0)
                    .bearing(28.0)
                    .build(),
                mapAnimationOptions { duration(3600) }
            )
            hasZoomedToLocation = true
        }
    }

    LaunchedEffect(isVisible, mapReady, idleGeneration) {
        if (!isVisible || !mapReady) return@LaunchedEffect
        delay(IDLE_TOUR_DELAY_MS)
        while (isVisible && mapReady) {
            TourStops.forEach { stop ->
                activeCityName = "World"
                activeRadius = EARTH_RADIUS_KM
                mapView.mapboxMap.flyTo(
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
                mapView.mapboxMap.flyTo(
                    CameraOptions.Builder()
                        .center(stop.coordinate)
                        .zoom(stop.zoom)
                        .pitch(66.0)
                        .bearing(stop.bearing)
                        .build(),
                    mapAnimationOptions { duration(4200) }
                )
                delay(TOUR_STOP_DWELL_MS)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
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
                            +circleLayer(POST_LAYER_ID, POST_SOURCE_ID) {
                                circleRadius(18.0)
                                circleColor("#ff2d8a")
                                circleOpacity(0.46)
                                circleStrokeColor("#ffffff")
                                circleStrokeOpacity(0.9)
                                circleStrokeWidth(2.5)
                                circleEmissiveStrength(1.0)
                            }
                        }
                    ) { style ->
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "show3dObjects", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showPointOfInterestLabels", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showRoadLabels", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "showPlaceLabels", Value.valueOf(true))
                        style.setStyleImportConfigProperty(MAPBOX_BASEMAP_IMPORT_ID, "lightPreset", Value.valueOf("dusk"))
                        mapReady = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxWidth().height(190.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent))))
        Column(Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopStart)) {
            Text(activeCityName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Tap the 3D map to choose exactly where a post should live.", color = Color.White.copy(alpha = .9f), fontSize = 16.sp)
        }
        Surface(Modifier.padding(end = 16.dp).align(Alignment.CenterEnd), color = Color.Black.copy(alpha = .42f), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("☁️", fontSize = 36.sp)
                Text("Rain", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(bottom = 80.dp, start = 20.dp).align(Alignment.BottomStart)) {
            Text("476", color = Color(0xFFFF3B30), fontSize = 42.sp, fontWeight = FontWeight.Black)
            HorizontalDivider(color = Color.White.copy(alpha = .5f), thickness = 1.dp, modifier = Modifier.width(100.dp).padding(vertical = 8.dp))
            Text(DemoPostStore.posts.size.toString(), color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
            Text("posts", color = Color.White, fontSize = 18.sp)
        }
        selectedCoordinate?.let { point ->
            Surface(Modifier.align(Alignment.BottomCenter).padding(bottom = 158.dp, start = 20.dp, end = 20.dp), color = Color.Black.copy(alpha = .72f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Post here", color = Color.White, fontWeight = FontWeight.Black)
                    Text("${"%.5f".format(point.latitude())}, ${"%.5f".format(point.longitude())}", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Button(onClick = { mediaPicker.launch("image/*") }, modifier = Modifier.padding(top = 8.dp)) { Text("Choose media") }
                }
            }
        }
        Spacer(Modifier.padding(bottom = 60.dp).size(80.dp).align(Alignment.BottomCenter))
        RadiusDisplay(Modifier.padding(bottom = 60.dp, end = 20.dp).align(Alignment.BottomEnd), radius = activeRadius)
    }
}

private const val POST_SOURCE_ID = "poplur-posts"
private const val POST_LAYER_ID = "poplur-post-markers"
private const val MAPBOX_BASEMAP_IMPORT_ID = "basemap"
private const val IDLE_TOUR_DELAY_MS = 30_000L
private const val TOUR_STOP_DWELL_MS = 30_000L
private const val DEFAULT_RADIUS_KM = 5_000
private const val SELECTED_LOCATION_RADIUS_KM = 850
private const val EARTH_RADIUS_KM = 6_371

private data class TourStop(
    val cityName: String,
    val coordinate: Point,
    val bearing: Double,
    val radiusKm: Int,
    val zoom: Double = 16.2
)

private val TourStops = listOf(
    TourStop("Paris", Point.fromLngLat(2.2945, 48.8584), 28.0, 105),
    TourStop("New York", Point.fromLngLat(-73.9857, 40.7484), -24.0, 132),
    TourStop("Tokyo", Point.fromLngLat(139.7454, 35.6586), 34.0, 148),
    TourStop("London", Point.fromLngLat(-0.1246, 51.5007), -32.0, 118),
    TourStop("Dubai", Point.fromLngLat(55.2744, 25.1972), 18.0, 96),
    TourStop("Toronto", Point.fromLngLat(-79.3871, 43.6426), 24.0, 84),
    TourStop("Sydney", Point.fromLngLat(151.2153, -33.8568), -28.0, 110)
)

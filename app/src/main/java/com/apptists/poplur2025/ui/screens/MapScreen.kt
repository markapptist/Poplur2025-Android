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
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
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
                mapView.mapboxMap.flyTo(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(18.8)
                        .pitch(67.5)
                        .bearing(22.0)
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
                    .zoom(18.8)
                    .pitch(67.5)
                    .bearing(22.0)
                    .build(),
                mapAnimationOptions { duration(3600) }
            )
            hasZoomedToLocation = true
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
                        style(Style.STANDARD_SATELLITE) {
                            +atmosphere { }
                            +projection(ProjectionName.GLOBE)
                            +fillExtrusionLayer(BUILDING_LAYER_ID, MAPBOX_COMPOSITE_SOURCE_ID) {
                                sourceLayer(MAPBOX_BUILDING_SOURCE_LAYER)
                                minZoom(15.0)
                                slot("middle")
                                fillExtrusionColor("#d7d0c7")
                                fillExtrusionOpacity(0.78)
                                fillExtrusionHeight(Expression.fromRaw(BUILDING_HEIGHT_EXPRESSION))
                                fillExtrusionBase(Expression.fromRaw(BUILDING_BASE_EXPRESSION))
                                fillExtrusionVerticalScale(1.35)
                                fillExtrusionVerticalGradient(true)
                                fillExtrusionAmbientOcclusionIntensity(0.55)
                                fillExtrusionAmbientOcclusionRadius(3.0)
                            }
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
                    ) {
                        mapReady = true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxWidth().height(190.dp).background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent))))
        Column(Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopStart)) {
            Text("Your Location", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Tap the hybrid map to choose exactly where a post should live.", color = Color.White.copy(alpha = .9f), fontSize = 16.sp)
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
        RadiusDisplay(Modifier.padding(bottom = 60.dp, end = 20.dp).align(Alignment.BottomEnd), radius = 5000)
    }
}

private const val POST_SOURCE_ID = "poplur-posts"
private const val POST_LAYER_ID = "poplur-post-markers"
private const val BUILDING_LAYER_ID = "poplur-building-extrusions"
private const val MAPBOX_COMPOSITE_SOURCE_ID = "composite"
private const val MAPBOX_BUILDING_SOURCE_LAYER = "building"
private const val BUILDING_HEIGHT_EXPRESSION =
    "[\"coalesce\",[\"get\",\"height\"],[\"*\",[\"coalesce\",[\"get\",\"levels\"],2],3.2],8]"
private const val BUILDING_BASE_EXPRESSION = "[\"coalesce\",[\"get\",\"min_height\"],0]"

package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.location.LocationProvider
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(isVisible: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    // Start as wide as Google Maps allows, then fly into the user's location.
    val globeView = LatLng(18.0, -98.0)
    val fallbackLocation = LatLng(40.758896, -73.985130)
    var selectedCoordinate by remember { mutableStateOf<LatLng?>(null) }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { }
    val mapView = remember {
        MapView(context, GoogleMapOptions()
            .camera(CameraPosition.fromLatLngZoom(globeView, 0.0f))
            .mapType(GoogleMap.MAP_TYPE_HYBRID)
            .liteMode(false)
            .compassEnabled(false)
            .mapToolbarEnabled(false)
            .tiltGesturesEnabled(true)
            .rotateGesturesEnabled(true))
    }
    val locationProvider = remember { LocationProvider(context) }
    var mapInstance by remember { mutableStateOf<GoogleMap?>(null) }
    var hasZoomedToLocation by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> try { mapView.onCreate(Bundle()) } catch (e: Exception) { Log.e("MapScreen", "onCreate", e) }
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) try { mapView.onCreate(Bundle()) } catch (_: Exception) {}
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    LaunchedEffect(mapInstance, hasLocationPermission) {
        mapInstance?.let { map ->
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            map.isBuildingsEnabled = true
            map.isMyLocationEnabled = hasLocationPermission
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isZoomControlsEnabled = false
            
            map.setMinZoomPreference(0.0f)
            map.setMaxZoomPreference(20.0f)
            if (!hasZoomedToLocation) {
                map.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(globeView)
                            .zoom(1.15f)
                            .tilt(0.0f)
                            .bearing(0.0f)
                            .build()
                    )
                )
            }

            map.setOnMapClickListener { latLng ->
                selectedCoordinate = latLng
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.8f))
            }
            map.clear()
            DemoPostStore.posts.forEach { post ->
                val lat = post.latitude
                val lng = post.longitude
                if (lat != null && lng != null) {
                    val point = LatLng(lat, lng)
                    map.addMarker(MarkerOptions().position(point).title(post.displayName).snippet(post.caption))
                    map.addCircle(CircleOptions()
                        .center(point)
                        .radius(post.radius.toDouble().coerceAtLeast(5000.0))
                        .strokeWidth(4f)
                        .strokeColor(0x66FF2D8A)
                        .fillColor(0x22FF2D8A))
                }
            }
        }
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationProvider.lastLocation { lat, lng ->
                val finalLat = lat ?: fallbackLocation.latitude
                val finalLng = lng ?: fallbackLocation.longitude
                userLocation = LatLng(finalLat, finalLng)
            }
        } else {
            userLocation = fallbackLocation
        }
    }

    LaunchedEffect(isVisible, mapInstance, userLocation) {
        val map = mapInstance
        val location = userLocation
        if (isVisible && map != null && location != null && !hasZoomedToLocation) {
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(globeView)
                        .zoom(1.15f)
                        .tilt(0.0f)
                        .bearing(0.0f)
                        .build()
                )
            )
            delay(1150)
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(location)
                        .zoom(7.0f)
                        .tilt(0.0f)
                        .bearing(0.0f)
                        .build()
                ),
                1900,
                null
            )
            delay(2050)
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(location)
                        .zoom(18.8f)
                        .tilt(67.5f)
                        .bearing(22.0f)
                        .build()
                ),
                3600,
                null
            )
            hasZoomedToLocation = true
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { mapInstance = it }
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
                    Text("${"%.5f".format(point.latitude)}, ${"%.5f".format(point.longitude)}", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Button(onClick = { mediaPicker.launch("image/*") }, modifier = Modifier.padding(top = 8.dp)) { Text("Choose media") }
                }
            }
        }
        Spacer(Modifier.padding(bottom = 60.dp).size(80.dp).align(Alignment.BottomCenter))
        RadiusDisplay(Modifier.padding(bottom = 60.dp, end = 20.dp).align(Alignment.BottomEnd), radius = 5000)
    }
}

package com.apptists.poplur2025.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.GoogleMap
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.location.LocationProvider

@SuppressLint("MissingPermission")
@Composable fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Times Square, NY Spoofed Location
    val spoofedLat = 40.758896
    val spoofedLng = -73.985130
    val initialLocation = LatLng(spoofedLat, spoofedLng)

    // OPTIMIZATION: Use a persistent MapView to avoid re-inflation costs
    val mapView = remember { 
        MapView(context, GoogleMapOptions()
            .camera(CameraPosition.fromLatLngZoom(initialLocation, 19f))
            .mapType(GoogleMap.MAP_TYPE_HYBRID) // Stay HYBRID as requested, but pre-set it
            .liteMode(false)
            .compassEnabled(false) // Disable unused UI elements for speed
            .mapToolbarEnabled(false)
            .tiltGesturesEnabled(true)
        ) 
    }
    val locationProvider = remember { LocationProvider(context) }
    var mapInstance by remember { mutableStateOf<GoogleMap?>(null) }

    // Sync MapView lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    try {
                        mapView.onCreate(Bundle())
                    } catch (e: Exception) {
                        Log.e("MapScreen", "Error in MapView.onCreate", e)
                    }
                }
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.CREATED)) {
            try { mapView.onCreate(Bundle()) } catch (e: Exception) {}
        }
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Config Logic - map camera is already set by GoogleMapOptions
    LaunchedEffect(mapInstance) {
        mapInstance?.let { map ->
            Log.d("MapScreen", "Configuring Map Instance Settings")
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false
            
            // Background check for real location (optional, doesn't block)
            locationProvider.lastLocation { lat, lng ->
                if (lat != null && lng != null) {
                    Log.d("MapScreen", "Real location found: $lat, $lng")
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { 
                mapView.apply {
                    getMapAsync { map ->
                        mapInstance = map
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Scrim for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // 3. Header Overlay
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = "Your Location",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Explore what's happening around you\nright now!",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }

        // 4. Middle Right Weather Widget
        Surface(
            modifier = Modifier
                .padding(end = 16.dp)
                .align(Alignment.CenterEnd),
            color = Color.Black.copy(alpha = 0.4f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("☁️", fontSize = 36.sp)
                Text("Rain", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 5. Bottom Left Overlay (Trending/Posts)
        Column(
            modifier = Modifier
                .padding(bottom = 80.dp, start = 20.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = "476",
                color = Color(0xFFFF3B30),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.5f), 
                thickness = 1.dp, 
                modifier = Modifier.width(100.dp).padding(vertical = 8.dp)
            )
            Text(
                text = "0",
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "posts",
                color = Color.White,
                fontSize = 18.sp
            )
        }

        // 6. Bottom Center placeholder space for the global Cube (managed by PoplurRoot)
        Spacer(
            modifier = Modifier
                .padding(bottom = 60.dp)
                .size(80.dp)
                .align(Alignment.BottomCenter)
        )

        // 7. Bottom Right Overlay (Radius)
        RadiusDisplay(
            radius = 0,
            totalRadius = 3959,
            modifier = Modifier
                .padding(bottom = 60.dp, end = 20.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

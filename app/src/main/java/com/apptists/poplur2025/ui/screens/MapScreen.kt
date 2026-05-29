package com.apptists.poplur2025.ui.screens

import android.annotation.SuppressLint
import android.os.Bundle
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.location.LocationProvider

@SuppressLint("MissingPermission")
@Composable fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Initialize Maps with the latest renderer early
    LaunchedEffect(Unit) {
        MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) { }
    }

    // Use remember for MapView and handle its lifecycle more robustly
    val mapView = remember { MapView(context) }
    val locationProvider = remember { LocationProvider(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        
        // Manually trigger initial lifecycle states if needed
        val currentState = lifecycleOwner.lifecycle.currentState
        if (currentState.isAtLeast(Lifecycle.State.CREATED)) mapView.onCreate(Bundle())
        if (currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // 1. Map View (Hybrid mode + High Zoom)
        AndroidView(
            factory = { 
                mapView.apply {
                    getMapAsync { map ->
                        map.mapType = GoogleMap.MAP_TYPE_HYBRID
                        map.isMyLocationEnabled = true
                        map.uiSettings.isMyLocationButtonEnabled = false
                        map.uiSettings.isTiltGesturesEnabled = true
                        
                        // Start with a zoomed in view
                        val toronto = LatLng(43.6532, -79.3832)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(toronto, 18f))
                        
                        // Try to move to actual current location and zoom in tight
                        locationProvider.lastLocation { lat, lng ->
                            if (lat != null && lng != null) {
                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 19f))
                            }
                        }
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

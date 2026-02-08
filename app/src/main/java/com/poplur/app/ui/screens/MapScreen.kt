package com.poplur.app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    // Minimal map screen: centers on last known location (if permission granted).
    val context = androidx.compose.ui.platform.LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }

    var start by remember { mutableStateOf(LatLng(43.6532, -79.3832)) } // Toronto fallback

    LaunchedEffect(Unit) {
        runCatching {
            val loc = fused.lastLocation.await()
            if (loc != null) start = LatLng(loc.latitude, loc.longitude)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true),
        uiSettings = MapUiSettings(myLocationButtonEnabled = true)
    ) {
        Marker(state = MarkerState(position = start), title = "You")
        Circle(
            center = start,
            radius = 250.0,
            fillColor = Color(0x33007AFF),
            strokeColor = Color(0x66007AFF),
            strokeWidth = 2f
        )
    }
}

// Small Task -> suspend bridge for FusedLocationProviderClient
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
    }

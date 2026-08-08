package com.poplur.app.ui.screens

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var zoom by remember { mutableStateOf(19f) }
    var showForecast by remember { mutableStateOf(false) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }

    LaunchedEffect(Unit) {
        runCatching {
            val loc = fused.lastLocation.await()
            if (loc != null) start = LatLng(loc.latitude, loc.longitude)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, zoom)
    }

    LaunchedEffect(start, zoom) {
        cameraPositionState.animate(
            update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(start, zoom),
            durationMs = 420
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
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

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(58.dp)
                    .clickable { showForecast = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.WbSunny, contentDescription = "Weather forecast", tint = Color(0xFFFFD54F))
                }
            }
            Spacer(Modifier.height(14.dp))
            Slider(
                value = zoom,
                onValueChange = { next ->
                    val rising = next > zoom
                    zoom = next
                    tone.startTone(if (rising) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 44)
                },
                valueRange = 15f..21f,
                modifier = Modifier
                    .height(220.dp)
                    .width(74.dp)
            )
        }

        if (showForecast) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .clickable { showForecast = false },
                contentAlignment = Alignment.Center
            ) {
                ForecastWindow(Modifier.padding(24.dp))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { tone.release() }
    }
}

@Composable
private fun ForecastWindow(modifier: Modifier = Modifier) {
    val days = listOf("Today" to "Sunny 27C", "Tue" to "Clouds 25C", "Wed" to "Rain 22C", "Thu" to "Clear 26C", "Fri" to "Hot 29C")
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF111018).copy(alpha = 0.94f)
    ) {
        Column(Modifier.padding(22.dp)) {
            Text("5 day forecast", color = Color.White, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            days.forEach { (day, summary) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(day, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.width(64.dp))
                    Text(summary, color = Color.White.copy(alpha = 0.86f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// Small Task -> suspend bridge for FusedLocationProviderClient
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resume(null) }
    }

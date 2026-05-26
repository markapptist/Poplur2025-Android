package com.apptists.poplur2025.ui.screens

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.apptists.poplur2025.data.DemoPostStore

@Composable fun MapScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        mutableStateOf<MapView?>(null)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.value?.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.value?.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.value?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.value?.onPause()
                Lifecycle.Event.ON_STOP -> mapView.value?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.value?.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView.value = this
                    getMapAsync { map ->
                        val toronto = LatLng(43.6532, -79.3832)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(toronto, 3.5f))
                        DemoPostStore.dominance.forEach { d ->
                            map.addMarker(MarkerOptions().position(LatLng(d.lat, d.lng)).title("${d.city}: ${d.leader} ${d.score}"))
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

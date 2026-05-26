package com.apptists.poplur2025.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.data.FilterRepository

@Composable fun CameraScreen() {
    val context = LocalContext.current; val owner = LocalLifecycleOwner.current
    Box(Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            val view = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                provider.unbindAll(); provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
            }, ContextCompat.getMainExecutor(ctx))
            view
        })
        Column(Modifier.fillMaxWidth().padding(16.dp)) { Text("CameraX + ML Kit equivalent", color = MaterialTheme.colorScheme.onPrimary); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterRepository.filters.forEach { AssistChip(onClick = {}, label = { Text(it.name) }) } } }
    }
}

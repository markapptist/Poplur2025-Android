package com.apptists.poplur2025.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.ui.components.RadiusDisplay

@Composable fun CameraScreen() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // Handle selected uri
    }

    Box(Modifier.fillMaxSize()) {
        // 1. Camera Preview
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
            val view = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            view
        })

        // 2. Top Controls Text
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(24.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = "Camera Controls",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap cube to take photo. Hold cube to\nrecord video",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                lineHeight = 20.sp
            )
        }

        // 3. Scanning Pill (Center)
        Surface(
            modifier = Modifier.align(Alignment.Center).padding(top = 100.dp),
            color = Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Scanning...",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 4. Bottom Left Camera Roll Button
        Surface(
            modifier = Modifier
                .padding(bottom = 60.dp, start = 20.dp)
                .size(70.dp)
                .align(Alignment.BottomStart),
            color = Color.Black.copy(alpha = 0.3f),
            shape = RoundedCornerShape(12.dp),
            onClick = { photoPickerLauncher.launch("image/*") }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 5. Bottom Center placeholder for Cube (handled by PoplurRoot)
        Spacer(
            modifier = Modifier
                .padding(bottom = 60.dp)
                .size(80.dp)
                .align(Alignment.BottomCenter)
        )

        // 6. Bottom Right Radius Display
        RadiusDisplay(
            radius = 0,
            totalRadius = 3959,
            modifier = Modifier
                .padding(bottom = 60.dp, end = 20.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

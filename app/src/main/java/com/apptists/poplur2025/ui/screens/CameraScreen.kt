package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RadioButtonChecked
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

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedMedia = uri }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx -> PreviewView(ctx) },
                update = { view ->
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            provider.unbindAll()
                            val camera = provider.bindToLifecycle(owner, selector, preview)
                            camera.cameraControl.enableTorch(torchEnabled)
                        } catch (e: Exception) { e.printStackTrace() }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Camera permission needed",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(Modifier.statusBarsPadding().padding(24.dp).align(Alignment.TopStart)) {
            Text("Camera Controls", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Tap cube to take photo. Hold cube to record video", color = Color.White.copy(alpha = .9f), fontSize = 16.sp, lineHeight = 20.sp)
            selectedMedia?.let { Text("Selected: ${it.lastPathSegment ?: "media"}", color = Color.White.copy(alpha = .8f), fontSize = 12.sp) }
        }

        Surface(modifier = Modifier.padding(bottom = 62.dp, start = 20.dp).size(58.dp).align(Alignment.BottomStart), color = Color.Black.copy(alpha = .3f), shape = RoundedCornerShape(12.dp), onClick = { photoPickerLauncher.launch("image/*") }) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(32.dp)) }
        }

        RadiusDisplay(Modifier.padding(bottom = 48.dp, end = 20.dp).align(Alignment.BottomEnd), radius = 0)
    }
}

@Composable
private fun CameraPill(onClick: () -> Unit, active: Boolean = false, content: @Composable () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (active) Color(0xFFFF2D8A) else Color.Black.copy(alpha = .35f), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

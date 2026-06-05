package com.apptists.poplur2025.ui.screens

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
    var useFrontCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedMedia = uri }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
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

        Column(Modifier.statusBarsPadding().padding(24.dp).align(Alignment.TopStart)) {
            Text("Camera Controls", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Tap cube to take photo. Hold cube to record video", color = Color.White.copy(alpha = .9f), fontSize = 16.sp, lineHeight = 20.sp)
            selectedMedia?.let { Text("Selected: ${it.lastPathSegment ?: "media"}", color = Color.White.copy(alpha = .8f), fontSize = 12.sp) }
        }

        Row(Modifier.statusBarsPadding().padding(20.dp).align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CameraPill(onClick = { torchEnabled = !torchEnabled }, active = torchEnabled) { Icon(Icons.Default.FlashOn, null, tint = Color.White) }
            CameraPill(onClick = { useFrontCamera = !useFrontCamera }) { Text("↺", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        }

        Surface(Modifier.align(Alignment.Center).padding(top = 100.dp), color = Color.Black.copy(alpha = .3f), shape = RoundedCornerShape(24.dp)) {
            Text(if (recording) "Recording..." else "Scanning...", color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        Surface(modifier = Modifier.padding(bottom = 60.dp, start = 20.dp).size(70.dp).align(Alignment.BottomStart), color = Color.Black.copy(alpha = .3f), shape = RoundedCornerShape(12.dp), onClick = { photoPickerLauncher.launch("image/*") }) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(32.dp)) }
        }

        Surface(modifier = Modifier.padding(bottom = 58.dp).size(86.dp).align(Alignment.BottomCenter), color = Color.White.copy(alpha = .16f), shape = CircleShape, onClick = { recording = !recording }) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.RadioButtonChecked, "Capture", tint = if (recording) Color.Red else Color.White, modifier = Modifier.size(64.dp)) }
        }

        RadiusDisplay(Modifier.padding(bottom = 60.dp, end = 20.dp).align(Alignment.BottomEnd), radius = 0)
    }
}

@Composable
private fun CameraPill(onClick: () -> Unit, active: Boolean = false, content: @Composable () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = if (active) Color(0xFFFF2D8A) else Color.Black.copy(alpha = .35f), modifier = Modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

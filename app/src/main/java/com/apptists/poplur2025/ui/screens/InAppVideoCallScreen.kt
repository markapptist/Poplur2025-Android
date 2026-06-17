package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.call.CallSessionManager
import kotlinx.coroutines.delay

private enum class VideoCallPhase { Connecting, Connected }

private enum class VideoFilter(val label: String) {
    None("None"),
    Warm("Warm"),
    Cool("Cool"),
    Beauty("Beauty"),
    Vintage("Vintage"),
    Glow("Glow")
}

@Composable
fun InAppVideoCallScreen(
    calleeName: String,
    onEnd: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var phase by remember { mutableStateOf(VideoCallPhase.Connecting) }
    var isMuted by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsed by remember { mutableIntStateOf(0) }
    var activeFilter by remember { mutableStateOf(VideoFilter.None) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        delay(2_000)
        phase = VideoCallPhase.Connected
    }

    LaunchedEffect(phase) {
        if (phase == VideoCallPhase.Connected) {
            while (true) {
                delay(1_000)
                elapsed++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Remote video area (placeholder — swap for Agora remote SurfaceView)
        RemoteVideoArea(calleeName = calleeName, phase = phase)

        // Local camera PiP with filter overlay
        if (cameraPermissionGranted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .width(110.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            ) {
                LocalCameraPip(
                    useFrontCamera = useFrontCamera,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.fillMaxSize()
                )
                // Color-grade filter overlay
                filterColorOverlay(activeFilter)?.let { overlayColor ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(overlayColor)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .width(110.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E24))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Timer overlay
        if (phase == VideoCallPhase.Connected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = CallSessionManager.formattedDuration(elapsed),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // AR filter strip
        if (phase == VideoCallPhase.Connected) {
            FilterStrip(
                activeFilter = activeFilter,
                onSelect = { activeFilter = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
            )
        }

        // Controls bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoCallControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                tint = if (isMuted) Color(0xFFE53935) else Color(0xFF38383E)
            ) { isMuted = !isMuted }

            VideoCallControlButton(
                icon = Icons.Default.CallEnd,
                label = "End",
                tint = Color(0xFFE53935),
                size = 72
            ) { onEnd() }

            VideoCallControlButton(
                icon = Icons.Default.FlipCameraAndroid,
                label = "Flip",
                tint = Color(0xFF38383E)
            ) { useFrontCamera = !useFrontCamera }
        }
    }
}

@Composable
private fun RemoteVideoArea(calleeName: String, phase: VideoCallPhase) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111116)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (phase == VideoCallPhase.Connecting) {
                ConnectingAvatar(calleeName)
            } else {
                // Remote stream slot — replace Box with Agora SurfaceView when integrated
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color(0xFF252530), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = calleeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = calleeName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConnectingAvatar(calleeName: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale1 by transition.animateFloat(1f, 1.4f, infiniteRepeatable(tween(1600, easing = EaseOut), RepeatMode.Restart), label = "s1")
    val scale2 by transition.animateFloat(1f, 1.4f, infiniteRepeatable(tween(1600, easing = EaseOut, delayMillis = 500), RepeatMode.Restart), label = "s2")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            Box(
                modifier = Modifier.size(130.dp).scale(scale2)
                    .background(Color.White.copy(alpha = 0.06f * (1.4f - scale2)), CircleShape)
            )
            Box(
                modifier = Modifier.size(130.dp).scale(scale1)
                    .background(Color.White.copy(alpha = 0.09f * (1.4f - scale1)), CircleShape)
            )
            Box(
                modifier = Modifier.size(96.dp).background(Color(0xFF252530), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = calleeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(calleeName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Connecting…", color = Color.White.copy(alpha = 0.55f), fontSize = 16.sp)
    }
}

@Composable
private fun LocalCameraPip(
    useFrontCamera: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
                               else CameraSelector.DEFAULT_BACK_CAMERA
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            }, ContextCompat.getMainExecutor(context))
        },
        modifier = modifier
    )
}

@Composable
private fun VideoCallControlButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    size: Int = 60,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(tint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size((size * 0.45f).dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilterStrip(
    activeFilter: VideoFilter,
    onSelect: (VideoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = VideoFilter.values()
    val icons = mapOf(
        VideoFilter.None to Icons.Default.CameraAlt,
        VideoFilter.Warm to Icons.Default.WbSunny,
        VideoFilter.Cool to Icons.Default.AcUnit,
        VideoFilter.Beauty to Icons.Default.AutoAwesome,
        VideoFilter.Vintage to Icons.Default.CameraAlt,
        VideoFilter.Glow to Icons.Default.Flare
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(filter) }
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (activeFilter == filter) Color.White else Color.White.copy(alpha = 0.18f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icons[filter] ?: Icons.Default.CameraAlt,
                        contentDescription = filter.label,
                        tint = if (activeFilter == filter) Color.Black else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = filter.label,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun filterColorOverlay(filter: VideoFilter): Color? = when (filter) {
    VideoFilter.None    -> null
    VideoFilter.Warm    -> Color(0x26FF8800)
    VideoFilter.Cool    -> Color(0x220044FF)
    VideoFilter.Beauty  -> Color(0x18FF88CC)
    VideoFilter.Vintage -> Color(0x3D664422)
    VideoFilter.Glow    -> Color(0x1AFFFFAA)
}

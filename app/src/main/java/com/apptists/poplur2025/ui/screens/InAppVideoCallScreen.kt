package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.call.CallSessionManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face as MLFace
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private enum class VideoCallPhase { Connecting, Connected }

private enum class VideoFilter(val label: String) {
    None("None"),
    Smooth("Smooth"),
    Beauty("Beauty"),
    Brighten("Brighten"),
    Glow("Glow"),
    Warm("Warm"),
    Cool("Cool"),
    Vintage("Vintage"),
    Sharpen("Sharp")
}

@Composable
fun InAppVideoCallScreen(
    calleeName: String,
    onEnd: () -> Unit
) {
    val context = LocalContext.current

    var phase by remember { mutableStateOf(VideoCallPhase.Connecting) }
    var isMuted by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var elapsed by remember { mutableIntStateOf(0) }
    var activeFilter by remember { mutableStateOf(VideoFilter.None) }
    var pipExpanded by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var microphonePermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        cameraPermissionGranted = grants[Manifest.permission.CAMERA] ?: cameraPermissionGranted
        microphonePermissionGranted = grants[Manifest.permission.RECORD_AUDIO] ?: microphonePermissionGranted
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted || !microphonePermissionGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
        delay(1_600)
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
        RemoteVideoArea(calleeName = calleeName, phase = phase)

        if (cameraPermissionGranted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .width(if (pipExpanded) 154.dp else 112.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .clickable { pipExpanded = !pipExpanded }
            ) {
                VideoCallCameraTile(
                    useFrontCamera = useFrontCamera,
                    activeFilter = activeFilter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .width(112.dp)
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E24))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.42f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (phase == VideoCallPhase.Connected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.38f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = CallSessionManager.formattedDuration(elapsed),
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            FilterStrip(
                activeFilter = activeFilter,
                onSelect = { activeFilter = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
            )
        }

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
            .background(Color(0xFF0C0D10)),
        contentAlignment = Alignment.Center
    ) {
        if (phase == VideoCallPhase.Connecting) {
            ConnectingAvatar(calleeName)
        } else {
            RemoteParticipantStage(calleeName = calleeName)
        }
    }
}

@Composable
private fun RemoteParticipantStage(calleeName: String) {
    val transition = rememberInfiniteTransition(label = "remote-stage")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF08090D),
                        Color(0xFF17111F),
                        Color(0xFF071516)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(430.dp)
                .offset(x = (-90f + 18f * drift).dp, y = (-150).dp)
                .background(Color(0x552B5CFF), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(x = (100f - 16f * drift).dp, y = 180.dp)
                .background(Color(0x33F85D91), CircleShape)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(172.dp)
                    .height(214.dp)
                    .clip(RoundedCornerShape(46.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(46.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .offset(y = (-22).dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = calleeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(calleeName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).background(Color(0xFF61FF78), CircleShape))
                Text(
                    text = "Live video",
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ConnectingAvatar(calleeName: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale1 by transition.animateFloat(
        1f,
        1.4f,
        infiniteRepeatable(tween(1600, easing = EaseOut), RepeatMode.Restart),
        label = "s1"
    )
    val scale2 by transition.animateFloat(
        1f,
        1.4f,
        infiniteRepeatable(tween(1600, easing = EaseOut, delayMillis = 500), RepeatMode.Restart),
        label = "s2"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale2)
                    .background(Color.White.copy(alpha = 0.06f * (1.4f - scale2)), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale1)
                    .background(Color.White.copy(alpha = 0.09f * (1.4f - scale1)), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF252530), CircleShape),
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
        Text("Connecting...", color = Color.White.copy(alpha = 0.55f), fontSize = 16.sp)
    }
}

@Composable
private fun VideoCallCameraTile(
    useFrontCamera: Boolean,
    activeFilter: VideoFilter,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzingFrame = remember { AtomicBoolean(false) }
    val faceDetector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )
    }

    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    var boundFrontCamera by remember { mutableStateOf<Boolean?>(null) }
    var detectedFaces by remember { mutableStateOf<List<VideoCallDetectedFace>>(emptyList()) }
    var analysisSize by remember { mutableStateOf(IntSize.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    val currentProvider by rememberUpdatedState(provider)
    val currentAnalysis by rememberUpdatedState(analysisUseCase)
    val currentFaceDetector by rememberUpdatedState(faceDetector)

    DisposableEffect(Unit) {
        onDispose {
            currentAnalysis?.clearAnalyzer()
            currentProvider?.unbindAll()
            analysisExecutor.shutdown()
            currentFaceDetector.close()
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            update = { previewView ->
                if (boundFrontCamera == useFrontCamera && provider != null) return@AndroidView

                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    val cameraProvider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage == null || !analyzingFrame.compareAndSet(false, true)) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val frameSize = IntSize(imageProxy.width, imageProxy.height)
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                faceDetector.process(inputImage)
                                    .addOnSuccessListener(mainExecutor) { faces ->
                                        analysisSize = frameSize
                                        detectedFaces = faces.map { it.toVideoCallDetectedFace(useFrontCamera) }
                                    }
                                    .addOnFailureListener(mainExecutor) {
                                        detectedFaces = emptyList()
                                    }
                                    .addOnCompleteListener(mainExecutor) {
                                        analyzingFrame.set(false)
                                        imageProxy.close()
                                    }
                            }
                        }
                    val selector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    runCatching {
                        analysisUseCase?.clearAnalyzer()
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                        provider = cameraProvider
                        analysisUseCase = analysis
                        boundFrontCamera = useFrontCamera
                    }.onFailure {
                        runCatching {
                            analysisUseCase?.clearAnalyzer()
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                            provider = cameraProvider
                            analysisUseCase = null
                            boundFrontCamera = useFrontCamera
                            detectedFaces = emptyList()
                        }
                    }
                }, mainExecutor)
            },
            modifier = Modifier.fillMaxSize()
        )

        filterColorOverlay(activeFilter)?.let { overlayColor ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor)
            )
        }

        VideoCallArOverlay(
            faces = detectedFaces,
            imageSize = analysisSize,
            overlaySize = overlaySize,
            mirrored = useFrontCamera,
            selectedFilter = activeFilter,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { overlaySize = it }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.48f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (activeFilter == VideoFilter.None) "You" else activeFilter.label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun VideoCallArOverlay(
    faces: List<VideoCallDetectedFace>,
    imageSize: IntSize,
    overlaySize: IntSize,
    mirrored: Boolean,
    selectedFilter: VideoFilter,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (selectedFilter == VideoFilter.None) return@Canvas

        if (faces.isEmpty() || imageSize.width <= 0 || imageSize.height <= 0 ||
            overlaySize.width <= 0 || overlaySize.height <= 0
        ) {
            if (selectedFilter == VideoFilter.Beauty || selectedFilter == VideoFilter.Glow) {
                drawSparkle(Offset(size.width * 0.28f, size.height * 0.24f), 9.dp.toPx(), selectedFilter.sparkleColor())
                drawSparkle(Offset(size.width * 0.72f, size.height * 0.32f), 7.dp.toPx(), selectedFilter.sparkleColor())
                drawSparkle(Offset(size.width * 0.38f, size.height * 0.70f), 8.dp.toPx(), selectedFilter.sparkleColor())
            }
            return@Canvas
        }

        faces.forEach { face ->
            val box = face.bounds.toVideoCallOverlayRect(
                imageWidth = imageSize.width.toFloat(),
                imageHeight = imageSize.height.toFloat(),
                viewWidth = size.width,
                viewHeight = size.height,
                mirrored = mirrored
            )

            val accent = selectedFilter.accentColor()
            drawRoundRect(
                color = accent.copy(alpha = if (selectedFilter == VideoFilter.Sharpen) 0.55f else 0.22f),
                topLeft = Offset(box.left, box.top),
                size = Size(box.width, box.height),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = if (selectedFilter == VideoFilter.Sharpen) 2.dp.toPx() else 1.2.dp.toPx())
            )

            when (selectedFilter) {
                VideoFilter.Smooth -> {
                    drawOval(
                        color = Color.White.copy(alpha = 0.09f),
                        topLeft = Offset(box.left + box.width * 0.12f, box.top + box.height * 0.08f),
                        size = Size(box.width * 0.76f, box.height * 0.70f)
                    )
                }
                VideoFilter.Beauty -> {
                    drawCircle(
                        color = Color(0xFFFF78B8).copy(alpha = 0.22f),
                        radius = box.width * 0.08f,
                        center = Offset(box.left + box.width * 0.30f, box.top + box.height * 0.58f)
                    )
                    drawCircle(
                        color = Color(0xFFFF78B8).copy(alpha = 0.22f),
                        radius = box.width * 0.08f,
                        center = Offset(box.left + box.width * 0.70f, box.top + box.height * 0.58f)
                    )
                    drawSparkle(Offset(box.left + box.width * 0.18f, box.top - box.height * 0.08f), 8.dp.toPx(), accent)
                    drawSparkle(Offset(box.right - box.width * 0.10f, box.top + box.height * 0.08f), 7.dp.toPx(), accent)
                }
                VideoFilter.Brighten -> {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.10f),
                        topLeft = Offset(box.left, box.top),
                        size = Size(box.width, box.height),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                }
                VideoFilter.Glow -> {
                    drawCircle(
                        color = Color(0xFFFFF176).copy(alpha = 0.18f),
                        radius = box.width * 0.62f,
                        center = Offset(box.center.x, box.center.y)
                    )
                    drawSparkle(Offset(box.left + box.width * 0.12f, box.top + box.height * 0.10f), 9.dp.toPx(), accent)
                    drawSparkle(Offset(box.right - box.width * 0.16f, box.bottom - box.height * 0.16f), 8.dp.toPx(), accent)
                }
                VideoFilter.Sharpen -> {
                    val stepX = box.width / 3f
                    val stepY = box.height / 4f
                    for (i in 1..2) {
                        val x = box.left + i * stepX
                        drawLine(accent.copy(alpha = 0.20f), Offset(x, box.top), Offset(x, box.bottom), 1.dp.toPx())
                    }
                    for (i in 1..3) {
                        val y = box.top + i * stepY
                        drawLine(accent.copy(alpha = 0.20f), Offset(box.left, y), Offset(box.right, y), 1.dp.toPx())
                    }
                }
                VideoFilter.Warm,
                VideoFilter.Cool,
                VideoFilter.Vintage,
                VideoFilter.None -> Unit
            }
        }
    }
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
        VideoFilter.None to Icons.Default.Close,
        VideoFilter.Smooth to Icons.Default.AutoAwesome,
        VideoFilter.Beauty to Icons.Default.Face,
        VideoFilter.Brighten to Icons.Default.LightMode,
        VideoFilter.Glow to Icons.Default.Flare,
        VideoFilter.Warm to Icons.Default.WbSunny,
        VideoFilter.Cool to Icons.Default.AcUnit,
        VideoFilter.Vintage to Icons.Default.CameraAlt,
        VideoFilter.Sharpen to Icons.Default.AutoAwesome
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(28.dp))
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
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun filterColorOverlay(filter: VideoFilter): Color? = when (filter) {
    VideoFilter.None -> null
    VideoFilter.Smooth -> Color(0x12FFFFFF)
    VideoFilter.Beauty -> Color(0x18FF88CC)
    VideoFilter.Brighten -> Color(0x22FFFFFF)
    VideoFilter.Glow -> Color(0x1AFFFFAA)
    VideoFilter.Warm -> Color(0x26FF8800)
    VideoFilter.Cool -> Color(0x220044FF)
    VideoFilter.Vintage -> Color(0x3D664422)
    VideoFilter.Sharpen -> null
}

private fun VideoFilter.accentColor(): Color = when (this) {
    VideoFilter.None -> Color.Transparent
    VideoFilter.Smooth -> Color.White
    VideoFilter.Beauty -> Color(0xFFFF78B8)
    VideoFilter.Brighten -> Color.White
    VideoFilter.Glow -> Color(0xFFFFF176)
    VideoFilter.Warm -> Color(0xFFFF9D4D)
    VideoFilter.Cool -> Color(0xFF70A8FF)
    VideoFilter.Vintage -> Color(0xFFD8A565)
    VideoFilter.Sharpen -> Color.White
}

private fun VideoFilter.sparkleColor(): Color = when (this) {
    VideoFilter.Glow -> Color(0xFFFFF176)
    else -> Color(0xFFFF78B8)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.22f, center.y - radius * 0.22f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.22f, center.y + radius * 0.22f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.22f, center.y + radius * 0.22f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.22f, center.y - radius * 0.22f)
        close()
    }
    drawPath(path = path, color = color.copy(alpha = 0.92f))
}

private fun Rect.toVideoCallOverlayRect(
    imageWidth: Float,
    imageHeight: Float,
    viewWidth: Float,
    viewHeight: Float,
    mirrored: Boolean
): ComposeRect {
    val scale = maxOf(viewWidth / imageWidth, viewHeight / imageHeight)
    val scaledWidth = imageWidth * scale
    val scaledHeight = imageHeight * scale
    val offsetX = (viewWidth - scaledWidth) / 2f
    val offsetY = (viewHeight - scaledHeight) / 2f
    val leftRaw = left * scale + offsetX
    val rightRaw = right * scale + offsetX
    val leftOut = if (mirrored) viewWidth - rightRaw else leftRaw
    val rightOut = if (mirrored) viewWidth - leftRaw else rightRaw
    return ComposeRect(
        left = leftOut,
        top = top * scale + offsetY,
        right = rightOut,
        bottom = bottom * scale + offsetY
    )
}

private fun MLFace.toVideoCallDetectedFace(mirrored: Boolean): VideoCallDetectedFace {
    return VideoCallDetectedFace(
        bounds = boundingBox,
        trackingId = trackingId,
        smilingProbability = smilingProbability,
        mirrored = mirrored
    )
}

private data class VideoCallDetectedFace(
    val bounds: Rect,
    val trackingId: Int?,
    val smilingProbability: Float?,
    val mirrored: Boolean
)

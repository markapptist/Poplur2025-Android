package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

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
    var selectedFilter by remember { mutableStateOf(ArFilter.None) }
    var detectedFaces by remember { mutableStateOf<List<DetectedFace>>(emptyList()) }
    var analysisSize by remember { mutableStateOf(IntSize.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var isAnalyzingFrame by remember { mutableStateOf(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
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
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedMedia = uri }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            faceDetector.close()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                },
                update = { view ->
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { imageAnalysis ->
                                imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage == null || isAnalyzingFrame) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    isAnalyzingFrame = true
                                    analysisSize = IntSize(imageProxy.width, imageProxy.height)
                                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    faceDetector.process(inputImage)
                                        .addOnSuccessListener { faces ->
                                            detectedFaces = faces.map { it.toDetectedFace(useFrontCamera) }
                                        }
                                        .addOnFailureListener {
                                            detectedFaces = emptyList()
                                        }
                                        .addOnCompleteListener {
                                            isAnalyzingFrame = false
                                            imageProxy.close()
                                        }
                                }
                            }
                        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            provider.unbindAll()
                            val camera = provider.bindToLifecycle(owner, selector, preview, analysis)
                            camera.cameraControl.enableTorch(torchEnabled && !useFrontCamera)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Enable Camera")
                }
            }
        }

        FaceArOverlay(
            faces = detectedFaces,
            imageSize = analysisSize,
            overlaySize = overlaySize,
            mirrored = useFrontCamera,
            selectedFilter = selectedFilter,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { overlaySize = it }
        )

        CameraStatusPill(
            faceCount = detectedFaces.size,
            isRecording = recording,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 14.dp, start = 16.dp)
                .align(Alignment.TopStart)
        )

        Row(
            Modifier
                .statusBarsPadding()
                .padding(top = 14.dp, end = 16.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CameraPill(
                onClick = { torchEnabled = !torchEnabled },
                active = torchEnabled && !useFrontCamera,
                enabled = !useFrontCamera
            ) {
                Icon(if (torchEnabled && !useFrontCamera) Icons.Default.FlashOn else Icons.Default.FlashOff, null, tint = Color.White)
            }
            CameraPill(onClick = { useFrontCamera = !useFrontCamera; torchEnabled = false }) {
                Icon(Icons.Default.Cameraswitch, null, tint = Color.White)
            }
        }

        ArFilterSelector(
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 190.dp)
        )

        Surface(
            modifier = Modifier
                .padding(bottom = 62.dp, start = 20.dp)
                .size(58.dp)
                .align(Alignment.BottomStart),
            color = Color.Black.copy(alpha = .34f),
            shape = RoundedCornerShape(12.dp),
            onClick = { photoPickerLauncher.launch("image/*") }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Surface(
            modifier = Modifier
                .padding(bottom = 70.dp)
                .size(82.dp)
                .align(Alignment.BottomCenter),
            color = Color.White.copy(alpha = if (recording) .22f else .16f),
            shape = CircleShape,
            onClick = { recording = !recording }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.RadioButtonChecked,
                    "Capture",
                    tint = if (recording) Color(0xFFFF3B30) else Color.White,
                    modifier = Modifier.size(62.dp)
                )
            }
        }

        RadiusDisplay(
            Modifier
                .padding(bottom = 48.dp, end = 20.dp)
                .align(Alignment.BottomEnd),
            radius = 0
        )

        selectedMedia?.let {
            Text(
                it.lastPathSegment ?: "Selected media",
                color = Color.White.copy(alpha = .82f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 132.dp)
            )
        }
    }
}

@Composable
private fun CameraStatusPill(faceCount: Int, isRecording: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = .38f),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Face, null, tint = Color.White, modifier = Modifier.size(19.dp))
            Text(
                when {
                    isRecording -> "Recording"
                    faceCount == 0 -> "Scanning"
                    faceCount == 1 -> "1 face"
                    else -> "$faceCount faces"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ArFilterSelector(selected: ArFilter, onSelect: (ArFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ArFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = { Text(filter.label, fontWeight = FontWeight.Bold) },
                leadingIcon = { Text(filter.icon) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Black.copy(alpha = .36f),
                    labelColor = Color.White,
                    iconColor = Color.White,
                    selectedContainerColor = Color(0xFFFF2D8A).copy(alpha = .86f),
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == filter,
                    borderColor = Color.White.copy(alpha = .34f),
                    selectedBorderColor = Color.White.copy(alpha = .74f)
                )
            )
        }
    }
}

@Composable
private fun FaceArOverlay(
    faces: List<DetectedFace>,
    imageSize: IntSize,
    overlaySize: IntSize,
    mirrored: Boolean,
    selectedFilter: ArFilter,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (imageSize.width <= 0 || imageSize.height <= 0 || overlaySize.width <= 0 || overlaySize.height <= 0) return@Canvas

        faces.forEach { face ->
            val box = face.bounds.toOverlayRect(
                imageWidth = imageSize.width.toFloat(),
                imageHeight = imageSize.height.toFloat(),
                viewWidth = size.width,
                viewHeight = size.height,
                mirrored = mirrored
            )

            drawRoundRect(
                color = Color(0xFF66E7FF).copy(alpha = .84f),
                topLeft = Offset(box.left, box.top),
                size = Size(box.width, box.height),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 2.4.dp.toPx())
            )

            when (selectedFilter) {
                ArFilter.None -> Unit
                ArFilter.Spark -> drawSparkFilter(box)
                ArFilter.Crown -> drawCrownFilter(box)
                ArFilter.Visor -> drawVisorFilter(box)
                ArFilter.Pop -> drawPopFilter(box)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkFilter(face: ComposeRect) {
    val center = Offset(face.center.x, face.top - face.height * .08f)
    repeat(7) { index ->
        val x = face.left + face.width * (.12f + index * .13f)
        val y = center.y + if (index % 2 == 0) -12.dp.toPx() else 10.dp.toPx()
        drawCircle(Color(0xFFFFD84A).copy(alpha = .9f), 5.dp.toPx(), Offset(x, y))
        drawCircle(Color(0xFFFF2D8A).copy(alpha = .7f), 2.6.dp.toPx(), Offset(x + 8.dp.toPx(), y - 8.dp.toPx()))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrownFilter(face: ComposeRect) {
    val crownHeight = face.height * .25f
    val baseY = face.top - face.height * .08f
    val crown = Path().apply {
        moveTo(face.left + face.width * .14f, baseY)
        lineTo(face.left + face.width * .26f, baseY - crownHeight)
        lineTo(face.left + face.width * .39f, baseY)
        lineTo(face.left + face.width * .5f, baseY - crownHeight * 1.18f)
        lineTo(face.left + face.width * .61f, baseY)
        lineTo(face.left + face.width * .74f, baseY - crownHeight)
        lineTo(face.left + face.width * .86f, baseY)
        close()
    }
    drawPath(crown, Color(0xFFFF2D8A).copy(alpha = .72f))
    drawPath(crown, Color.White.copy(alpha = .75f), style = Stroke(width = 2.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVisorFilter(face: ComposeRect) {
    val visorTop = face.top + face.height * .34f
    val visorHeight = face.height * .18f
    drawRoundRect(
        color = Color(0xFF111827).copy(alpha = .72f),
        topLeft = Offset(face.left + face.width * .18f, visorTop),
        size = Size(face.width * .64f, visorHeight),
        cornerRadius = CornerRadius(visorHeight / 2f, visorHeight / 2f)
    )
    drawRoundRect(
        color = Color(0xFF66E7FF).copy(alpha = .52f),
        topLeft = Offset(face.left + face.width * .22f, visorTop + visorHeight * .2f),
        size = Size(face.width * .56f, visorHeight * .22f),
        cornerRadius = CornerRadius(visorHeight / 2f, visorHeight / 2f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPopFilter(face: ComposeRect) {
    val bubbleSize = face.width * .22f
    drawCircle(Color(0xFFFF2D8A).copy(alpha = .72f), bubbleSize, Offset(face.left + face.width * .18f, face.top + face.height * .18f))
    drawCircle(Color(0xFF66E7FF).copy(alpha = .62f), bubbleSize * .72f, Offset(face.right - face.width * .16f, face.top + face.height * .22f))
    drawCircle(Color.White.copy(alpha = .8f), bubbleSize * .18f, Offset(face.left + face.width * .22f, face.top + face.height * .12f))
}

private fun Rect.toOverlayRect(
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

private fun Face.toDetectedFace(mirrored: Boolean): DetectedFace {
    return DetectedFace(
        bounds = boundingBox,
        trackingId = trackingId,
        smilingProbability = smilingProbability,
        mirrored = mirrored
    )
}

private data class DetectedFace(
    val bounds: Rect,
    val trackingId: Int?,
    val smilingProbability: Float?,
    val mirrored: Boolean
)

private enum class ArFilter(val label: String, val icon: String) {
    None("Clean", "Off"),
    Spark("Spark", "*"),
    Crown("Crown", "^"),
    Visor("Visor", "[]"),
    Pop("Pop", "P")
}

@Composable
private fun CameraPill(
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = when {
            active -> Color(0xFFFF2D8A)
            enabled -> Color.Black.copy(alpha = .38f)
            else -> Color.Black.copy(alpha = .18f)
        },
        modifier = Modifier.size(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.ShaderNumberText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face as MLFace
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun CameraScreen(
    photoTrigger: Int = 0,
    recordTrigger: Int = 0,
    initialShowFilters: Boolean = false,
    onFiltersToggle: (Boolean) -> Unit = {},
    onMediaCaptured: (Uri, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val isPreview = LocalInspectionMode.current
    var hasCameraPermission by remember {
        mutableStateOf(if (isPreview) true else ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var frontFlashBurst by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var recordingSecondsRemaining by remember { mutableIntStateOf(10) }
    var captureInFlight by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var selectedFilter by remember { mutableStateOf(ArFilter.Beauty) }
    var showFilterPicker by remember { mutableStateOf(initialShowFilters) }
    
    LaunchedEffect(showFilterPicker) {
        onFiltersToggle(showFilterPicker)
    }

    var captureOutput by remember { mutableStateOf<String?>(null) }
    var detectedFaces by remember { mutableStateOf<List<DetectedFace>>(emptyList()) }
    var analysisSize by remember { mutableStateOf(IntSize.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var isAnalyzingFrame by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var imageAnalysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val scope = rememberCoroutineScope()
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val shutterSound = remember {
        if (isPreview) null else CameraShutter()
    }
    val faceDetector = remember {
        if (isPreview) null else FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedMedia = uri }
    val toggleCamera = {
        useFrontCamera = !useFrontCamera
        torchEnabled = false
        frontFlashBurst = false
        detectedFaces = emptyList()
    }
    val toggleFlash = {
        val nextFlashState = !torchEnabled
        torchEnabled = nextFlashState
        captureOutput = if (nextFlashState) "Flash on" else "Flash off"
    }
    val releaseCameraUseCases = {
        runCatching { imageAnalysisUseCase?.clearAnalyzer() }
        runCatching { cameraProvider?.unbindAll() }
        boundCamera = null
        imageCapture = null
        imageAnalysisUseCase = null
        videoCapture = null
        detectedFaces = emptyList()
        isAnalyzingFrame = false
    }
    val capturePhoto = {
        val capture = imageCapture
        if (captureInFlight) {
            Unit
        } else if (capture == null) {
            captureOutput = "Camera not ready"
        } else {
            captureInFlight = true
            shutterSound?.play()
            val name = "Poplur_${timestampName()}"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Poplur")
            }
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ).build()
            capture.takePicture(
                outputOptions,
                mainExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val uri = outputFileResults.savedUri
                        captureOutput = "Photo captured"
                        if (uri != null) {
                            releaseCameraUseCases()
                            scope.launch {
                                delay(120)
                                onMediaCaptured(uri, false)
                            }
                        } else {
                            captureInFlight = false
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        captureInFlight = false
                        captureOutput = "Photo failed"
                        exception.printStackTrace()
                    }
                }
            )
        }
    }
    val startTenSecondRecording: () -> Recording? = {
        val video = videoCapture
        if (video == null) {
            captureOutput = "Video not ready"
            null
        } else if (activeRecording == null) {
            val name = "Poplur_${timestampName()}"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Poplur")
            }
            val outputOptions = MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(values).build()
            
            @OptIn(androidx.camera.video.ExperimentalAudioApi::class)
            var pendingRecording = video.output.prepareRecording(context, outputOptions)
            
            val hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasAudioPermission) {
                pendingRecording = pendingRecording.withAudioEnabled()
            }
            
            recording = true
            recordingSecondsRemaining = 10
            captureOutput = "Recording"
            val startedRecording = pendingRecording.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Finalize -> {
                        recording = false
                        recordingSecondsRemaining = 0
                        activeRecording = null
                        if (!event.hasError()) {
                            val uri = event.outputResults.outputUri
                            captureOutput = "Video captured"
                            releaseCameraUseCases()
                            scope.launch {
                                delay(120)
                                onMediaCaptured(uri, true)
                            }
                        } else {
                            captureOutput = "Video failed"
                        }
                    }
                }
            }
            activeRecording = startedRecording
            startedRecording
        } else {
            activeRecording
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    LaunchedEffect(photoTrigger) {
        if (photoTrigger > 0) {
            activeRecording?.stop()
            if (useFrontCamera && torchEnabled) {
                frontFlashBurst = true
                delay(180)
            }
            capturePhoto()
            if (useFrontCamera && torchEnabled) {
                delay(180)
                frontFlashBurst = false
            }
        }
    }

    LaunchedEffect(recordTrigger) {
        if (recordTrigger > 0) {
            if (recording) return@LaunchedEffect
            val startedRecording = startTenSecondRecording()
            if (startedRecording != null) {
                for (remaining in 10 downTo 1) {
                    recordingSecondsRemaining = remaining
                    delay(1_000)
                }
                recordingSecondsRemaining = 0
                startedRecording.stop()
            }
        }
    }

    LaunchedEffect(torchEnabled, boundCamera, useFrontCamera) {
        boundCamera?.cameraControl?.enableTorch(!useFrontCamera && torchEnabled)
    }

    LaunchedEffect(captureOutput) {
        if (captureOutput != null) {
            delay(2000)
            captureOutput = null
        }
    }

    val currentActiveRecording by rememberUpdatedState(activeRecording)
    val currentImageAnalysisUseCase by rememberUpdatedState(imageAnalysisUseCase)
    val currentCameraProvider by rememberUpdatedState(cameraProvider)
    val currentFaceDetector by rememberUpdatedState(faceDetector)

    DisposableEffect(Unit) {
        onDispose {
            currentActiveRecording?.close()
            currentImageAnalysisUseCase?.clearAnalyzer()
            currentCameraProvider?.unbindAll()
            analysisExecutor.shutdown()
            currentFaceDetector?.close()
        }
    }

    DisposableEffect(shutterSound) {
        onDispose {
            shutterSound?.release()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { /* Capture is handled ONLY via the navigation cube in PoplurRoot */ },
                        onDoubleTap = { toggleCamera() },
                        onLongPress = {
                            showFilterPicker = true
                        }
                    )
                }
                .pointerInput(useFrontCamera, torchEnabled) {
                    var totalDragY = 0f
                    detectDragGestures(
                        onDragStart = { totalDragY = 0f },
                        onDrag = { change, dragAmount ->
                            totalDragY += dragAmount.y
                            change.consume()
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(totalDragY) > 70f) {
                                toggleFlash()
                            }
                        }
                    )
                }
        ) {
            if (hasCameraPermission) {
                if (isPreview) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Camera Preview Placeholder", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    key(useFrontCamera) {
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
                                    cameraProvider = provider
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                                    val photo = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    val recorder = Recorder.Builder()
                                        .setQualitySelector(
                                            QualitySelector.from(
                                                Quality.HD,
                                                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                                            )
                                        )
                                        .build()
                                    val video = VideoCapture.withOutput(recorder)
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { imageAnalysis ->
                                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                                @androidx.camera.core.ExperimentalGetImage
                                                val mediaImage = imageProxy.image
                                                if (mediaImage == null || isAnalyzingFrame) {
                                                    imageProxy.close()
                                                    return@setAnalyzer
                                                }
                                                isAnalyzingFrame = true
                                                analysisSize = IntSize(imageProxy.width, imageProxy.height)
                                                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                faceDetector?.process(inputImage)
                                                    ?.addOnSuccessListener { faces ->
                                                        detectedFaces = faces.map { it.toDetectedFace(useFrontCamera) }
                                                    }
                                                    ?.addOnFailureListener {
                                                        detectedFaces = emptyList()
                                                    }
                                                    ?.addOnCompleteListener {
                                                        isAnalyzingFrame = false
                                                        imageProxy.close()
                                                    }
                                            }
                                        }
                                    val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        imageAnalysisUseCase?.clearAnalyzer()
                                        provider.unbindAll()
                                        boundCamera = provider.bindToLifecycle(owner, selector, preview, photo, video, analysis)
                                        imageCapture = photo
                                        imageAnalysisUseCase = analysis
                                        videoCapture = video
                                    } catch (e: Exception) {
                                        try {
                                            imageAnalysisUseCase?.clearAnalyzer()
                                            provider.unbindAll()
                                            boundCamera = provider.bindToLifecycle(owner, selector, preview, photo, video)
                                            imageCapture = photo
                                            imageAnalysisUseCase = null
                                            videoCapture = video
                                            detectedFaces = emptyList()
                                        } catch (fallback: Exception) {
                                            fallback.printStackTrace()
                                            imageCapture = null
                                            imageAnalysisUseCase = null
                                            videoCapture = null
                                        }
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            }
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA)) }) {
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

            if (useFrontCamera && (torchEnabled || frontFlashBurst)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = if (frontFlashBurst) 0.92f else 0.34f))
                )
            }
        }

        // Top Left: Flash Control
        IconButton(
            onClick = { toggleFlash() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 14.dp, start = 16.dp)
                .align(Alignment.TopStart),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(
                if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                "Toggle Flash",
                tint = if (torchEnabled) Color.Yellow else Color.White
            )
        }

        // Top Right: Switch Camera
        IconButton(
            onClick = { toggleCamera() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 14.dp, end = 16.dp)
                .align(Alignment.TopEnd),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(Icons.Default.FlipCameraAndroid, "Switch Camera", tint = Color.White)
        }

        if (recording) {
            RecordingTimerBadge(
                secondsRemaining = recordingSecondsRemaining,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, bottom = 88.dp)
            )
        }

        if (showFilterPicker) {
            ArFilterSelector(
                selected = selectedFilter,
                onSelect = {
                    selectedFilter = it
                },
                onDismiss = { showFilterPicker = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 220.dp)
            )
        }

        // Bottom Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 32.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Gallery Thumbnail (Bottom Left)
            if (!recording) {
                Surface(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .size(64.dp),
                    color = Color.Black.copy(alpha = .34f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { photoPickerLauncher.launch("image/*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                // Empty spacer to keep layout if needed, or just let RadiusDisplay align end
                Spacer(Modifier.size(64.dp))
            }

            // Radius (Bottom Right)
            RadiusDisplay(
                radius = 0,
                totalRadius = 6371,
                unit = "km",
                modifier = Modifier.widthIn(min = 112.dp)
            )
        }


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

        captureOutput?.let { output ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 174.dp),
                color = Color.Black.copy(alpha = .58f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    output,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RecordingTimerBadge(
    secondsRemaining: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "RECORDING",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            )
        }

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier.width(92.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShaderNumberText(
                secondsRemaining.coerceAtLeast(0).toString(),
                color = Color.Red,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp,
                intensity = 1.18f,
                autoSize = true
            )
            HorizontalDivider(
                color = Color.White,
                thickness = 2.dp,
                modifier = Modifier.width(70.dp).padding(vertical = 4.dp)
            )
            ShaderNumberText(
                "seconds",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 26.sp,
                intensity = 0.55f,
                autoSize = true
            )
        }
    }
}

@Composable
fun CapturedOutputScreen(
    uri: Uri,
    isVideo: Boolean,
    onBack: () -> Unit,
    onPost: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isVideo) {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(uri)
                            setOnPreparedListener { player ->
                                player.isLooping = true
                                player.setVolume(0.7f, 0.7f)
                                start()
                            }
                        }
                    },
                    update = { view ->
                        view.setVideoURI(uri)
                        view.start()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = "Captured photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isVideo) "VIDEO OUTPUT" else "PHOTO OUTPUT",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }

        // Bottom Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onPost,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Post to Feed", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            
        }
    }
}


@Composable
private fun ArFilterSelector(
    selected: ArFilter,
    onSelect: (ArFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArFilter.entries.forEach { filter ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .clickable { onSelect(filter) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected == filter) {
                                    Brush.linearGradient(listOf(Color(0xFF8E8EFA), Color(0xFFE08EFA)))
                                } else {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))
                                }
                            )
                            .padding(if (selected == filter) 3.dp else 1.5.dp) // Glow/Ring effect
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            filter.iconVector,
                            contentDescription = filter.label,
                            tint = Color.White,
                            modifier = Modifier.size(if (selected == filter) 36.dp else 30.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        filter.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (selected == filter) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
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

            /* Bounding box removed - ML is background only */
            /*
            drawRoundRect(
                color = Color(0xFF66E7FF).copy(alpha = .84f),
                topLeft = Offset(box.left, box.top),
                size = Size(box.width, box.height),
                cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                style = Stroke(width = 2.4.dp.toPx())
            )
            */

            when (selectedFilter) {
                ArFilter.None -> Unit
                ArFilter.Smooth -> Unit // Implement these later
                ArFilter.Beauty -> Unit
                ArFilter.Brighten -> Unit
                ArFilter.Glow -> Unit
            }
        }
    }
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

private fun MLFace.toDetectedFace(mirrored: Boolean): DetectedFace {
    return DetectedFace(
        bounds = boundingBox,
        trackingId = trackingId,
        smilingProbability = smilingProbability,
        mirrored = mirrored
    )
}

private fun timestampName(): String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())


private data class DetectedFace(
    val bounds: Rect,
    val trackingId: Int?,
    val smilingProbability: Float?,
    val mirrored: Boolean
)

private enum class ArFilter(val label: String, val iconVector: ImageVector) {
    None("None", Icons.Default.Close),
    Smooth("Smooth", Icons.Default.AutoAwesome),
    Beauty("Beauty", Icons.Default.Face),
    Brighten("Brighten", Icons.Default.LightMode),
    Glow("Glow", Icons.Default.WbSunny)
}

private class CameraShutter {
    // Use reflection for MediaActionSound to avoid NoClassDefFoundError in Previews
    private val sound: Any? = try {
        val cls = Class.forName("android.media.MediaActionSound")
        val instance = cls.getDeclaredConstructor().newInstance()
        cls.getMethod("load", Int::class.java).invoke(instance, 0) // 0 = SHUTTER_CLICK
        instance
    } catch (e: Exception) {
        null
    }

    fun play() {
        try {
            sound?.let {
                it.javaClass.getMethod("play", Int::class.java).invoke(it, 0)
            }
        } catch (e: Exception) {
            // Ignore sound errors
        }
    }

    fun release() {
        try {
            sound?.let {
                it.javaClass.getMethod("release").invoke(it)
            }
        } catch (e: Exception) {
            // Ignore release errors
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun CameraScreenPreview() {
    MaterialTheme {
        CameraScreen(initialShowFilters = true)
    }
}

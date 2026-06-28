package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import coil.compose.AsyncImage
import com.apptists.poplur2025.data.AdGenerationService
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.FaceAdGenerationService
import com.apptists.poplur2025.data.LiveBroadcastLocation
import com.apptists.poplur2025.data.MusicSearchTrack
import com.apptists.poplur2025.data.OutputMusicMixer
import com.apptists.poplur2025.data.TVStyleAnalyzer
import com.apptists.poplur2025.data.VideoAdGenerationService
import com.apptists.poplur2025.data.TVStyleMetadata
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.MusicSearchSheet
import com.apptists.poplur2025.ui.components.ShaderNumberText
import com.apptists.poplur2025.ui.components.PoplurText
import com.apptists.poplur2025.ui.components.poplurCountdownDisplayModifier
import com.apptists.poplur2025.ui.components.poplurRadiusDisplayModifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face as MLFace
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
    liveTrigger: Int = 0,
    filtersTrigger: Int = 0,
    initialShowFilters: Boolean = false,
    objectDetectionEnabled: Boolean = false,
    currentUserProfile: UserProfile = UserProfile(),
    onFiltersToggle: (Boolean) -> Unit = {},
    onMediaCaptured: (Uri, Boolean) -> Unit = { _, _ -> },
    onObjectDetected: () -> Unit = {}
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
    var isLiveVideoActive by remember { mutableStateOf(false) }
    var liveVideoSeconds by remember { mutableIntStateOf(0) }
    var liveViewerCount by remember { mutableIntStateOf(128) }
    var liveLocationName by remember { mutableStateOf<String?>(null) }
    var showLiveLocationPicker by remember { mutableStateOf(false) }
    var captureInFlight by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<Uri?>(null) }
    var selectedFilter by remember { mutableStateOf(ArFilter.Beauty) }
    var showFilterPicker by remember { mutableStateOf(initialShowFilters) }
    
    LaunchedEffect(showFilterPicker) {
        onFiltersToggle(showFilterPicker)
    }

    var captureOutput by remember { mutableStateOf<String?>(null) }
    var detectedFaces by remember { mutableStateOf<List<DetectedFace>>(emptyList()) }
    var detectedObjectLabels by remember { mutableStateOf<List<com.apptists.poplur2025.data.DetectedObject>>(emptyList()) }
    var lastDetectedLabelKey by remember { mutableStateOf("") }
    var analysisSize by remember { mutableStateOf(IntSize.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var isAnalyzingFrame by remember { mutableStateOf(false) }
    val mlDetectionManager = remember { if (isPreview) null else com.apptists.poplur2025.ml.MLDetectionManager() }
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
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedMedia = uri
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            onMediaCaptured(uri, mimeType.startsWith("video/"))
        }
    }
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
    val stopLiveVideo = {
        if (isLiveVideoActive) {
            isLiveVideoActive = false
            liveVideoSeconds = 0
            liveViewerCount = 0
            liveLocationName = null
            DemoPostStore.stopLocalLiveBroadcast()
            captureOutput = "Live ended"
        }
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
        if (isLiveVideoActive) {
            captureOutput = "End live to take photo"
            Unit
        } else if (captureInFlight) {
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
        if (isLiveVideoActive) {
            captureOutput = "End live to record"
            null
        } else if (video == null) {
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
    val toggleLiveVideo = {
        if (isLiveVideoActive) {
            stopLiveVideo()
        } else {
            showLiveLocationPicker = true
            captureOutput = "Choose live location"
        }
    }
    val startLiveVideoAt: (LiveBroadcastLocation) -> Unit = { location ->
        activeRecording?.stop()
        activeRecording = null
        recording = false
        recordingSecondsRemaining = 0
        liveVideoSeconds = 0
        liveViewerCount = 128
        liveLocationName = location.title
        DemoPostStore.startLiveBroadcast(location, liveViewerCount)
        isLiveVideoActive = true
        showLiveLocationPicker = false
        captureOutput = "Live from ${location.title}"
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
            if (isLiveVideoActive) {
                captureOutput = "End live to take photo"
                return@LaunchedEffect
            }
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
            if (isLiveVideoActive) {
                captureOutput = "End live to record"
                return@LaunchedEffect
            }
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

    LaunchedEffect(liveTrigger) {
        if (liveTrigger > 0) {
            toggleLiveVideo()
        }
    }

    LaunchedEffect(filtersTrigger) {
        if (filtersTrigger > 0) {
            showFilterPicker = true
        }
    }

    LaunchedEffect(isLiveVideoActive) {
        while (isLiveVideoActive) {
            delay(1_000)
            liveVideoSeconds += 1
            liveViewerCount = 128 + ((liveVideoSeconds * 7) % 43)
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

    DisposableEffect(context) {
        if (!isPreview) com.apptists.poplur2025.ml.CameraAIAnnouncer.init(context)
        onDispose { if (!isPreview) com.apptists.poplur2025.ml.CameraAIAnnouncer.shutdown() }
    }

    val aiCameraAnnouncement by com.apptists.poplur2025.ml.CameraAIAnnouncer.announcement.collectAsState()

    LaunchedEffect(detectedFaces, useFrontCamera, currentUserProfile) {
        if (!isPreview) {
            com.apptists.poplur2025.ml.CameraAIAnnouncer.announceFaceDetected(
                faceCount = detectedFaces.size,
                profile = if (useFrontCamera) currentUserProfile else null
            )
        }
    }

    LaunchedEffect(objectDetectionEnabled) {
        if (!objectDetectionEnabled) {
            detectedObjectLabels = emptyList()
            lastDetectedLabelKey = ""
            if (!isPreview) com.apptists.poplur2025.ml.CameraAIAnnouncer.announceMLObjects(emptyList())
        }
    }

    LaunchedEffect(detectedObjectLabels, objectDetectionEnabled) {
        if (!isPreview && objectDetectionEnabled) {
            com.apptists.poplur2025.ml.CameraAIAnnouncer.announceMLObjects(detectedObjectLabels)
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
                        PoplurText("Camera Preview Placeholder", color = Color.White, fontWeight = FontWeight.Bold)
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
                                                val rotation = imageProxy.imageInfo.rotationDegrees

                                                // Count pending async tasks so imageProxy is only closed once both finish
                                                val pending = java.util.concurrent.atomic.AtomicInteger(
                                                    (if (faceDetector != null) 1 else 0) +
                                                        (if (objectDetectionEnabled && mlDetectionManager != null) 1 else 0)
                                                )
                                                val doClose: () -> Unit = {
                                                    if (pending.decrementAndGet() == 0) {
                                                        isAnalyzingFrame = false
                                                        imageProxy.close()
                                                    }
                                                }
                                                if (pending.get() == 0) {
                                                    isAnalyzingFrame = false
                                                    imageProxy.close()
                                                    return@setAnalyzer
                                                }

                                                // ML object detection (best-effort, STREAM_MODE handles dropped frames)
                                                if (objectDetectionEnabled && mlDetectionManager != null) {
                                                    mlDetectionManager.analyze(mediaImage, rotation) { objects ->
                                                        if (objects.isNotEmpty()) {
                                                            detectedObjectLabels = objects
                                                            val key = objects.take(3).joinToString(",") { it.label }
                                                            if (key != lastDetectedLabelKey) {
                                                                lastDetectedLabelKey = key
                                                                onObjectDetected()
                                                            }
                                                        } else if (detectedObjectLabels.isNotEmpty()) {
                                                            detectedObjectLabels = emptyList()
                                                            lastDetectedLabelKey = ""
                                                        }
                                                        doClose()
                                                    }
                                                }

                                                // Face detection
                                                faceDetector?.process(inputImage)
                                                    ?.addOnSuccessListener { faces ->
                                                        detectedFaces = faces.map { it.toDetectedFace(useFrontCamera) }
                                                    }
                                                    ?.addOnFailureListener {
                                                        detectedFaces = emptyList()
                                                    }
                                                    ?.addOnCompleteListener { doClose() }
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
                        PoplurText("Enable Camera")
                    }
                }
            }

            FaceArOverlay(
                faces = detectedFaces,
                imageSize = analysisSize,
                overlaySize = overlaySize,
                mirrored = useFrontCamera,
                selectedFilter = selectedFilter,
                username = if (useFrontCamera) currentUserProfile.username else null,
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

        // Top Center: ML object detection overlay
        if (objectDetectionEnabled) {
            MLObjectShaderOverlay(
                objects = detectedObjectLabels,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 14.dp)
            )
        }

        // Top Left: Flash Control
        IconButton(
            onClick = { toggleFlash() },
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 14.dp, start = 16.dp)
                .align(Alignment.TopStart),
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
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
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
        ) {
            Icon(Icons.Default.FlipCameraAndroid, "Switch Camera", tint = Color.White)
        }

        if (recording) {
            RecordingTimerBadge(
                secondsRemaining = recordingSecondsRemaining,
                modifier = poplurCountdownDisplayModifier()
            )
        }

        if (isLiveVideoActive) {
            LiveVideoBadge(
                seconds = liveVideoSeconds,
                viewers = liveViewerCount,
                locationName = liveLocationName,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 18.dp)
            )
        }

        if (showLiveLocationPicker) {
            LiveLocationPickerSheet(
                locations = DemoPostStore.liveLocationChoices,
                onSelect = startLiveVideoAt,
                onDismiss = {
                    showLiveLocationPicker = false
                    captureOutput = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 120.dp)
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

        // Gallery Thumbnail (Bottom Left)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 32.dp, bottom = 62.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (!recording) {
                Surface(
                    modifier = Modifier
                        .size(64.dp),
                    color = Color.Black.copy(alpha = .34f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { photoPickerLauncher.launch("*/*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                Spacer(Modifier.size(64.dp))
            }
        }

        RadiusDisplay(
            radius = currentUserProfile.radius,
            unit = "m",
            modifier = poplurRadiusDisplayModifier(Modifier.widthIn(min = 112.dp))
        )


        selectedMedia?.let {
            PoplurText(
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
                PoplurText(
                    output,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        // AI Announcer pill — shows briefly when TTS speaks a detection event
        AnimatedVisibility(
            visible = aiCameraAnnouncement != null && !isPreview,
            enter = fadeIn(tween(160)) + slideInVertically { it / 3 },
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 130.dp)
        ) {
            val label = aiCameraAnnouncement ?: ""
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            ) {
                PoplurText(
                    label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
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
            PoplurText(
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
private fun LiveVideoBadge(
    seconds: Int,
    viewers: Int,
    locationName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF2B2B))
                )
                PoplurText(
                    "LIVE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp
                )
            }
            PoplurText(
                formatLiveDuration(seconds),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            PoplurText(
                "$viewers watching",
                color = Color(0xFF4DFF58),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            if (!locationName.isNullOrBlank()) {
                PoplurText(
                    locationName,
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveLocationPickerSheet(
    locations: List<LiveBroadcastLocation>,
    onSelect: (LiveBroadcastLocation) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 18.dp
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xF5080910),
                            Color(0xF522050B),
                            Color(0xF505221F)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF3333).copy(alpha = 0.85f),
                            Color(0xFF18D8E8).copy(alpha = 0.52f),
                            Color.White.copy(alpha = 0.18f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFF3333),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    PoplurText(
                        "Go Live",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    PoplurText(
                        "Choose where the live radius starts",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            locations.take(6).forEach { location ->
                Surface(
                    onClick = { onSelect(location) },
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .height(60.dp)
                            .padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF4DFF58),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            PoplurText(
                                location.title,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            PoplurText(
                                "${location.subtitle} - ${formatLiveRadius(location.radiusMeters)}",
                                color = Color.White.copy(alpha = 0.58f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatLiveRadius(radiusMeters: Int): String =
    when {
        radiusMeters >= 1_000_000 -> String.format(Locale.US, "%.1fM m", radiusMeters / 1_000_000f)
        radiusMeters >= 1_000 -> String.format(Locale.US, "%.0fk m", radiusMeters / 1_000f)
        else -> "$radiusMeters m"
    }

private fun formatLiveDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remaining)
}

// MARK: - TV analysis state

private sealed interface TVState {
    object Idle : TVState
    object Analyzing : TVState
    data class Ready(val meta: TVStyleMetadata) : TVState
    object Failed : TVState
}

@Composable
fun CapturedOutputScreen(
    uri: Uri,
    isVideo: Boolean,
    onBack: () -> Unit,
    filterId: String = "original",
    continueLabel: String = "Next",
    onContinue: () -> Unit = {},
    showCreationOptions: Boolean = false,
    onUseOriginal: () -> Unit = onContinue,
    onUsePoplurAI: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CapturedMediaPreview(uri = uri, isVideo = isVideo, filterId = filterId)

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showCreationOptions) {
                Button(
                    onClick = onUseOriginal,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PoplurText("Post Original", fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
                }
                Button(
                    onClick = onUsePoplurAI,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3838), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PoplurText("Add Poplur AI", fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
                }
                TextButton(onClick = onBack) {
                    PoplurText("Back", color = Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.Bold)
                }
            } else {
                // Action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onBack() }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Retake",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        PoplurText(
                            "Retake",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onContinue() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = continueLabel,
                            tint = Color(0xFFFF3838),
                            modifier = Modifier.size(44.dp)
                        )
                        PoplurText(
                            continueLabel,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class PostCaptureChannelOption(
    val id: String,
    val number: Int,
    val emoji: String,
    val title: String,
    val subtitle: String
)

private data class PostCaptureFilterOption(
    val id: String,
    val title: String,
    val colors: List<Color>
)

private val postCaptureFilters = listOf(
    PostCaptureFilterOption("original", "Original", listOf(Color.Gray, Color.Black)),
    PostCaptureFilterOption("pop", "Pop", listOf(Color(0xFFFF4FD8), Color(0xFFFFD91A))),
    PostCaptureFilterOption("noir", "Noir", listOf(Color.White, Color.Black)),
    PostCaptureFilterOption("chrome", "Chrome", listOf(Color(0xFF62F7FF), Color(0xFF8F3AEB))),
    PostCaptureFilterOption("warm", "Warm", listOf(Color(0xFFFF8C1A), Color(0xFFFF3333))),
    PostCaptureFilterOption("cool", "Cool", listOf(Color(0xFF2078FF), Color(0xFF4DFF58)))
)

val postCaptureChannels = listOf(
    PostCaptureChannelOption("food", 101, "🍔", "Food", "Restaurants, recipes, cravings"),
    PostCaptureChannelOption("gaming", 102, "🎮", "Gaming", "Streams, clips, wins"),
    PostCaptureChannelOption("music", 103, "🎵", "Music", "Songs, shows, studios"),
    PostCaptureChannelOption("city", 104, "🏙️", "City", "Local scenes and street moments"),
    PostCaptureChannelOption("style", 105, "💄", "Style", "Beauty, fashion, looks"),
    PostCaptureChannelOption("sports", 106, "⚽", "Sports", "Games, training, highlights"),
    PostCaptureChannelOption("film", 107, "🎬", "Film", "Movies, edits, reviews"),
    PostCaptureChannelOption("shopping", 108, "🛍️", "Shopping", "Finds, hauls, drops"),
    PostCaptureChannelOption("french", 109, "🇫🇷", "French", "French-language broadcasts"),
    PostCaptureChannelOption("spanish", 110, "🇪🇸", "Spanish", "Spanish-language broadcasts")
)

@Composable
fun CapturedMediaEditorScreen(
    uri: Uri,
    isVideo: Boolean,
    selectedFilterId: String,
    trimSeconds: Float,
    selectedMusicUri: Uri?,
    onFilterSelected: (String) -> Unit,
    onTrimSecondsChanged: (Float) -> Unit,
    onMusicSelected: (Uri?) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val musicPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let(onMusicSelected)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CapturedMediaPreview(uri = uri, isVideo = isVideo, filterId = selectedFilterId)

        Column(Modifier.statusBarsPadding().padding(18.dp).align(Alignment.TopStart)) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.58f),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            PoplurText("Edit capture", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(top = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                postCaptureFilters.forEach { filter ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onFilterSelected(filter.id) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Brush.linearGradient(filter.colors), CircleShape)
                                .border(
                                    width = if (selectedFilterId == filter.id) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedFilterId == filter.id) {
                                PoplurText("✓", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        PoplurText(
                            filter.title,
                            color = Color.White.copy(alpha = if (selectedFilterId == filter.id) 1f else 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isVideo) {
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PoplurText("Trim", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        PoplurText("${trimSeconds.toInt()}s", color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = trimSeconds,
                        onValueChange = { onTrimSecondsChanged(it.coerceIn(1f, 10f)) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF3838),
                            activeTrackColor = Color(0xFFFF3838)
                        )
                    )
                }
            }

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
                    }
                    musicPickerLauncher.launch(intent)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedMusicUri == null) Color.White.copy(alpha = 0.12f) else Color(0xFF1DB954),
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            ) {
                PoplurText(
                    if (selectedMusicUri == null) "♫ Add Music" else "♪ Music Added",
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Button(
                onClick = onNext,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            ) {
                PoplurText("Continue", fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun CapturedChannelScreen(
    uri: Uri,
    isVideo: Boolean,
    filterId: String,
    trimSeconds: Float?,
    knownMetadata: TVStyleMetadata? = null,
    musicUri: Uri? = null,
    isPoplurAIOutput: Boolean = false,
    onBack: () -> Unit,
    onPosted: (PostCaptureChannelOption, TVStyleMetadata, Uri, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedChannel by remember { mutableStateOf(postCaptureChannels.first()) }
    var tvState by remember { mutableStateOf<TVState>(TVState.Idle) }
    var isPosting by remember { mutableStateOf(false) }
    val knownRecommendation = remember(knownMetadata) { knownMetadata?.let { recommendedChannelFor(it) } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CapturedMediaPreview(uri = uri, isVideo = isVideo, filterId = filterId)
        if (tvState is TVState.Analyzing) ScanningOverlay()
        (tvState as? TVState.Ready)?.meta?.let { TVBroadcastOverlay(it) }

        Column(Modifier.statusBarsPadding().padding(18.dp).align(Alignment.TopStart)) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.58f),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", modifier = Modifier.padding(10.dp).size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            PoplurText("Choose channel", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.86f))
                .padding(top = 16.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                postCaptureChannels.forEach { channel ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedChannel == channel) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isPosting) { selectedChannel = channel }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PoplurText(channel.emoji, fontSize = 27.sp, modifier = Modifier.width(42.dp))
                        Column(Modifier.weight(1f)) {
                            PoplurText("CH ${channel.number}  ${channel.title}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            PoplurText(channel.subtitle, color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                        if (selectedChannel == channel) {
                            PoplurText("✓", color = Color(0xFF4DFF58), fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            ChannelValidationNotice(
                selectedChannel = selectedChannel,
                recommendedChannel = knownRecommendation,
                hasKnownMetadata = knownMetadata != null
            )

            Button(
                enabled = !isPosting,
                onClick = {
                    isPosting = true
                    tvState = TVState.Analyzing
                    scope.launch {
                        val analyzedMeta = knownMetadata
                            ?: runCatching { TVStyleAnalyzer.analyze(context, uri, isVideo) }
                                .getOrElse { fallbackTVMetadata(selectedChannel) }
                        val finalChannel = recommendedChannelFor(analyzedMeta) ?: selectedChannel
                        val finalMeta = analyzedMeta.routedTo(finalChannel)
                        val (outputUri, outputIsVideo) = preparePostOutput(
                            context = context,
                            uri = uri,
                            isVideo = isVideo,
                            filterId = filterId,
                            trimSeconds = trimSeconds,
                            musicUri = musicUri
                        )
                        tvState = TVState.Ready(finalMeta)
                        delay(350)
                        onPosted(finalChannel, finalMeta, outputUri, outputIsVideo)
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3838), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    PoplurText("AI is making Poplur TV...", fontWeight = FontWeight.Black)
                } else {
                    PoplurText(
                        if (isPoplurAIOutput) "Post Poplur TV" else "Post to ${selectedChannel.title}",
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelValidationNotice(
    selectedChannel: PostCaptureChannelOption,
    recommendedChannel: PostCaptureChannelOption?,
    hasKnownMetadata: Boolean
) {
    val willMove = hasKnownMetadata && recommendedChannel != null && recommendedChannel.id != selectedChannel.id
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PoplurText(if (willMove) "⚠" else "✓", color = if (willMove) Color(0xFFFFD84D) else Color(0xFF4DFF58), fontWeight = FontWeight.Black)
        PoplurText(
            if (willMove && recommendedChannel != null) {
                "AI check: this fits CH ${recommendedChannel.number} ${recommendedChannel.title}. It will post there instead."
            } else {
                "AI will verify this content and move it to the right channel if needed."
            },
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CapturedMediaPreview(uri: Uri, isVideo: Boolean, filterId: String = "original") {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isVideo) {
            val player = remember(uri) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = 0.7f
                    prepare()
                    playWhenReady = true
                }
            }
            LaunchedEffect(player) { player.play() }
            DisposableEffect(player) { onDispose { player.pause(); player.release() } }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        this.player = player
                    }
                },
                update = { view ->
                    if (view.player !== player) view.player = player
                    if (!player.isPlaying) player.play()
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
        CaptureFilterOverlay(filterId)
    }
}

@Composable
private fun CaptureFilterOverlay(filterId: String) {
    val color = when (filterId) {
        "pop" -> Color(0xFFFF4FD8).copy(alpha = 0.14f)
        "noir" -> Color.Black.copy(alpha = 0.22f)
        "chrome" -> Color(0xFF62F7FF).copy(alpha = 0.13f)
        "warm" -> Color(0xFFFF8C1A).copy(alpha = 0.17f)
        "cool" -> Color(0xFF2078FF).copy(alpha = 0.17f)
        else -> Color.Transparent
    }
    if (color != Color.Transparent) {
        Box(Modifier.fillMaxSize().background(color))
    }
}

private fun fallbackTVMetadata(channel: PostCaptureChannelOption): TVStyleMetadata =
    TVStyleMetadata(
        channel = when (channel.id) {
            "food" -> "food"
            "music" -> "music"
            "city" -> "city"
            "sports" -> "sports"
            "style" -> "fashion"
            "film" -> "entertainment"
            else -> "entertainment"
        },
        tvFormat = "highlight_reel",
        adConcept = "${channel.title.lowercase(Locale.US)} local broadcast",
        caption = "LOCAL SIGNAL",
        tagline = "A fresh Poplur moment is now live from the map.",
        broadcastName = "POPLUR ${channel.title.uppercase(Locale.US)}"
    )

private fun recommendedChannelFor(meta: TVStyleMetadata, prompt: String = ""): PostCaptureChannelOption? {
    val text = listOf(meta.channel, meta.adConcept, meta.caption, meta.tagline, meta.broadcastName, prompt)
        .joinToString(" ")
        .lowercase(Locale.US)
    return recommendedChannelFromText(text)
}

private fun recommendedChannelFromPrompt(prompt: String): PostCaptureChannelOption? =
    recommendedChannelFromText(prompt.lowercase(Locale.US))

private fun recommendedChannelFromText(text: String): PostCaptureChannelOption? {
    val matches = listOf(
        "food" to listOf("food", "restaurant", "burger", "pizza", "recipe", "coffee", "drink", "meal", "kitchen"),
        "gaming" to listOf("gaming", "game", "stream", "console", "esport", "controller", "playthrough"),
        "music" to listOf("music", "song", "studio", "concert", "artist", "album", "beat", "dj"),
        "sports" to listOf("sports", "fitness", "workout", "training", "soccer", "basketball", "gym", "game day"),
        "style" to listOf("style", "fashion", "beauty", "makeup", "skincare", "outfit", "hair"),
        "shopping" to listOf("shopping", "shop", "buy", "haul", "drop", "deal", "store", "product"),
        "city" to listOf("city", "street", "local", "news", "neighborhood", "traffic", "downtown", "tech"),
        "film" to listOf("film", "movie", "cinema", "trailer", "show", "entertainment", "edit"),
        "french" to listOf("french", "français", "france"),
        "spanish" to listOf("spanish", "español", "spain", "latino")
    )
    val id = matches.firstOrNull { (_, keywords) -> keywords.any { text.contains(it) } }?.first
    return id?.let { match -> postCaptureChannels.firstOrNull { it.id == match } }
}

private fun TVStyleMetadata.routedTo(channel: PostCaptureChannelOption): TVStyleMetadata {
    val target = when (channel.id) {
        "food" -> "food"
        "music" -> "music"
        "city" -> "city"
        "sports" -> "sports"
        "style" -> "fashion"
        "film" -> "entertainment"
        else -> "entertainment"
    }
    if (this.channel == target && broadcastName.contains(channel.title, ignoreCase = true)) return this
    return copy(channel = target, broadcastName = "POPLUR ${channel.title.uppercase(Locale.US)}")
}

private suspend fun preparePostOutput(
    context: android.content.Context,
    uri: Uri,
    isVideo: Boolean,
    filterId: String,
    trimSeconds: Float?,
    musicUri: Uri?
): Pair<Uri, Boolean> {
    if (isVideo) {
        val sourceFile = copyUriToCache(context, uri, "mp4") ?: return uri to true
        var workingFile = if (trimSeconds != null) {
            OutputMusicMixer.trimVideo(context, sourceFile, trimSeconds.toDouble()) ?: sourceFile
        } else {
            sourceFile
        }
        if (musicUri != null) {
            workingFile = OutputMusicMixer.mixAudio(context, musicUri, workingFile) ?: workingFile
        }
        return Uri.fromFile(workingFile) to true
    }

    val bitmap = decodeBitmapForUri(context, uri) ?: return uri to false
    if (musicUri != null) {
        val videoFile = OutputMusicMixer.imageWithAudio(context, bitmap, musicUri, filterId = filterId)
        if (videoFile != null) return Uri.fromFile(videoFile) to true
    }
    if (filterId != "original") {
        val filtered = OutputMusicMixer.applyFilter(bitmap, filterId)
        val file = java.io.File(context.cacheDir, "post_${System.currentTimeMillis()}.jpg")
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            file.outputStream().use { filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
        }
        return Uri.fromFile(file) to false
    }
    return uri to false
}

private suspend fun copyUriToCache(context: android.content.Context, uri: Uri, extension: String): java.io.File? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val file = java.io.File(context.cacheDir, "post_${System.currentTimeMillis()}.$extension")
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            inputStream.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (_: Exception) {
            null
        }
    }

private suspend fun decodeBitmapForUri(context: android.content.Context, uri: Uri): android.graphics.Bitmap? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }

// MARK: - Describe Ad Screen

@Composable
fun DescribeAdScreen(
    uri: Uri,
    isVideo: Boolean,
    coordinate: Pair<Double, Double>?,
    onBack: () -> Unit,
    // bitmap? is null for video-only output; videoUrl? is non-null when AI returns a video
    onAdGenerated: (android.graphics.Bitmap?, String?, TVStyleMetadata) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var promptText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Back", modifier = Modifier.padding(10.dp).size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    PoplurText("Describe Poplur AI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    coordinate?.let {
                        PoplurText(
                            String.format("%.4f, %.4f", it.first, it.second),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            PoplurText(
                "Tell Poplur AI what to make",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            PoplurText(
                "Write one prompt. Photos generate photos, and videos generate videos.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))

            AdPromptField(
                value = promptText,
                onValueChange = { promptText = it.take(500) }
            )

            Spacer(Modifier.height(20.dp))
            errorMessage?.let {
                PoplurText(it, color = Color(0xFFFF5555), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
            }

            Button(
                enabled = !isGenerating && promptText.isNotBlank(),
                onClick = {
                    isGenerating = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val meta = runCatching { TVStyleAnalyzer.analyze(context, uri, isVideo) }
                                .getOrElse { fallbackTVMetadata(recommendedChannelFromPrompt(promptText) ?: postCaptureChannels.first()) }
                            val prompt = buildAdPrompt(
                                meta = meta,
                                channel = recommendedChannelFor(meta, promptText.trim()),
                                coordinate = coordinate,
                                userPrompt = promptText.trim()
                            )

                            // Decode captured frame for face-preserving generation
                            val faceBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                if (isVideo) {
                                    val retriever = MediaMetadataRetriever()
                                    try {
                                        retriever.setDataSource(context, uri)
                                        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    } finally { retriever.release() }
                                } else {
                                    context.contentResolver.openInputStream(uri)?.use {
                                        BitmapFactory.decodeStream(it)
                                    }
                                }
                            }

                            if (faceBitmap != null) {
                                if (isVideo) {
                                    val videoUrl = FaceAdGenerationService.generateVideo(faceBitmap, prompt)
                                    onAdGenerated(null, videoUrl, meta)
                                } else {
                                    val adBitmap = FaceAdGenerationService.generateImage(faceBitmap, prompt)
                                    onAdGenerated(adBitmap, null, meta)
                                }
                            } else if (isVideo) {
                                val videoUrl = VideoAdGenerationService.generate(prompt)
                                onAdGenerated(null, videoUrl, meta)
                            } else {
                                val bitmap = AdGenerationService.generate(prompt)
                                onAdGenerated(bitmap, null, meta)
                            }
                        } catch (e: com.apptists.poplur2025.data.TVAnalyzerError.NoAPIKey) {
                            errorMessage = "Claude API key missing."
                        } catch (e: com.apptists.poplur2025.data.TVAnalyzerError.HttpError) {
                            errorMessage = "AI analysis failed (HTTP ${e.code}) — check your API key."
                        } catch (e: com.apptists.poplur2025.data.TVAnalyzerError.ParseError) {
                            errorMessage = "AI response couldn't be parsed — try again."
                        } catch (e: Exception) {
                            errorMessage = "Generation failed — check your internet connection and try again."
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    PoplurText("Generating…", fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    PoplurText("✦ Generate Poplur AI", fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdPromptField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PoplurText("✦", fontSize = 22.sp, color = Color.White)
            Column(Modifier.weight(1f)) {
                PoplurText("PROMPT", color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            PoplurText(
                                "A cinematic Poplur ad about...",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        inner()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 86.dp)
                )
            }
        }
    }
}

private fun buildAdPrompt(
    meta: TVStyleMetadata,
    channel: PostCaptureChannelOption?,
    coordinate: Pair<Double, Double>?,
    userPrompt: String
): String {
    val resolvedChannel = channel ?: recommendedChannelFor(meta, userPrompt) ?: postCaptureChannels.first()
    val channelCtx = "${resolvedChannel.emoji} ${resolvedChannel.title}"
    val coords = coordinate?.let { String.format("%.2f°, %.2f°", it.first, it.second) } ?: ""
    val userContext = if (userPrompt.isBlank()) "" else "User prompt: $userPrompt. "
    return "${userContext}A stunning cinematic TV advertisement for ${meta.broadcastName}. " +
        "${meta.tvFormat.replace("_", " ")} format. " +
        "Ad concept: ${meta.adConcept}. Channel: $channelCtx. ${if (coords.isNotEmpty()) "Location: $coords. " else ""}" +
        "Ultra-high production value, broadcast television quality, vivid saturated colors, " +
        "professional product photography, advertising industry standard. No text overlays."
}

// MARK: - Ad Preview Screen

@Composable
fun AdPreviewScreen(
    adBitmap: android.graphics.Bitmap?,
    adVideoUrl: String?,
    selectedChannel: PostCaptureChannelOption,
    meta: TVStyleMetadata,
    onBack: () -> Unit,
    // Provides the processed output URI and whether it's a video so PoplurRoot can post it.
    onBroadcast: (outputUri: Uri, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPosting by remember { mutableStateOf(false) }
    var selectedFilterId by remember { mutableStateOf("original") }
    var selectedMusicUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMusicTrack by remember { mutableStateOf<MusicSearchTrack?>(null) }
    var showMusicSearch by remember { mutableStateOf(false) }
    var trimSeconds by remember { mutableFloatStateOf(10f) }

    val musicPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.data?.let {
            selectedMusicUri = it
            selectedMusicTrack = null
        }
    }

    val isVideoAd = adVideoUrl != null

    // ExoPlayer for video preview
    val exoPlayer = if (isVideoAd) remember(adVideoUrl) {
        ExoPlayer.Builder(context).build().apply {
            adVideoUrl?.let { setMediaItem(MediaItem.fromUri(it)) }
            prepare(); repeatMode = Player.REPEAT_MODE_ONE; playWhenReady = true
        }
    } else null
    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (showMusicSearch) {
            MusicSearchSheet(
                selectedTrack = selectedMusicTrack,
                onDismiss = { showMusicSearch = false },
                onSelect = { track ->
                    selectedMusicTrack = track
                    selectedMusicUri = null
                    showMusicSearch = false
                },
                onUseLocalFile = {
                    showMusicSearch = false
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
                    }
                    musicPickerLauncher.launch(intent)
                }
            )
        }

        // Preview — video or image
        if (isVideoAd && exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (adBitmap != null) {
            AsyncImage(
                model = adBitmap,
                contentDescription = "Generated ad",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            // Live filter overlay (same colour-tint approach as raw capture)
            CaptureFilterOverlay(selectedFilterId)
        }
        TVBroadcastOverlay(meta)

        Column(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(18.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.58f),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", modifier = Modifier.padding(10.dp).size(22.dp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(top = 14.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filter strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                postCaptureFilters.forEach { filter ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(60.dp).clickable { selectedFilterId = filter.id }
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(Brush.linearGradient(filter.colors), CircleShape)
                                .border(
                                    width = if (selectedFilterId == filter.id) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedFilterId == filter.id) PoplurText("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(5.dp))
                        PoplurText(filter.title, color = Color.White.copy(alpha = if (selectedFilterId == filter.id) 1f else 0.65f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Trim slider (video output only)
            if (isVideoAd) {
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PoplurText("Trim", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        PoplurText("${trimSeconds.toInt()}s", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                    Slider(
                        value = trimSeconds,
                        onValueChange = { trimSeconds = it.coerceIn(1f, 10f) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFF3838), activeTrackColor = Color(0xFFFF3838))
                    )
                }
            }

            selectedMusicTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF4DFF58).copy(alpha = 0.82f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        PoplurText("♪", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Column(Modifier.weight(1f)) {
                        PoplurText(track.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        PoplurText("${track.artist} • ${track.provider.displayName}", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    PoplurText(
                        "×",
                        modifier = Modifier.clickable { selectedMusicTrack = null },
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Music + Broadcast row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Music picker button
                Surface(
                    onClick = { showMusicSearch = true },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selectedMusicUri != null || selectedMusicTrack != null) Color(0xFF1DB954) else Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White,
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PoplurText(if (selectedMusicUri != null || selectedMusicTrack != null) "♪" else "♫", fontSize = 16.sp)
                        PoplurText(
                            when {
                                selectedMusicUri != null -> "Music File"
                                selectedMusicTrack != null -> "Track Linked"
                                else -> "Add Music"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Broadcast button
                Button(
                    enabled = !isPosting,
                    onClick = {
                        isPosting = true
                        scope.launch {
                            try {
                                val outputUri: Uri
                                val outputIsVideo: Boolean
                                val music = selectedMusicUri

                                if (isVideoAd && adVideoUrl != null) {
                                    // Download generated video, apply trim + optional music
                                    val dlFile = OutputMusicMixer.downloadVideo(context, adVideoUrl)
                                    if (dlFile != null) {
                                        val trimmedFile = OutputMusicMixer.trimVideo(context, dlFile, trimSeconds.toDouble())
                                            ?: dlFile
                                        dlFile.delete()
                                        val finalFile = if (music != null) {
                                            OutputMusicMixer.mixAudio(context, music, trimmedFile) ?: trimmedFile
                                        } else trimmedFile
                                        outputUri = Uri.fromFile(finalFile)
                                        outputIsVideo = true
                                    } else {
                                        isPosting = false
                                        return@launch
                                    }
                                } else if (adBitmap != null) {
                                    val filtered = OutputMusicMixer.applyFilter(adBitmap, selectedFilterId)
                                    if (music != null) {
                                        val videoFile = OutputMusicMixer.imageWithAudio(context, filtered, music, filterId = "original")
                                        if (videoFile != null) {
                                            outputUri = Uri.fromFile(videoFile)
                                            outputIsVideo = true
                                        } else {
                                            // Music mix failed — fall back to image
                                            val imgFile = java.io.File(context.cacheDir, "ad_${System.currentTimeMillis()}.jpg")
                                            imgFile.outputStream().use { filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                                            outputUri = Uri.fromFile(imgFile)
                                            outputIsVideo = false
                                        }
                                    } else {
                                        val imgFile = java.io.File(context.cacheDir, "ad_${System.currentTimeMillis()}.jpg")
                                        imgFile.outputStream().use { filtered.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it) }
                                        outputUri = Uri.fromFile(imgFile)
                                        outputIsVideo = false
                                    }
                                } else {
                                    isPosting = false
                                    return@launch
                                }

                                onBroadcast(outputUri, outputIsVideo)
                            } catch (e: Exception) {
                                isPosting = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3838), contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        PoplurText("Preparing…", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    } else {
                        PoplurText("Pick Channel", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// MARK: - TV Broadcast Overlay (composable)

@Composable
private fun TVBroadcastOverlay(meta: TVStyleMetadata) {
    var lowerThirdVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(220)
        lowerThirdVisible = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vignette
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width * 0.85f
                )
            )
        }

        // Channel bug — top left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp)
                .background(Color.Black.copy(alpha = 0.66f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PoplurText(channelEmoji(meta.channel), fontSize = 14.sp)
            PoplurText(
                meta.broadcastName.uppercase(),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
        }

        // HD badge — top right
        PoplurText(
            "HD",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 10.dp)
                .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 3.dp),
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )

        // Lower third — slides up
        AnimatedVisibility(
            visible = lowerThirdVisible,
            enter = slideInVertically { it } + fadeIn(tween(380, easing = FastOutSlowInEasing)),
            exit  = slideOutVertically { it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
                        )
                    )
                    .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PoplurText(
                    meta.caption.uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 24.sp,
                    maxLines = 2
                )
                PoplurText(
                    meta.tagline,
                    color = Color.White.copy(alpha = 0.84f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
            }
        }
    }
}

// MARK: - Scanning overlay

@Composable
private fun ScanningOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.54f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                for (i in 0..2) {
                    val diameter = (40 + i * 24).dp
                    Box(
                        modifier = Modifier
                            .size(diameter * pulse)
                            .background(Color.Transparent, CircleShape)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.Red.copy(alpha = 0.55f - i * 0.14f),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
                Icon(
                    Icons.Default.Add, // reuse icon slot — we just want a placeholder center mark
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            PoplurText(
                "SCANNING BROADCAST",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.8.sp
            )
        }
    }
}

// MARK: - Channel emoji helper

private fun channelEmoji(channel: String): String = when (channel) {
    "food"          -> "🍔"
    "fitness"       -> "💪"
    "sports"        -> "⚽"
    "beauty"        -> "💄"
    "fashion"       -> "👗"
    "music"         -> "🎵"
    "tech"          -> "💻"
    "news"          -> "📺"
    "city"          -> "🏙️"
    "entertainment" -> "🎬"
    else            -> "📡"
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
                    PoplurText(
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
    username: String? = null,
    modifier: Modifier = Modifier
) {
    val usernameLabel = remember(username) {
        username
            ?.trim()
            ?.removePrefix("@")
            ?.takeIf { it.isNotBlank() }
            ?.let { "@$it" }
    }

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
                color = Color(0xFF4DFF58).copy(alpha = .9f),
                topLeft = Offset(box.left, box.top),
                size = Size(box.width, box.height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            usernameLabel?.let { label ->
                val horizontalPadding = 8.dp.toPx()
                val badgeHeight = 24.dp.toPx()
                val badgeGap = 8.dp.toPx()
                val edgePadding = 8.dp.toPx()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.BLACK
                    textSize = 12.sp.toPx()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val badgeWidth = paint.measureText(label) + horizontalPadding * 2
                val maxLeft = (size.width - badgeWidth - edgePadding).coerceAtLeast(edgePadding)
                val badgeLeft = box.left.coerceIn(edgePadding, maxLeft)
                val badgeTop = (box.top - badgeHeight - badgeGap).coerceAtLeast(edgePadding)

                drawRoundRect(
                    color = Color(0xFF4DFF58).copy(alpha = .92f),
                    topLeft = Offset(badgeLeft, badgeTop),
                    size = Size(badgeWidth, badgeHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    badgeLeft + horizontalPadding,
                    badgeTop + badgeHeight / 2f - (paint.descent() + paint.ascent()) / 2f,
                    paint
                )
            }

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

// MARK: - ML Object Detection Overlay

@Composable
private fun MLObjectShaderOverlay(
    objects: List<com.apptists.poplur2025.data.DetectedObject>,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    AnimatedVisibility(
        visible = objects.isNotEmpty(),
        enter = fadeIn(tween(200)) + slideInVertically { -it },
        exit = fadeOut(tween(300)) + slideOutVertically { -it },
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            objects.firstOrNull()?.let { primary ->
                ShaderNumberText(
                    text = primary.label.uppercase(java.util.Locale.US),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    intensity = 1.5f,
                    letterSpacing = 2.8.sp
                )
            }
            if (objects.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    objects.drop(1).take(3).forEachIndexed { idx, obj ->
                        PoplurText(
                            obj.label.replaceFirstChar { it.uppercase(java.util.Locale.US) },
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .graphicsLayer {
                                    rotationX = 9f
                                    cameraDistance = 8f * density
                                }
                        )
                    }
                }
            }
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

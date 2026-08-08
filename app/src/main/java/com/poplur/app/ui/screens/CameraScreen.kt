package com.poplur.app.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

@SuppressLint("RestrictedApi")
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var recording by remember { mutableStateOf<Recording?>(null) }
    var status by remember { mutableStateOf("Ready") }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val executor: Executor = remember { context.mainExecutor }

    LaunchedEffect(lifecycleOwner) {
        status = "Starting preview..."
        runCatching {
            val provider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build()

            val videoCapture = VideoCapture.withOutput(recorder)

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                videoCapture
            )

            CameraHolder.videoCapture = videoCapture
            status = "Preview started"
        }.onFailure {
            status = "Camera unavailable: ${it.localizedMessage ?: "check permission"}"
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(status)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val vc = CameraHolder.videoCapture ?: return@Button
                        if (recording != null) return@Button

                        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
                            .format(System.currentTimeMillis())

                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        }

                        val output = MediaStoreOutputOptions.Builder(
                            context.contentResolver,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        ).setContentValues(contentValues).build()

                        recording = vc.output
                            .prepareRecording(context, output)
                            .withAudioEnabled()
                            .start(executor) { event ->
                                when (event) {
                                    is VideoRecordEvent.Start -> status = "Recording…"
                                    is VideoRecordEvent.Finalize -> {
                                        status = if (!event.hasError()) "Saved: ${'$'}{event.outputResults.outputUri}" else "Error: ${'$'}{event.error}"
                                        recording = null
                                    }
                                }
                            }
                    }
                ) { Text("Record") }

                OutlinedButton(
                    onClick = {
                        recording?.stop()
                        recording = null
                        status = "Stopping…"
                    },
                    enabled = recording != null
                ) { Text("Stop") }
            }
            Spacer(Modifier.height(6.dp))
            Text("Tip: add Firebase Storage upload after finalize.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private object CameraHolder {
    var videoCapture: VideoCapture<Recorder>? = null
}

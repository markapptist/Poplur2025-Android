package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.apptists.poplur2025.data.AIVideoJobStatus
import com.apptists.poplur2025.data.AIVideoService
import com.apptists.poplur2025.data.AIVideoStoryboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun AIVideoProgressScreen(
    onBack: () -> Unit,
    onPostClip: (uri: android.net.Uri, isVideo: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val status by AIVideoService.status.collectAsState()
    val storyboard by AIVideoService.storyboard.collectAsState()
    val clipUrls by AIVideoService.clipUrls.collectAsState()
    var selectedClipIndex by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 16.dp, bottom = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    "Generating",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Status bar
            StatusBanner(status = status)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Storyboard section
                storyboard?.let { board ->
                    item {
                        StoryboardCard(board)
                    }
                }

                // Clip cards
                if (clipUrls.isNotEmpty() || status is AIVideoJobStatus.GeneratingClip || status == AIVideoJobStatus.Complete) {
                    val targetCount = storyboard?.shots?.size ?: 3
                    itemsIndexed((0 until targetCount).toList()) { index, _ ->
                        val url = clipUrls.getOrNull(index)
                        val isThisGenerating = status is AIVideoJobStatus.GeneratingClip &&
                                (status as AIVideoJobStatus.GeneratingClip).current == index + 1
                        ClipCard(
                            index = index,
                            url = url,
                            isGenerating = isThisGenerating,
                            isSelected = selectedClipIndex == index && url != null,
                            onSelect = { if (url != null) selectedClipIndex = index }
                        )
                    }
                }

                // Error
                if (status is AIVideoJobStatus.Failed) {
                    item {
                        Text(
                            "Generation failed: ${(status as AIVideoJobStatus.Failed).reason}",
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }

            // Post button
            val selectedUrl = clipUrls.getOrNull(selectedClipIndex)
            val canPost = status == AIVideoJobStatus.Complete && selectedUrl != null && !isDownloading

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val url = selectedUrl ?: return@Button
                        isDownloading = true
                        scope.launch {
                            try {
                                val uri = downloadToCache(context, url, selectedClipIndex)
                                onPostClip(uri, true)
                            } catch (e: Exception) {
                                isDownloading = false
                            }
                        }
                    },
                    enabled = canPost,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4DFF58),
                        disabledContainerColor = Color.White.copy(alpha = 0.10f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            "Post Clip ${selectedClipIndex + 1}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(status: AIVideoJobStatus) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse-alpha"
    )

    val (icon, label, color) = when (status) {
        AIVideoJobStatus.Idle -> Triple("⏳", "Preparing…", Color(0xFFB47DFF))
        AIVideoJobStatus.Storyboarding -> Triple("🎬", "Writing storyboard…", Color(0xFFB47DFF))
        is AIVideoJobStatus.GeneratingClip -> Triple(
            "🎥",
            "Generating clip ${status.current} of ${status.total}…",
            Color(0xFF4DFF58)
        )
        AIVideoJobStatus.Complete -> Triple("✅", "All clips ready!", Color(0xFF4DFF58))
        is AIVideoJobStatus.Failed -> Triple("❌", "Failed", Color(0xFFFF6B6B))
    }

    val isActive = status !is AIVideoJobStatus.Complete && status !is AIVideoJobStatus.Failed

    AnimatedContent(
        targetState = label,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "status-label"
    ) { labelText ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.08f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 18.sp, modifier = Modifier.alpha(if (isActive) alpha else 1f))
            Spacer(Modifier.width(10.dp))
            Text(
                labelText,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(if (isActive) alpha else 1f)
            )
        }
    }
}

@Composable
private fun StoryboardCard(board: AIVideoStoryboard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            board.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            board.style,
            color = Color(0xFFB47DFF),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        board.shots.forEach { shot ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB47DFF).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${shot.shot}",
                        color = Color(0xFFB47DFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        shot.type.replaceFirstChar { it.uppercase() },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        shot.prompt,
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipCard(
    index: Int,
    url: String?,
    isGenerating: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val borderColor = when {
        isSelected -> Color(0xFF4DFF58)
        isGenerating -> Color(0xFFB47DFF)
        url != null -> Color.White.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.08f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = url != null) { onSelect() }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Text(
                "CLIP ${index + 1}",
                color = if (url != null) Color(0xFF4DFF58) else Color.White.copy(alpha = 0.42f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            if (isSelected) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4DFF58).copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("SELECTED", color = Color(0xFF4DFF58), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            if (isGenerating) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    color = Color(0xFFB47DFF),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (url != null) {
            val player = remember(url) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(url))
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    prepare()
                    playWhenReady = true
                }
            }
            DisposableEffect(url) {
                onDispose { player.release() }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                if (isGenerating) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFFB47DFF),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Generating…",
                            color = Color(0xFFB47DFF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        "Pending",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

private suspend fun downloadToCache(
    context: android.content.Context,
    url: String,
    index: Int
): android.net.Uri = withContext(Dispatchers.IO) {
    val file = File(context.cacheDir, "ai_clip_${index}_${System.currentTimeMillis()}.mp4")
    URL(url).openStream().use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    android.net.Uri.fromFile(file)
}

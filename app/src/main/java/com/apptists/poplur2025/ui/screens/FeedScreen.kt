package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.Post
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay
import com.apptists.poplur2025.ui.components.UserInfoOverlay
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun FeedScreen(
    currentPostIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    val currentPost = remember(currentPostIndex) { DemoPostStore.getPostForIndex(currentPostIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Ultra-sensitive vertical swipe detection
            .pointerInput(currentPostIndex) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        var totalY = 0f
                        var triggered = false
                        drag(down.id) { change ->
                            if (!triggered) {
                                val delta = change.position.y - change.previousPosition.y
                                totalY += delta
                                if (abs(totalY) > 40f) { // Faster trigger threshold (40px)
                                    if (totalY > 0) {
                                        onIndexChange(currentPostIndex - 1)
                                    } else {
                                        onIndexChange(currentPostIndex + 1)
                                    }
                                    triggered = true
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // 1. Background Content (TV Flip Effect)
        AnimatedContent(
            targetState = currentPost,
            transitionSpec = {
                // TV style flip: Instant fade for speed
                fadeIn(animationSpec = tween(100, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(100, easing = FastOutSlowInEasing)))
            },
            label = "ChannelFlip"
        ) { post ->
            Box(modifier = Modifier.fillMaxSize()) {
                MediaContent(post = post)
                
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )
            }
        }

        // 2. Top Overlay (Animated User Data)
        AnimatedContent(
            targetState = currentPost,
            transitionSpec = {
                (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart),
            label = "UserInfoAnimation"
        ) { post ->
            UserInfoOverlay(
                displayName = post.displayName,
                location = post.locationName
            )
        }

        // 3. Bottom Left Overlay (Animated Post Data)
        AnimatedContent(
            targetState = currentPost,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            },
            modifier = Modifier
                .padding(bottom = 58.dp, start = 20.dp)
                .align(Alignment.BottomStart),
            label = "TrendingAnimation"
        ) { post ->
            TrendingDisplay(
                postsCount = post.userPostsCount,
                radiusChange = post.radiusChange
            )
        }

        // 4. Bottom Center placeholder space for the global Cube
        Spacer(
            modifier = Modifier
                .padding(bottom = 86.dp)
                .size(112.dp)
                .align(Alignment.BottomCenter)
        )

        // 5. Bottom Right Overlay (Animated Radius Data)
        AnimatedContent(
            targetState = currentPost,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it }).togetherWith(fadeOut() + slideOutHorizontally { it })
            },
            modifier = Modifier
                .padding(bottom = 48.dp, end = 20.dp)
                .align(Alignment.BottomEnd),
            label = "RadiusAnimation"
        ) { post ->
            RadiusDisplay(
                radius = post.radius,
                unit = "mi"
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaContent(post: Post) {
    val context = LocalContext.current
    var hasError by remember(post.id) { mutableStateOf(false) }
    var isLoading by remember(post.id) { mutableStateOf(true) }
    var enableVideo by remember(post.id) { mutableStateOf(false) }

    LaunchedEffect(post.id, post.videoUrl) {
        enableVideo = false
        if (post.videoUrl != null) {
            delay(900)
            enableVideo = true
        }
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            post.videoUrl != null && enableVideo && !hasError -> {
                val exoPlayer = remember(post.videoUrl) {
                    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .setConnectTimeoutMs(2000) // Fast timeout
                        .setReadTimeoutMs(2000)
                        .setAllowCrossProtocolRedirects(true)
                    
                    val mediaSourceFactory = DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(httpDataSourceFactory)

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build()

                    ExoPlayer.Builder(context)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .setAudioAttributes(audioAttributes, true)
                        .build().apply {
                            // Optimized for fast start
                            val mediaItem = MediaItem.Builder()
                                .setUri(post.videoUrl)
                                .build()
                            setMediaItem(mediaItem)
                            repeatMode = Player.REPEAT_MODE_ONE
                            addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(state: Int) {
                                    isLoading = state == Player.STATE_BUFFERING
                                }
                                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                    println("ExoPlayer Error: ${error.message}")
                                    hasError = true
                                    isLoading = false
                                }
                            })
                            prepare()
                            playWhenReady = true
                        }
                }
                
                DisposableEffect(exoPlayer) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            useController = false
                            player = exoPlayer
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            post.videoUrl != null && post.mediaUrl != null -> {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { isLoading = true },
                    onSuccess = { isLoading = false },
                    onError = {
                        hasError = true
                        isLoading = false
                    }
                )
            }
            post.mediaUrl != null -> {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = post.caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { isLoading = true },
                    onSuccess = { isLoading = false },
                    onError = { 
                        hasError = true
                        isLoading = false
                    }
                )
            }
            hasError -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Media unavailable",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                hasError = true
            }
        }
        
        if (isLoading && !hasError) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
        }
    }
}

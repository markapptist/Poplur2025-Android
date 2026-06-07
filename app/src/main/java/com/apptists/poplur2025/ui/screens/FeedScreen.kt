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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.ads.AdManager
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
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun FeedScreen(
    currentPostIndex: Int,
    onIndexChange: (Int) -> Unit
) {
    val currentSlot = remember(currentPostIndex) { resolveFeedSlot(currentPostIndex) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Background Content (TV Flip Effect)
        AnimatedContent(
            targetState = currentSlot,
            transitionSpec = {
                // TV style flip: Instant fade for speed
                fadeIn(animationSpec = tween(100, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(100, easing = FastOutSlowInEasing)))
            },
            label = "ChannelFlip"
        ) { slot ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (slot) {
                    is FeedSlot.PostSlot -> {
                        MediaContent(post = slot.post)
                    }
                    is FeedSlot.AdSlot -> FeedAdContent(slot = slot)
                }
                
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
            targetState = currentSlot,
            transitionSpec = {
                (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart),
            label = "UserInfoAnimation"
        ) { slot ->
            when (slot) {
                is FeedSlot.PostSlot -> UserInfoOverlay(
                    displayName = slot.post.displayName,
                    location = slot.post.locationName
                )
                is FeedSlot.AdSlot -> UserInfoOverlay(
                    displayName = "Sponsored",
                    location = slot.locationLabel
                )
            }
        }

        // 3. Bottom Left Overlay (Animated Post Data)
        AnimatedContent(
            targetState = currentSlot,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            },
            modifier = Modifier
                .padding(bottom = 58.dp, start = 20.dp)
                .align(Alignment.BottomStart),
            label = "TrendingAnimation"
        ) { slot ->
            when (slot) {
                is FeedSlot.PostSlot -> TrendingDisplay(
                    postsCount = slot.post.userPostsCount,
                    radiusChange = slot.post.radiusChange
                )
                is FeedSlot.AdSlot -> TrendingDisplay(
                    postsCount = slot.engagementCount,
                    radiusChange = slot.radiusChange
                )
            }
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
            targetState = currentSlot,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it }).togetherWith(fadeOut() + slideOutHorizontally { it })
            },
            modifier = Modifier
                .padding(bottom = 48.dp, end = 20.dp)
                .align(Alignment.BottomEnd),
            label = "RadiusAnimation"
        ) { slot ->
            RadiusDisplay(
                radius = when (slot) {
                    is FeedSlot.PostSlot -> slot.post.radius
                    is FeedSlot.AdSlot -> slot.radius
                },
                unit = "mi"
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .feedSwipeDetector(currentPostIndex, onIndexChange)
        )
    }
}

private fun Modifier.feedSwipeDetector(
    currentPostIndex: Int,
    onIndexChange: (Int) -> Unit
): Modifier = pointerInput(currentPostIndex) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var totalY = 0f
            var triggered = false
            drag(down.id) { change ->
                if (!triggered) {
                    val delta = change.position.y - change.previousPosition.y
                    totalY += delta
                    if (abs(totalY) > 28f) {
                        onIndexChange(if (totalY > 0) currentPostIndex - 1 else currentPostIndex + 1)
                        triggered = true
                        change.consume()
                    }
                }
            }
        }
    }
}

private const val AD_FREQUENCY = 6
private const val FEED_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

private sealed interface FeedSlot {
    data class PostSlot(val post: Post) : FeedSlot
    data class AdSlot(
        val id: String,
        val headline: String,
        val body: String,
        val locationLabel: String,
        val radius: Int,
        val engagementCount: Int,
        val radiusChange: Int
    ) : FeedSlot
}

private fun resolveFeedSlot(index: Int): FeedSlot {
    val normalized = if (index < 0) {
        val cycle = AD_FREQUENCY * DemoPostStore.posts.size
        (cycle + (index % cycle)) % cycle
    } else {
        index
    }
    if ((normalized + 1) % AD_FREQUENCY == 0) {
        return demoAdSlot(normalized / AD_FREQUENCY)
    }
    val adsBefore = (normalized + 1) / AD_FREQUENCY
    return FeedSlot.PostSlot(DemoPostStore.getPostForIndex(normalized - adsBefore))
}

private fun demoAdSlot(adIndex: Int): FeedSlot.AdSlot {
    val ads = listOf(
        FeedSlot.AdSlot("ad-$adIndex-1", "Poplur Pro", "Boost your next post where people are already watching.", "Sponsored nearby", 1250, 84, 18),
        FeedSlot.AdSlot("ad-$adIndex-2", "Tonight's Drop", "Find events, food, and creator moments before they peak.", "Sponsored trend", 890, 126, 24),
        FeedSlot.AdSlot("ad-$adIndex-3", "Creator Tools", "Turn live clips into local reach with smarter posting.", "Sponsored creator", 1420, 62, 11)
    )
    return ads[adIndex % ads.size].copy(id = "ad-$adIndex")
}

@Composable
private fun FeedAdContent(slot: FeedSlot.AdSlot) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2B1648), Color.Black),
                    radius = 950f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SPONSORED", color = Color.White.copy(alpha = .62f), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(slot.headline, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text(slot.body, color = Color.White.copy(alpha = .86f), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = FEED_BANNER_AD_UNIT_ID
                        AdManager.loadBanner(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
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

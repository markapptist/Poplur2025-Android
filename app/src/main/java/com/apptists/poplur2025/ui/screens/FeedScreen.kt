package com.apptists.poplur2025.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
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
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun FeedScreen(
    currentPostIndex: Int,
    isVisible: Boolean = true,
    onIndexChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val currentSlot = remember(currentPostIndex) { resolveFeedSlot(currentPostIndex) }
    var showFeedLogo by remember { mutableStateOf(true) }
    
    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                val afd = context.assets.openFd("pop.mp3")
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepareAsync()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }

    LaunchedEffect(Unit) {
        showFeedLogo = true
        mediaPlayer.setOnPreparedListener { it.start() }
        if (!mediaPlayer.isPlaying) {
            try { mediaPlayer.start() } catch (e: Exception) {}
        }
        delay(2_500)
        showFeedLogo = false
    }

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
                        MediaContent(post = slot.post, isVisible = isVisible)
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
                    location = "Nearby"
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
                .padding(bottom = 48.dp, start = 20.dp)
                .align(Alignment.BottomStart),
            label = "TrendingAnimation"
        ) { slot ->
            when (slot) {
                is FeedSlot.PostSlot -> TrendingDisplay(
                    postsCount = slot.post.userPostsCount,
                    radiusChange = slot.post.radiusChange,
                    modifier = Modifier,
                    contentColor = Color.White
                )
                is FeedSlot.AdSlot -> TrendingDisplay(
                    postsCount = 84, // Real ad engagement could be synced if available
                    radiusChange = 18,
                    modifier = Modifier,
                    contentColor = Color.White
                )
            }
        }

        // 4. Removed navigation cube placeholder

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
                    is FeedSlot.AdSlot -> 1250 // Ad radius
                },
                unit = "mi"
            )
        }

        if (showFeedLogo) {
            val logoAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400),
                label = "feedLogoAlpha"
            )
            logoBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Poplur",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp)
                        .graphicsLayer {
                            alpha = logoAlpha
                            scaleX = 0.9f + logoAlpha * 0.1f
                            scaleY = 0.9f + logoAlpha * 0.1f
                        }
                )
            }
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
private const val FEED_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val ENABLE_FEED_VIDEO_AUTOPLAY = true

private sealed interface FeedSlot {
    data class PostSlot(val post: Post) : FeedSlot
    data class AdSlot(val id: String) : FeedSlot
}

private fun resolveFeedSlot(index: Int): FeedSlot {
    val normalized = if (index < 0) {
        val cycle = AD_FREQUENCY * DemoPostStore.posts.size
        (cycle + (index % cycle)) % cycle
    } else {
        index
    }
    if ((normalized + 1) % AD_FREQUENCY == 0) {
        return FeedSlot.AdSlot("ad-$normalized")
    }
    val adsBefore = (normalized + 1) / AD_FREQUENCY
    return FeedSlot.PostSlot(DemoPostStore.getPostForIndex(normalized - adsBefore))
}

@Composable
private fun FeedAdContent(slot: FeedSlot.AdSlot) {
    val context = LocalContext.current
    var nativeAd by remember(slot.id) { mutableStateOf<NativeAd?>(null) }
    var adLoadFailed by remember(slot.id) { mutableStateOf(false) }

    DisposableEffect(slot.id) {
        var disposed = false
        AdManager.loadNativeAd(
            context = context,
            adUnitId = FEED_NATIVE_AD_UNIT_ID,
            onLoaded = { loadedAd ->
                if (disposed) {
                    loadedAd.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = loadedAd
                    adLoadFailed = false
                }
            },
            onFailed = {
                adLoadFailed = true
            }
        )
        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (nativeAd != null) {
            AndroidView(
                factory = { createFullScreenNativeAdView(it) },
                update = { view ->
                    nativeAd?.let { bindFullScreenNativeAd(view, it) }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            FeedAdFallback(slot = slot, loading = !adLoadFailed)
        }
    }
}

@Composable
private fun FeedAdFallback(slot: FeedSlot.AdSlot, loading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF101820), Color(0xFF31103A), Color.Black)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFED3B8F).copy(alpha = 0.42f), Color.Transparent),
                        radius = 900f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 160.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("SPONSORED", color = Color.White.copy(alpha = .74f), fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            if (loading) {
                LinearProgressIndicator(
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = .18f),
                    modifier = Modifier.width(132.dp)
                )
            } else {
                Text("Ad currently unavailable", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

private fun createFullScreenNativeAdView(context: Context): NativeAdView {
    val adView = NativeAdView(context)
    val media = MediaView(context).apply { tag = NativeAdAsset.Media }
    val icon = ImageView(context).apply { tag = NativeAdAsset.Icon }
    val sponsoredText = TextView(context)
    val headlineText = TextView(context).apply { tag = NativeAdAsset.Headline }
    val bodyText = TextView(context).apply { tag = NativeAdAsset.Body }
    val advertiserText = TextView(context).apply { tag = NativeAdAsset.Advertiser }
    val ctaText = TextView(context).apply { tag = NativeAdAsset.CallToAction }

    adView.setBackgroundColor(AndroidColor.BLACK)

    media.layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    adView.addView(media)
    adView.mediaView = media

    adView.addView(
        View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.argb(230, 0, 0, 0))
            )
        },
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.heightPixels * 0.55f).toInt(),
            Gravity.BOTTOM
        )
    )

    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
        setPadding(context.dp(28), context.dp(20), context.dp(28), context.dp(148))
    }
    adView.addView(
        content,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
    )

    val sponsorRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    content.addView(
        sponsorRow,
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    )

    icon.scaleType = ImageView.ScaleType.CENTER_CROP
    icon.background = roundedDrawable(AndroidColor.WHITE, context.dp(12).toFloat())
    sponsorRow.addView(icon, LinearLayout.LayoutParams(context.dp(44), context.dp(44)).apply { rightMargin = context.dp(12) })
    adView.iconView = icon

    val sponsorColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    sponsorRow.addView(sponsorColumn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

    sponsoredText.text = "SPONSORED"
    sponsoredText.setTextColor(AndroidColor.argb(190, 255, 255, 255))
    sponsoredText.textSize = 12f
    sponsoredText.typeface = Typeface.DEFAULT_BOLD
    sponsorColumn.addView(sponsoredText)

    advertiserText.setTextColor(AndroidColor.WHITE)
    advertiserText.textSize = 15f
    advertiserText.typeface = Typeface.DEFAULT_BOLD
    advertiserText.maxLines = 1
    advertiserText.ellipsize = TextUtils.TruncateAt.END
    sponsorColumn.addView(advertiserText)
    adView.advertiserView = advertiserText

    headlineText.setTextColor(AndroidColor.WHITE)
    headlineText.textSize = 44f
    headlineText.typeface = Typeface.DEFAULT_BOLD
    headlineText.maxLines = 3
    headlineText.ellipsize = TextUtils.TruncateAt.END
    content.addView(headlineText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(22) })
    adView.headlineView = headlineText

    bodyText.setTextColor(AndroidColor.argb(230, 255, 255, 255))
    bodyText.textSize = 18f
    bodyText.maxLines = 3
    bodyText.ellipsize = TextUtils.TruncateAt.END
    content.addView(bodyText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(10) })
    adView.bodyView = bodyText

    ctaText.gravity = Gravity.CENTER
    ctaText.setTextColor(AndroidColor.BLACK)
    ctaText.textSize = 18f
    ctaText.typeface = Typeface.DEFAULT_BOLD
    ctaText.background = roundedDrawable(AndroidColor.WHITE, context.dp(26).toFloat())
    content.addView(ctaText, LinearLayout.LayoutParams(context.dp(190), context.dp(54)).apply { topMargin = context.dp(24) })
    adView.callToActionView = ctaText

    return adView
}

private fun bindFullScreenNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    val media = adView.findNativeAsset<MediaView>(NativeAdAsset.Media)
    val icon = adView.findNativeAsset<ImageView>(NativeAdAsset.Icon)
    val headlineText = adView.findNativeAsset<TextView>(NativeAdAsset.Headline)
    val bodyText = adView.findNativeAsset<TextView>(NativeAdAsset.Body)
    val advertiserText = adView.findNativeAsset<TextView>(NativeAdAsset.Advertiser)
    val ctaText = adView.findNativeAsset<TextView>(NativeAdAsset.CallToAction)

    media.mediaContent = nativeAd.mediaContent
    headlineText.text = nativeAd.headline.orEmpty()
    bodyText.text = nativeAd.body.orEmpty()
    bodyText.visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
    advertiserText.text = nativeAd.advertiser ?: nativeAd.store ?: "Sponsored"
    ctaText.text = nativeAd.callToAction ?: "Learn More"

    val iconDrawable = nativeAd.icon?.drawable
    if (iconDrawable != null) {
        icon.setImageDrawable(iconDrawable)
        icon.visibility = View.VISIBLE
    } else {
        icon.visibility = View.GONE
    }

    adView.setNativeAd(nativeAd)
}

private enum class NativeAdAsset { Media, Icon, Headline, Body, Advertiser, CallToAction }

@Suppress("UNCHECKED_CAST")
private fun <T : View> NativeAdView.findNativeAsset(asset: NativeAdAsset): T =
    findViewWithTag<View>(asset) as T

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun roundedDrawable(color: Int, radius: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun MediaContent(post: Post, isVisible: Boolean = true) {
    val context = LocalContext.current
    var hasError by remember(post.id) { mutableStateOf(false) }
    var isLoading by remember(post.id) { mutableStateOf(true) }
    var enableVideo by remember(post.id) { mutableStateOf(false) }

    LaunchedEffect(post.id, post.videoUrl) {
        enableVideo = ENABLE_FEED_VIDEO_AUTOPLAY && post.videoUrl != null
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            ENABLE_FEED_VIDEO_AUTOPLAY && post.videoUrl != null && enableVideo && !hasError -> {
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
                            playWhenReady = isVisible
                        }
                }
                
                LaunchedEffect(isVisible) {
                    exoPlayer.playWhenReady = isVisible
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

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    MaterialTheme {
        FeedScreen(
            currentPostIndex = 0,
            onIndexChange = {}
        )
    }
}

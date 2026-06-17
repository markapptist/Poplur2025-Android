package com.apptists.poplur2025.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.text.TextUtils
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.ads.AdManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.AsyncImage
import com.apptists.poplur2025.data.DemoPostStore

import com.apptists.poplur2025.data.Post
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.components.FeedLaunchCover
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay
import com.apptists.poplur2025.ui.components.UserInfoOverlay
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FeedScreen(
    currentPostIndex: Int,
    isVisible: Boolean = true,
    tvGuideRequestToken: Int = 0,
    timelineToggleRequestToken: Int = 0,
    homeChromeInactive: Boolean = false,
    onOpenUser: (UserProfile) -> Unit = {},
    onTvGuideVisibilityChanged: (Boolean) -> Unit = {},
    onUserInteraction: () -> Unit = {},
    onIndexChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val isInspectionMode = LocalInspectionMode.current
    val currentSlot = remember(currentPostIndex) { resolveFeedSlot(currentPostIndex) }
    val guideChannels = remember { buildFeedTvGuideChannels() }
    var showTimelineView by remember { mutableStateOf(false) }
    var showTvGuide by remember { mutableStateOf(false) }
    var showFeedLogo by remember { mutableStateOf(true) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var selectedGuideChannelId by remember { mutableStateOf("feed") }
    var activeFeedPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var likedPulseToken by remember { mutableIntStateOf(0) }
    var showLikeBurst by remember { mutableStateOf(false) }
    val likedSlotIds = remember { mutableStateListOf<String>() }
    val feedRotationY by animateFloatAsState(
        targetValue = if (showTimelineView) 180f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "feedFlipRotation"
    )
    val showingTimelineSide = feedRotationY > 90f

    LaunchedEffect(tvGuideRequestToken) {
        if (tvGuideRequestToken > 0) showTvGuide = true
    }

    LaunchedEffect(timelineToggleRequestToken) {
        if (timelineToggleRequestToken > 0) {
            onUserInteraction()
            showTvGuide = false
            showTimelineView = true
        }
    }

    LaunchedEffect(showTvGuide) {
        onTvGuideVisibilityChanged(showTvGuide)
    }

    DisposableEffect(Unit) {
        onDispose { onTvGuideVisibilityChanged(false) }
    }
    
    val mediaPlayer = remember {
        if (isInspectionMode) null else {
            MediaPlayer().apply {
                try {
                    val afd = context.assets.openFd("pop.mp3")
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    setOnPreparedListener { it.start() }
                    prepareAsync()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    val playFeedPop = remember(mediaPlayer) {
        {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) player.pause()
                    player.seekTo(0)
                    player.start()
                } catch (_: Exception) {
                    // The async media player may not be prepared during the very first frame.
                }
            }
        }
    }

    fun likeCurrentSlot() {
        val slotId = currentSlot.feedSlotId
        if (!likedSlotIds.contains(slotId)) {
            likedSlotIds += slotId
        }
        onUserInteraction()
        playFeedPop()
        likedPulseToken += 1
        showLikeBurst = true
    }

    LaunchedEffect(likedPulseToken) {
        if (likedPulseToken <= 0) return@LaunchedEffect
        delay(680)
        showLikeBurst = false
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            activeFeedPlayer = null
            return@LaunchedEffect
        }
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                playFeedPop()
            }
        }
        showFeedLogo = true
        confettiTrigger += 1
        delay(2_500)
        showFeedLogo = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    cameraDistance = 16f * density
                    rotationY = if (showingTimelineSide) 90f else feedRotationY
                    alpha = if (showingTimelineSide) 0f else 1f
                }
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
                        MediaContent(
                            post = slot.post,
                            isVisible = isVisible,
                            useExternalVideoSurface = showTvGuide,
                            onActivePlayerChanged = { player -> activeFeedPlayer = player }
                        )
                    }
                    is FeedSlot.AdSlot -> FeedAdContent(
                        slot = slot,
                        isVisible = isVisible,
                        useExternalVideoSurface = showTvGuide,
                        onActivePlayerChanged = { player -> activeFeedPlayer = player }
                    )
                }
            }
        }

        if (!homeChromeInactive) {
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
                        location = slot.post.locationName,
                        channelNumber = slot.channelNumber,
                        channelEmoji = slot.channelEmoji,
                        channelCategory = slot.channelCategory
                    )
                    is FeedSlot.AdSlot -> UserInfoOverlay(
                        displayName = slot.creative.brandName,
                        location = slot.creative.locationName,
                        channelNumber = 100,
                        channelEmoji = "📢",
                        channelCategory = slot.creative.creativeKind
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
                        postsCount = slot.creative.postsCount,
                        radiusChange = slot.creative.radiusChange,
                        modifier = Modifier,
                        contentColor = Color.White
                    )
                }
            }

            // 4. Bottom Right Overlay (Animated Radius Data)
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
                        is FeedSlot.AdSlot -> slot.creative.radius
                    },
                    unit = "mi"
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(80f)
                .feedDoubleTapDetector(
                    onTap = onUserInteraction,
                    onDoubleTap = { likeCurrentSlot() }
                )
                .feedSwipeDetector(currentPostIndex, onIndexChange, onUserInteraction)
        )
        }

        if (showingTimelineSide) {
            PoplurNewspaper(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        cameraDistance = 16f * density
                        rotationY = feedRotationY - 180f
                    }
                    .zIndex(60f),
                onDoubleTap = { showTimelineView = false }
            )
        }

        BreakingNewsConfetti(
            trigger = confettiTrigger,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(70f)
        )

        androidx.compose.animation.AnimatedVisibility(
            visible = showLikeBurst,
            enter = scaleIn(initialScale = 0.42f, animationSpec = tween(150)) + fadeIn(animationSpec = tween(120)),
            exit = scaleOut(targetScale = 1.7f, animationSpec = tween(260)) + fadeOut(animationSpec = tween(260)),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(86f)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF365E),
                modifier = Modifier
                    .size(116.dp)
                    .shadow(18.dp, CircleShape)
            )
        }

        if (homeChromeInactive && !showingTimelineSide && !showTvGuide) {
            FeedLiveCommentsOverlay(
                slot = currentSlot,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(88f)
            )
        }

        // Full-screen launch cover — above everything while feed loads
        androidx.compose.animation.AnimatedVisibility(
            visible = showFeedLogo,
            enter = androidx.compose.animation.fadeIn(animationSpec = tween(200)),
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(500)),
            modifier = Modifier.fillMaxSize().zIndex(90f)
        ) {
            FeedLaunchCover(onPopSound = { playFeedPop() })
        }

        if (showTvGuide) {
            val currentPostName = when (val slot = currentSlot) {
                is FeedSlot.PostSlot -> slot.post.displayName
                is FeedSlot.AdSlot -> slot.creative.brandName
            }
            FeedTvGuideOverlay(
                channels = guideChannels,
                currentChannelId = selectedGuideChannelId,
                currentPostName = currentPostName,
                activeFeedPlayer = activeFeedPlayer,
                onChannelSelected = { channel ->
                    selectedGuideChannelId = channel.id
                    onIndexChange(channel.index)
                    showTvGuide = false
                }
            )
        }
    }
}

@Composable
private fun BreakingNewsConfetti(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) { buildBreakingNewsConfetti(trigger) }

    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2_200, easing = LinearEasing)
        )
    }

    if (trigger <= 0 || progress.value >= 1f) return

    ComposeCanvas(modifier = modifier) {
        particles.forEach { particle ->
            val localProgress = ((progress.value - particle.delay) / (1f - particle.delay)).coerceIn(0f, 1f)
            if (localProgress <= 0f || localProgress >= 1f) return@forEach

            val alpha = if (localProgress > 0.82f) {
                ((1f - localProgress) / 0.18f).coerceIn(0f, 1f)
            } else {
                1f
            }
            val x = size.width * particle.xFraction +
                particle.driftPx * sin(localProgress * Math.PI.toFloat() * 2f)
            val y = -48f + (size.height + 130f) * localProgress * particle.speed
            val pieceSize = particle.sizeDp.toPx()

            rotate(
                degrees = particle.rotation + localProgress * particle.spin,
                pivot = Offset(x, y)
            ) {
                drawRoundRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(x - pieceSize * 0.5f, y - pieceSize * 0.24f),
                    size = Size(pieceSize, pieceSize * 0.48f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

private data class BreakingNewsConfettiPiece(
    val xFraction: Float,
    val delay: Float,
    val driftPx: Float,
    val speed: Float,
    val sizeDp: androidx.compose.ui.unit.Dp,
    val rotation: Float,
    val spin: Float,
    val color: Color
)

private fun buildBreakingNewsConfetti(trigger: Int): List<BreakingNewsConfettiPiece> {
    val random = Random(trigger * 7_919)
    val colors = listOf(
        Color(0xFFFF3158),
        Color(0xFFFFD91A),
        Color(0xFFFF7A2D),
        Color(0xFFFFFFFF)
    )

    return List(110) {
        BreakingNewsConfettiPiece(
            xFraction = random.nextFloat(),
            delay = random.nextFloat() * 0.28f,
            driftPx = random.nextFloat() * 120f - 60f,
            speed = 0.84f + random.nextFloat() * 0.46f,
            sizeDp = (8 + random.nextInt(12)).dp,
            rotation = random.nextFloat() * 360f,
            spin = 220f + random.nextFloat() * 520f,
            color = colors[random.nextInt(colors.size)]
        )
    }
}

private fun Modifier.feedDoubleTapDetector(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit
): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onTap = { onTap() },
        onDoubleTap = { onDoubleTap() }
    )
}

private fun Modifier.feedSwipeDetector(
    currentPostIndex: Int,
    onIndexChange: (Int) -> Unit,
    onInteraction: () -> Unit
): Modifier = pointerInput(currentPostIndex) {
    var totalY = 0f
    var triggered = false
    detectVerticalDragGestures(
        onDragStart = {
            totalY = 0f
            triggered = false
            onInteraction()
        },
        onVerticalDrag = { change, dragAmount ->
            if (!triggered) {
                totalY += dragAmount
                if (abs(totalY) > 28f) {
                    onIndexChange(if (totalY > 0) currentPostIndex - 1 else currentPostIndex + 1)
                    triggered = true
                    change.consume()
                }
            }
        }
    )
}

private data class FeedLiveComment(
    val id: String,
    val username: String,
    val text: String
)

@Composable
private fun FeedLiveCommentsOverlay(
    slot: FeedSlot,
    modifier: Modifier = Modifier
) {
    val slotId = slot.feedSlotId
    val seededComments = remember(slotId) { seedLiveComments(slot) }
    var userComments by remember(slotId) { mutableStateOf<List<FeedLiveComment>>(emptyList()) }
    var draft by remember(slotId) { mutableStateOf("") }
    val visibleComments = remember(seededComments, userComments) {
        (seededComments + userComments).takeLast(5)
    }

    Box(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 18.dp)
                .widthIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleComments.forEach { comment ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.animateContentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comment.username.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = "${comment.username}  ${comment.text}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 17.sp,
                        modifier = Modifier
                            .shadow(4.dp)
                            .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 2.dp, vertical = 1.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = {
                        Text("Comment", color = Color.White.copy(alpha = 0.58f), fontWeight = FontWeight.Bold)
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.58f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.38f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.34f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.34f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val trimmed = draft.trim()
                        if (trimmed.isNotEmpty()) {
                            userComments = userComments + FeedLiveComment(
                                id = "user-${System.nanoTime()}",
                                username = "you",
                                text = trimmed
                            )
                            draft = ""
                        }
                    },
                    enabled = draft.trim().isNotEmpty(),
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.34f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (draft.trim().isEmpty()) Color.White.copy(alpha = 0.36f) else Color.White
                    )
                }
            }
        }
    }
}

private val FeedSlot.feedSlotId: String
    get() = when (this) {
        is FeedSlot.PostSlot -> post.id
        is FeedSlot.AdSlot -> id
    }

private fun seedLiveComments(slot: FeedSlot): List<FeedLiveComment> {
    val displayName = when (slot) {
        is FeedSlot.PostSlot -> slot.post.displayName.ifBlank { slot.post.username }
        is FeedSlot.AdSlot -> slot.creative.brandName
    }.ifBlank { "this" }
    val samples = listOf(
        FeedLiveComment("live-1", "vi.xi.mmvi", "this is live right now"),
        FeedLiveComment("live-2", "leurdavoir", "keep the camera there"),
        FeedLiveComment("live-3", "monsekee", "the radius is going crazy"),
        FeedLiveComment("live-4", "poplur", "watching $displayName")
    )
    val offset = Math.floorMod(slot.feedSlotId.hashCode(), samples.size)
    return samples.drop(offset) + samples.take(offset)
}

private data class FeedTvGuideChannel(
    val id: String,
    val index: Int,
    val number: Int,
    val emoji: String,
    val category: String,
    val profile: UserProfile,
    val program: String,
    val videoUrl: String? = null
)

private fun buildFeedTvGuideChannels(): List<FeedTvGuideChannel> {
    val categories = listOf(
        "🍔" to "Food",
        "🎮" to "Gaming",
        "🎵" to "Music",
        "🏙️" to "City",
        "💄" to "Style",
        "⚽" to "Sports",
        "🎬" to "Film",
        "🛍️" to "Shopping"
    )
    val uniquePosts = DemoPostStore.posts
        .filter { it.userId.isNotBlank() && it.userId != "ad_partner" }
        .distinctBy { it.userId }
        .take(categories.size)

    return uniquePosts.mapIndexed { index, post ->
        val (emoji, category) = categories[index % categories.size]
        FeedTvGuideChannel(
            id = "channel_${post.userId.ifBlank { index.toString() }}",
            index = index,
            number = 101 + index,
            emoji = emoji,
            category = category,
            profile = UserProfile(
                id = post.userId,
                username = post.username,
                displayName = post.displayName,
                bio = post.caption,
                radius = post.radius,
                posts = post.userPostsCount
            ),
            program = post.locationName.ifBlank { post.caption },
            videoUrl = post.videoUrl
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Poplur Newspaper
// ─────────────────────────────────────────────────────────────────────────────

private data class NpMapEvent(
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val userCount: Int, val timeAgo: String
)

private data class NpTrending(
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val growthPercent: Int, val activeUsers: Int, val timeAgo: String
)

private data class NpDominance(
    val username: String,
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val radiusText: String, val score: Int, val timeAgo: String
)

private data class NpUserPost(
    val username: String,
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val likes: Int, val comments: Int, val timeAgo: String
)

private sealed class NpArticle {
    data class MapEvent(val d: NpMapEvent)   : NpArticle()
    data class Trending(val d: NpTrending)   : NpArticle()
    data class Dominance(val d: NpDominance) : NpArticle()
    data class UserPost(val d: NpUserPost)   : NpArticle()
}

@Composable
private fun PoplurNewspaper(
    modifier: Modifier = Modifier,
    onDoubleTap: () -> Unit
) {
    val articles = remember { NewspaperContentGen.generate(24) }
    val today = remember {
        java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US).format(java.util.Date())
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .feedDoubleTapDetector(onTap = {}, onDoubleTap = onDoubleTap)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 140.dp)) {
            item { NewspaperMasthead(date = today) }
            items(articles.size) { index ->
                NewspaperCard(article = articles[index], edition = index + 1)
            }
        }
    }
}

@Composable
private fun NewspaperMasthead(date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("POPLUR", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text(" DAILY", color = Color(0xFFFFD91A), fontSize = 38.sp, fontWeight = FontWeight.ExtraLight)
            Spacer(Modifier.weight(1f))
            Text(date, color = Color.White.copy(alpha = 0.38f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.85f), thickness = 2.dp)
        Spacer(Modifier.height(3.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.18f), thickness = 1.dp)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun NewspaperCard(article: NpArticle, edition: Int) {
    val chipBg: Color
    val chipFg: Color
    val chipLabel: String
    val flag: String
    val city: String
    val timeAgo: String
    val headline: String
    val detail: String
    val stat1: String
    val stat2: String
    var extraRow: (@Composable () -> Unit)? = null

    when (article) {
        is NpArticle.MapEvent -> {
            chipBg = Color(0xFFFFD91A); chipFg = Color.Black; chipLabel = "MAP EVENT"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "👥 ${npCount(article.d.userCount)} users nearby"
            stat2 = "📍 Live tracking"
        }
        is NpArticle.Trending -> {
            chipBg = Color(0xFF2EE880); chipFg = Color.Black; chipLabel = "TRENDING"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "📈 +${article.d.growthPercent}% engagement"
            stat2 = "🟢 ${npCount(article.d.activeUsers)} active"
        }
        is NpArticle.Dominance -> {
            chipBg = Color(0xFFD91818); chipFg = Color.White; chipLabel = "DOMINANCE"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "🎯 ${article.d.radiusText} radius"
            stat2 = "⭐ ${npCount(article.d.score)} score"
            extraRow = {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👑", fontSize = 24.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "@${article.d.username}",
                        color = Color(0xFFFFD91A),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        is NpArticle.UserPost -> {
            chipBg = Color.White; chipFg = Color.Black; chipLabel = "USER POST"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "❤️ ${npCount(article.d.likes)} likes"
            stat2 = "💬 ${article.d.comments} comments"
            extraRow = {
                Text(
                    "@${article.d.username}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .background(Color(0xFF0D0D0D), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp)
    ) {
        // Category + location row
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                chipLabel,
                color = chipFg, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                modifier = Modifier
                    .background(chipBg, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("$flag $city", color = Color.White.copy(alpha = 0.52f), fontSize = 12.sp, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text(timeAgo, color = Color.White.copy(alpha = 0.32f), fontSize = 11.sp)
        }

        // Optional extra row (crown+username, or @username)
        extraRow?.invoke()

        // Headline
        Text(
            headline,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 30.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )

        // Separator
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.09f),
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Detail body
        Text(
            detail,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Stats
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stat1, color = Color.White.copy(alpha = 0.38f), fontSize = 12.sp, maxLines = 1)
            Text(stat2, color = Color.White.copy(alpha = 0.38f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

private fun npCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000     -> "%.1fK".format(n / 1_000.0)
    else           -> "$n"
}

private object NewspaperContentGen {

    private val cities = listOf(
        "Tokyo" to "🇯🇵", "London" to "🇬🇧", "New York" to "🇺🇸",
        "Paris" to "🇫🇷", "Sydney" to "🇦🇺", "Toronto" to "🇨🇦",
        "Dubai" to "🇦🇪", "Singapore" to "🇸🇬", "Berlin" to "🇩🇪",
        "Barcelona" to "🇪🇸", "Seoul" to "🇰🇷", "São Paulo" to "🇧🇷",
        "Mumbai" to "🇮🇳", "Bangkok" to "🇹🇭", "Istanbul" to "🇹🇷",
        "Lagos" to "🇳🇬", "Cairo" to "🇪🇬", "Mexico City" to "🇲🇽",
        "Jakarta" to "🇮🇩", "Chicago" to "🇺🇸"
    )

    private val mapHeadlines = listOf(
        "Viral Surge Detected Near Central District" to
            "Poplur AI flagged an unusual spike in post activity within a 2 km radius. Over 3,200 users captured content here in the last 90 minutes — a 580% jump above baseline.",
        "Flash Crowd Emerges at the Waterfront" to
            "Thermal activity on the Poplur map suggests a spontaneous gathering near the harbor. Real-time post velocity has tripled since 11:40 PM.",
        "Stadium Energy Spikes — Major Event Underway" to
            "AI analysis of geotagged post density confirms a live event. Post likes are averaging 4.2× higher than non-event content.",
        "Urban Exploration Trend Sweeps the Old Quarter" to
            "A coordinated wave of Poplur users is documenting the historic district. AI identified a 'discovery cluster' — 14 unique rooftops tagged in under 3 hours.",
        "Unofficial Street Festival — Not on Any Calendar" to
            "Post clusters and density heatmaps point to a music gathering near the market district. Poplur AI classifies this as an 'organic event emergence.'",
        "Dawn Patrol Runners Form Largest Fitness Cluster of the Week" to
            "Riverside park runners generated the highest fitness post density this week. AI estimates 800+ active users within a 500 m corridor at first light.",
        "Night Market Buzz at Seasonal Record" to
            "Poplur's AI tracked a 12-hour buildup of food and shopping posts. Engagement on night market content is 2.9× the platform average."
    )

    private data class TrendTemplate(
        val headline: String, val detail: String, val growth: Int, val active: Int
    )
    private val trendHeadlines = listOf(
        TrendTemplate("Post Volume Triples — City Breaks into Global Top 10",
            "An unprecedented wave of content creation catapulted this city into Poplur's weekly global leaderboard. AI attributes the surge to weather, a cultural moment, and influencer activity.", 312, 14_200),
        TrendTemplate("City's Engagement Rate Hits All-Time Platform Record",
            "Average post likes-per-view surpassed every prior benchmark on Poplur. AI identifies rising creators in food, street art, and fitness as key drivers.", 287, 9_800),
        TrendTemplate("Overnight Viral Moment Puts City on the Global Map",
            "A single video generated 2.1M views in 8 hours, triggering a cascade of follow-on posts. Poplur AI classifies this as a 'viral echo event.'", 445, 22_500),
        TrendTemplate("Longest-Running Trend This Year — Day 4 Continues",
            "This city has maintained trending status for four consecutive days — the longest streak in 2024. Sustained content quality and high radius posts keep momentum going.", 198, 7_400),
        TrendTemplate("New Creator Cohort Drives 8× Weekend Engagement Spike",
            "Poplur AI detected an emerging cluster of high-quality creators in this metro who collectively drove an 8× spike in city-level engagement over one weekend.", 792, 31_000)
    )

    private val domHeadlines = listOf(
        "Claims Full Control of Metro District After 72-Hour Push" to
            "Through sustained high-radius posts and aggressive engagement, this user has locked down the metro zone. Challengers within 10 km have been pushed off the leaderboard.",
        "Breaks City Record — Largest Radius Ever Logged in Region" to
            "A single post blanketed the entire metropolitan area, outranking 340 competing accounts. Poplur AI flagged this as a historic radius achievement.",
        "Retains Crown for Third Consecutive Week" to
            "Despite heavy competition, this user defended their dominant position for three weeks running. AI analysis cites content consistency as the primary factor.",
        "Tactical Late-Night Push Flips the City Leaderboard" to
            "In a calculated off-peak move at 2:47 AM, high-quality posts flipped the entire city ranking. AI ranked this as one of the top-10 most strategic dominance moves this year.",
        "From Zero to City Champion in Just 11 Days" to
            "This account went from newcomer to city champion in a record 11 days. Rapid follower growth, viral content, and precision radius targeting are the winning formula."
    )

    private val postHeadlines = listOf(
        "Captured Something the City Hasn't Seen Before" to
            "This post's unique angle generated immediate traction. Poplur AI flagged it as 'discovery content' — documenting a moment with genuine novelty value.",
        "A Moment the Algorithm Loved" to
            "High engagement velocity, strong radius, and early reshare activity pushed this post into the trending queue within minutes of upload.",
        "Local Insight. Global Reach." to
            "What started as a personal moment quickly resonated far beyond this creator's immediate circle. The post's radius has expanded three times since publication.",
        "Raw. Unfiltered. Trending." to
            "No filters, no production budget — just authentic street-level content that the Poplur community responded to immediately.",
        "This Post Is Why Poplur Exists" to
            "A perfectly timed, genuinely local moment — exactly the kind of content that defines what Poplur was built for."
    )

    private val domNames = listOf(
        "xX_King_Xx", "CityQueen_Riri", "BlazeRunner", "NightOwl_Kai",
        "UrbanHawk", "NovaStar_Lena", "GridLock_Ty", "SkyWatcher_J",
        "PoplurElite99", "StreetKing_Mav", "TowerVault_Em", "CityPulse_Zara"
    )

    private val times       = listOf("2m", "5m", "11m", "18m", "27m", "43m", "1h", "2h", "3h", "5h", "8h", "12h")
    private val radii       = listOf("500m", "2km", "5km", "12km", "28km", "50km", "100km", "500km")
    private val scores      = listOf(1_200, 3_400, 7_800, 15_000, 42_000, 88_000, 210_000, 550_000)
    private val userCounts  = listOf(820, 1_240, 2_100, 3_400, 560, 4_800, 720, 9_300)
    private val fakeLikes   = listOf(88, 234, 1_200, 340, 56, 3_100, 780)
    private val fakeComms   = listOf(12, 34, 88, 210, 7, 400, 55)
    private val fakeHandles = listOf("MavericK", "SkylineJ", "UrbanLens", "NovaBeat")

    fun generate(count: Int): List<NpArticle> = (0 until count).map { gi ->
        val (cityName, cityFlag) = cities[(gi * 7 + gi * 3) % cities.size]
        val timeAgo = times[gi % times.size] + " ago"
        when (gi % 20) {
            in 0..5 -> {
                val (h, d) = mapHeadlines[gi % mapHeadlines.size]
                NpArticle.MapEvent(NpMapEvent(cityName, cityFlag, h, d, userCounts[gi % userCounts.size], timeAgo))
            }
            in 6..10 -> {
                val t = trendHeadlines[gi % trendHeadlines.size]
                NpArticle.Trending(NpTrending(cityName, cityFlag, t.headline, t.detail, t.growth, t.active, timeAgo))
            }
            in 11..15 -> {
                val (h, d) = domHeadlines[gi % domHeadlines.size]
                NpArticle.Dominance(NpDominance(domNames[gi % domNames.size], cityName, cityFlag, h, d,
                    radii[gi % radii.size], scores[gi % scores.size], timeAgo))
            }
            else -> {
                val (h, d) = postHeadlines[gi % postHeadlines.size]
                NpArticle.UserPost(NpUserPost(fakeHandles[gi % fakeHandles.size], cityName, cityFlag, h, d,
                    fakeLikes[gi % fakeLikes.size], fakeComms[gi % fakeComms.size], timeAgo))
            }
        }
    }
}

@Composable
private fun FeedTvGuideOverlay(
    channels: List<FeedTvGuideChannel>,
    currentChannelId: String,
    currentPostName: String,
    activeFeedPlayer: ExoPlayer?,
    onChannelSelected: (FeedTvGuideChannel) -> Unit
) {
    val guideGreen = Color(0xFF4EFF40)
    var isTabletMenuOpen by remember { mutableStateOf(true) }
    // Tracks which channel the PiP is previewing — starts on the current feed channel.
    // Single-tap a row updates this; double-tap navigates to the feed.
    var previewedChannelId by remember(currentChannelId) { mutableStateOf(currentChannelId) }
    val previewedChannel = channels.firstOrNull { it.id == previewedChannelId } ?: channels.firstOrNull()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(120f)
            .background(Color.Black.copy(alpha = 0.78f))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isTablet = maxWidth >= 700.dp
        val pipWidth = if (isTablet) 200.dp else 148.dp
        val pipHeight = pipWidth * (16f / 9f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        ) {
            FeedTvGuideHeader(
                isTablet = isTablet,
                isRailOpen = isTabletMenuOpen,
                guideGreen = guideGreen,
                onToggleRail = { isTabletMenuOpen = !isTabletMenuOpen }
            )

            if (isTablet) {
                Row(Modifier.fillMaxSize()) {
                    if (isTabletMenuOpen) {
                        FeedTvGuideRail(
                            channels = channels,
                            currentChannelId = previewedChannelId,
                            guideGreen = guideGreen,
                            onChannelSelected = { previewedChannelId = it.id },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                    // List with floating PiP overlay
                    Box(modifier = Modifier.weight(1f)) {
                        FeedTvGuideList(
                            channels = channels,
                            previewedChannelId = previewedChannelId,
                            guideGreen = guideGreen,
                            onChannelPreviewed = { previewedChannelId = it.id },
                            onChannelSelected = onChannelSelected,
                            modifier = Modifier.fillMaxSize()
                        )
                        previewedChannel?.let { ch ->
                            GuidePipVideoPane(
                                channel = ch,
                                currentPostName = if (ch.id == currentChannelId) currentPostName else "",
                                activeFeedPlayer = activeFeedPlayer,
                                guideGreen = guideGreen,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 12.dp)
                                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp))
                                    .width(pipWidth)
                                    .height(pipHeight)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            } else {
                // Phone: list with floating PiP corner overlay
                Box(modifier = Modifier.fillMaxSize()) {
                    FeedTvGuideList(
                        channels = channels,
                        previewedChannelId = previewedChannelId,
                        guideGreen = guideGreen,
                        onChannelPreviewed = { previewedChannelId = it.id },
                        onChannelSelected = onChannelSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                    previewedChannel?.let { ch ->
                        GuidePipVideoPane(
                            channel = ch,
                            currentPostName = if (ch.id == currentChannelId) currentPostName else "",
                            activeFeedPlayer = activeFeedPlayer,
                            guideGreen = guideGreen,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 12.dp)
                                .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp))
                                .width(pipWidth)
                                .height(pipHeight)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedTvGuideHeader(
    isTablet: Boolean,
    isRailOpen: Boolean,
    guideGreen: Color,
    onToggleRail: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.86f))
            .padding(horizontal = if (isTablet) 24.dp else 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isTablet) {
            Text(
                text = if (isRailOpen) "☰" else "☷",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .width(44.dp)
                    .clickable(onClick = onToggleRail)
            )
        }
        Column {
            Text("TV GUIDE", color = Color.White, fontSize = if (isTablet) 34.sp else 28.sp, fontWeight = FontWeight.Black)
            Text("LIVE CHANNELS", color = guideGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FeedTvGuideRail(
    channels: List<FeedTvGuideChannel>,
    currentChannelId: String,
    guideGreen: Color,
    onChannelSelected: (FeedTvGuideChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.46f))
            .padding(top = 10.dp)
    ) {
        channels.forEach { channel ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(if (channel.id == currentChannelId) guideGreen.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onChannelSelected(channel) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(channel.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(channel.number.toString(), color = guideGreen, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Text(
                    channel.category.uppercase(),
                    color = Color.White.copy(alpha = if (channel.id == currentChannelId) 1f else 0.62f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeedTvGuideList(
    channels: List<FeedTvGuideChannel>,
    previewedChannelId: String,
    guideGreen: Color,
    onChannelPreviewed: (FeedTvGuideChannel) -> Unit,
    onChannelSelected: (FeedTvGuideChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.36f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text("CH", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(96.dp))
            Text("CATEGORY", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(134.dp))
            Text("ON NOW", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(channels) { channel ->
                FeedTvGuideRow(
                    channel = channel,
                    isPreviewed = channel.id == previewedChannelId,
                    guideGreen = guideGreen,
                    onChannelPreviewed = { onChannelPreviewed(channel) },
                    onChannelSelected = { onChannelSelected(channel) }
                )
            }
        }
    }
}

@Composable
private fun FeedTvGuideRow(
    channel: FeedTvGuideChannel,
    isPreviewed: Boolean,
    guideGreen: Color,
    onChannelPreviewed: () -> Unit,
    onChannelSelected: () -> Unit
) {
    val rowColor = when {
        isPreviewed -> Color(0xFF172917)
        channel.number % 2 == 0 -> Color(0xFF121316)
        else -> Color(0xFF0B0D0F)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(rowColor)
            .pointerInput(channel.id) {
                detectTapGestures(
                    onTap = { onChannelPreviewed() },
                    onDoubleTap = { onChannelSelected() }
                )
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.width(96.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(channel.emoji, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = channel.number.toString(),
                color = guideGreen,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = channel.category.uppercase(),
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(134.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.profile.displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = channel.program,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "LIVE",
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(guideGreen)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun GuidePipVideoPane(
    channel: FeedTvGuideChannel,
    currentPostName: String,
    activeFeedPlayer: ExoPlayer?,
    guideGreen: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoUrl = channel.videoUrl

    val fallbackPlayer = remember(videoUrl, activeFeedPlayer) {
        if (activeFeedPlayer != null || videoUrl == null) null
        else ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.fromUri(videoUrl)
            setMediaItem(item)
            volume = 0f
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(fallbackPlayer) { onDispose { fallbackPlayer?.release() } }

    val pipPlayer = activeFeedPlayer ?: fallbackPlayer

    Box(modifier = modifier) {
        // Video surface
        if (pipPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).also { textureView ->
                        textureView.isOpaque = false
                        pipPlayer.setVideoTextureView(textureView)
                    }
                },
                update = { textureView ->
                    pipPlayer.setVideoTextureView(textureView)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        // Green border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, guideGreen.copy(alpha = 0.80f), RoundedCornerShape(10.dp))
        )

        // Bottom info scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.80f)
                        )
                    )
                )
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(channel.emoji, fontSize = 13.sp)
                    Text(
                        "CH ${channel.number}",
                        color = guideGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    currentPostName.ifBlank { channel.profile.displayName },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "LIVE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(guideGreen)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private const val AD_FREQUENCY = 6
private const val FEED_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
private const val ENABLE_FEED_VIDEO_AUTOPLAY = true
private const val FEED_VIDEO_FIRST_FRAME_TIMEOUT_MS = 6_500L

private data class PoplurAdCreative(
    val id: String,
    val brandName: String,
    val headline: String,
    val body: String,
    val callToAction: String,
    val creativeKind: String,
    val posterUrl: String,
    val videoUrl: String?,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val postsCount: Int,
    val radiusChange: Int,
    val driveFileId: String? = null
)

private val POPLUR_AD_CREATIVES = listOf(
    PoplurAdCreative(
        id = "nearby-reel",
        brandName = "Poplur Ad",
        headline = "Real people are already nearby",
        body = "Human-first Poplur ad slot for finding pop-ups, food, and people around you.",
        callToAction = "Open the map",
        creativeKind = "Live human creative",
        posterUrl = "file:///android_asset/feed_ads/poplur_ad_real_humans.jpg",
        videoUrl = null,
        locationName = "New York, NY",
        latitude = 40.7128,
        longitude = -74.0060,
        radius = 1250,
        postsCount = 840,
        radiusChange = 18
    ),
    PoplurAdCreative(
        id = "three-d-city-tour",
        brandName = "Poplur Ad",
        headline = "Your city is already talking",
        body = "3D human figures bring local radius discovery into a premium city ad slot.",
        callToAction = "Start a Poplur tour",
        creativeKind = "3D human creative",
        posterUrl = "file:///android_asset/feed_ads/poplur_ad_3d_group.jpg",
        videoUrl = null,
        locationName = "Toronto, ON",
        latitude = 43.6532,
        longitude = -79.3832,
        radius = 950,
        postsCount = 730,
        radiusChange = 14
    ),
    PoplurAdCreative(
        id = "radius-hero",
        brandName = "Poplur Ad",
        headline = "Post it where it matters",
        body = "Dimensional creator-led ad slot for radius sharing, local discovery, and city energy.",
        callToAction = "Share with your radius",
        creativeKind = "3D figure creative",
        posterUrl = "file:///android_asset/feed_ads/poplur_ad_3d_creator.jpg",
        videoUrl = null,
        locationName = "Los Angeles, CA",
        latitude = 34.0522,
        longitude = -118.2437,
        radius = 1500,
        postsCount = 910,
        radiusChange = 22
    )
)

private fun poplurAdCreativeForIndex(index: Int): PoplurAdCreative =
    POPLUR_AD_CREATIVES[Math.floorMod(index, POPLUR_AD_CREATIVES.size)]

private fun PoplurAdCreative.toPost(slotId: String): Post = Post(
    id = slotId,
    userId = "poplur_ad_$id",
    username = "poplur_ad",
    displayName = brandName,
    userPostsCount = postsCount,
    caption = headline,
    mediaUrl = posterUrl,
    videoUrl = videoUrl,
    latitude = latitude,
    longitude = longitude,
    locationName = locationName,
    likes = postsCount,
    comments = max(12, postsCount / 12),
    radius = radius,
    totalRadius = radius * 4,
    radiusChange = radiusChange
)


private data class FeedChannel(val number: Int, val emoji: String, val category: String)

private val FEED_CHANNELS = listOf(
    FeedChannel(101, "🍔", "Food"),
    FeedChannel(102, "🎮", "Gaming"),
    FeedChannel(103, "🎵", "Music"),
    FeedChannel(104, "🏙️", "City"),
    FeedChannel(105, "💄", "Style"),
    FeedChannel(106, "⚽", "Sports"),
    FeedChannel(107, "🎬", "Film"),
    FeedChannel(108, "🛍️", "Shopping")
)

private sealed interface FeedSlot {
    data class PostSlot(
        val post: Post,
        val channelNumber: Int,
        val channelEmoji: String,
        val channelCategory: String
    ) : FeedSlot
    data class AdSlot(val id: String, val creative: PoplurAdCreative) : FeedSlot
}

private fun resolveFeedSlot(index: Int): FeedSlot {
    val normalized = if (index < 0) {
        val cycle = AD_FREQUENCY * DemoPostStore.posts.size
        (cycle + (index % cycle)) % cycle
    } else {
        index
    }
    if ((normalized + 1) % AD_FREQUENCY == 0) {
        return FeedSlot.AdSlot(
            id = "poplur-ad-$normalized",
            creative = poplurAdCreativeForIndex(normalized)
        )
    }
    val adsBefore = (normalized + 1) / AD_FREQUENCY
    val postIndex = normalized - adsBefore
    val ch = FEED_CHANNELS[Math.floorMod(postIndex, FEED_CHANNELS.size)]
    return FeedSlot.PostSlot(
        post = DemoPostStore.getPostForIndex(postIndex),
        channelNumber = ch.number,
        channelEmoji = ch.emoji,
        channelCategory = ch.category
    )
}

@Composable
private fun FeedAdContent(
    slot: FeedSlot.AdSlot,
    isVisible: Boolean,
    useExternalVideoSurface: Boolean,
    onActivePlayerChanged: (ExoPlayer?) -> Unit
) {
    val adPost = remember(slot.id) { slot.creative.toPost(slot.id) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MediaContent(
            post = adPost,
            isVisible = isVisible,
            useExternalVideoSurface = useExternalVideoSurface,
            onActivePlayerChanged = onActivePlayerChanged
        )
        PoplurAdOverlay(creative = slot.creative)
    }
}

@Composable
private fun PoplurAdOverlay(creative: PoplurAdCreative) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.76f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 138.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "POPLUR AD",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = creative.headline,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 37.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = creative.body,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .padding(horizontal = 17.dp, vertical = 9.dp)
            ) {
                Text(
                    text = creative.callToAction,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



private fun createFullScreenNativeAdView(context: Context): NativeAdView {
    val adView = NativeAdView(context)
    val media = MediaView(context).apply {
        tag = NativeAdAsset.Media
        setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    }
    val icon = ImageView(context).apply { tag = NativeAdAsset.Icon }
    val sponsoredText = TextView(context)
    val headlineText = TextView(context).apply { tag = NativeAdAsset.Headline }
    val bodyText = TextView(context).apply { tag = NativeAdAsset.Body }
    val advertiserText = TextView(context).apply { tag = NativeAdAsset.Advertiser }
    val ctaText = TextView(context).apply { tag = NativeAdAsset.CallToAction }

    adView.setBackgroundColor(AndroidColor.TRANSPARENT)

    media.setBackgroundColor(AndroidColor.TRANSPARENT)
    media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
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
    media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
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
fun MediaContent(
    post: Post,
    isVisible: Boolean = true,
    useExternalVideoSurface: Boolean = false,
    onActivePlayerChanged: (ExoPlayer?) -> Unit = {}
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    var hasError by remember(post.id) { mutableStateOf(false) }
    var isLoading by remember(post.id) { mutableStateOf(true) }
    var hasRenderedVideo by remember(post.id) { mutableStateOf(false) }
    var videoFailed by remember(post.id) { mutableStateOf(false) }
    var posterFailed by remember(post.id) { mutableStateOf(false) }
    var imageUrl by remember(post.id) { mutableStateOf(post.mediaUrl ?: fallbackFeedImageUrl(post.id)) }
    var videoSize by remember(post.id) { mutableStateOf<VideoSize?>(null) }

    fun acceptOrReplaceImage(drawable: Drawable, failPostWhenRejected: Boolean) {
        if (drawable.hasLikelyBlackBars()) {
            val fallbackUrl = fallbackFeedImageUrl(post.id)
            if (imageUrl != fallbackUrl) {
                imageUrl = fallbackUrl
                isLoading = true
            } else {
                posterFailed = true
                isLoading = false
                if (failPostWhenRejected) hasError = true
            }
        } else {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isInspectionMode) {
            // Show poster in inspection mode to avoid black screen
            AsyncImage(
                model = post.mediaUrl,
                contentDescription = post.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            when {
                ENABLE_FEED_VIDEO_AUTOPLAY && post.videoUrl != null && !videoFailed -> {
                    val exoPlayer = remember(post.id, post.videoUrl) {
                        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                            .setUserAgent("Poplur2025 Android Player")
                            .setConnectTimeoutMs(8_000)
                            .setReadTimeoutMs(12_000)
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
                                val mediaItem = MediaItem.Builder()
                                    .setUri(post.videoUrl)
                                    .build()
                                setMediaItem(mediaItem)
                                repeatMode = Player.REPEAT_MODE_ONE
                                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                                volume = 1f
                                addListener(object : Player.Listener {
                                    override fun onPlaybackStateChanged(state: Int) {
                                        isLoading = state == Player.STATE_BUFFERING || (state == Player.STATE_READY && !hasRenderedVideo)
                                    }

                                    override fun onRenderedFirstFrame() {
                                        hasRenderedVideo = true
                                        isLoading = false
                                    }

                                    override fun onVideoSizeChanged(size: VideoSize) {
                                        videoSize = size
                                    }

                                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                        println("ExoPlayer Error: ${error.message}")
                                        videoFailed = true
                                        hasError = false
                                        isLoading = false
                                    }
                                })
                                prepare()
                                playWhenReady = isVisible
                            }
                    }

                    LaunchedEffect(exoPlayer, isVisible, post.id) {
                        if (isVisible) {
                            onActivePlayerChanged(exoPlayer)
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                            }
                            exoPlayer.playWhenReady = true
                            exoPlayer.play()
                        } else {
                            onActivePlayerChanged(null)
                            exoPlayer.playWhenReady = false
                            exoPlayer.pause()
                        }
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose {
                            onActivePlayerChanged(null)
                            exoPlayer.pause()
                            exoPlayer.release()
                        }
                    }

                    LaunchedEffect(post.id, post.videoUrl, hasRenderedVideo, videoFailed) {
                        if (!hasRenderedVideo && !videoFailed) {
                            delay(FEED_VIDEO_FIRST_FRAME_TIMEOUT_MS)
                            if (!hasRenderedVideo && !videoFailed) {
                                videoFailed = true
                                isLoading = false
                            }
                        }
                    }

                    if (!hasRenderedVideo && !posterFailed) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = post.caption,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(2f),
                            contentScale = ContentScale.Crop,
                            onLoading = { isLoading = true },
                            onSuccess = { state ->
                                acceptOrReplaceImage(state.result.drawable, failPostWhenRejected = false)
                            },
                            onError = {
                                val fallbackUrl = fallbackFeedImageUrl(post.id)
                                if (imageUrl != fallbackUrl) {
                                    imageUrl = fallbackUrl
                                } else {
                                    posterFailed = true
                                    isLoading = false
                                }
                            }
                        )
                    }

                    AndroidView(
                        factory = { viewContext ->
                            TextureView(viewContext).apply {
                                isOpaque = false
                                isClickable = false
                                isFocusable = false
                                alpha = 0f
                                visibility = View.INVISIBLE
                                if (!useExternalVideoSurface) {
                                    exoPlayer.setVideoTextureView(this)
                                }
                            }
                        },
                        update = { textureView ->
                            if (!useExternalVideoSurface) {
                                exoPlayer.setVideoTextureView(textureView)
                            }
                            textureView.applyCenterCropTransform(videoSize)
                            textureView.alpha = if (hasRenderedVideo) 1f else 0f
                            textureView.visibility = if (hasRenderedVideo) View.VISIBLE else View.INVISIBLE
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                    )
                }
                imageUrl.isNotBlank() -> {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = post.caption,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f),
                        contentScale = ContentScale.Crop,
                        onLoading = { isLoading = true },
                        onSuccess = { state ->
                            acceptOrReplaceImage(state.result.drawable, failPostWhenRejected = true)
                        },
                        onError = {
                            val fallbackUrl = fallbackFeedImageUrl(post.id)
                            if (imageUrl != fallbackUrl) {
                                imageUrl = fallbackUrl
                            } else {
                                posterFailed = true
                                isLoading = false
                                hasError = true
                            }
                        }
                    )
                }
                else -> {
                    hasError = true
                }
            }
        }

        if (hasError) {
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

        if (isLoading && !hasError) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f))
        }
    }
}

private fun fallbackFeedImageUrl(seed: String): String {
    val names = arrayOf("10", "28", "37", "58", "82", "96", "102", "119", "128", "164", "175", "184")
    val index = Math.floorMod(seed.hashCode(), names.size)
    return "file:///android_asset/feed_media/feed_${names[index]}.jpg"
}

@Suppress("DEPRECATION")
private fun TextureView.applyCenterCropTransform(videoSize: VideoSize?) {
    val viewWidth = width.toFloat()
    val viewHeight = height.toFloat()
    val size = videoSize
    val sourceWidth = size?.width ?: 0
    val sourceHeight = size?.height ?: 0
    if (viewWidth <= 0f || viewHeight <= 0f || sourceWidth <= 0 || sourceHeight <= 0) {
        setTransform(null)
        return
    }

    val pixelRatio = size?.pixelWidthHeightRatio ?: 1f
    val rotated = size?.unappliedRotationDegrees == 90 || size?.unappliedRotationDegrees == 270
    val videoWidth = if (rotated) sourceHeight.toFloat() else sourceWidth * pixelRatio
    val videoHeight = if (rotated) sourceWidth * pixelRatio else sourceHeight.toFloat()
    val scale = max(viewWidth / videoWidth, viewHeight / videoHeight)
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale
    val matrix = Matrix().apply {
        setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, viewWidth / 2f, viewHeight / 2f)
    }
    setTransform(matrix)
}

private fun Drawable.hasLikelyBlackBars(): Boolean {
    // If it's a BitmapDrawable, handle hardware bitmaps safely
    if (this is android.graphics.drawable.BitmapDrawable) {
        val bm = this.bitmap
        if (bm != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && bm.config == Bitmap.Config.HARDWARE) {
            val softwareBitmap = bm.copy(Bitmap.Config.ARGB_8888, false)
            return softwareBitmap?.hasLikelyBlackBars() ?: false
        }
    }

    val sourceWidth = intrinsicWidth
    val sourceHeight = intrinsicHeight
    if (sourceWidth < 24 || sourceHeight < 24) return false

    val sampleWidth = minOf(sourceWidth, 96)
    val sampleHeight = minOf(sourceHeight, 96)
    val bitmap = Bitmap.createBitmap(sampleWidth, sampleHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val previousBounds = android.graphics.Rect(bounds)
    setBounds(0, 0, sampleWidth, sampleHeight)
    draw(canvas)
    setBounds(previousBounds)
    return bitmap.hasLikelyBlackBars()
}

private fun Bitmap.hasLikelyBlackBars(): Boolean {
    // If it's a hardware bitmap, we must copy it to a software bitmap to access pixels
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        val softwareBitmap = copy(Bitmap.Config.ARGB_8888, false)
        return softwareBitmap?.hasLikelyBlackBars() ?: false
    }

    if (width < 24 || height < 24) return false

    val stripHeight = max(2, (height * 0.07f).roundToInt())
    val stripWidth = max(2, (width * 0.07f).roundToInt())
    val centerXStart = width / 4
    val centerXEnd = max(centerXStart + 1, width * 3 / 4)
    val centerYStart = height / 4
    val centerYEnd = max(centerYStart + 1, height * 3 / 4)

    fun darkRatio(xRange: IntRange, yRange: IntRange): Float {
        var darkPixels = 0
        var countedPixels = 0

        for (y in yRange) {
            for (x in xRange) {
                val pixel = getPixel(x, y)
                if (AndroidColor.alpha(pixel) <= 32) continue

                countedPixels += 1
                val brightness = (AndroidColor.red(pixel) + AndroidColor.green(pixel) + AndroidColor.blue(pixel)) / 3
                if (brightness < 28) {
                    darkPixels += 1
                }
            }
        }

        return if (countedPixels == 0) 0f else darkPixels.toFloat() / countedPixels.toFloat()
    }

    val top = darkRatio(0 until width, 0 until stripHeight)
    val bottom = darkRatio(0 until width, (height - stripHeight) until height)
    val left = darkRatio(0 until stripWidth, 0 until height)
    val right = darkRatio((width - stripWidth) until width, 0 until height)
    val center = darkRatio(centerXStart until centerXEnd, centerYStart until centerYEnd)

    val hasLetterbox = top > 0.96f && bottom > 0.96f
    val hasPillarbox = left > 0.96f && right > 0.96f
    return (hasLetterbox || hasPillarbox) && center < 0.75f
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

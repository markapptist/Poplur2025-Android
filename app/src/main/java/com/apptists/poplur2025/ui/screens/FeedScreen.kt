package com.apptists.poplur2025.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.apptists.poplur2025.ui.PopularObjectCatalog
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.apptists.poplur2025.R
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
import coil.request.ImageRequest
import com.apptists.poplur2025.data.DemoPostStore

import com.apptists.poplur2025.data.Post
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.data.isDeliveryEligible
import com.apptists.poplur2025.ui.components.PoplurText
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.ShaderIcon
import com.apptists.poplur2025.ui.components.ShaderNumberText
import com.apptists.poplur2025.util.PoplurEQSync
import com.apptists.poplur2025.ui.components.ThemedSkinPlate
import com.apptists.poplur2025.ui.components.TrendingDisplay
import com.apptists.poplur2025.ui.components.UserInfoOverlay
import com.apptists.poplur2025.ui.components.poplurCountdownDisplayModifier
import com.apptists.poplur2025.ui.components.poplurRadiusDisplayModifier
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.uiTheme
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.apptists.poplur2025.data.AITranslationCaptionProvider
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import com.apptists.poplur2025.data.PoplurLanguage
import com.apptists.poplur2025.data.FeedBumperService
import java.util.Locale

private enum class FeedChannelStepDirection {
    Up,
    Down
}

@Composable
fun FeedScreen(
    currentPostIndex: Int,
    isVisible: Boolean = true,
    tvGuideRequestToken: Int = 0,
    targetChannelQuery: String = "",
    targetChannelToken: Int = 0,
    timelineToggleRequestToken: Int = 0,
    likeRequestToken: Int = 0,
    openProfileRequestToken: Int = 0,
    submittedComment: String = "",
    commentSubmitToken: Int = 0,
    preferredLanguage: PoplurLanguage = PoplurLanguage.English,
    closedCaptionsEnabled: Boolean = true,
    homeChromeInactive: Boolean = false,
    cubeSkin: CubeSkin = CubeSkin.Default,
    onOpenUser: (UserProfile) -> Unit = {},
    onTvGuideVisibilityChanged: (Boolean) -> Unit = {},
    onChannelObjectPop: (String) -> Unit = {},
    onUserInteraction: () -> Unit = {},
    onIndexChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val isInspectionMode = LocalInspectionMode.current
    val liveBroadcastRevision = DemoPostStore.liveBroadcastRevision.intValue
    val currentSlot = remember(currentPostIndex, liveBroadcastRevision) { resolveFeedSlot(currentPostIndex) }
    val guideChannels = remember(liveBroadcastRevision) { buildFeedTvGuideChannels() }
    var showTimelineView by remember { mutableStateOf(false) }
    var showTvGuide by remember { mutableStateOf(false) }
    var showFeedEntryLogo by remember { mutableStateOf(false) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var tvTransitionToken by remember { mutableIntStateOf(0) }
    var tvTransitionChannelNumber by remember { mutableIntStateOf(100) }
    var tvTransitionChannelCategory by remember { mutableStateOf("") }
    var tvTransitionChannelIconResId by remember { mutableStateOf<Int?>(null) }
    var selectedGuideChannelId by remember { mutableStateOf("feed") }
    var activeFeedPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var allowFeedVideoPlayback by remember { mutableStateOf(false) }
    var captionPlaybackSeconds by remember { mutableLongStateOf(0L) }
    var likedPulseToken by remember { mutableIntStateOf(0) }
    var showLikeBurst by remember { mutableStateOf(false) }
    val likedSlotIds = remember { mutableStateListOf<String>() }
    var showCommentCredits by remember { mutableStateOf(false) }
    val userCommentCreditsBySlot = remember { mutableStateMapOf<String, List<FeedCommentCredit>>() }
    var channelButtonPopDirection by remember { mutableStateOf<FeedChannelStepDirection?>(null) }
    var channelButtonPopToken by remember { mutableIntStateOf(0) }
    var deliveryPost by remember { mutableStateOf<Post?>(null) }
    val eqLanes by PoplurEQSync.activeLanes.collectAsState()
    val feedVideoVolumeMultiplier = PoplurEQSync.feedVideoVolumeMultiplier(eqLanes)
    val showingTimelineSide = showTimelineView
    val currentChannelForObject by rememberUpdatedState(
        when (val slot = currentSlot) {
            is FeedSlot.PostSlot -> slot.channelCategory
            is FeedSlot.AdSlot -> slot.creative.channelCategory
        }
    )

    LaunchedEffect(activeFeedPlayer, currentSlot.feedSlotId) {
        captionPlaybackSeconds = 0L
        val player = activeFeedPlayer ?: return@LaunchedEffect
        while (true) {
            captionPlaybackSeconds = (player.currentPosition / 1000L).coerceAtLeast(0L)
            delay(500)
        }
    }

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
        onDispose {
            onTvGuideVisibilityChanged(false)
            PoplurEQSync.end(PoplurEQSync.Lane.FeedVideo)
        }
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
        play@{
            if (!PoplurEQSync.shouldPlayTransientSound(minimumIntervalMs = 140L)) return@play
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

    fun openCurrentPostProfile() {
        val slot = currentSlot
        if (slot is FeedSlot.PostSlot && slot.post.userId.isNotBlank() && slot.post.userId != "ad_partner") {
            onUserInteraction()
            playFeedPop()
            onOpenUser(slot.post.toUserProfile())
        }
    }

    fun commentsForCredits(slot: FeedSlot): List<FeedCommentCredit> {
        return (seedCommentCredits(slot) + userCommentCreditsBySlot[slot.feedSlotId].orEmpty()).takeLast(12)
    }

    fun triggerChannelButtonPop(direction: FeedChannelStepDirection) {
        channelButtonPopDirection = direction
        channelButtonPopToken += 1
    }

    fun changeChannel(direction: FeedChannelStepDirection) {
        onUserInteraction()
        showCommentCredits = false
        triggerChannelButtonPop(direction)
        playFeedPop()

        val targetIndex = adjacentChannelFeedIndex(currentPostIndex, direction)
        when (val targetSlot = resolveFeedSlot(targetIndex)) {
            is FeedSlot.PostSlot -> {
                tvTransitionChannelNumber = targetSlot.channelNumber
                tvTransitionChannelCategory = targetSlot.channelCategory
                tvTransitionChannelIconResId = targetSlot.channelIconResId
            }
            is FeedSlot.AdSlot -> {
                tvTransitionChannelNumber = 100
                tvTransitionChannelCategory = targetSlot.creative.creativeKind
                tvTransitionChannelIconResId = null
            }
        }
        tvTransitionToken += 1
        onIndexChange(targetIndex)
    }

    fun jumpToChannel(query: String) {
        val targetChannel = feedChannelForQuery(query) ?: return
        onUserInteraction()
        showCommentCredits = false
        playFeedPop()
        tvTransitionChannelNumber = targetChannel.number
        tvTransitionChannelCategory = targetChannel.category
        tvTransitionChannelIconResId = targetChannel.iconResId
        tvTransitionToken += 1
        onIndexChange(feedIndexForChannelNumber(currentPostIndex, targetChannel.number))
    }

    fun advanceToNextPost() {
        showCommentCredits = false
        when (val s = currentSlot) {
            is FeedSlot.PostSlot -> {
                tvTransitionChannelNumber = s.channelNumber
                tvTransitionChannelCategory = s.channelCategory
                tvTransitionChannelIconResId = s.channelIconResId
            }
            is FeedSlot.AdSlot -> {
                tvTransitionChannelNumber = 100
                tvTransitionChannelCategory = s.creative.creativeKind
                tvTransitionChannelIconResId = null
            }
        }
        tvTransitionToken += 1
        onIndexChange(currentPostIndex + 1)
    }

    LaunchedEffect(targetChannelToken) {
        if (targetChannelToken > 0 && targetChannelQuery.isNotBlank()) {
            showTvGuide = false
            showTimelineView = false
            jumpToChannel(targetChannelQuery)
        }
    }

    LaunchedEffect(likeRequestToken) {
        if (likeRequestToken > 0) likeCurrentSlot()
    }

    LaunchedEffect(openProfileRequestToken) {
        if (openProfileRequestToken > 0) openCurrentPostProfile()
    }

    LaunchedEffect(commentSubmitToken, submittedComment) {
        val trimmed = submittedComment.trim()
        if (commentSubmitToken > 0 && trimmed.isNotEmpty()) {
            val slotId = currentSlot.feedSlotId
            userCommentCreditsBySlot[slotId] = userCommentCreditsBySlot[slotId].orEmpty() + FeedCommentCredit(
                id = "you-$commentSubmitToken",
                username = "you",
                text = trimmed
            )
        }
    }

    LaunchedEffect(currentSlot.feedSlotId, showTvGuide, showingTimelineSide) {
        showCommentCredits = false
    }

    LaunchedEffect(likedPulseToken) {
        if (likedPulseToken <= 0) return@LaunchedEffect
        delay(680)
        showLikeBurst = false
    }

    LaunchedEffect(channelButtonPopToken) {
        if (channelButtonPopToken <= 0) return@LaunchedEffect
        delay(220)
        channelButtonPopDirection = null
    }

    // Clear player when app is backgrounded; play pop + confetti on re-entry
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            activeFeedPlayer = null
            allowFeedVideoPlayback = false
            PoplurEQSync.end(PoplurEQSync.Lane.FeedVideo)
            return@LaunchedEffect
        }
        delay(1_400)
        allowFeedVideoPlayback = true
    }

    LaunchedEffect(activeFeedPlayer, isVisible) {
        if (isVisible && activeFeedPlayer != null) {
            PoplurEQSync.begin(PoplurEQSync.Lane.FeedVideo)
        } else {
            PoplurEQSync.end(PoplurEQSync.Lane.FeedVideo)
        }
    }

    LaunchedEffect(isVisible, activeFeedPlayer) {
        if (!isVisible) return@LaunchedEffect
        while (true) {
            delay(60_000L)
            if (activeFeedPlayer?.isPlaying != true && !showTvGuide && !showingTimelineSide) {
                playFeedPop()
                onChannelObjectPop(randomChannelCubeObject(currentChannelForObject))
            }
        }
    }

    LaunchedEffect(currentSlot.feedSlotId) {
        val slot = currentSlot
        if (slot is FeedSlot.PostSlot) {
            FeedBumperService.prefetch(slot.post, slot.channelCategory, slot.feedSlotId, context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (!showingTimelineSide) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                            isMuted = false,
                            allowVideoPlayback = allowFeedVideoPlayback,
                            volumeMultiplier = feedVideoVolumeMultiplier,
                            useExternalVideoSurface = showTvGuide,
                            onActivePlayerChanged = { player -> activeFeedPlayer = player },
                            onCreditsVisibilityChanged = { showCommentCredits = it },
                            onAdvanceToNext = { advanceToNextPost() }
                        )
                    }
                    is FeedSlot.AdSlot -> FeedAdContent(
                        slot = slot,
                        isVisible = isVisible,
                        allowVideoPlayback = allowFeedVideoPlayback,
                        volumeMultiplier = feedVideoVolumeMultiplier,
                        useExternalVideoSurface = showTvGuide,
                        onActivePlayerChanged = { player -> activeFeedPlayer = player },
                        onCreditsVisibilityChanged = { showCommentCredits = it },
                        onAdvanceToNext = { advanceToNextPost() }
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
                    .padding(top = 48.dp, start = 16.dp)
                    .align(Alignment.TopStart)
                    .zIndex(86f),
                label = "UserInfoAnimation"
            ) { slot ->
                when (slot) {
                    is FeedSlot.PostSlot -> UserInfoOverlay(
                        displayName = slot.post.displayName,
                        location = slot.post.locationName,
                        channelNumber = slot.channelNumber,
                        channelEmoji = slot.channelEmoji,
                        channelCategory = slot.channelCategory,
                        channelIconResId = slot.channelIconResId
                    )
                    is FeedSlot.AdSlot -> UserInfoOverlay(
                        displayName = slot.creative.brandName,
                        location = slot.creative.locationName,
                        channelNumber = slot.creative.channelNumber,
                        channelEmoji = slot.creative.channelEmoji,
                        channelCategory = slot.creative.channelCategory,
                        channelIconResId = slot.creative.channelIconResId
                    )
                }
            }

            // 3. Bottom Left Overlay (Animated Post Data)
            AnimatedContent(
                targetState = currentSlot,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                modifier = poplurCountdownDisplayModifier().zIndex(86f),
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
                modifier = poplurRadiusDisplayModifier().zIndex(86f),
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

        if (closedCaptionsEnabled && !showingTimelineSide && !showTvGuide && !homeChromeInactive) {
            val slot = currentSlot
            if (slot is FeedSlot.PostSlot && slot.post.videoUrl != null) {
                AICaptionOverlay(
                    post = slot.post,
                    channelCategory = slot.channelCategory,
                    preferredLanguage = preferredLanguage,
                    playbackSeconds = captionPlaybackSeconds,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 116.dp)
                        .zIndex(84f)
                )
            }
        }

        if (!showingTimelineSide && !showTvGuide) {
            FeedChannelStepperOverlay(
                cubeSkin = cubeSkin,
                activeDirection = channelButtonPopDirection,
                onUp = { changeChannel(FeedChannelStepDirection.Up) },
                onDown = { changeChannel(FeedChannelStepDirection.Down) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .zIndex(92f)
            )
        }

        val deliveryEligiblePost = (currentSlot as? FeedSlot.PostSlot)
            ?.post
            ?.takeIf { it.isDeliveryEligible() }
        if (deliveryEligiblePost != null && !showingTimelineSide && !showTvGuide && !homeChromeInactive) {
            DeliveryButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 22.dp, bottom = 214.dp)
                    .zIndex(94f),
                onClick = {
                    onUserInteraction()
                    deliveryPost = deliveryEligiblePost
                }
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(80f)
                .pointerInput(currentSlot.feedSlotId) {
                    detectTapGestures(onTap = { onUserInteraction() })
                }
                .feedSwipeDetector(
                    currentPostIndex = currentPostIndex,
                    onIndexChange = onIndexChange,
                    onInteraction = onUserInteraction,
                    onVerticalSwipe = { isUp ->
                        triggerChannelButtonPop(if (isUp) FeedChannelStepDirection.Up else FeedChannelStepDirection.Down)
                    }
                )
        )
        }
        }

        if (showingTimelineSide) {
            PoplurNewspaper(
                posts = DemoPostStore.posts,
                modifier = Modifier
                    .fillMaxSize()
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
            ShaderIcon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF365E),
                modifier = Modifier
                    .size(116.dp)
                    .shadow(18.dp, CircleShape)
            )
        }

        if (showCommentCredits && !showingTimelineSide && !showTvGuide) {
            FeedCommentCreditsOverlay(
                comments = commentsForCredits(currentSlot),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(88f)
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showFeedEntryLogo,
            enter = scaleIn(initialScale = 0.72f, animationSpec = tween(140)) + fadeIn(animationSpec = tween(120)),
            exit = scaleOut(targetScale = 1.28f, animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(90f)
        ) {
            FeedEntryLogoPop()
        }

        if (tvTransitionToken > 0) {
            key(tvTransitionToken) {
                val use3D = remember { kotlin.random.Random.nextBoolean() }
                if (use3D) {
                    TVStaticTransitionOverlay3D(
                        channelNumber = tvTransitionChannelNumber,
                        channelCategory = tvTransitionChannelCategory,
                        channelIconResId = tvTransitionChannelIconResId
                    )
                } else {
                    TVStaticTransitionOverlay(
                        channelNumber = tvTransitionChannelNumber,
                        channelCategory = tvTransitionChannelCategory,
                        channelIconResId = tvTransitionChannelIconResId
                    )
                }
            }
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

        deliveryPost?.let { post ->
            DeliveryFlowDialog(
                post = post,
                onDismiss = { deliveryPost = null }
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

@Composable
private fun FeedEntryLogoPop() {
    val context = LocalContext.current
    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }
    val scale = remember { Animatable(0.76f) }

    LaunchedEffect(Unit) {
        scale.snapTo(0.76f)
        scale.animateTo(1.26f, animationSpec = tween(120, easing = FastOutSlowInEasing))
        scale.animateTo(0.94f, animationSpec = tween(90, easing = FastOutSlowInEasing))
        scale.animateTo(1.0f, animationSpec = tween(80, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .size(156.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        contentAlignment = Alignment.Center
    ) {
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = "Poplur",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ShaderNumberText(
                text = "POPLUR",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TVStaticTransitionOverlay(channelNumber: Int, channelCategory: String, channelIconResId: Int? = null) {
    val context = LocalContext.current
    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }
    val overlayAlpha = remember { Animatable(1f) }
    // noiseTime drives per-frame Canvas redraws via state tracking
    val noiseTime = remember { Animatable(0f) }
    var logoVisible by remember { mutableStateOf(true) }
    var channelVisible by remember { mutableStateOf(true) }
    val logoScale = remember { Animatable(0.82f) }
    val channelScale = remember { Animatable(0.86f) }

    LaunchedEffect(Unit) {
        // Noise driver (infinite, cancelled when scope ends)
        launch { noiseTime.animateTo(9999f, infiniteRepeatable(tween(99999, easing = LinearEasing))) }
        // Audio
        launch(kotlinx.coroutines.Dispatchers.IO) { playTVStaticAudio() }
        // Logo and channel identity begin on the first frames so the feed never outruns the tune card.
        launch {
            logoScale.animateTo(1.22f, tween(70, easing = FastOutSlowInEasing))
            logoScale.animateTo(0.92f, tween(50, easing = FastOutSlowInEasing))
            logoScale.animateTo(1.04f, tween(40, easing = FastOutSlowInEasing))
            logoScale.animateTo(1.0f, tween(30, easing = FastOutSlowInEasing))
        }
        launch {
            channelScale.animateTo(1.20f, tween(70, easing = FastOutSlowInEasing))
            channelScale.animateTo(0.94f, tween(50, easing = FastOutSlowInEasing))
            channelScale.animateTo(1.03f, tween(40, easing = FastOutSlowInEasing))
            channelScale.animateTo(1.0f, tween(30, easing = FastOutSlowInEasing))
        }
        // Fade only after the channel number/logo have had a readable beat.
        delay(500)
        overlayAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
    }

    if (overlayAlpha.value <= 0f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(900f)
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Static noise background - optimized to reduce per-frame Random calls
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val t = (noiseTime.value * 1000).toInt()
            val random = Random(t)
            val lineH = 6.dp.toPx()
            var y = 0f
            while (y < size.height) {
                val v = random.nextFloat()
                drawRect(
                    Color(v, v, v, 0.45f + random.nextFloat() * 0.35f),
                    topLeft = Offset(0f, y),
                    size = Size(size.width, lineH)
                )
                y += lineH
            }
            // Glitch bands - fewer than before
            repeat(2) {
                val bY = random.nextFloat() * size.height
                drawRect(
                    Color.White.copy(alpha = 0.12f + random.nextFloat() * 0.22f),
                    Offset(0f, bY), Size(size.width, 4f + random.nextFloat() * 22f)
                )
            }
        }
        // Scanline overlay
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val gap = 3.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawRect(Color.Black.copy(alpha = 0.30f), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                y += gap
            }
        }
        // Channel identity card
        if (channelIconResId != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (logoVisible) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(channelIconResId),
                        contentDescription = "CH $channelNumber $channelCategory",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .wrapContentHeight()
                            .graphicsLayer { scaleX = logoScale.value; scaleY = logoScale.value }
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                if (channelVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { scaleX = channelScale.value; scaleY = channelScale.value }
                    ) {
                        ShaderNumberText(
                            text = "CH $channelNumber",
                            color = Color(0xFF4EFF40),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black
                        )
                        ShaderNumberText(
                            text = channelCategory.uppercase(),
                            color = Color(0xFF4EFF40),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (logoVisible) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer { scaleX = logoScale.value; scaleY = logoScale.value },
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = logoBitmap,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize(1.40f)
                                    .graphicsLayer {
                                        alpha = 0.35f
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                            Color(0xFFFF40C8),
                                            androidx.compose.ui.graphics.BlendMode.Modulate
                                        )
                                    }
                            )
                            androidx.compose.foundation.Image(
                                bitmap = logoBitmap,
                                contentDescription = "Poplur",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ShaderNumberText("POPLUR", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                if (channelVisible) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { scaleX = channelScale.value; scaleY = channelScale.value }
                    ) {
                        ShaderNumberText(
                            text = "CH $channelNumber",
                            color = Color(0xFF4EFF40),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black
                        )
                        ShaderNumberText(
                            text = channelCategory.uppercase(),
                            color = Color(0xFF4EFF40),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

private fun playTVStaticAudio() {
    try {
        val sampleRate = 22050
        val durationMs = 680
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val fadeIn = minOf(1f, i / 480f)
            val fadeOut = minOf(1f, (numSamples - i) / 1100f)
            val env = minOf(fadeIn, fadeOut)
            val noise = (Random.nextFloat() * 2 - 1) * 0.28f * env
            val hum = (kotlin.math.sin(2.0 * Math.PI * 59.94 * i / sampleRate) * 0.07).toFloat()
            val sample = ((noise + hum) * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i * 2] = (sample and 0xFF).toByte()
            buffer[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        val minBuf = android.media.AudioTrack.getMinBufferSize(
            sampleRate,
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = android.media.AudioTrack(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            android.media.AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            maxOf(buffer.size, minBuf),
            android.media.AudioTrack.MODE_STATIC,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        Thread.sleep(durationMs.toLong() + 120)
        audioTrack.stop()
        audioTrack.release()
    } catch (_: Exception) {}
}

// MARK: - 3D perspective TV static transition (graphicsLayer rotationY / rotationX)

@Composable
private fun TVStaticTransitionOverlay3D(channelNumber: Int, channelCategory: String, channelIconResId: Int? = null) {
    val context = LocalContext.current
    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }

    val overlayAlpha = remember { Animatable(1f) }
    val noiseTime    = remember { Animatable(0f) }

    // Logo 3D flip: Y rotation from 90° (edge-on) → bounce to 0° (face-on)
    var logoVisible by remember { mutableStateOf(true) }
    val logoRotY  = remember { Animatable(16f) }
    val logoScale = remember { Animatable(0.92f) }

    // Text 3D tilt-in: rotationX from -40° (facing up/away) → 0°, plus rise from below
    var textVisible by remember { mutableStateOf(true) }
    val textRotX   = remember { Animatable(-12f) }
    val textTransY = remember { Animatable(22f) }
    val textScale  = remember { Animatable(0.90f) }

    LaunchedEffect(Unit) {
        // Noise driver
        launch { noiseTime.animateTo(9999f, infiniteRepeatable(tween(99999, easing = LinearEasing))) }
        // Audio
        launch(kotlinx.coroutines.Dispatchers.IO) { playTVStaticAudio() }

        launch {
            logoRotY.animateTo(-8f, tween(90, easing = FastOutSlowInEasing))
            logoRotY.animateTo(4f,  tween(60, easing = FastOutSlowInEasing))
            logoRotY.animateTo(0f,  tween(40, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1.16f, tween(70, easing = FastOutSlowInEasing))
            logoScale.animateTo(0.94f, tween(50, easing = FastOutSlowInEasing))
            logoScale.animateTo(1.0f,  tween(40, easing = FastOutSlowInEasing))
        }

        launch {
            textRotX.animateTo(6f,  tween(90, easing = FastOutSlowInEasing))
            textRotX.animateTo(-3f, tween(50, easing = FastOutSlowInEasing))
            textRotX.animateTo(0f,  tween(40, easing = FastOutSlowInEasing))
        }
        launch {
            textTransY.animateTo(-8f, tween(90, easing = FastOutSlowInEasing))
            textTransY.animateTo(4f,  tween(50, easing = FastOutSlowInEasing))
            textTransY.animateTo(0f,  tween(40, easing = FastOutSlowInEasing))
        }
        launch {
            textScale.animateTo(1.15f, tween(70, easing = FastOutSlowInEasing))
            textScale.animateTo(0.95f, tween(50, easing = FastOutSlowInEasing))
            textScale.animateTo(1.0f,  tween(40, easing = FastOutSlowInEasing))
        }

        delay(500)
        overlayAlpha.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
    }

    if (overlayAlpha.value <= 0f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(900f)
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Animated static noise background - optimized
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val t = (noiseTime.value * 1000).toInt()
            val random = Random(t)
            val lineH = 6.dp.toPx()
            var y = 0f
            while (y < size.height) {
                val v = random.nextFloat()
                drawRect(
                    Color(v, v, v, 0.45f + random.nextFloat() * 0.35f),
                    topLeft = Offset(0f, y),
                    size = Size(size.width, lineH)
                )
                y += lineH
            }
            repeat(2) {
                val bY = random.nextFloat() * size.height
                drawRect(
                    Color.White.copy(alpha = 0.12f + random.nextFloat() * 0.22f),
                    Offset(0f, bY), Size(size.width, 4f + random.nextFloat() * 22f)
                )
            }
        }
        // Scanline overlay
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val gap = 3.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawRect(Color.Black.copy(alpha = 0.30f), Offset(0f, y), Size(size.width, 1.dp.toPx()))
                y += gap
            }
        }

        if (channelIconResId != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (logoVisible) {
                    val density = LocalDensity.current.density
                    androidx.compose.foundation.Image(
                        painter = painterResource(channelIconResId),
                        contentDescription = "CH $channelNumber $channelCategory",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .wrapContentHeight()
                            .graphicsLayer {
                                rotationY = logoRotY.value
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                                cameraDistance = 10f * density
                            }
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                if (textVisible) {
                    val density = LocalDensity.current.density
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            rotationX = textRotX.value
                            translationY = textTransY.value * density
                            scaleX = textScale.value
                            scaleY = textScale.value
                            cameraDistance = 10f * density
                        }
                    ) {
                        ShaderNumberText(
                            text = "CH $channelNumber",
                            color = Color(0xFF4EFF40),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            intensity = 0.92f
                        )
                        PoplurText(
                            text = channelCategory.uppercase(),
                            color = Color(0xFF4EFF40),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            intensity = 0.82f
                        )
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 3D logo flip
                if (logoVisible && logoBitmap != null) {
                    val density = LocalDensity.current.density
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                rotationY = logoRotY.value
                                scaleX = logoScale.value
                                scaleY = logoScale.value
                                cameraDistance = 10f * density
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = logoBitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(1.40f).graphicsLayer {
                                alpha = 0.32f
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    Color(0xFFFF40C8), androidx.compose.ui.graphics.BlendMode.Modulate
                                )
                            }
                        )
                        androidx.compose.foundation.Image(
                            bitmap = logoBitmap,
                            contentDescription = "Poplur",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3D channel text tilt-in
                if (textVisible) {
                    val density = LocalDensity.current.density
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            rotationX = textRotX.value
                            translationY = textTransY.value * density
                            scaleX = textScale.value
                            scaleY = textScale.value
                            cameraDistance = 10f * density
                        }
                    ) {
                        ShaderNumberText(
                            text = "CH $channelNumber",
                            color = Color(0xFF4EFF40),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            intensity = 0.92f
                        )
                        PoplurText(
                            text = channelCategory.uppercase(),
                            color = Color(0xFF4EFF40),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            intensity = 0.82f
                        )
                    }
                }
            }
        }
    }
}

private fun randomChannelCubeObject(category: String): String {
    return PopularObjectCatalog.randomObjectForChannel(category)
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
    onInteraction: () -> Unit,
    onVerticalSwipe: (isUp: Boolean) -> Unit
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
                    val isUp = totalY < 0
                    onVerticalSwipe(isUp)
                    onIndexChange(if (isUp) currentPostIndex + 1 else currentPostIndex - 1)
                    triggered = true
                    change.consume()
                }
            }
        }
    )
}

@Composable
private fun FeedChannelStepperOverlay(
    cubeSkin: CubeSkin,
    activeDirection: FeedChannelStepDirection?,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(82.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FeedChannelImageButton(
            cubeSkin = cubeSkin,
            direction = FeedChannelStepDirection.Up,
            activeDirection = activeDirection,
            onClick = onUp
        )

        FeedChannelCenterButton(cubeSkin = cubeSkin, isActive = activeDirection != null)

        FeedChannelImageButton(
            cubeSkin = cubeSkin,
            direction = FeedChannelStepDirection.Down,
            activeDirection = activeDirection,
            onClick = onDown
        )
    }
}

@Composable
private fun FeedChannelImageButton(
    cubeSkin: CubeSkin,
    direction: FeedChannelStepDirection,
    activeDirection: FeedChannelStepDirection?,
    onClick: () -> Unit
) {
    val isActive = activeDirection == direction
    val theme = cubeSkin.uiTheme()
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.22f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "feed-channel-step-scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.62f else 0.26f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "feed-channel-step-glow"
    )
    val icon = when (direction) {
        FeedChannelStepDirection.Up -> Icons.Filled.KeyboardArrowUp
        FeedChannelStepDirection.Down -> Icons.Filled.KeyboardArrowDown
    }
    val cornerRadius = when (cubeSkin) {
        CubeSkin.Toy -> 13.dp
        CubeSkin.Ice -> 16.dp
        CubeSkin.Camo, CubeSkin.Chocolate, CubeSkin.Gold -> 10.dp
        else -> 17.dp
    }
    val tint = when (cubeSkin) {
        CubeSkin.Toy -> if (direction == FeedChannelStepDirection.Up) Color(0xFF084CAC) else Color(0xFFE6262B)
        CubeSkin.Rainbow, CubeSkin.Default -> Color.White
        else -> theme.primaryText
    }

    ThemedSkinPlate(
        skin = cubeSkin,
        active = isActive,
        cornerRadius = cornerRadius,
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow((6f + glowAlpha * 12f).dp, RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick)
    ) {
        ShaderIcon(
            imageVector = icon,
            contentDescription = if (direction == FeedChannelStepDirection.Up) "Channel up" else "Channel down",
            tint = tint,
            modifier = Modifier
                .size(54.dp)
                .graphicsLayer {
                    shadowElevation = if (isActive) 14f else 7f
                    alpha = if (isActive) 1f else 0.9f
                }
        )
    }
}

@Composable
private fun FeedChannelCenterButton(
    cubeSkin: CubeSkin,
    isActive: Boolean
) {
    val theme = cubeSkin.uiTheme()
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "feed-channel-ch-scale"
    )
    ThemedSkinPlate(
        skin = cubeSkin,
        active = isActive,
        cornerRadius = when (cubeSkin) {
            CubeSkin.Camo, CubeSkin.Chocolate, CubeSkin.Gold -> 8.dp
            CubeSkin.Toy -> 10.dp
            else -> 13.dp
        },
        modifier = Modifier
            .width(66.dp)
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        PoplurText(
            text = "CH",
            color = when (cubeSkin) {
                CubeSkin.Toy -> Color(0xFFE6262B)
                CubeSkin.Rainbow, CubeSkin.Default -> Color.White
                else -> theme.primaryText
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            intensity = 0.78f,
            style = TextStyle(
                shadow = Shadow(
                    color = theme.shadow,
                    offset = Offset(0f, 2f),
                    blurRadius = if (isActive) 14f else 8f
                )
            )
        )
    }
}

private data class FeedCommentCredit(
    val id: String,
    val username: String,
    val text: String
)

@Composable
private fun FeedCommentCreditsOverlay(
    comments: List<FeedCommentCredit>,
    modifier: Modifier = Modifier
) {
    val progress = remember(comments) { Animatable(0f) }

    LaunchedEffect(comments) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 7_600, easing = LinearEasing)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val startY = screenHeightPx + 120f
        val endY = -screenHeightPx * 0.40f
        val yOffset = startY + (endY - startY) * progress.value

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { androidx.compose.ui.unit.IntOffset(0, yOffset.roundToInt()) }
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            ShaderNumberText(
                text = "COMMENTS",
                color = Color(0xFFFFD91A),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.1.sp
            )
            comments.forEach { comment ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShaderNumberText(
                        text = comment.username.uppercase(),
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    ShaderNumberText(
                        text = comment.text,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 25.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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

private fun Post.toUserProfile(): UserProfile =
    UserProfile(
        id = userId,
        username = username,
        displayName = displayName.ifBlank { username.ifBlank { userId } },
        bio = caption.ifBlank { locationName },
        radius = radius,
        posts = userPostsCount
    )

@Composable
private fun AICaptionOverlay(
    post: Post,
    channelCategory: String,
    preferredLanguage: PoplurLanguage,
    playbackSeconds: Long,
    modifier: Modifier = Modifier
) {
    val caption = remember(post.id, channelCategory, preferredLanguage, playbackSeconds) {
        AITranslationCaptionProvider.captionFor(
            post = post,
            channelCategory = channelCategory,
            preferredLanguage = preferredLanguage,
            playbackSeconds = playbackSeconds
        )
    }

    Column(
        modifier = modifier
            .widthIn(max = 460.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.68f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ShaderNumberText(
            text = caption.badge,
            color = Color(0xFF4DFF58),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ShaderNumberText(
            text = caption.text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun seedCommentCredits(slot: FeedSlot): List<FeedCommentCredit> {
    val displayName = when (slot) {
        is FeedSlot.PostSlot -> slot.post.displayName.ifBlank { slot.post.username }
        is FeedSlot.AdSlot -> slot.creative.brandName
    }.ifBlank { "this" }
    val samples = listOf(
        FeedCommentCredit("credit-1", "vi.xi.mmvi", "this is live right now"),
        FeedCommentCredit("credit-2", "leurdavoir", "keep the camera there"),
        FeedCommentCredit("credit-3", "monsekee", "the radius is going crazy"),
        FeedCommentCredit("credit-4", "poplur", "watching $displayName")
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
    val iconResId: Int? = null,
    val profile: UserProfile,
    val displayNames: List<String> = emptyList(),
    val program: String,
    val videoUrl: String? = null
)

private fun buildFeedTvGuideChannels(): List<FeedTvGuideChannel> {
    val uniquePosts = DemoPostStore.posts
        .filter { it.userId.isNotBlank() && it.userId != "ad_partner" }
        .distinctBy { it.userId }
        .take(FEED_CHANNELS.size * 6) // extra for multi-name cycling

    val fallbackNames = listOf(
        "Mia", "Jay", "Alex Johnson", "Samira Khan", "Diego Morales",
        "Mina Park", "Noah Lee", "Ava Chen", "Kai Brooks", "Leo Vance",
        "Priya Sen", "Carlos R", "Yuki T", "Fatima Al"
    )
    val channelCount = FEED_CHANNELS.size

    return FEED_CHANNELS.mapIndexed { index, channel ->
        val fallbackPost = uniquePosts.getOrNull(index) ?: DemoPostStore.getPostForIndex(index)
        val category = channel.category
        val post = if (category.equals("Live", ignoreCase = true)) {
            DemoPostStore.visibleLivePostsForViewer(fallbackPost, limit = 1).firstOrNull()
                ?: DemoPostStore.livePosts.firstOrNull()
                ?: fallbackPost
        } else {
            fallbackPost
        }

        val primaryName = if (category.equals("Live", ignoreCase = true)) {
            post.displayName.ifBlank { "Poplur Live" }
        } else {
            languageChannelDisplayName(category) ?: post.displayName
        }

        // Collect up to 5 names via round-robin through remaining posts
        val names = mutableListOf(primaryName)
        var round = 1
        while (names.size < 5) {
            val postIndex = index + round * channelCount
            val extraPost = uniquePosts.getOrNull(postIndex)
            val name = extraPost?.displayName?.ifBlank { null }
                ?: fallbackNames[(index + round * 3) % fallbackNames.size]
            if (!names.contains(name)) names.add(name)
            round++
            if (round > 6) break
        }

        FeedTvGuideChannel(
            id = if (category.equals("Live", ignoreCase = true)) "channel_live" else "channel_${post.userId.ifBlank { index.toString() }}",
            index = index,
            number = channel.number,
            emoji = channel.emoji,
            category = category,
            iconResId = channel.iconResId,
            profile = UserProfile(
                id = post.userId,
                username = post.username,
                displayName = primaryName,
                bio = post.caption,
                radius = post.radius,
                posts = post.userPostsCount
            ),
            displayNames = names,
            program = if (category.equals("Live", ignoreCase = true)) {
                "Radius live now"
            } else {
                languageChannelProgram(category) ?: post.locationName.ifBlank { post.caption }
            },
            videoUrl = if (category.equals("Live", ignoreCase = true)) null else languageChannelVideoUrl(category) ?: channel.videoUrl ?: post.videoUrl
        )
    }
}

@Composable
private fun FeedChannelIcon(iconResId: Int?, fallbackEmoji: String, size: Dp, fallbackFontSize: TextUnit) {
    if (iconResId != null) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .drawWithCache {
                    val shader = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFF4DFF58).copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(this.size.width, this.size.height)
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(shader, blendMode = BlendMode.SrcAtop)
                    }
                }
        )
    } else {
        PoplurText(fallbackEmoji, fontSize = fallbackFontSize, intensity = 0.62f)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Poplur Newspaper
// ─────────────────────────────────────────────────────────────────────────────

private data class NpMapEvent(
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val userCount: Int, val timeAgo: String,
    val lat: Double = 0.0, val lon: Double = 0.0
)

private data class NpTrending(
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val growthPercent: Int, val activeUsers: Int, val timeAgo: String,
    val lat: Double = 0.0, val lon: Double = 0.0
)

private data class NpDominance(
    val username: String,
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val radiusText: String, val score: Int, val timeAgo: String,
    val lat: Double = 0.0, val lon: Double = 0.0
)

private data class NpUserPost(
    val username: String,
    val city: String, val flag: String,
    val headline: String, val detail: String,
    val likes: Int, val comments: Int, val timeAgo: String,
    val lat: Double = 0.0, val lon: Double = 0.0,
    val mediaUrl: String? = null
)

private sealed class NpArticle {
    data class MapEvent(val d: NpMapEvent)   : NpArticle()
    data class Trending(val d: NpTrending)   : NpArticle()
    data class Dominance(val d: NpDominance) : NpArticle()
    data class UserPost(val d: NpUserPost)   : NpArticle()
}

@Composable
private fun PoplurNewspaper(
    posts: List<Post> = DemoPostStore.posts,
    modifier: Modifier = Modifier,
    onDoubleTap: () -> Unit
) {
    val articles = remember(posts) { NewspaperContentGen.generate(24, posts) }
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
            ShaderNumberText("POPLUR", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            ShaderNumberText(" DAILY", color = Color(0xFFFFD91A), fontSize = 38.sp, fontWeight = FontWeight.ExtraLight)
            Spacer(Modifier.weight(1f))
            ShaderNumberText(date, color = Color.White.copy(alpha = 0.38f), fontSize = 11.sp, fontWeight = FontWeight.Normal)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.85f), thickness = 2.dp)
        Spacer(Modifier.height(3.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.18f), thickness = 1.dp)
        Spacer(Modifier.height(6.dp))
    }
}

private enum class NpVisualKind { MapEvent, Trending, Dominance, UserPost }

private data class NpVisualPoint(val x: Float, val y: Float, val radius: Float, val alpha: Float)

@Composable
private fun NewspaperArticleVisual(
    kind: NpVisualKind,
    city: String,
    flag: String,
    headline: String,
    accent: Color,
    lat: Double,
    lon: Double,
    username: String?,
    mediaUrl: String?,
    modifier: Modifier = Modifier
) {
    val seed = remember(kind, headline, city, username) {
        stableNewsVisualSeed("${kind.name}-$headline-$city-${username.orEmpty()}")
    }
    val points = remember(seed) {
        val random = Random(seed)
        List(18) {
            NpVisualPoint(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.025f + random.nextFloat() * 0.11f,
                alpha = 0.10f + random.nextFloat() * 0.22f
            )
        }
    }
    val mapZoom = if (kind == NpVisualKind.Dominance) 11 else 12

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF111118))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
    ) {
        if (kind == NpVisualKind.UserPost && mediaUrl != null) {
            AsyncImage(
                model = mediaUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            AsyncImage(
                model = mapTileUrl(lat, lon, mapZoom),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.78f))
                    )
                )
        )

        ComposeCanvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val base = minOf(w, h)

            repeat(5) { i ->
                val phase = ((seed shr (i * 5)) and 63) / 63f
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = Offset(w * (0.08f + phase * 0.82f), -18f),
                    end = Offset(w * (0.08f + phase * 0.82f) + (i - 2) * 36f, h + 28f),
                    strokeWidth = 1.3.dp.toPx()
                )
            }

            when (kind) {
                NpVisualKind.Dominance -> {
                    val center = Offset(w * 0.58f, h * 0.46f)
                    repeat(4) { i ->
                        drawCircle(
                            color = accent.copy(alpha = 0.78f - i * 0.12f),
                            radius = base * (0.18f + i * 0.16f),
                            center = center,
                            style = Stroke(width = if (i == 0) 4.dp.toPx() else 2.dp.toPx())
                        )
                    }
                    repeat(9) { i ->
                        val angle = (i / 9f) * Math.PI.toFloat() * 2f + (seed % 29) / 29f
                        val radius = base * (0.24f + ((seed shr i) and 7) / 60f)
                        val point = Offset(
                            x = center.x + cos(angle) * radius,
                            y = center.y + sin(angle) * radius
                        )
                        drawLine(accent.copy(alpha = 0.34f), center, point, strokeWidth = 1.dp.toPx())
                        drawCircle(Color.White.copy(alpha = 0.75f), radius = 3.dp.toPx(), center = point)
                    }
                }
                NpVisualKind.Trending -> {
                    val cols = 7
                    val rows = 4
                    val cellW = w / cols
                    val cellH = h / rows
                    repeat(rows) { row ->
                        repeat(cols) { col ->
                            val mix = ((row * cols + col) * 37 + seed % 97) / 100f
                            drawRoundRect(
                                color = accent.copy(alpha = 0.05f + (mix % 1f) * 0.18f),
                                topLeft = Offset(col * cellW + 4.dp.toPx(), row * cellH + 4.dp.toPx()),
                                size = Size(cellW - 8.dp.toPx(), cellH - 8.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }
                    var previous: Offset? = null
                    repeat(8) { i ->
                        val point = Offset(
                            x = (i / 7f) * w,
                            y = h * (0.78f - i * 0.07f + (((seed shr i) and 3) * 0.018f))
                        )
                        previous?.let { drawLine(accent.copy(alpha = 0.92f), it, point, strokeWidth = 4.dp.toPx()) }
                        previous = point
                    }
                }
                NpVisualKind.MapEvent -> {
                    points.forEach {
                        val center = Offset(it.x * w, it.y * h)
                        val radius = it.radius * base
                        drawCircle(accent.copy(alpha = it.alpha), radius = radius, center = center)
                        drawCircle(
                            Color.White.copy(alpha = 0.13f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                NpVisualKind.UserPost -> {
                    val center = Offset(w * 0.5f, h * 0.5f)
                    points.forEachIndexed { index, _ ->
                        val angle = (index / points.size.toFloat()) * Math.PI.toFloat() * 2f + (seed % 31) / 31f
                        val radius = base * (0.13f + (index % 5) * 0.042f)
                        val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
                        drawLine(accent.copy(alpha = 0.18f), center, point, strokeWidth = 1.dp.toPx())
                        drawCircle(Color.White.copy(alpha = 0.7f), radius = 2.2.dp.toPx(), center = point)
                    }
                    drawRoundRect(
                        color = accent.copy(alpha = 0.68f),
                        topLeft = Offset(center.x - base * 0.24f, center.y - base * 0.24f),
                        size = Size(base * 0.48f, base * 0.48f),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PoplurText(
                    when (kind) {
                        NpVisualKind.Dominance -> "LIVE MAP CAPTURE"
                        NpVisualKind.UserPost -> "AI STORY IMAGE"
                        else -> "AI GENERATED"
                    },
                    color = if (kind == NpVisualKind.Dominance) Color.White else Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    intensity = 0.56f,
                    modifier = Modifier
                        .background(accent, RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(7.dp))
                PoplurText("$flag $city", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, intensity = 0.56f)
            }
            Spacer(Modifier.height(6.dp))
            PoplurText(
                if (kind == NpVisualKind.Dominance && username != null) "@$username territory lock" else headline,
                color = Color.White,
                fontSize = if (kind == NpVisualKind.Dominance) 16.sp else 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                intensity = 0.68f
            )
        }
    }
}

private fun stableNewsVisualSeed(text: String): Int {
    var result = 5381
    text.forEach { char -> result = result * 33 + char.code }
    return result and Int.MAX_VALUE
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
    val visualKind: NpVisualKind
    val visualAccent: Color
    val visualLat: Double
    val visualLon: Double
    val visualUsername: String?
    val visualMediaUrl: String?
    var extraRow: (@Composable () -> Unit)? = null

    when (article) {
        is NpArticle.MapEvent -> {
            chipBg = Color(0xFFFFD91A); chipFg = Color.Black; chipLabel = "MAP EVENT"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "👥 ${npCount(article.d.userCount)} users nearby"
            stat2 = "📍 Live tracking"
            visualKind = NpVisualKind.MapEvent
            visualAccent = chipBg
            visualLat = article.d.lat
            visualLon = article.d.lon
            visualUsername = null
            visualMediaUrl = null
        }
        is NpArticle.Trending -> {
            chipBg = Color(0xFF2EE880); chipFg = Color.Black; chipLabel = "TRENDING"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "📈 +${article.d.growthPercent}% engagement"
            stat2 = "🟢 ${npCount(article.d.activeUsers)} active"
            visualKind = NpVisualKind.Trending
            visualAccent = chipBg
            visualLat = article.d.lat
            visualLon = article.d.lon
            visualUsername = null
            visualMediaUrl = null
        }
        is NpArticle.Dominance -> {
            chipBg = Color(0xFFD91818); chipFg = Color.White; chipLabel = "DOMINANCE"
            flag = article.d.flag; city = article.d.city; timeAgo = article.d.timeAgo
            headline = article.d.headline; detail = article.d.detail
            stat1 = "🎯 ${article.d.radiusText} radius"
            stat2 = "⭐ ${npCount(article.d.score)} score"
            visualKind = NpVisualKind.Dominance
            visualAccent = chipBg
            visualLat = article.d.lat
            visualLon = article.d.lon
            visualUsername = article.d.username
            visualMediaUrl = null
            extraRow = {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PoplurText("👑", fontSize = 24.sp, intensity = 0.56f)
                    Spacer(Modifier.width(6.dp))
                    ShaderNumberText(
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
            visualKind = NpVisualKind.UserPost
            visualAccent = chipBg
            visualLat = article.d.lat
            visualLon = article.d.lon
            visualUsername = article.d.username
            visualMediaUrl = article.d.mediaUrl
            extraRow = {
                ShaderNumberText(
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
            PoplurText(
                chipLabel,
                color = chipFg, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                intensity = 0.56f,
                modifier = Modifier
                    .background(chipBg, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Spacer(Modifier.width(8.dp))
            ShaderNumberText("$flag $city", color = Color.White.copy(alpha = 0.52f), fontSize = 12.sp, fontWeight = FontWeight.Normal, maxLines = 1)
            Spacer(Modifier.weight(1f))
            ShaderNumberText(timeAgo, color = Color.White.copy(alpha = 0.32f), fontSize = 11.sp, fontWeight = FontWeight.Normal)
        }

        // Optional extra row (crown+username, or @username)
        extraRow?.invoke()

        // Headline
        ShaderNumberText(
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

        NewspaperArticleVisual(
            kind = visualKind,
            city = city,
            flag = flag,
            headline = headline,
            accent = visualAccent,
            lat = visualLat,
            lon = visualLon,
            username = visualUsername,
            mediaUrl = visualMediaUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Detail body
        ShaderNumberText(
            detail,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 21.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Stats
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShaderNumberText(stat1, color = Color.White.copy(alpha = 0.38f), fontSize = 12.sp, fontWeight = FontWeight.Normal, maxLines = 1)
            ShaderNumberText(stat2, color = Color.White.copy(alpha = 0.38f), fontSize = 12.sp, fontWeight = FontWeight.Normal, maxLines = 1)
        }
    }
}

private fun npCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000     -> "%.1fK".format(n / 1_000.0)
    else           -> "$n"
}

private object NewspaperContentGen {

    private data class CityEntry(val name: String, val flag: String, val lat: Double, val lon: Double)

    private val cities = listOf(
        CityEntry("Tokyo",       "🇯🇵",  35.6762,  139.6503),
        CityEntry("London",      "🇬🇧",  51.5074,   -0.1278),
        CityEntry("New York",    "🇺🇸",  40.7128,  -74.0060),
        CityEntry("Paris",       "🇫🇷",  48.8566,    2.3522),
        CityEntry("Sydney",      "🇦🇺", -33.8688,  151.2093),
        CityEntry("Toronto",     "🇨🇦",  43.6532,  -79.3832),
        CityEntry("Dubai",       "🇦🇪",  25.2048,   55.2708),
        CityEntry("Singapore",   "🇸🇬",   1.3521,  103.8198),
        CityEntry("Berlin",      "🇩🇪",  52.5200,   13.4050),
        CityEntry("Barcelona",   "🇪🇸",  41.3851,    2.1734),
        CityEntry("Seoul",       "🇰🇷",  37.5665,  126.9780),
        CityEntry("São Paulo",   "🇧🇷", -23.5505,  -46.6333),
        CityEntry("Mumbai",      "🇮🇳",  19.0760,   72.8777),
        CityEntry("Bangkok",     "🇹🇭",  13.7563,  100.5018),
        CityEntry("Istanbul",    "🇹🇷",  41.0082,   28.9784),
        CityEntry("Lagos",       "🇳🇬",   6.5244,    3.3792),
        CityEntry("Cairo",       "🇪🇬",  30.0444,   31.2357),
        CityEntry("Mexico City", "🇲🇽",  19.4326,  -99.1332),
        CityEntry("Jakarta",     "🇮🇩",  -6.2088,  106.8456),
        CityEntry("Chicago",     "🇺🇸",  41.8781,  -87.6298)
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

    fun generate(count: Int, posts: List<Post> = emptyList()): List<NpArticle> = (0 until count).map { gi ->
        val city = cities[(gi * 7 + gi * 3) % cities.size]
        val timeAgo = times[gi % times.size] + " ago"
        val jitter = { (-0.08..0.08).random() }
        when (gi % 20) {
            in 0..5 -> {
                val (h, d) = mapHeadlines[gi % mapHeadlines.size]
                NpArticle.MapEvent(NpMapEvent(city.name, city.flag, h, d, userCounts[gi % userCounts.size], timeAgo,
                    city.lat + jitter(), city.lon + jitter()))
            }
            in 6..10 -> {
                val t = trendHeadlines[gi % trendHeadlines.size]
                NpArticle.Trending(NpTrending(city.name, city.flag, t.headline, t.detail, t.growth, t.active, timeAgo,
                    city.lat + jitter(), city.lon + jitter()))
            }
            in 11..15 -> {
                val (h, d) = domHeadlines[gi % domHeadlines.size]
                NpArticle.Dominance(NpDominance(domNames[gi % domNames.size], city.name, city.flag, h, d,
                    radii[gi % radii.size], scores[gi % scores.size], timeAgo,
                    city.lat + jitter(), city.lon + jitter()))
            }
            else -> {
                val (h, d) = postHeadlines[gi % postHeadlines.size]
                val post = posts.getOrNull(gi % max(1, posts.size))
                NpArticle.UserPost(NpUserPost(
                    username = post?.displayName?.ifBlank { post.username } ?: fakeHandles[gi % fakeHandles.size],
                    city = post?.locationName?.substringBefore(",")?.ifBlank { city.name } ?: city.name,
                    flag = city.flag,
                    headline = h,
                    detail = d,
                    likes = post?.likes ?: fakeLikes[gi % fakeLikes.size],
                    comments = post?.comments ?: fakeComms[gi % fakeComms.size],
                    timeAgo = timeAgo,
                    lat = post?.latitude ?: (city.lat + jitter()),
                    lon = post?.longitude ?: (city.lon + jitter()),
                    mediaUrl = post?.mediaUrl
                ))
            }
        }
    }
}

private fun ClosedFloatingPointRange<Double>.random(): Double =
    start + Math.random() * (endInclusive - start)

private fun mapTileUrl(lat: Double, lon: Double, zoom: Int = 12): String {
    val n = 1 shl zoom
    val x = ((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    val latRad = Math.toRadians(lat)
    val y = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
    return "https://a.basemaps.cartocdn.com/dark_matter_nolabels/$zoom/$x/$y.png"
}

@Composable
private fun FeedTvGuideOverlay(
    channels: List<FeedTvGuideChannel>,
    currentChannelId: String,
    currentPostName: String,
    activeFeedPlayer: ExoPlayer?,
    onChannelSelected: (FeedTvGuideChannel) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("poplur_settings", Context.MODE_PRIVATE) }
    var useFlipAnimation by remember { mutableStateOf(prefs.getBoolean("tv_guide_flip_animation", true)) }

    val guideGreen = Color(0xFF4EFF40)
    var isTabletMenuOpen by remember { mutableStateOf(true) }
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

        Column(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f))) {
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
                    FeedTvGuideContent(
                        channels = channels,
                        previewedChannelId = previewedChannelId,
                        currentChannelId = currentChannelId,
                        currentPostName = currentPostName,
                        activeFeedPlayer = activeFeedPlayer,
                        guideGreen = guideGreen,
                        useFlipAnimation = useFlipAnimation,
                        onChannelPreviewed = { previewedChannelId = it.id },
                        onChannelSelected = onChannelSelected,
                        previewedChannel = previewedChannel,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                FeedTvGuideContent(
                    channels = channels,
                    previewedChannelId = previewedChannelId,
                    currentChannelId = currentChannelId,
                    currentPostName = currentPostName,
                    activeFeedPlayer = activeFeedPlayer,
                    guideGreen = guideGreen,
                    useFlipAnimation = useFlipAnimation,
                    onChannelPreviewed = { previewedChannelId = it.id },
                    onChannelSelected = onChannelSelected,
                    previewedChannel = previewedChannel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FeedTvGuideContent(
    channels: List<FeedTvGuideChannel>,
    previewedChannelId: String,
    currentChannelId: String,
    currentPostName: String,
    activeFeedPlayer: ExoPlayer?,
    guideGreen: Color,
    useFlipAnimation: Boolean,
    onChannelPreviewed: (FeedTvGuideChannel) -> Unit,
    onChannelSelected: (FeedTvGuideChannel) -> Unit,
    previewedChannel: FeedTvGuideChannel?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(Color.Black.copy(alpha = 0.36f))) {
        // PiP in its own band — never overlaps the channel rows
        previewedChannel?.let { ch ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GuidePipVideoPane(
                    channel = ch,
                    currentPostName = if (ch.id == currentChannelId) currentPostName else "",
                    activeFeedPlayer = activeFeedPlayer,
                    guideGreen = guideGreen,
                    modifier = Modifier
                        .width(62.dp)
                        .height(110.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FeedChannelIcon(ch.iconResId, ch.emoji, 22.dp, 14.sp)
                        PoplurText("CH ${ch.number}", color = guideGreen, fontSize = 16.sp, fontWeight = FontWeight.Black, intensity = 0.86f)
                    }
                    PoplurText(ch.category.uppercase(), color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp, intensity = 0.58f)
                    PoplurText(ch.program, color = Color.White.copy(alpha = 0.42f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, intensity = 0.48f)
                }
                PoplurText(
                    text = "LIVE",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    intensity = 0.56f,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(guideGreen)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Divider(color = guideGreen.copy(alpha = 0.22f), thickness = 1.dp)
        }
        FeedTvGuideList(
            channels = channels,
            previewedChannelId = previewedChannelId,
            guideGreen = guideGreen,
            useFlipAnimation = useFlipAnimation,
            onChannelPreviewed = onChannelPreviewed,
            onChannelSelected = onChannelSelected,
            modifier = Modifier.fillMaxSize()
        )
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
            PoplurText(
                text = if (isRailOpen) "☰" else "☷",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                intensity = 0.78f,
                modifier = Modifier
                    .width(44.dp)
                    .clickable(onClick = onToggleRail)
            )
        }
        Column {
            PoplurText("TV GUIDE", color = Color.White, fontSize = if (isTablet) 34.sp else 28.sp, fontWeight = FontWeight.Black, intensity = 0.92f)
            PoplurText("LIVE CHANNELS", color = guideGreen, fontSize = 12.sp, fontWeight = FontWeight.Black, intensity = 0.72f)
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
                FeedChannelIcon(channel.iconResId, channel.emoji, 34.dp, 22.sp)
                Spacer(Modifier.width(10.dp))
                ShaderNumberText(channel.number.toString(), color = guideGreen, fontSize = 18.sp, fontWeight = FontWeight.Black, intensity = 0.8f)
                Spacer(Modifier.width(10.dp))
                PoplurText(
                    channel.category.uppercase(),
                    color = Color.White.copy(alpha = if (channel.id == currentChannelId) 1f else 0.62f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    intensity = if (channel.id == currentChannelId) 0.78f else 0.56f
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
    useFlipAnimation: Boolean,
    onChannelPreviewed: (FeedTvGuideChannel) -> Unit,
    onChannelSelected: (FeedTvGuideChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            PoplurText("CH", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, fontWeight = FontWeight.Black, intensity = 0.54f, modifier = Modifier.width(80.dp))
            PoplurText("CATEGORY", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, fontWeight = FontWeight.Black, intensity = 0.54f, modifier = Modifier.width(90.dp))
            PoplurText("ON NOW", color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp, fontWeight = FontWeight.Black, intensity = 0.54f, modifier = Modifier.weight(1f))
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
                    useFlipAnimation = useFlipAnimation,
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
    useFlipAnimation: Boolean,
    onChannelPreviewed: () -> Unit,
    onChannelSelected: () -> Unit
) {
    val names = if (channel.displayNames.isNotEmpty()) channel.displayNames else listOf(channel.profile.displayName)
    var nameIndex by remember(channel.id) { mutableStateOf(0) }

    // Stagger cycle start by channel number to avoid all rows flipping at once
    LaunchedEffect(channel.id) {
        if (names.size <= 1) return@LaunchedEffect
        kotlinx.coroutines.delay((channel.number % 5) * 500L)
        while (true) {
            kotlinx.coroutines.delay(2500)
            nameIndex = (nameIndex + 1) % names.size
        }
    }

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
        Row(modifier = Modifier.width(80.dp), verticalAlignment = Alignment.CenterVertically) {
            FeedChannelIcon(channel.iconResId, channel.emoji, 42.dp, 26.sp)
            Spacer(Modifier.width(10.dp))
            ShaderNumberText(text = channel.number.toString(), color = guideGreen, fontSize = 28.sp, fontWeight = FontWeight.Black, intensity = if (isPreviewed) 0.9f else 0.72f)
        }
        PoplurText(
            text = channel.category.uppercase(),
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            intensity = 0.68f,
            modifier = Modifier.width(90.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            val enterTransition = if (useFlipAnimation)
                slideInVertically { it } + fadeIn()
            else
                slideInHorizontally { it } + fadeIn()
            val exitTransition = if (useFlipAnimation)
                slideOutVertically { -it } + fadeOut()
            else
                slideOutHorizontally { -it } + fadeOut()

            AnimatedContent(
                targetState = names[nameIndex],
                transitionSpec = {
                    (enterTransition).togetherWith(exitTransition)
                        .using(SizeTransform(clip = false))
                },
                label = "username-${channel.id}"
            ) { name ->
                PoplurText(
                    text = name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    intensity = 0.74f
                )
            }
            PoplurText(
                text = channel.program,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                intensity = 0.54f
            )
        }
        Spacer(Modifier.width(8.dp))
        PoplurText(
            text = "LIVE",
            color = Color.Black,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            intensity = 0.52f,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(guideGreen)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
        else ExoPlayer.Builder(context)
            .setReleaseTimeoutMs(FEED_PLAYER_RELEASE_TIMEOUT_MS)
            .setDetachSurfaceTimeoutMs(FEED_PLAYER_DETACH_SURFACE_TIMEOUT_MS)
            .build()
            .apply {
                val item = MediaItem.fromUri(videoUrl)
                setMediaItem(item)
                volume = 0f
                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                prepare()
                playWhenReady = true
            }
    }
    DisposableEffect(fallbackPlayer) {
        onDispose {
            fallbackPlayer?.let { player ->
                runCatching {
                    player.pause()
                    player.clearVideoSurface()
                    player.clearMediaItems()
                    player.release()
                }.onFailure { it.printStackTrace() }
            }
        }
    }

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
                    FeedChannelIcon(channel.iconResId, channel.emoji, 22.dp, 13.sp)
                    PoplurText(
                        "CH ${channel.number}",
                        color = guideGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        intensity = 0.8f
                    )
                }
                PoplurText(
                    currentPostName.ifBlank { channel.profile.displayName },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    intensity = 0.68f
                )
                PoplurText(
                    "LIVE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    intensity = 0.52f,
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
private const val FEED_IMAGE_CREDITS_DELAY_MS = 6_300L
private const val FEED_IMAGE_MAX_VISIBLE_MS = 10_000L
private const val FEED_PLAYER_RELEASE_TIMEOUT_MS = 500L
private const val FEED_PLAYER_DETACH_SURFACE_TIMEOUT_MS = 500L

private fun googleDriveMediaUrl(fileId: String): String =
    "https://drive.google.com/uc?export=download&id=$fileId&type=video"

private data class PoplurAdCreative(
    val id: String,
    val channelId: String,
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
    val channelNumber: Int = 100,
    val channelEmoji: String = "📢",
    val channelCategory: String = "Sponsored",
    val channelIconResId: Int? = null,
    val driveFileId: String? = null
)

private val POPLUR_CHANNEL_AD_CREATIVES = mapOf(
    "food" to PoplurAdCreative(
        id = "food-drive-ad",
        channelId = "food",
        brandName = "Poplur Food",
        headline = "Poplur Food is serving nearby",
        body = "Food creators, cravings, and local spots are live inside the Poplur radius.",
        callToAction = "Watch Food",
        creativeKind = "Food channel ad",
        posterUrl = googleDriveMediaUrl("1cNOqd9w6gGpeoU7oMC1xf7iuUcv0l94E"),
        videoUrl = googleDriveMediaUrl("1cNOqd9w6gGpeoU7oMC1xf7iuUcv0l94E"),
        locationName = "Tokyo, Japan",
        latitude = 35.6762,
        longitude = 139.6503,
        radius = 1350,
        postsCount = 1010,
        radiusChange = 21,
        channelNumber = 101,
        channelEmoji = "🍔",
        channelCategory = "Food",
        channelIconResId = R.drawable.poplur_channel_101_food,
        driveFileId = "1cNOqd9w6gGpeoU7oMC1xf7iuUcv0l94E"
    ),
    "sports" to PoplurAdCreative(
        id = "sports-drive-ad",
        channelId = "sports",
        brandName = "Poplur Sports",
        headline = "Sports energy is live",
        body = "Follow the plays, fans, and stadium moments moving through Poplur.",
        callToAction = "Watch Sports",
        creativeKind = "Sports channel ad",
        posterUrl = googleDriveMediaUrl("1FmRd8-hKDd-pkOueGXjDuDS-SpGeIL27"),
        videoUrl = googleDriveMediaUrl("1FmRd8-hKDd-pkOueGXjDuDS-SpGeIL27"),
        locationName = "New York, NY",
        latitude = 40.8296,
        longitude = -73.9262,
        radius = 1600,
        postsCount = 1104,
        radiusChange = 24,
        channelNumber = 104,
        channelEmoji = "⚽",
        channelCategory = "Sports",
        channelIconResId = R.drawable.poplur_channel_104_sports,
        driveFileId = "1FmRd8-hKDd-pkOueGXjDuDS-SpGeIL27"
    ),
    "shopping" to PoplurAdCreative(
        id = "shopping-drive-ad",
        channelId = "shopping",
        brandName = "Poplur Shopping",
        headline = "Shopping drops are nearby",
        body = "Sales, hauls, and local shopping moments are popping up on Poplur.",
        callToAction = "Watch Shopping",
        creativeKind = "Shopping channel ad",
        posterUrl = googleDriveMediaUrl("1nKmjGcUTQW1gn1FVkFMWVwn9ErtH7XmM"),
        videoUrl = googleDriveMediaUrl("1nKmjGcUTQW1gn1FVkFMWVwn9ErtH7XmM"),
        locationName = "Toronto, ON",
        latitude = 43.6544,
        longitude = -79.3807,
        radius = 1250,
        postsCount = 980,
        radiusChange = 19,
        channelNumber = 112,
        channelEmoji = "🛍️",
        channelCategory = "Shopping",
        channelIconResId = R.drawable.poplur_channel_112_shopping,
        driveFileId = "1nKmjGcUTQW1gn1FVkFMWVwn9ErtH7XmM"
    ),
    "music" to PoplurAdCreative(
        id = "music-drive-ad",
        channelId = "music",
        brandName = "Poplur Music",
        headline = "Music is moving through the city",
        body = "Studios, stages, and creator sounds are broadcasting through Poplur.",
        callToAction = "Watch Music",
        creativeKind = "Music channel ad",
        posterUrl = googleDriveMediaUrl("1tfukw5oiNk1VtqEy4wcgToHKNzTq22kT"),
        videoUrl = googleDriveMediaUrl("1tfukw5oiNk1VtqEy4wcgToHKNzTq22kT"),
        locationName = "Los Angeles, CA",
        latitude = 34.0522,
        longitude = -118.2437,
        radius = 1500,
        postsCount = 1070,
        radiusChange = 20,
        channelNumber = 107,
        channelEmoji = "🎵",
        channelCategory = "Music",
        channelIconResId = R.drawable.poplur_channel_107_music,
        driveFileId = "1tfukw5oiNk1VtqEy4wcgToHKNzTq22kT"
    ),
    "cartoon" to PoplurAdCreative(
        id = "cartoon-drive-ad",
        channelId = "cartoon",
        brandName = "Poplur Cartoon",
        headline = "Cartoon worlds are live",
        body = "2D and 3D Poplur stories turn the feed into a bright animated channel.",
        callToAction = "Watch Cartoon",
        creativeKind = "Cartoon channel ad",
        posterUrl = googleDriveMediaUrl("1NyDZewxeHe6R-aiziY5yBVCnwdeFTfZS"),
        videoUrl = googleDriveMediaUrl("1NyDZewxeHe6R-aiziY5yBVCnwdeFTfZS"),
        locationName = "Burbank, CA",
        latitude = 34.1381,
        longitude = -118.3534,
        radius = 1180,
        postsCount = 875,
        radiusChange = 16,
        channelNumber = 113,
        channelEmoji = "🎨",
        channelCategory = "Cartoon",
        channelIconResId = R.drawable.poplur_channel_113_cartoon,
        driveFileId = "1NyDZewxeHe6R-aiziY5yBVCnwdeFTfZS"
    ),
    "kids" to PoplurAdCreative(
        id = "kids-drive-ad",
        channelId = "kids",
        brandName = "Poplur Kids",
        headline = "Kids channel is playing",
        body = "Friendly Poplur moments for games, cartoons, learning, and family-safe fun.",
        callToAction = "Watch Kids",
        creativeKind = "Kids channel ad",
        posterUrl = googleDriveMediaUrl("1rx5BE095nIuA5lyZHictzB09-VsdiAIk"),
        videoUrl = googleDriveMediaUrl("1rx5BE095nIuA5lyZHictzB09-VsdiAIk"),
        locationName = "Orlando, FL",
        latitude = 28.5384,
        longitude = -81.3789,
        radius = 1100,
        postsCount = 820,
        radiusChange = 15,
        channelNumber = 114,
        channelEmoji = "🧸",
        channelCategory = "Kids",
        channelIconResId = R.drawable.poplur_channel_114_kids,
        driveFileId = "1rx5BE095nIuA5lyZHictzB09-VsdiAIk"
    ),
    "comedy" to PoplurAdCreative(
        id = "comedy-drive-ad",
        channelId = "comedy",
        brandName = "Poplur Comedy",
        headline = "Comedy is going live",
        body = "Sketches, reactions, and funny local moments are ready on Poplur.",
        callToAction = "Watch Comedy",
        creativeKind = "Comedy channel ad",
        posterUrl = googleDriveMediaUrl("1Byb1h62MEdDd4xnPb6Ao-_dQXK9T02sX"),
        videoUrl = googleDriveMediaUrl("1Byb1h62MEdDd4xnPb6Ao-_dQXK9T02sX"),
        locationName = "Chicago, IL",
        latitude = 41.8781,
        longitude = -87.6298,
        radius = 1200,
        postsCount = 925,
        radiusChange = 17,
        channelNumber = 115,
        channelEmoji = "😂",
        channelCategory = "Comedy",
        channelIconResId = R.drawable.poplur_channel_115_comedy,
        driveFileId = "1Byb1h62MEdDd4xnPb6Ao-_dQXK9T02sX"
    )
)

private val POPLUR_AD_CREATIVES = listOf(
    PoplurAdCreative(
        id = "news-first",
        channelId = "news",
        brandName = "Poplur Ad",
        headline = "Poplur News is live",
        body = "Real Poplur ad creative for the News channel, built to feel native inside the feed.",
        callToAction = "Open News",
        creativeKind = "News creative",
        posterUrl = "file:///android_asset/feed_ads/poplur_ad_news_first.jpg",
        videoUrl = null,
        locationName = "New York, NY",
        latitude = 40.7128,
        longitude = -74.0060,
        radius = 1250,
        postsCount = 840,
        radiusChange = 18,
        channelNumber = 109,
        channelEmoji = "📰",
        channelCategory = "News",
        channelIconResId = R.drawable.poplur_channel_109_news
    ),
    PoplurAdCreative(
        id = "three-d-city-tour",
        channelId = "sponsored",
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
        channelId = "sponsored",
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

private fun poplurAdCreativeForChannel(channel: FeedChannel): PoplurAdCreative? =
    POPLUR_CHANNEL_AD_CREATIVES[channel.category.lowercase(Locale.US)]

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
    radiusChange = radiusChange,
    channelId = channelCategory.lowercase(Locale.US)
)


private data class FeedChannel(
    val number: Int,
    val emoji: String,
    val category: String,
    val iconResId: Int? = null,
    val sourceLanguageCode: String = "en",
    val videoUrl: String? = null
)

private val FEED_CHANNELS = listOf(
    FeedChannel(101, "🍔", "Food", R.drawable.poplur_channel_101_food),
    FeedChannel(102, "🔴", "Live", R.drawable.poplur_channel_101_food),
    FeedChannel(103, "💄", "Beauty", R.drawable.poplur_channel_103_beauty),
    FeedChannel(104, "⚽", "Sports", R.drawable.poplur_channel_104_sports),
    FeedChannel(105, "📸", "Modelling", R.drawable.poplur_channel_105_modelling),
    FeedChannel(106, "👗", "Fashion", R.drawable.poplur_channel_106_fashion),
    FeedChannel(107, "🎵", "Music", R.drawable.poplur_channel_107_music),
    FeedChannel(108, "💻", "Tech", R.drawable.poplur_channel_108_tech),
    FeedChannel(109, "📰", "News", R.drawable.poplur_channel_109_news),
    FeedChannel(110, "🇫🇷", "French", R.drawable.poplur_channel_110_french, sourceLanguageCode = "fr", videoUrl = languageChannelVideoUrl("French")),
    FeedChannel(111, "🇪🇸", "Spanish", R.drawable.poplur_channel_111_spanish, sourceLanguageCode = "es", videoUrl = languageChannelVideoUrl("Spanish")),
    FeedChannel(112, "🛍️", "Shopping", R.drawable.poplur_channel_112_shopping),
    FeedChannel(113, "🎨", "Cartoon", R.drawable.poplur_channel_113_cartoon),
    FeedChannel(114, "🧸", "Kids", R.drawable.poplur_channel_114_kids),
    FeedChannel(115, "😂", "Comedy", R.drawable.poplur_channel_115_comedy)
)

private sealed interface FeedSlot {
    data class PostSlot(
        val post: Post,
        val channelNumber: Int,
        val channelEmoji: String,
        val channelCategory: String,
        val channelIconResId: Int?
    ) : FeedSlot
    data class AdSlot(val id: String, val creative: PoplurAdCreative) : FeedSlot
}

private fun adjacentChannelFeedIndex(currentIndex: Int, direction: FeedChannelStepDirection): Int {
    val currentSlot = resolveFeedSlot(currentIndex)
    val currentChannelIndex = when (currentSlot) {
        is FeedSlot.PostSlot -> FEED_CHANNELS.indexOfFirst { it.number == currentSlot.channelNumber }
        is FeedSlot.AdSlot -> -1
    }.takeIf { it >= 0 } ?: 0
    val targetChannel = FEED_CHANNELS[
        Math.floorMod(
            currentChannelIndex + if (direction == FeedChannelStepDirection.Up) 1 else -1,
            FEED_CHANNELS.size
        )
    ]
    val step = if (direction == FeedChannelStepDirection.Up) 1 else -1
    var candidate = currentIndex + step
    repeat(AD_FREQUENCY * FEED_CHANNELS.size * 3) {
        val slot = resolveFeedSlot(candidate)
        if (slot is FeedSlot.PostSlot && slot.channelNumber == targetChannel.number) {
            return candidate
        }
        candidate += step
    }
    return currentIndex + step
}

private fun feedChannelForQuery(query: String): FeedChannel? {
    val normalized = query
        .trim()
        .lowercase(Locale.US)
        .replace("modeling", "modelling")
    if (normalized.isBlank()) return null

    return FEED_CHANNELS.firstOrNull { channel ->
        val category = channel.category.lowercase(Locale.US)
        normalized == category ||
            normalized == channel.number.toString() ||
            normalized.contains(category) ||
            normalized.contains(channel.number.toString())
    }
}

private fun feedIndexForChannelNumber(currentIndex: Int, channelNumber: Int): Int {
    var candidate = currentIndex
    repeat(AD_FREQUENCY * FEED_CHANNELS.size * 4) {
        val slot = resolveFeedSlot(candidate)
        if (slot is FeedSlot.PostSlot && slot.channelNumber == channelNumber) {
            return candidate
        }
        candidate += 1
    }
    return currentIndex
}

private val feedSlotCache = mutableMapOf<Int, FeedSlot>()

private fun resolveFeedSlot(index: Int): FeedSlot {
    val normalized = if (index < 0) {
        val cycle = AD_FREQUENCY * DemoPostStore.posts.size
        (cycle + (index % cycle)) % cycle
    } else {
        index
    }

    feedSlotCache[normalized]?.let { return it }

    val slot = if ((normalized + 1) % AD_FREQUENCY == 0) {
        val channelForAd = FEED_CHANNELS[Math.floorMod(normalized / AD_FREQUENCY, FEED_CHANNELS.size)]
        FeedSlot.AdSlot(
            id = "poplur-ad-$normalized",
            creative = poplurAdCreativeForChannel(channelForAd) ?: poplurAdCreativeForIndex(normalized)
        )
    } else {
        val adsBefore = (normalized + 1) / AD_FREQUENCY
        val postIndex = normalized - adsBefore
        val ch = FEED_CHANNELS[Math.floorMod(postIndex, FEED_CHANNELS.size)]
        val basePost = DemoPostStore.getPostForIndex(postIndex)

        if (ch.category.equals("Live", ignoreCase = true)) {
            val visibleLivePosts = DemoPostStore.visibleLivePostsForViewer(basePost, limit = 8)
            val livePost = if (visibleLivePosts.isNotEmpty()) {
                visibleLivePosts[Math.floorMod(postIndex / FEED_CHANNELS.size, visibleLivePosts.size)]
            } else {
                DemoPostStore.livePosts.firstOrNull() ?: basePost.copy(
                    id = "live_placeholder_$postIndex",
                    displayName = "Poplur Live",
                    caption = "No live broadcasts in this radius",
                    channelId = "live",
                    isLiveStream = true,
                    liveStartedAtMillis = System.currentTimeMillis(),
                    liveRadiusMeters = basePost.radius.coerceAtLeast(5_000)
                )
            }
            FeedSlot.PostSlot(
                post = livePost.copy(
                    channelId = "live",
                    isLiveStream = true,
                    videoUrl = null,
                    mediaUrl = null
                ),
                channelNumber = ch.number,
                channelEmoji = ch.emoji,
                channelCategory = ch.category,
                channelIconResId = ch.iconResId
            )
        } else {
            val langVideoUrls = languageChannelVideoUrls(ch.category)
            val effectiveSourceCode = if (langVideoUrls.isNotEmpty())
                languageChannelSourceCode(ch.category, postIndex)
            else
                ch.sourceLanguageCode
            val effectiveVideoUrl = if (langVideoUrls.isNotEmpty())
                langVideoUrls[Math.floorMod(postIndex, langVideoUrls.size)]
            else
                ch.videoUrl
            val post = basePost.copy(
                id = if (effectiveVideoUrl == null) basePost.id else "${basePost.id}_${effectiveSourceCode}_channel",
                displayName = languageChannelDisplayName(ch.category) ?: basePost.displayName,
                caption = languageChannelProgram(ch.category) ?: basePost.caption,
                sourceLanguageCode = effectiveSourceCode,
                videoUrl = effectiveVideoUrl ?: basePost.videoUrl
            )
            FeedSlot.PostSlot(
                post = post,
                channelNumber = ch.number,
                channelEmoji = ch.emoji,
                channelCategory = ch.category,
                channelIconResId = ch.iconResId
            )
        }
    }

    if (feedSlotCache.size > 200) feedSlotCache.clear() // Prevent infinite growth
    feedSlotCache[normalized] = slot
    return slot
}

private fun languageChannelVideoUrl(category: String): String? =
    when (category.lowercase()) {
        "french"  -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        "spanish" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        else -> null
    }

// Per-post video pool for language channels: alternates between native-language and English URLs.
// All use speaking videos so closed-caption recognition has audible speech to process.
private fun languageChannelVideoUrls(category: String): List<String> =
    when (category.lowercase()) {
        "french" -> listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",       // with dialogue (EN — translate in FR channel)
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",   // narrated EN
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFuns.mp4",       // narrated EN
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4"   // narrated EN
        )
        "spanish" -> listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",    // narrated EN
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",       // with dialogue
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4", // narrated EN
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4" // narrated EN
        )
        else -> emptyList()
    }

// Even indices = native channel language; odd indices = English (needs auto-caption).
private fun languageChannelSourceCode(category: String, postIndex: Int): String =
    when (category.lowercase()) {
        "french" -> if (postIndex % 2 == 0) "fr" else "en"
        "spanish" -> if (postIndex % 2 == 0) "es" else "en"
        else -> "en"
    }

private fun languageChannelDisplayName(category: String): String? =
    when (category.lowercase()) {
        "french" -> "Poplur Français"
        "spanish" -> "Poplur Español"
        else -> null
    }

private fun languageChannelProgram(category: String): String? =
    when (category.lowercase()) {
        "french" -> "French-language videos with AI captions"
        "spanish" -> "Spanish-language videos with AI captions"
        else -> null
    }

@Composable
private fun FeedAdContent(
    slot: FeedSlot.AdSlot,
    isVisible: Boolean,
    allowVideoPlayback: Boolean,
    volumeMultiplier: Float,
    useExternalVideoSurface: Boolean,
    onActivePlayerChanged: (ExoPlayer?) -> Unit,
    onCreditsVisibilityChanged: (Boolean) -> Unit = {},
    onAdvanceToNext: () -> Unit = {}
) {
    val adPost = remember(slot.id) { slot.creative.toPost(slot.id) }
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        MediaContent(
            post = adPost,
            isVisible = isVisible,
            allowVideoPlayback = allowVideoPlayback,
            volumeMultiplier = volumeMultiplier,
            useExternalVideoSurface = useExternalVideoSurface,
            onActivePlayerChanged = onActivePlayerChanged,
            onCreditsVisibilityChanged = onCreditsVisibilityChanged,
            onAdvanceToNext = onAdvanceToNext
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
            PoplurText(
                text = "POPLUR AD",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                intensity = 0.68f,
                maxLines = 1
            )
            PoplurText(
                text = creative.headline,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 37.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                intensity = 0.88f
            )
            PoplurText(
                text = creative.body,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                intensity = 0.64f
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .padding(horizontal = 17.dp, vertical = 9.dp)
            ) {
                PoplurText(
                    text = creative.callToAction,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    intensity = 0.7f
                )
            }
        }
    }
}



private fun createFullScreenNativeAdView(context: Context): NativeAdView {
    val adView = NativeAdView(context)
    val displayMetrics = context.resources.displayMetrics
    val cardHeight = displayMetrics.heightPixels
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

    val card = FrameLayout(context).apply {
        background = roundedDrawable(AndroidColor.BLACK, context.dp(8).toFloat())
        clipToOutline = true
    }
    adView.addView(
        card,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    )

    media.setBackgroundColor(AndroidColor.TRANSPARENT)
    media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
    media.layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    card.addView(media)
    adView.mediaView = media

    card.addView(
        View(context).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.argb(230, 0, 0, 0))
            )
        },
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (cardHeight * 0.55f).toInt(),
            Gravity.BOTTOM
        )
    )

    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.START
        setPadding(context.dp(18), context.dp(18), context.dp(18), context.dp(20))
    }
    card.addView(
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
    headlineText.textSize = 32f
    headlineText.typeface = Typeface.DEFAULT_BOLD
    headlineText.maxLines = 2
    headlineText.ellipsize = TextUtils.TruncateAt.END
    content.addView(headlineText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(16) })
    adView.headlineView = headlineText

    bodyText.setTextColor(AndroidColor.argb(230, 255, 255, 255))
    bodyText.textSize = 16f
    bodyText.maxLines = 3
    bodyText.ellipsize = TextUtils.TruncateAt.END
    content.addView(bodyText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(10) })
    adView.bodyView = bodyText

    ctaText.gravity = Gravity.CENTER
    ctaText.setTextColor(AndroidColor.BLACK)
    ctaText.textSize = 16f
    ctaText.typeface = Typeface.DEFAULT_BOLD
    ctaText.background = roundedDrawable(AndroidColor.WHITE, context.dp(24).toFloat())
    content.addView(ctaText, LinearLayout.LayoutParams(context.dp(172), context.dp(48)).apply { topMargin = context.dp(18) })
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
    isMuted: Boolean = false,
    allowVideoPlayback: Boolean = true,
    volumeMultiplier: Float = 1f,
    useExternalVideoSurface: Boolean = false,
    onActivePlayerChanged: (ExoPlayer?) -> Unit = {},
    onFirstFrameRendered: () -> Unit = {},
    onCreditsVisibilityChanged: (Boolean) -> Unit = {},
    onAdvanceToNext: () -> Unit = {}
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
    val showingStillImage = isInspectionMode || !allowVideoPlayback || !ENABLE_FEED_VIDEO_AUTOPLAY || post.videoUrl == null || videoFailed

    if (post.isLiveStream && post.videoUrl == null) {
        LaunchedEffect(post.id, isVisible) {
            onActivePlayerChanged(null)
            onCreditsVisibilityChanged(false)
            isLoading = false
        }
        LiveStreamContent(post = post)
        return
    }

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

    LaunchedEffect(post.id, imageUrl, isVisible, showingStillImage) {
        if (!isVisible || !showingStillImage) {
            onCreditsVisibilityChanged(false)
            return@LaunchedEffect
        }
        onCreditsVisibilityChanged(false)
        delay(FEED_IMAGE_CREDITS_DELAY_MS)
        onCreditsVisibilityChanged(true)
        delay(FEED_IMAGE_MAX_VISIBLE_MS - FEED_IMAGE_CREDITS_DELAY_MS)
        onCreditsVisibilityChanged(false)
        onAdvanceToNext()
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
                allowVideoPlayback && ENABLE_FEED_VIDEO_AUTOPLAY && post.videoUrl != null && !videoFailed -> {
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
                            .setReleaseTimeoutMs(FEED_PLAYER_RELEASE_TIMEOUT_MS)
                            .setDetachSurfaceTimeoutMs(FEED_PLAYER_DETACH_SURFACE_TIMEOUT_MS)
                            .build().apply {
                                val mediaItem = MediaItem.Builder()
                                    .setUri(post.videoUrl)
                                    .setClippingConfiguration(
                                        MediaItem.ClippingConfiguration.Builder()
                                            .setStartPositionMs(1_500L)
                                            .build()
                                    )
                                    .build()
                                setMediaItem(mediaItem)
                                repeatMode = Player.REPEAT_MODE_OFF
                                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                                volume = if (isMuted) 0f else volumeMultiplier
                                addListener(object : Player.Listener {
                                    override fun onPlaybackStateChanged(state: Int) {
                                        isLoading = state == Player.STATE_BUFFERING || (state == Player.STATE_READY && !hasRenderedVideo)
                                        if (state == Player.STATE_ENDED) {
                                            onAdvanceToNext()
                                        }
                                    }

                                    override fun onRenderedFirstFrame() {
                                        hasRenderedVideo = true
                                        isLoading = false
                                        onFirstFrameRendered()
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
                            exoPlayer.playWhenReady = true
                            exoPlayer.play()
                        } else {
                            onActivePlayerChanged(null)
                            exoPlayer.playWhenReady = false
                            exoPlayer.pause()
                        }
                    }

                    LaunchedEffect(exoPlayer, isMuted, volumeMultiplier) {
                        exoPlayer.volume = if (isMuted) 0f else volumeMultiplier
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose {
                            onActivePlayerChanged(null)
                            runCatching {
                                exoPlayer.pause()
                                exoPlayer.clearVideoSurface()
                                exoPlayer.clearMediaItems()
                                exoPlayer.release()
                            }.onFailure { it.printStackTrace() }
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

                    LaunchedEffect(exoPlayer, isVisible, hasRenderedVideo, videoFailed, post.id) {
                        if (!isVisible || !hasRenderedVideo || videoFailed) {
                            onCreditsVisibilityChanged(false)
                            return@LaunchedEffect
                        }
                        while (isVisible && hasRenderedVideo && !videoFailed) {
                            val duration = exoPlayer.duration
                            val position = exoPlayer.currentPosition
                            if (duration > 0 && duration != C.TIME_UNSET) {
                                val remainingMillis = duration - position
                                val leadMillis = (duration / 4L).coerceIn(2_000L, 4_200L)
                                onCreditsVisibilityChanged(remainingMillis in 120L..leadMillis)
                            } else {
                                onCreditsVisibilityChanged(false)
                            }
                            delay(250)
                        }
                        onCreditsVisibilityChanged(false)
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
                                // Don't set player here, wait for update
                            }
                        },
                        update = { textureView ->
                            // Only attach if not already attached to this specific view
                            // ExoPlayer doesn't expose the current view, so we use a tag to track it.
                            if (!useExternalVideoSurface && textureView.tag != exoPlayer) {
                                textureView.tag = exoPlayer
                                exoPlayer.setVideoTextureView(textureView)
                            }

                            // Only re-apply transform if video size changed or first attachment
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
                ShaderIcon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                PoplurText(
                    text = "Media unavailable",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    intensity = 0.52f
                )
            }
        }

    }
}

@Composable
private fun LiveStreamContent(post: Post) {
    val pulse = rememberInfiniteTransition(label = "live-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-pulse-scale"
    )
    val radiusText = remember(post.id, post.liveRadiusMeters, post.radius) {
        formatLiveRadius(post.liveRadiusMeters ?: post.radius)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF010407),
                        Color(0xFF170207),
                        Color(0xFF021714)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size((130 + index * 58).dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.62f - index * 0.12f
                    }
                    .border(
                        width = 2.dp,
                        color = Color.Red.copy(alpha = 0.28f - index * 0.05f),
                        shape = CircleShape
                    )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 26.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                PoplurText(
                    "LIVE",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    intensity = 0.78f
                )
            }

            PoplurText(
                text = post.displayName.ifBlank { post.locationName.ifBlank { "Poplur Live" } },
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 40.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                intensity = 0.9f
            )
            PoplurText(
                text = post.locationName.ifBlank { "Live channel" },
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                intensity = 0.66f
            )
            PoplurText(
                text = "${post.likes.coerceAtLeast(1)} watching - $radiusText radius",
                color = Color(0xFF4DFF58),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                intensity = 0.78f,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.36f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

private fun formatLiveRadius(radiusMeters: Int): String =
    when {
        radiusMeters >= 1_000_000 -> String.format(Locale.US, "%.1fM m", radiusMeters / 1_000_000f)
        radiusMeters >= 1_000 -> String.format(Locale.US, "%.0fk m", radiusMeters / 1_000f)
        else -> "$radiusMeters m"
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
    if (this is android.graphics.drawable.BitmapDrawable) {
        return bitmap?.hasLikelyBlackBars() ?: false
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
    runCatching { draw(canvas) }.getOrElse {
        setBounds(previousBounds)
        return false
    }
    setBounds(previousBounds)
    return bitmap.hasLikelyBlackBars()
}

private fun Bitmap.hasLikelyBlackBars(): Boolean {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        val softwareBitmap = runCatching { copy(Bitmap.Config.ARGB_8888, false) }.getOrNull()
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
                val pixel = runCatching { getPixel(x, y) }.getOrNull() ?: return 0f
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

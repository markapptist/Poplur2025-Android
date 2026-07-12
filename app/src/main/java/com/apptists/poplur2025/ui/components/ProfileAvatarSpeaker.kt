package com.apptists.poplur2025.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.gdx.MarkHeadFragment
import com.apptists.poplur2025.gdx.ProfileTalkingHeadFragment
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.uiTheme
import java.util.Locale

/**
 * Animated speaking avatar for the Profile screen.
 * Replaces the static profile image with an animated avatar that speaks stats on tap.
 *
 * - 2D mode: LibGDX sprite animation using the supplied expression frames.
 * - 3D mode: LibGDX renders the bundled Meshy Mark head GLB.
 * Controlled via [is3D]. Both modes tap to speak stats via Android TTS.
 */
@Composable
fun ProfileAvatarSpeaker(
    avatarUrl: String?,
    totalRadiusMeters: Int,
    postCount: Int,
    size: androidx.compose.ui.unit.Dp = 96.dp,
    is3D: Boolean = false,
    accentColor: Color = Color(0xFF9B59F5),
    cubeSkin: CubeSkin = CubeSkin.Default,
    onWakeActivated: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val theme = cubeSkin.uiTheme()
    val render3DHead = is3D
    var isSpeaking by remember { mutableStateOf(false) }
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }

    // Init TTS
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
            }
        }
        tts.value = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    // Listen for utterance finish to reset isSpeaking
    val listener = remember {
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { isSpeaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { isSpeaking = false }
        }
    }
    LaunchedEffect(tts.value) {
        tts.value?.setOnUtteranceProgressListener(listener)
    }

    // 3D floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "avatar3d")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "floatY"
    )
    val rotY by infiniteTransition.animateFloat(
        initialValue = -4f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "rotY"
    )

    // Mouth animation
    val mouthScale by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(120, easing = LinearEasing), RepeatMode.Reverse),
        label = "mouth"
    )

    // Glow pulse when speaking
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = {
                val engine = tts.value ?: return@clickable
                if (isSpeaking) { engine.stop(); isSpeaking = false; return@clickable }
                isSpeaking = true
                val radiusKm = totalRadiusMeters / 1000
                val radiusMi = (totalRadiusMeters * 0.000621371).toInt()
                val radiusText = when {
                    radiusKm >= 1000 -> "${radiusKm / 1000} thousand kilometers"
                    radiusKm >= 1 -> "$radiusKm kilometers"
                    else -> "just getting started"
                }
                val postText = if (postCount == 1) "1 post" else "$postCount posts"
                val text = "You have $radiusText of radius worldwide, from $postText."
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "profile_stats")
            })
    ) {
        // Outer container matches iOS ProfileAvatarSpeakerView's outer ZStack (size * 1.42 × 1.28).
        // The head itself is size * 1.12 × 1.20 inside it, matching iOS MarkHeadView's frame.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(size * 1.42f)
                .height(size * 1.28f)
        ) {
            // Glow ring when speaking
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(size * 1.34f)
                        .clip(CircleShape)
                        .background(theme.accent.copy(alpha = if (isSpeaking) glowAlpha * 0.34f else 0f))
                )
            }

            if (render3DHead) {
                // SceneView (Filament) renders the head in its own GL context. The old
                // libGDX MarkHeadGdxView never appeared on Android because it contended
                // with the process-global Gdx.* singletons owned by the cube overlay.
                // Sway/bob are animated inside the scene, so no graphicsLayer rotation.
                MarkHeadSceneView(
                    isSpeaking = isSpeaking,
                    modifier = Modifier
                        .width(size * 1.12f)
                        .height(size * 1.20f)
                )
            } else {
                ProfileTalkingHeadGdxView(
                    isSpeaking = isSpeaking,
                    modifier = Modifier
                        .width(size * 1.12f)
                        .height(size * 1.20f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Speaking indicator bars
        if (isSpeaking) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                repeat(4) { i ->
                    val barHeight by infiniteTransition.animateFloat(
                        initialValue = 4f, targetValue = 14f,
                        animationSpec = infiniteRepeatable(
                            tween(200, delayMillis = i * 60, easing = EaseInOutSine),
                            RepeatMode.Reverse
                        ),
                        label = "bar$i"
                    )
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.accent)
                    )
                }
            }
        } else {
            // Hint icon when idle — tappable to manually trigger the AI wake effect
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(50))
                    .then(
                        if (onWakeActivated != null) Modifier.clickable { onWakeActivated() }
                        else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                repeat(4) { index ->
                    Box(
                        Modifier
                            .width(3.dp)
                            .height((6 + index % 2 * 5).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.accent.copy(alpha = 0.72f))
                    )
                }
            }
        }
    }
}

private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

@Composable
private fun ProfileTalkingHeadGdxView(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val containerId = remember { View.generateViewId() }
    val fragmentTag = remember(containerId) { "profile_talking_head_$containerId" }
    val latestSpeaking by rememberUpdatedState(isSpeaking)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                clipChildren = false
                clipToPadding = false
                addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        (v as? FragmentContainerView)
                            ?.attachProfileTalkingHead(activity, fragmentTag, latestSpeaking)
                    }

                    override fun onViewDetachedFromWindow(v: View) = Unit
                })
            }
        },
        update = { container ->
            container.attachProfileTalkingHead(activity, fragmentTag, isSpeaking)
        }
    )

    DisposableEffect(activity, containerId) {
        onDispose {
            val fragmentActivity = activity ?: return@onDispose
            val manager = fragmentActivity.supportFragmentManager
            val fragment = manager.findFragmentByTag(fragmentTag)
            if (fragment != null && !manager.isStateSaved) {
                manager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}

@Composable
private fun MarkHeadGdxView(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val containerId = remember { View.generateViewId() }
    val fragmentTag = remember(containerId) { "mark_head_$containerId" }
    val latestSpeaking by rememberUpdatedState(isSpeaking)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                clipChildren = false
                clipToPadding = false
                addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {
                        (v as? FragmentContainerView)
                            ?.attachMarkHead(activity, fragmentTag, latestSpeaking)
                    }

                    override fun onViewDetachedFromWindow(v: View) = Unit
                })
            }
        },
        update = { container ->
            container.attachMarkHead(activity, fragmentTag, isSpeaking)
        }
    )

    DisposableEffect(activity, containerId) {
        onDispose {
            val fragmentActivity = activity ?: return@onDispose
            val manager = fragmentActivity.supportFragmentManager
            val fragment = manager.findFragmentByTag(fragmentTag)
            if (fragment != null && !manager.isStateSaved) {
                manager.beginTransaction()
                    .remove(fragment)
                    .commitAllowingStateLoss()
            }
        }
    }
}

private fun FragmentContainerView.attachMarkHead(
    activity: FragmentActivity?,
    tag: String,
    isSpeaking: Boolean
) {
    val fragmentActivity = activity ?: return
    val manager = fragmentActivity.supportFragmentManager
    val existing = manager.findFragmentByTag(tag) as? MarkHeadFragment
    if (existing != null) {
        if (getTag() == tag) setTag(null)
        existing.setSpeaking(isSpeaking)
        return
    }
    if (!isAttachedToWindow) return
    if (getTag() == tag) return
    if (manager.isStateSaved) return
    setTag(tag)
    runCatching {
        val fragment = MarkHeadFragment().also { it.setSpeaking(isSpeaking) }
        // commitNow (not commitAllowingStateLoss) so the transaction executes synchronously,
        // right here inside runCatching — the async variant posts to the main-thread queue and
        // can run after this container view has already been detached (e.g. rapid tab switching
        // away from Profile), throwing an uncaught "No view found for id" outside this try/catch.
        manager.beginTransaction()
            .replace(id, fragment, tag)
            .runOnCommit { setTag(null) }
            .commitNowAllowingStateLoss()
    }.onFailure {
        setTag(null)
        post {
            if (isAttachedToWindow) attachMarkHead(activity, tag, isSpeaking)
        }
    }
}

private fun FragmentContainerView.attachProfileTalkingHead(
    activity: FragmentActivity?,
    tag: String,
    isSpeaking: Boolean
) {
    val fragmentActivity = activity ?: return
    val manager = fragmentActivity.supportFragmentManager
    val existing = manager.findFragmentByTag(tag) as? ProfileTalkingHeadFragment
    if (existing != null) {
        if (getTag() == tag) setTag(null)
        existing.setSpeaking(isSpeaking)
        return
    }
    if (!isAttachedToWindow) return
    if (getTag() == tag) return
    if (manager.isStateSaved) return
    setTag(tag)
    runCatching {
        val fragment = ProfileTalkingHeadFragment().also { it.setSpeaking(isSpeaking) }
        // See attachMarkHead above: commitNow so a detached-container race throws inside this
        // runCatching instead of crashing later on the main-thread queue.
        manager.beginTransaction()
            .replace(id, fragment, tag)
            .runOnCommit { setTag(null) }
            .commitNowAllowingStateLoss()
    }.onFailure {
        setTag(null)
        post {
            if (isAttachedToWindow) attachProfileTalkingHead(activity, tag, isSpeaking)
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

private data class ProfileHeadPalette(
    val fill: Color,
    val face: Color,
    val line: Color,
    val accent: Color,
    val accent2: Color,
    val glow: Color,
    val hair: Color,
    val shirt: Color,
    val isToy: Boolean,
    val hasHalo: Boolean
)

private fun CubeSkin.profileHeadPalette(): ProfileHeadPalette {
    val theme = uiTheme()
    return when (this) {
        CubeSkin.Toy -> ProfileHeadPalette(
            fill = Color(0xFFFFBC7A),
            face = Color(0xFFFFBE85),
            line = Color(0xFF152960),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = Color(0x55FFB900),
            hair = Color(0xFF52280F),
            shirt = Color(0xFF0F3EAC),
            isToy = true,
            hasHalo = true
        )
        CubeSkin.Chocolate -> ProfileHeadPalette(
            fill = Color(0xFF632A0D),
            face = Color(0xFF7A3814),
            line = Color(0xFFFFC78A),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = theme.shadow,
            hair = Color(0xFF331404),
            shirt = Color(0xFF421C07),
            isToy = false,
            hasHalo = true
        )
        CubeSkin.Camo -> ProfileHeadPalette(
            fill = Color(0xFF333A1F),
            face = Color(0xFF454729),
            line = Color(0xFFE0CC61),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = theme.shadow,
            hair = Color(0xFF1C2411),
            shirt = Color(0xFF202B16),
            isToy = false,
            hasHalo = true
        )
        CubeSkin.Ice -> ProfileHeadPalette(
            fill = Color(0xFF0A477A),
            face = Color(0xFF0F6AB8),
            line = Color(0xFFD1F5FF),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = Color(0x886BDFFF),
            hair = Color(0xFF6BDFFF),
            shirt = Color(0xFF06335C),
            isToy = false,
            hasHalo = false
        )
        CubeSkin.Carbon, CubeSkin.Clay -> ProfileHeadPalette(
            fill = Color(0xFF800851),
            face = Color(0xFFD61A9E),
            line = Color.White.copy(alpha = 0.92f),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = theme.shadow,
            hair = Color(0xFF2E031F),
            shirt = Color(0xFF420633),
            isToy = false,
            hasHalo = false
        )
        CubeSkin.Rainbow -> ProfileHeadPalette(
            fill = Color(0xFF101533),
            face = Color(0xFFEB856F),
            line = Color.White,
            accent = Color(0xFF29F2FF),
            accent2 = Color(0xFFFF33D9),
            glow = Color(0x7729F2FF),
            hair = Color(0xFF250638),
            shirt = Color(0xFF08153D),
            isToy = false,
            hasHalo = false
        )
        CubeSkin.Gold -> ProfileHeadPalette(
            fill = Color(0xFF94660F),
            face = Color(0xFFD6A32E),
            line = Color(0xFFFFF09A),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = theme.shadow,
            hair = Color(0xFF5F3D0A),
            shirt = Color(0xFF2E2108),
            isToy = false,
            hasHalo = true
        )
        CubeSkin.Default, CubeSkin.Space -> ProfileHeadPalette(
            fill = Color(0xFF111017),
            face = Color(0xFF1F1A2A),
            line = Color.White.copy(alpha = 0.92f),
            accent = theme.accent,
            accent2 = theme.accent2,
            glow = theme.shadow,
            hair = Color(0xFF3D0F70),
            shirt = Color(0xFF0D0D12),
            isToy = false,
            hasHalo = false
        )
    }
}

@Composable
private fun ThemedProfileHead(
    skin: CubeSkin,
    is3D: Boolean,
    isSpeaking: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val palette = skin.profileHeadPalette()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(palette.fill)
    ) {
        if (palette.hasHalo) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.82f)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.16f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize(if (is3D) 0.82f else 0.74f)
                .clip(CircleShape)
                .background(palette.face.copy(alpha = if (isSpeaking) 0.98f else 0.88f))
                .border(2.dp, palette.line.copy(alpha = 0.58f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = if (is3D) 14.dp else 18.dp)
                .fillMaxWidth(0.58f)
                .height(if (is3D) 34.dp else 26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.hair.copy(alpha = 0.94f))
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isSpeaking) 14.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset(y = (-4).dp)
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(if (isSpeaking) 13.dp else 10.dp)
                        .clip(CircleShape)
                        .background(palette.line)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(y = if (isSpeaking) 26.dp else 24.dp)
                .width(if (isSpeaking) 34.dp else 42.dp)
                .height(if (isSpeaking) 18.dp else 8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(palette.line.copy(alpha = 0.88f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.72f)
                .height(if (is3D) 32.dp else 25.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(palette.shirt)
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (isSpeaking) 3.dp else 1.5.dp,
                    color = (if (isSpeaking) accentColor else palette.accent).copy(alpha = if (isSpeaking) 0.92f else 0.54f),
                    shape = RoundedCornerShape(30.dp)
                )
        )
    }
}

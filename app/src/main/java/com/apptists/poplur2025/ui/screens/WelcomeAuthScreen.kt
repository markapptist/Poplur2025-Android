package com.apptists.poplur2025.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AccountType
import com.apptists.poplur2025.data.BusinessProfile
import com.apptists.poplur2025.data.BusinessVerificationMethod
import com.apptists.poplur2025.data.BusinessVerificationStatus
import com.apptists.poplur2025.data.Secrets
import androidx.compose.foundation.shape.CircleShape
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.apptists.poplur2025.ui.components.MarkHeadSceneView
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode

/**
 * Vertical center of the native cube overlay on Welcome, as a fraction of screen height.
 * PoplurRoot feeds this to PersistentCubeOverlay, and AuthLayoutMetrics.welcomeCubeBottom
 * derives the cube's bottom edge from it so Compose content can sit under the cube.
 */
internal const val WELCOME_CUBE_CENTER_FROM_TOP_FRACTION = 0.4056f

private tailrec fun android.content.Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun WelcomeAuthScreen(
    authViewModel: AuthViewModel,
    onLabelChange: (String) -> Unit = {},
    onBackAvailable: ((() -> Unit)?) -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    preLoginMusic: com.apptists.poplur2025.ui.components.PreLoginMusicPlayer? = null,
    onWelcomePermissionsGranted: (Boolean) -> Unit = {}
) {
    WelcomeAuthScreenContent(
        onSignIn = { email, password, onResult ->
            authViewModel.signIn(email, password, onResult)
        },
        onSignUp = { email, password, accountType, businessProfile, onResult ->
            authViewModel.signUp(email, password, accountType, businessProfile, onResult)
        },
        onLabelChange = onLabelChange,
        onBackAvailable = onBackAvailable,
        onSignUpSuccess = onSignUpSuccess,
        preLoginMusic = preLoginMusic,
        onWelcomePermissionsGranted = onWelcomePermissionsGranted
    )
}

@Composable
internal fun WelcomeAuthScreenContent(
    initialMode: AuthMode = AuthMode.Welcome,
    onSignIn: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onSignUp: (String, String, AccountType, BusinessProfile?, (Boolean, String?) -> Unit) -> Unit,
    onLabelChange: (String) -> Unit = {},
    onBackAvailable: ((() -> Unit)?) -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    preLoginMusic: com.apptists.poplur2025.ui.components.PreLoginMusicPlayer? = null,
    onWelcomePermissionsGranted: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var mode by remember { mutableStateOf(initialMode) }
    var welcomePermissionsGranted by rememberSaveable { mutableStateOf(isPreview) }
    val welcomePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        welcomePermissionsGranted = cameraGranted && locationGranted
        onWelcomePermissionsGranted(welcomePermissionsGranted)
    }

    // Stripe page-transition state
    val stripeVisible = remember { mutableStateOf(false) }
    val stripeMidpoint = remember { mutableStateOf<(() -> Unit)?>(null) }
    fun triggerStripe(action: () -> Unit) {
        if (stripeVisible.value) return
        stripeMidpoint.value = action
        stripeVisible.value = true
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(error) {
        if (error != null) {
            delay(3200)
            error = null
        }
    }
    var loading by remember { mutableStateOf(false) }
    var logoBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var welcomeLogoBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        logoBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
            }.getOrNull()
        }
        welcomeLogoBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("welcome_logo.png").use {
                    BitmapFactory.decodeStream(it).asImageBitmap()
                }
            }.getOrNull()
        }
    }

    LaunchedEffect(mode) {
        when (mode) {
            AuthMode.Welcome -> onLabelChange("poplur")
            AuthMode.Login -> onLabelChange("back")
            AuthMode.SignUp -> onLabelChange("back")
        }
    }

    LaunchedEffect(mode, isPreview) {
        if (mode != AuthMode.Welcome) return@LaunchedEffect
        if (isPreview) {
            welcomePermissionsGranted = true
            onWelcomePermissionsGranted(true)
            return@LaunchedEffect
        }
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted && locationGranted) {
            welcomePermissionsGranted = true
            onWelcomePermissionsGranted(true)
        } else {
            welcomePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(mode) {
        // The persistent cube shows a "back" face and serves as the back button on every
        // auth screen except Welcome (the root of the flow, nothing to go back to).
        // SignUp's own back action (first step -> Welcome, else -> previous step) is
        // reported from within BusinessSignupTicketContent instead, since it varies per step.
        when (mode) {
            AuthMode.Welcome -> onBackAvailable(null)
            AuthMode.Login -> onBackAvailable {
                error = null; mode = AuthMode.Welcome
            }
            AuthMode.SignUp -> Unit
        }
    }

    // Canonical Good Copy: artwork is present immediately, logo drops at 300ms, and the
    // two controls fade in together at 1.3s. The cube is managed independently by PoplurRoot.
    val bannerOffsetY = remember { Animatable(0f) }
    val welcomeLogoOffsetY = remember { Animatable(-1000f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(300)
        welcomeLogoOffsetY.animateTo(
            0f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 150f)
        )
    }
    LaunchedEffect(Unit) {
        delay(1300)
        buttonsAlpha.animateTo(1f, animationSpec = tween(500))
    }

    val activity = remember(context) { context.findFragmentActivity() }
    val host = activity as? com.apptists.poplur2025.ui.components.PersistentCubeOverlayHost
    // One mute button controls both the confetti sound effect and the pre-login background
    // music — starts from whichever the music player already has persisted.
    var isMuted by rememberSaveable { mutableStateOf(preLoginMusic?.isMuted ?: false) }

    DisposableEffect(Unit) {
        host?.showConfettiOverlay()
        onDispose {
            host?.hideConfettiOverlay()
        }
    }

    LaunchedEffect(isMuted) {
        host?.setConfettiMuted(isMuted)
        if (preLoginMusic != null && preLoginMusic.isMuted != isMuted) {
            preLoginMusic.toggleMute()
        }
    }

    // Poplur AI is HEARD, not seen — it speaks the same greeting as iOS on each auth screen
    // (no visible UI). Muted by the same toggle as the music/confetti.
    val authGuideTts = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) authGuideTts.value?.language = Locale.US
        }
        authGuideTts.value = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    LaunchedEffect(mode, isMuted, authGuideTts.value) {
        val engine = authGuideTts.value ?: return@LaunchedEffect
        if (isMuted) { engine.stop(); return@LaunchedEffect }
        val greeting = when (mode) {
            AuthMode.Welcome -> "Welcome to Poplur — every post has a reach radius you control, from your block to the whole planet. Tap Sign Up to join, or Sign In to come back."
            AuthMode.Login -> "Good to have you back. Enter your email and password. If you need a reset, tap FORGOT."
            AuthMode.SignUp -> "Let's get you set up. Choose personal for your own posts, or business if you represent a brand."
        }
        engine.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "auth_${mode.name}")
    }

    BoxWithConstraints(
        Modifier.fillMaxSize()
    ) {
        val authLayout = remember(maxWidth, maxHeight, mode) {
            AuthLayoutMetrics(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                mode = mode
            )
        }

        // Good Copy keeps the Welcome artwork clean; the mute control remains available on
        // Login and Sign Up where it does not compete with the hero logo.
        if (mode != AuthMode.Welcome) {
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .zIndex(50f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                        .border(1.5.dp, Color.Black.copy(alpha = 0.08f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color(0xFFFF2D8E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // iOS SignUpView uses a plain white background (no pink ticket card), so only
        // draw the ticket panel for Welcome/Login to match. The white fill stops short of
        // the bottom so the decorative persistent cube (rendered behind Compose) still shows
        // through, matching Welcome/Login's reserved bottom clearance.
        // Capture BoxWithConstraints' maxHeight — it isn't reachable via implicit receiver
        // inside the AnimatedContent content lambda (that scope is AnimatedContentScope).
        val panelMaxHeight = maxHeight

        // Horizontal carousel between the three auth panels (welcome=0, login=1, signup=2),
        // matching the iOS WelcomeView refactor: forward navigation slides the new panel in
        // from the right; back navigation slides it in from the left. The SpriteKit-equivalent
        // background (decorative cube behind Compose) and confetti overlay stay persistent.
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val dir = if (forward) 1 else -1
                (slideInHorizontally(animationSpec = tween(380)) { w -> dir * w } + fadeIn(tween(380))) togetherWith
                    (slideOutHorizontally(animationSpec = tween(380)) { w -> -dir * w } + fadeOut(tween(380)))
            },
            modifier = Modifier.fillMaxSize(),
            label = "authCarousel"
        ) { panel ->
            Box(Modifier.fillMaxSize()) {
                if (panel == AuthMode.Welcome) {
                    WelcomeBackdrop(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                            .offset { IntOffset(0, bannerOffsetY.value.roundToInt()) }
                    )
                } else if (panel == AuthMode.SignUp) {
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .height((panelMaxHeight - 260.dp).coerceAtLeast(0.dp))
                            .background(Color.White)
                    )
                } else {
                    TicketPanel(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .zIndex(1f)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .offset { IntOffset(0, bannerOffsetY.value.roundToInt()) }
                            .padding(
                                start = authLayout.ticketHorizontalInset,
                                end = authLayout.ticketHorizontalInset,
                                top = authLayout.ticketTopInset,
                                bottom = authLayout.ticketBottomInset
                            )
                    )
                }

                Box(Modifier.fillMaxSize().zIndex(3f)) {
                    when (panel) {
                        AuthMode.Welcome -> WelcomeTicketContent(
                            logoBitmap = welcomeLogoBitmap ?: logoBitmap,
                            layout = authLayout,
                            logoOffsetY = welcomeLogoOffsetY.value,
                            buttonsAlpha = buttonsAlpha.value,
                            onSignIn = { mode = AuthMode.Login },
                            onCreateAccount = { mode = AuthMode.SignUp }
                        )
                        AuthMode.Login -> LoginTicketContent(
                            logoBitmap = logoBitmap,
                            email = email,
                            password = password,
                            error = error,
                            loading = loading,
                            mode = panel,
                            layout = authLayout,
                            onEmailChange = { email = it },
                            onPasswordChange = { password = it },
                            onSubmit = {
                                if (email.isBlank() || password.isBlank()) {
                                    error = "Enter an email and password."
                                } else {
                                    loading = true
                                    error = null
                                    onSignIn(email, password) { ok, msg ->
                                        loading = false
                                        if (!ok) error = msg
                                    }
                                }
                            }
                        )
                        AuthMode.SignUp -> BusinessSignupTicketContent(
                            logoBitmap = logoBitmap,
                            error = error,
                            loading = loading,
                            layout = authLayout,
                            onBack = { error = null; mode = AuthMode.Welcome },
                            onSubmit = { signupEmail, signupPassword, signupAccountType, businessProfile ->
                                loading = true
                                error = null
                                onSignUp(signupEmail, signupPassword, signupAccountType, businessProfile) { ok, msg ->
                                    loading = false
                                    if (!ok) error = msg else onSignUpSuccess()
                                }
                            },
                            onValidationError = { error = it },
                            onLabelChange = onLabelChange,
                            onBackAvailable = onBackAvailable
                        )
                    }
                }
            }
        }

        // Poplur AI is HEARD, not seen (matches iOS): no visible guide card — it only speaks the
        // greeting via TextToSpeech (see the AuthMode TTS LaunchedEffect above).

        ErrorBanner(
            message = error,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(20f)
        )

        if (stripeVisible.value) {
            StripeTransitionOverlay(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f),
                onMidpoint = {
                    stripeMidpoint.value?.invoke()
                    stripeMidpoint.value = null
                },
                onComplete = { stripeVisible.value = false }
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it * 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 12.dp)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF2D96))
            Text(
                message.orEmpty(),
                color = Color(0xFFFF2D96),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun WelcomeTicketContent(
    logoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    layout: AuthLayoutMetrics,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    logoOffsetY: Float = 0f,
    buttonsAlpha: Float = 1f
) {
    // Cream extruded clay type from the prototype good-copy-welcome.html `.clayText` layers.
    val clayCream = Color(0xFFF8EFDC)
    val clayShadow = Shadow(
        color = Color(0xFF9E0E4E).copy(alpha = 0.5f),
        offset = Offset(0f, 4f),
        blurRadius = 8f
    )
    Box(
        Modifier.fillMaxSize()
    ) {
        WelcomeLogo(
            logoBitmap = logoBitmap,
            floats = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.welcomeLogoTop)
                .size(layout.welcomeLogoSize)
                .graphicsLayer {
                    translationY = logoOffsetY
                }
        )

        // Prototype Welcome copy — replaces the old local-radio line. Full-width column so the
        // AI status caption (good-copy-welcome.html `.aiText`, width 84% centered) renders as a
        // centered full-width line below the tagline instead of clipping — mirrors iOS's
        // AuthConfettiBackground copy VStack `.frame(maxWidth: .infinity)`.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = layout.welcomeCubeBottom + 8.dp)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome to Poplur!",
                color = clayCream,
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = LocalTextStyle.current.copy(shadow = clayShadow)
            )
            Text(
                text = "Post. Explore. Connect.\nExpand your world.",
                color = clayCream,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = LocalTextStyle.current.copy(shadow = clayShadow)
            )
            Text(
                text = "Poplur AI is shaping tonight’s map for you…",
                color = clayCream,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                style = LocalTextStyle.current.copy(shadow = clayShadow)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.welcomeButtonTop)
                .alpha(buttonsAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(layout.welcomeButtonGap)
        ) {
            WelcomeShaderButton(
                text = "Sign In",
                width = layout.welcomeButtonWidth,
                height = layout.welcomeButtonHeight,
                shadowOffset = layout.welcomeButtonShadowOffset,
                textSize = layout.welcomeButtonTextSize,
                onClick = onSignIn
            )

            WelcomeShaderButton(
                text = "Sign Up",
                width = layout.welcomeButtonWidth,
                height = layout.welcomeButtonHeight,
                shadowOffset = layout.welcomeButtonShadowOffset,
                textSize = layout.welcomeButtonTextSize,
                onClick = onCreateAccount
            )
        }
    }
}

@Composable
private fun WelcomeShaderButton(
    text: String,
    width: Dp,
    height: Dp,
    shadowOffset: Dp,
    textSize: Float,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(height / 2f)
    // Dark clay dough pill (prototype good-copy-welcome.html dark-mode `.cap-btn`): near-black
    // body, a pink clay rim that reads as a glow, and solid pink pressed text.
    val pinkRim = Color(0xFFF4459C)
    val density = LocalDensity.current
    val textShadow = with(density) {
        Shadow(
            color = Color(0xFF8E0F4C),
            offset = Offset(0f, 2.5.dp.toPx()),
            blurRadius = 2.dp.toPx()
        )
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = shadowOffset)
                .background(Color(0xFF5A0228).copy(alpha = 0.55f), shape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF322126),
                            Color(0xFF241619),
                            Color(0xFF16090D)
                        )
                    )
                )
                .border(3.dp, pinkRim, shape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                // Soft top-left dough highlight (inset 0 3px 4px rgba(255,255,255,0.22)).
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.72f, size.height * 0.72f)
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
                // Pressed bottom shade (inset 0 -4px 7px rgba(0,0,0,0.5)).
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        startY = size.height * 0.5f,
                        endY = size.height
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
            Text(
                text = text,
                color = pinkRim,
                fontWeight = FontWeight.Black,
                fontSize = textSize.sp,
                style = LocalTextStyle.current.copy(shadow = textShadow)
            )
        }
    }
}

@Composable
private fun LoginTicketContent(
    logoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    email: String,
    password: String,
    error: String?,
    loading: Boolean,
    mode: AuthMode,
    layout: AuthLayoutMetrics,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        WelcomeLogo(
            logoBitmap = logoBitmap,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.loginLogoTop)
                .size(layout.logoSize)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = layout.formHorizontalInset,
                    end = layout.formHorizontalInset,
                    top = layout.loginFieldsTop
                )
                .fillMaxWidth()
        ) {
            LoginFields(
                email = email,
                password = password,
                error = error,
                layout = layout,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange
            )
        }

        Button(
            onClick = onSubmit,
            enabled = !loading,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = layout.formHorizontalInset,
                    end = layout.formHorizontalInset,
                    top = layout.submitButtonTop
                )
                .fillMaxWidth()
                .height(layout.submitButtonHeight),
            shape = RoundedCornerShape(layout.submitButtonHeight / 2f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.75f),
                disabledContentColor = Color.Black.copy(alpha = 0.55f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
        ) {
            Text(
                when (mode) {
                    AuthMode.Login -> if (loading) "Signing In" else "Sign In"
                    AuthMode.SignUp -> if (loading) "Creating" else "Create"
                    AuthMode.Welcome -> "Sign In"
                },
                fontWeight = FontWeight.Black,
                fontSize = layout.submitButtonTextSize.sp
            )
        }
    }
}

@Composable
private fun BusinessSignupTicketContent(
    logoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    error: String?,
    loading: Boolean,
    layout: AuthLayoutMetrics,
    onBack: () -> Unit,
    onSubmit: (String, String, AccountType, BusinessProfile?) -> Unit,
    onValidationError: (String?) -> Unit,
    onLabelChange: (String) -> Unit = {},
    onBackAvailable: ((() -> Unit)?) -> Unit = {}
) {
    var signupStep by rememberSaveable { mutableStateOf(0) }
    var cubeSpinToken by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        onLabelChange("back")
    }

    var selectedAccountType by rememberSaveable { mutableStateOf(AccountType.User.wireValue) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var businessName by rememberSaveable { mutableStateOf("") }
    var businessCategory by rememberSaveable { mutableStateOf("") }
    var businessDescription by rememberSaveable { mutableStateOf("") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("43.6532") }
    var longitude by rememberSaveable { mutableStateOf("-79.3832") }
    var contactEmail by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var website by rememberSaveable { mutableStateOf("") }
    var logoPhotoNotes by rememberSaveable { mutableStateOf("") }
    var verificationRequested by rememberSaveable { mutableStateOf(false) }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var proofSummary by rememberSaveable { mutableStateOf("") }
    val selectedMethods = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()

    val accountType = AccountType.fromWire(selectedAccountType)
    fun setVerificationMethod(method: BusinessVerificationMethod, selected: Boolean) {
        if (selected) {
            if (!selectedMethods.contains(method.wireValue)) selectedMethods.add(method.wireValue)
        } else {
            selectedMethods.remove(method.wireValue)
        }
    }

    fun buildBusinessProfile(): BusinessProfile? =
        if (accountType == AccountType.Business) {
            BusinessProfile(
                businessName = businessName.trim(),
                category = businessCategory.trim(),
                description = businessDescription.trim(),
                address = businessAddress.trim(),
                latitude = latitude.toDoubleOrNull() ?: 43.6532,
                longitude = longitude.toDoubleOrNull() ?: -79.3832,
                contactEmail = contactEmail.trim(),
                contactPhone = contactPhone.trim(),
                website = website.trim(),
                logoPhotoNotes = logoPhotoNotes.trim(),
                verificationStatus = BusinessVerificationStatus.PendingReview,
                verificationMethods = selectedMethods.toList(),
                proofSummary = proofSummary.trim(),
                requiresReverification = false
            )
        } else {
            null
        }

    fun submitSignup() {
        val validationError = validateSignup(
            accountType = accountType,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            businessName = businessName,
            businessCategory = businessCategory,
            businessAddress = businessAddress,
            latitude = latitude,
            longitude = longitude,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            verificationRequested = verificationRequested,
            acceptedTerms = acceptedTerms,
            selectedMethods = selectedMethods,
            proofSummary = proofSummary
        )
        if (validationError != null) {
            onValidationError(validationError)
            return
        }
        onValidationError(null)
        onSubmit(email.trim(), password, accountType, buildBusinessProfile())
    }

    fun moveToStep(nextStep: Int) {
        cubeSpinToken += 1
        signupStep = nextStep
        onValidationError(null)
    }

    fun advanceSignup() {
        if (loading) return
        when (signupStep) {
            0 -> moveToStep(1)
            1 -> {
                val validationError = validateAccountStep(email, password, confirmPassword)
                if (validationError != null) {
                    onValidationError(validationError)
                    return
                }
                if (accountType == AccountType.Business) moveToStep(2) else submitSignup()
            }
            2 -> {
                val validationError = validateBusinessProfileStep(
                    businessName = businessName,
                    businessCategory = businessCategory,
                    businessAddress = businessAddress,
                    latitude = latitude,
                    longitude = longitude,
                    contactEmail = contactEmail,
                    contactPhone = contactPhone
                )
                if (validationError != null) {
                    onValidationError(validationError)
                    return
                }
                moveToStep(3)
            }
            3 -> {
                val validationError = validateVerificationStep(
                    verificationRequested = verificationRequested,
                    acceptedTerms = acceptedTerms,
                    selectedMethods = selectedMethods,
                    proofSummary = proofSummary
                )
                if (validationError != null) {
                    onValidationError(validationError)
                    return
                }
                moveToStep(4)
            }
            else -> submitSignup()
        }
    }

    fun stepBack() {
        onValidationError(null)
        signupStep = when (signupStep) {
            0 -> 0
            1 -> 0
            2 -> 1
            3 -> 2
            else -> 3
        }
    }

    LaunchedEffect(signupStep) {
        // The persistent cube's "back" face doubles as the back button here too.
        onBackAvailable {
            if (signupStep == 0) onBack() else stepBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = if (signupStep == 0) {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = layout.formHorizontalInset)
                    .padding(top = 24.dp, bottom = 20.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = layout.formHorizontalInset)
                    .padding(top = 24.dp, bottom = 190.dp)
            },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeLogo(
                logoBitmap = logoBitmap,
                modifier = Modifier.size((layout.logoSize * 0.58f).coerceIn(116.dp, 150.dp))
            )
            if (signupStep == 0) {
                Spacer(Modifier.weight(1f))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SignupStepPagination(currentStep = 0, totalSteps = 5)
                    Text(
                        signupStepTitle(signupStep),
                        color = Color.Black,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        signupStepSubtitle(signupStep, accountType),
                        color = Color.Black.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    SignupAccountTypeCard(
                        title = "Personal",
                        isBusiness = false,
                        selected = accountType == AccountType.User,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedAccountType = AccountType.User.wireValue
                        onValidationError(null)
                    }
                    SignupAccountTypeCard(
                        title = "Business",
                        isBusiness = true,
                        selected = accountType == AccountType.Business,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedAccountType = AccountType.Business.wireValue
                        if (contactEmail.isBlank()) contactEmail = email
                        onValidationError(null)
                    }
                }
                Spacer(Modifier.weight(1f))
                SignupNavigationCube(
                    onBack = onBack,
                    onForward = { advanceSignup() }
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    signupStepTitle(signupStep),
                    color = Color.Black,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    signupStepSubtitle(signupStep, accountType),
                    color = Color.Black.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))

                if (signupStep == 1) SignupSection("Account") {
                    SignupTextField("Email", email) {
                        email = it
                        if (contactEmail.isBlank()) contactEmail = it
                    }
                    SignupTextField("Password", password, isPassword = true) { password = it }
                    SignupTextField("Confirm password", confirmPassword, isPassword = true) { confirmPassword = it }
                }

                if (accountType == AccountType.Business && signupStep == 2) {
                    SignupSection("Create Business Profile") {
                        SignupTextField("Business name", businessName) { businessName = it }
                        SignupTextField("Business category", businessCategory) { businessCategory = it }
                        SignupTextField("Description", businessDescription, singleLine = false) { businessDescription = it }
                        SignupTextField("Address / location on map", businessAddress) { businessAddress = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SignupTextField("Latitude", latitude, modifier = Modifier.weight(1f)) { latitude = it }
                            SignupTextField("Longitude", longitude, modifier = Modifier.weight(1f)) { longitude = it }
                        }
                        SignupMapLocationPreview(
                            businessName = businessName.ifBlank { "Business location" },
                            address = businessAddress,
                            latitude = latitude,
                            longitude = longitude
                        )
                        SignupTextField("Contact email", contactEmail) { contactEmail = it }
                        SignupTextField("Contact phone", contactPhone) { contactPhone = it }
                        SignupTextField("Website", website) { website = it }
                        SignupTextField("Logo/photos notes", logoPhotoNotes, singleLine = false) { logoPhotoNotes = it }
                    }
                }

                if (accountType == AccountType.Business && signupStep == 3) {
                    SignupSection("Request Verification") {
                        Button(
                            onClick = { verificationRequested = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)
                        ) {
                            Text(
                                if (verificationRequested) "Verification Requested" else "Verify My Business",
                                fontWeight = FontWeight.Black
                            )
                        }

                        if (verificationRequested) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = acceptedTerms,
                                    onCheckedChange = { acceptedTerms = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color.Black, uncheckedColor = Color.Black)
                                )
                                Text(
                                    "I agree that major changes to name, address, or ownership can trigger re-verification.",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Choose proof methods",
                                color = Color.Black.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            BusinessVerificationMethod.entries.forEach { method ->
                                val checked = selectedMethods.contains(method.wireValue)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { setVerificationMethod(method, !checked) }
                                        .background(Color.Black.copy(alpha = 0.05f))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { setVerificationMethod(method, it) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color.Black, uncheckedColor = Color.Black)
                                    )
                                    Text(
                                        method.label,
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            SignupTextField("Submit proof details", proofSummary, singleLine = false) { proofSummary = it }
                        }
                    }
                }

                if (accountType == AccountType.Business && signupStep == 4) {
                    SignupSection("Review Process") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BusinessVerificationStatus.entries.forEach { status ->
                                val active = status == if (verificationRequested) {
                                    BusinessVerificationStatus.PendingReview
                                } else {
                                    BusinessVerificationStatus.NotVerified
                                }
                                Text(
                                    status.label,
                                    color = if (active) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (active) Color.Black else Color.Black.copy(alpha = 0.05f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Text(
                            "Approved businesses show a verified badge and unlock business-only features. Rejected businesses can see the reason and resubmit proof.",
                            color = Color.Black.copy(alpha = 0.74f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = !loading,
                    onClick = { advanceSignup() },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Black.copy(alpha = 0.72f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        signupActionTitle(signupStep, accountType, loading),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SignupStepPagination(currentStep: Int, totalSteps: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(totalSteps) { index ->
                Box(
                    Modifier
                        .width(if (index == currentStep) 16.dp else 6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index == currentStep) Color(0xFFFF2D96)
                            else Color.Black.copy(alpha = 0.16f)
                        )
                )
            }
        }
        Text(
            "${currentStep + 1} / $totalSteps",
            color = Color.Black.copy(alpha = 0.58f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun SignupAccountTypeCard(
    title: String,
    isBusiness: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = if (isBusiness) Color(0xFFFF2D96) else Color(0xFF34C759)
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) accent.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.04f))
            .border(1.5.dp, accent.copy(alpha = if (selected) 0.7f else 0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
            if (isBusiness) {
                SignupModelScene(
                    modelPath = "signup/zoom_buildings.glb",
                    modifier = Modifier.size(42.dp),
                    background = Color(0xFFF3E9F8),
                    rotationY = -28f
                )
            } else {
                MarkHeadSceneView(
                    isSpeaking = false,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
                    headBackground = Color(0xFFF7E7EF)
                )
            }
            Text(
                title,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF080207), Color(0xFF751244), Color(0xFFFF2D8A), Color(0xFF371020), Color(0xFFFF4D9E))
                    )
                )
            )
        }
    }
}

@Composable
private fun SignupNavigationCube(onBack: () -> Unit, onForward: () -> Unit) {
    var showingBack by rememberSaveable { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        SignupModelScene(
            modelPath = "login_cube/poplr_login_cube_equal_faces.glb",
            modifier = Modifier
                .size(124.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragEnd = { if (kotlin.math.abs(dragX) > 24f) showingBack = !showingBack },
                        onDrag = { change, amount -> change.consume(); dragX += amount.x }
                    )
                }
                .clickable { if (showingBack) onBack() else onForward() },
            background = Color(0xFFFF2D96),
            rotationY = if (showingBack) 124f else 214f,
            rotationX = -26f
        )
        Text(
            if (showingBack) "Back" else "Continue",
            color = Color.Black.copy(alpha = 0.62f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SignupModelScene(
    modelPath: String,
    modifier: Modifier,
    background: Color,
    rotationY: Float,
    rotationX: Float = -14f
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            SceneView(ctx).apply {
                lifecycle = lifecycleOwner.lifecycle
                scene.skybox = Skybox.Builder().color(background.red, background.green, background.blue, 1f).build(engine)
                indirectLight = IndirectLight.Builder().irradiance(1, floatArrayOf(1f, 1f, 1.05f)).intensity(55_000f).build(engine)
                val instance = modelLoader.createModelInstance(modelPath)
                val model = ModelNode(modelInstance = instance, scaleToUnits = 1.55f, centerOrigin = Float3(0f, 0f, 0f))
                model.rotation = Float3(rotationX, rotationY, 0f)
                childNodes = listOf(model)
                cameraNode.position = Float3(0f, 0f, 3.4f)
                cameraNode.lookAt(model)
                tag = model
            }
        },
        update = { view -> (view.tag as? ModelNode)?.rotation = Float3(rotationX, rotationY, 0f) },
        onRelease = { it.destroy() }
    )
}

@Composable
private fun SignupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
        content()
    }
}

@Composable
private fun SignupTextField(
    placeholder: String,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = Color.Black.copy(alpha = 0.36f), fontWeight = FontWeight.Bold)
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = LocalTextStyle.current.copy(color = Color.Black, fontWeight = FontWeight.Bold),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Black.copy(alpha = 0.32f),
            focusedContainerColor = Color.Black.copy(alpha = 0.04f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.04f),
            cursorColor = Color.Black
        )
    )
}

@Composable
private fun SignupMapLocationPreview(
    businessName: String,
    address: String,
    latitude: String,
    longitude: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Map location", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Text(businessName, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(address.ifBlank { "Add an address so Poplur can place this business on the map." }, color = Color.Black.copy(alpha = 0.74f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("${latitude.ifBlank { "lat" }}, ${longitude.ifBlank { "lng" }}", color = Color.Black.copy(alpha = 0.74f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun signupStepTitle(step: Int): String = when (step) {
    0 -> "Choose Account"
    1 -> "Create Login"
    2 -> "Business Profile"
    3 -> "Verify Business"
    else -> "Review & Submit"
}

private fun signupStepSubtitle(step: Int, accountType: AccountType): String = when (step) {
    0 -> "Choose a personal user account or a verified business account."
    1 -> if (accountType == AccountType.Business) {
        "Create the login for the business admin account."
    } else {
        "Create your login, then continue with normal user onboarding."
    }
    2 -> "Attach the business to a real map location and add its public details."
    3 -> "Request verification and submit proof so Poplur can review the business."
    else -> if (accountType == AccountType.Business) {
        "Confirm the request before submitting the business for review."
    } else {
        "Create your account."
    }
}

private fun signupActionTitle(step: Int, accountType: AccountType, loading: Boolean): String = when {
    loading -> "Submitting"
    step == 1 && accountType == AccountType.User -> "Create User Account"
    step >= 4 && accountType == AccountType.Business -> "Submit Business for Review"
    else -> "Continue"
}

private fun validateAccountStep(
    email: String,
    password: String,
    confirmPassword: String
): String? {
    if (email.isBlank() || password.isBlank()) return "Enter an email and password."
    if (password.length < 6) return "Password must be at least 6 characters."
    if (password != confirmPassword) return "Passwords do not match."
    return null
}

private fun validateBusinessProfileStep(
    businessName: String,
    businessCategory: String,
    businessAddress: String,
    latitude: String,
    longitude: String,
    contactEmail: String,
    contactPhone: String
): String? {
    if (businessName.isBlank() || businessCategory.isBlank() || businessAddress.isBlank()) {
        return "Add your business name, category, and map address."
    }
    if (latitude.toDoubleOrNull() == null || longitude.toDoubleOrNull() == null) {
        return "Enter a valid latitude and longitude for the business map location."
    }
    if (contactEmail.isBlank() && contactPhone.isBlank()) {
        return "Add a business contact email or phone number."
    }
    return null
}

private fun validateVerificationStep(
    verificationRequested: Boolean,
    acceptedTerms: Boolean,
    selectedMethods: List<String>,
    proofSummary: String
): String? {
    if (!verificationRequested || !acceptedTerms) {
        return "Tap Verify My Business and agree to the verification terms."
    }
    if (selectedMethods.isEmpty()) return "Choose at least one verification method."
    if (proofSummary.isBlank()) return "Submit proof details for review."
    return null
}

private fun validateSignup(
    accountType: AccountType,
    email: String,
    password: String,
    confirmPassword: String,
    businessName: String,
    businessCategory: String,
    businessAddress: String,
    latitude: String,
    longitude: String,
    contactEmail: String,
    contactPhone: String,
    verificationRequested: Boolean,
    acceptedTerms: Boolean,
    selectedMethods: List<String>,
    proofSummary: String
): String? {
    validateAccountStep(email, password, confirmPassword)?.let { return it }
    if (accountType == AccountType.User) return null
    validateBusinessProfileStep(
        businessName = businessName,
        businessCategory = businessCategory,
        businessAddress = businessAddress,
        latitude = latitude,
        longitude = longitude,
        contactEmail = contactEmail,
        contactPhone = contactPhone
    )?.let { return it }
    validateVerificationStep(
        verificationRequested = verificationRequested,
        acceptedTerms = acceptedTerms,
        selectedMethods = selectedMethods,
        proofSummary = proofSummary
    )?.let { return it }
    return null
}

@Composable
private fun WelcomeBackdrop(
    modifier: Modifier = Modifier
) {
    val welcomePink = Color(0xFFFF2D96)
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val pulseClock = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "welcomeWorldMap")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4_800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "welcomeWorldMapPulse"
        )
        progress
    } else {
        0f
    }
    val density = LocalDensity.current
    // Prototype good-copy-welcome.html `.pink` quilted texture presses a fine dot grid at a
    // 10px tile across the whole pink field (not the old continent-clustered dots).
    val dotSpacingPx = with(density) { 10.dp.toPx() }
    var backdropSize by remember { mutableStateOf(IntSize.Zero) }
    val worldDots = remember(backdropSize, dotSpacingPx) {
        buildWelcomeWorldMapDots(
            width = backdropSize.width.toFloat(),
            height = backdropSize.height.toFloat(),
            spacing = dotSpacingPx
        )
    }

    Box(modifier.background(welcomePink)) {
        // Static all-over quilted pressed-dot texture — matches the prototype's
        // radial-gradient(rgba(214,16,108,0.38) 1.6px, transparent 2.2px) at a 10px tile.
        // Drawn in its own Canvas so it is not re-rasterized every animation frame.
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { backdropSize = it }
        ) {
            val pressedRadius = 1.6.dp.toPx()
            val pressedColor = Color(0xFFD6106C).copy(alpha = 0.38f)
            worldDots.forEach { dot ->
                if (!dot.isBright) {
                    drawCircle(
                        color = pressedColor,
                        radius = pressedRadius,
                        center = Offset(dot.x, dot.y)
                    )
                }
            }
        }

        // Sparse all-over living highlights — subtle, no longer continent-clustered.
        Canvas(Modifier.fillMaxSize()) {
            val brightRadius = 2.3.dp.toPx()
            worldDots.forEach { dot ->
                if (dot.isBright) {
                    val wave = if (animationsEnabled) {
                        val phase = (pulseClock + dot.phase) % 1f
                        ((sin(phase * 2f * PI) + 1.0) * 0.5).toFloat()
                    } else {
                        0.55f
                    }
                    val pulse = wave * wave
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f + pulse * 0.32f),
                        radius = brightRadius * (0.85f + pulse * 0.3f),
                        center = Offset(dot.x, dot.y)
                    )
                }
            }
        }
    }
}

private data class WelcomeWorldDot(
    val x: Float,
    val y: Float,
    val isBright: Boolean,
    val phase: Float
)

private fun buildWelcomeWorldMapDots(
    width: Float,
    height: Float,
    spacing: Float
): List<WelcomeWorldDot> {
    if (width <= 0f || height <= 0f || spacing <= 0f) return emptyList()

    val dots = ArrayList<WelcomeWorldDot>()
    val rowCount = (height / spacing).toInt() + 1
    val columnCount = (width / spacing).toInt() + 1

    for (row in 0 until rowCount) {
        val y = (row + 0.5f) * spacing
        if (y >= height) continue

        for (column in 0 until columnCount) {
            val x = (column + 0.5f) * spacing
            if (x >= width) continue

            // The prototype presses its dot texture across the entire pink field, so the grid
            // is no longer gated to the WELCOME_WORLD_LANDMASSES continents (that produced the
            // corner-clustered look). Every grid point becomes a pressed dot; a sparse, stable,
            // scattered subset is promoted to a living highlight.
            val isBright = ((row * 73 + column * 151 + row * column * 19) % 41) < 3
            val phaseHash = (row * 29 + column * 47 + row * column * 7) % 97
            dots += WelcomeWorldDot(
                x = x,
                y = y,
                isBright = isBright,
                phase = phaseHash / 97f
            )
        }
    }

    return dots
}

private fun polygonContains(x: Float, y: Float, polygon: List<Offset>): Boolean {
    var inside = false
    var previous = polygon.lastIndex

    polygon.indices.forEach { current ->
        val currentPoint = polygon[current]
        val previousPoint = polygon[previous]
        val crossesRow = (currentPoint.y > y) != (previousPoint.y > y)
        if (crossesRow) {
            val crossingX = (previousPoint.x - currentPoint.x) *
                (y - currentPoint.y) / (previousPoint.y - currentPoint.y) + currentPoint.x
            if (x < crossingX) inside = !inside
        }
        previous = current
    }

    return inside
}

// Normalized, intentionally simplified coastlines. Stretching the coordinates to the
// available portrait bounds lets the dotted world span nearly the full phone or tablet
// backdrop while retaining recognizable continental silhouettes.
private val WELCOME_WORLD_LANDMASSES = listOf(
    // North America
    listOf(
        Offset(0.02f, 0.12f), Offset(0.08f, 0.07f), Offset(0.18f, 0.08f),
        Offset(0.25f, 0.13f), Offset(0.31f, 0.20f), Offset(0.28f, 0.28f),
        Offset(0.22f, 0.30f), Offset(0.19f, 0.37f), Offset(0.13f, 0.35f),
        Offset(0.10f, 0.29f), Offset(0.05f, 0.26f)
    ),
    // Greenland
    listOf(
        Offset(0.31f, 0.04f), Offset(0.38f, 0.02f), Offset(0.44f, 0.08f),
        Offset(0.41f, 0.17f), Offset(0.34f, 0.16f), Offset(0.30f, 0.10f)
    ),
    // Central America
    listOf(
        Offset(0.20f, 0.30f), Offset(0.29f, 0.29f), Offset(0.35f, 0.37f),
        Offset(0.32f, 0.42f), Offset(0.27f, 0.37f), Offset(0.22f, 0.36f)
    ),
    // South America
    listOf(
        Offset(0.30f, 0.38f), Offset(0.39f, 0.37f), Offset(0.46f, 0.44f),
        Offset(0.45f, 0.56f), Offset(0.41f, 0.66f), Offset(0.38f, 0.82f),
        Offset(0.33f, 0.72f), Offset(0.31f, 0.58f), Offset(0.27f, 0.48f)
    ),
    // Europe
    listOf(
        Offset(0.46f, 0.17f), Offset(0.52f, 0.12f), Offset(0.60f, 0.15f),
        Offset(0.62f, 0.23f), Offset(0.57f, 0.30f), Offset(0.49f, 0.28f),
        Offset(0.45f, 0.23f)
    ),
    // Africa
    listOf(
        Offset(0.47f, 0.28f), Offset(0.58f, 0.25f), Offset(0.66f, 0.34f),
        Offset(0.64f, 0.49f), Offset(0.59f, 0.67f), Offset(0.53f, 0.72f),
        Offset(0.49f, 0.59f), Offset(0.43f, 0.45f)
    ),
    // Asia
    listOf(
        Offset(0.57f, 0.13f), Offset(0.66f, 0.08f), Offset(0.79f, 0.10f),
        Offset(0.91f, 0.16f), Offset(0.97f, 0.25f), Offset(0.91f, 0.34f),
        Offset(0.83f, 0.36f), Offset(0.78f, 0.44f), Offset(0.69f, 0.40f),
        Offset(0.63f, 0.31f), Offset(0.58f, 0.28f)
    ),
    // India and Southeast Asia
    listOf(
        Offset(0.67f, 0.34f), Offset(0.75f, 0.35f), Offset(0.78f, 0.48f),
        Offset(0.73f, 0.56f), Offset(0.69f, 0.45f), Offset(0.64f, 0.41f)
    ),
    // Japan
    listOf(
        Offset(0.91f, 0.30f), Offset(0.94f, 0.27f), Offset(0.95f, 0.38f),
        Offset(0.92f, 0.42f)
    ),
    // Australia
    listOf(
        Offset(0.75f, 0.64f), Offset(0.84f, 0.60f), Offset(0.94f, 0.66f),
        Offset(0.92f, 0.79f), Offset(0.83f, 0.84f), Offset(0.74f, 0.78f),
        Offset(0.71f, 0.70f)
    ),
    // Madagascar
    listOf(
        Offset(0.66f, 0.57f), Offset(0.69f, 0.59f), Offset(0.68f, 0.72f),
        Offset(0.65f, 0.68f)
    ),
    // Antarctica
    listOf(
        Offset(0.08f, 0.90f), Offset(0.24f, 0.87f), Offset(0.40f, 0.91f),
        Offset(0.57f, 0.88f), Offset(0.76f, 0.90f), Offset(0.93f, 0.87f),
        Offset(0.98f, 0.95f), Offset(0.04f, 0.95f)
    )
)

@Composable
private fun TicketPanel(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val panelColor = Color(0xFFFF2D8E) // Vibrant pink matching screenshot
        val corner = 24.dp.toPx()
        val bottomRadius = 14.dp.toPx()
        val bottomStep = 42.dp.toPx()

        // Background with subtle vertical gradient for "metallic/glass" depth
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFF4B9B), panelColor),
                startY = 0f,
                endY = size.height
            ),
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(corner, corner)
        )

        // Glass highlight at the top
        drawRoundRect(
            color = Color.White.copy(alpha = 0.15f),
            topLeft = Offset(8.dp.toPx(), 8.dp.toPx()),
            size = Size(size.width - 16.dp.toPx(), 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        var scallopX = bottomStep * 0.5f
        while (scallopX < size.width) {
            drawCircle(
                color = Color.White,
                radius = bottomRadius,
                center = Offset(scallopX, size.height + bottomRadius * 0.2f)
            )
            scallopX += bottomStep
        }

        // Inner glass border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
            size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.8f.dp.toPx())
        )
    }
}

@Composable
private fun LoginFields(
    email: String,
    password: String,
    error: String?,
    layout: AuthLayoutMetrics,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LoginLineField(
            email,
            onEmailChange,
            "E-Mail",
            layout,
            { Text("@", color = Color.White, fontSize = layout.fieldTextSize.sp, fontWeight = FontWeight.Black) }
        )
        Spacer(Modifier.height(layout.fieldGap))
        LoginLineField(
            password,
            onPasswordChange,
            "Password",
            layout,
            { Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(layout.fieldIconSize)) },
            { Text("FORGOT", color = Color.White, fontSize = layout.forgotTextSize.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp) },
            PasswordVisualTransformation()
        )
    }
}

@Composable
private fun LoginLineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    layout: AuthLayoutMetrics,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(layout.fieldHeight), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(layout.fieldLeadingWidth), contentAlignment = Alignment.CenterStart) { leading() }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = visualTransformation,
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = layout.fieldTextSize.sp, fontWeight = FontWeight.Black),
                modifier = Modifier.weight(1f),
                decorationBox = { it -> Box(contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Text(placeholder, color = Color.White.copy(alpha = 0.36f), fontSize = layout.fieldTextSize.sp, fontWeight = FontWeight.Black); it() } }
            )
            trailing?.let { Spacer(Modifier.width(12.dp)); Box(modifier = Modifier, contentAlignment = Alignment.CenterEnd) { it() } }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.86f), thickness = 2.dp)
    }
}

private fun Dp.safeCoerceIn(minV: Dp, maxV: Dp): Dp = coerceIn(minOf(minV, maxV), maxOf(minV, maxV))

private class AuthLayoutMetrics(screenWidth: Dp, screenHeight: Dp, mode: AuthMode) {
    val scale = (minOf(screenWidth.coerceAtMost(430.dp)/430.dp, screenHeight/932.dp)).coerceIn(0.86f, 1.18f)
    private val welcomeScale = minOf(screenWidth / 430.dp, screenHeight / 932.dp).coerceIn(0.72f, 1.18f)
    val ticketHorizontalInset = (screenWidth * 0.05f).coerceIn(16.dp, 32.dp)
    val ticketTopInset = if (mode == AuthMode.Welcome) 48.dp else 100.dp
    // Ticket should fill more vertical space. Decrease bottom inset.
    val ticketBottomInset = (if (mode == AuthMode.Welcome) 80.dp else 120.dp)
    val logoSize = (screenWidth * 0.48f).coerceIn(200.dp, 240.dp)
    val ticketHeight = (screenHeight - ticketTopInset - ticketBottomInset).coerceAtLeast(1.dp)
    // Canonical Good Copy uses a 430 × 932 layout. Scaling those exact coordinates keeps the
    // visual centers at roughly y=155 (logo), y=350 (cube), y=566/635 (buttons) on 390 × 845.
    val welcomeLogoTop = 96.dp * welcomeScale
    val welcomeLogoSize = 148.dp * welcomeScale
    val welcomeButtonWidth = 250.dp * welcomeScale
    val welcomeButtonHeight = 58.dp * welcomeScale
    val welcomeButtonGap = 28.dp * welcomeScale
    val welcomeButtonShadowOffset = 6.dp * welcomeScale
    val welcomeButtonTextSize = 22f * welcomeScale
    // Buttons anchor to the BOTTOM like the prototype `.btns { bottom: clamp(88px, 12vh, 132px) }`
    // (good-copy-welcome.html) and iOS's Sign Up center at 635/845. The previous 200dp bottom
    // margin pushed the pills up into the tagline/status copy; a 12vh clamp keeps them low with
    // clear space above the copy on every height.
    private val welcomeButtonBottomMargin = (screenHeight * 0.12f).coerceIn(88.dp, 132.dp)
    val welcomeButtonTop = screenHeight - welcomeButtonBottomMargin -
        (welcomeButtonHeight * 2f + welcomeButtonGap)
    // Bottom edge of the native cube overlay, measured from the top of the screen. The cube is
    // not a Compose child, so this mirrors the geometry MainActivity applies: a fixed
    // CUBE_OVERLAY_SIZE_DP (200dp) square whose center sits at PoplurRoot's Welcome
    // centerFromTopFraction (0.4056). Keep these three values in sync.
    private val welcomeCubeSize = 200.dp
    val welcomeCubeBottom = (screenHeight * WELCOME_CUBE_CENTER_FROM_TOP_FRACTION) + (welcomeCubeSize / 2f)
    val backButtonStart = (screenWidth * 0.068f).coerceIn(24.dp, 34.dp); val backButtonTop = (screenHeight * 0.073f).coerceIn(66.dp, 84.dp); val backButtonSize = (68.dp * scale).coerceIn(60.dp, 74.dp); val backIconSize = (38.dp * scale).coerceIn(34.dp, 42.dp)
    val formHorizontalInset = (screenWidth * 0.122f).coerceIn(44.dp, 58.dp); val loginLogoTop = (screenHeight * 0.202f).coerceIn(184.dp, 218.dp); val submitButtonHeight = (screenHeight * 0.102f).coerceIn(88.dp, 106.dp); val fieldGap = (screenHeight * 0.028f).coerceIn(22.dp, 30.dp); val fieldHeight = (58.dp * scale).coerceIn(52.dp, 64.dp)
    val submitButtonTop = (ticketTopInset + ticketHeight - submitButtonHeight - (ticketHeight * 0.075f)).safeCoerceIn(ticketTopInset + (ticketHeight * 0.68f), ticketTopInset + ticketHeight - submitButtonHeight - 34.dp)
    val loginFieldsTop = (submitButtonTop - (fieldHeight * 2f) - fieldGap - (ticketHeight * 0.06f)).safeCoerceIn(ticketTopInset + (ticketHeight * 0.52f), submitButtonTop - (fieldHeight * 2f) - fieldGap - 22.dp)
    val submitButtonTextSize = (30f * scale).coerceIn(28f, 34f)
    val fieldLeadingWidth = (56.dp * scale).coerceIn(50.dp, 62.dp); val fieldIconSize = (34.dp * scale).coerceIn(30.dp, 38.dp); val fieldTextSize = (34f * scale).coerceIn(30f, 38f); val forgotTextSize = (17f * scale).coerceIn(15f, 19f)
}

@Composable
private fun WelcomeLogo(
    logoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier,
    floats: Boolean = true
) {
    // Matches iOS's SKAction float loop: amplitude 10pt, 2.6s period, ease in/out, forever.
    val infiniteTransition = rememberInfiniteTransition(label = "welcomeLogoAnim")
    val floatProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(modifier = modifier.graphicsLayer {
        translationY = if (floats) -10f * floatProgress else 0f
    }, contentAlignment = Alignment.Center) {
        if (logoBitmap != null) {
            Image(bitmap = logoBitmap, contentDescription = "Poplur", modifier = Modifier.fillMaxSize())
        } else {
            LogoFallback(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LogoFallback(modifier: Modifier) { Surface(modifier = modifier.clip(RoundedCornerShape(42.dp)), color = Color(0xFF2A1238)) { Box(contentAlignment = Alignment.Center) { Text(text = "P", color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Bold) } } }

internal enum class AuthMode { Welcome, Login, SignUp }

// MARK: - Auth AI guide

private fun authGuideGreeting(mode: AuthMode) = when (mode) {
    AuthMode.Welcome -> "Welcome to Poplur — every post has a reach radius you control, from your block to the whole planet. Tap Sign Up to join, or Sign In to come back."
    AuthMode.Login -> "Good to have you back. Enter your email and password. Tap FORGOT if you need a reset."
    AuthMode.SignUp -> "Let's get you set up. Choose personal for your own posts, or business if you represent a brand."
}

private fun authGuideSystemPrompt(mode: AuthMode) = when (mode) {
    AuthMode.Welcome -> "You are Poplur's onboarding guide. Poplur is a location-based social app — every post has a radius that controls how far it reaches. Answer in 1-2 warm, friendly sentences."
    AuthMode.Login -> "You are Poplur's sign-in helper. The user is logging in with email and password. If stuck, they can tap FORGOT to reset. Answer in 1-2 sentences."
    AuthMode.SignUp -> "You are Poplur's sign-up guide. Personal accounts are for individual creators; business accounts unlock brand features. Answer in 1-2 friendly sentences."
}

private suspend fun callClaudeForAuth(mode: AuthMode, question: String): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val key = Secrets.claudeApiKey
            val url = URL("https://api.anthropic.com/v1/messages")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("x-api-key", key)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.setRequestProperty("content-type", "application/json")
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.doOutput = true
            val body = JSONObject().apply {
                put("model", "claude-haiku-4-5-20251001")
                put("max_tokens", 80)
                put("system", authGuideSystemPrompt(mode))
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", question)
                }))
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray()) }
            val response = conn.inputStream.bufferedReader().readText()
            JSONObject(response)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
        }.getOrDefault(authGuideGreeting(mode))
    }
}

@Composable
private fun TypingDotsIndicator() {
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0..2) {
            val alpha = if (phase.toInt() == i) 1f else 0.28f
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun AuthAIGuide(mode: AuthMode, modifier: Modifier = Modifier) {
    val brand = Color(0xFFFF2D96)
    val coroutineScope = rememberCoroutineScope()

    var displayedText by remember(mode) { mutableStateOf("") }
    var isTyping by remember(mode) { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    var isAwaiting by remember { mutableStateOf(false) }
    var typingJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun typeText(text: String, charDelayMs: Long = 22L) {
        typingJob?.cancel()
        typingJob = coroutineScope.launch {
            try {
                isTyping = true
                for (char in text) {
                    displayedText += char
                    delay(charDelayMs)
                }
            } finally {
                isTyping = false
            }
        }
    }

    LaunchedEffect(mode) {
        displayedText = ""
        isTyping = false
        typeText(authGuideGreeting(mode))
    }

    fun sendQuestion() {
        val q = userInput.trim()
        if (q.isEmpty() || isAwaiting) return
        userInput = ""
        isExpanded = false
        isAwaiting = true
        typingJob?.cancel()
        displayedText = ""
        coroutineScope.launch {
            val answer = callClaudeForAuth(mode, q)
            isAwaiting = false
            typeText(answer, 18L)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(brand)
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // P avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
            ) {
                Text("P", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAwaiting) {
                    TypingDotsIndicator()
                } else {
                    val cursor = if (isTyping) "▎" else ""
                    Text(
                        text = displayedText + cursor,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 5
                    )
                }

                if (isExpanded) {
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.25f)))
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
                            decorationBox = { inner ->
                                if (userInput.isEmpty()) {
                                    Text("Ask a question…", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(
                            onClick = { sendQuestion() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            if (!isExpanded) {
                Text("∨", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// MARK: - Stripe page-transition overlay

@Composable
internal fun GoldConfettiEffect() {
    val particleCount = 55
    val angles  = remember { FloatArray(particleCount) { Random.nextFloat() * 360f } }
    val speeds  = remember { FloatArray(particleCount) { Random.nextFloat() * 2.5f + 1.2f } }
    val sizes   = remember { FloatArray(particleCount) { Random.nextFloat() * 12f + 6f } }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1600, easing = LinearEasing))
    }

    val t = progress.value
    Canvas(Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val gold = Color(1f, 0.82f, 0.18f)

        for (i in 0 until particleCount) {
            val rad = angles[i] * PI.toFloat() / 180f
            val dist = speeds[i] * t * size.width * 0.7f
            val x = cx + cos(rad) * dist
            val y = cy + sin(rad) * dist
            val alpha = (1f - t * 0.8f).coerceAtLeast(0f)
            val sz = sizes[i] * (1f - t * 0.4f)
            drawRect(
                color = gold.copy(alpha = alpha),
                topLeft = Offset(x - sz / 2, y - sz / 2),
                size = Size(sz, sz)
            )
        }
    }
}

@Composable
internal fun StripeTransitionOverlay(
    modifier: Modifier = Modifier,
    onMidpoint: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val onMidpointState = rememberUpdatedState(onMidpoint)
    val onCompleteState = rememberUpdatedState(onComplete)

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = android.net.Uri.parse(
                "android.resource://${context.packageName}/raw/auth_transition"
            )
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    // Fire midpoint at 3 s — screen visually covered by the opening of the effect
    LaunchedEffect(Unit) {
        delay(3_000)
        onMidpointState.value()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onCompleteState.value()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun WelcomeAuthScreenPreview() {
    MaterialTheme {
        WelcomeAuthScreenContent(
            initialMode = AuthMode.Welcome,
            onSignIn = { _, _, _ -> },
            onSignUp = { _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeAuthScreenLoginPreview() {
    MaterialTheme {
        WelcomeAuthScreenContent(
            initialMode = AuthMode.Login,
            onSignIn = { _, _, _ -> },
            onSignUp = { _, _, _, _, _ -> }
        )
    }
}

package com.apptists.poplur2025.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.ui.ConfettiAnimationFragment
import kotlinx.coroutines.delay

@Composable
fun WelcomeAuthScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(AuthMode.Welcome) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val logoBitmap = remember {
        runCatching {
            context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
        }.getOrNull()
    }
    val confettiContainerId = remember { android.view.View.generateViewId() }
    var showConfetti by remember { mutableStateOf(false) }
    var confettiRunId by remember { mutableIntStateOf(0) }
    var welcomeAnimationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        welcomeAnimationStarted = false
        showConfetti = false
        if (mode == AuthMode.Welcome) {
            confettiRunId += 1
            delay(80)
            welcomeAnimationStarted = true
            delay(820)
            showConfetti = true
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val authLayout = remember(maxWidth, maxHeight, mode) {
            AuthLayoutMetrics(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                mode = mode
            )
        }

        if (showConfetti && mode == AuthMode.Welcome) {
            key(confettiRunId) {
                val activity = context as? FragmentActivity
                DisposableEffect(activity, confettiContainerId, confettiRunId) {
                    onDispose {
                        val fragmentManager = activity?.supportFragmentManager ?: return@onDispose
                        fragmentManager.findFragmentById(confettiContainerId)?.let { fragment ->
                            fragmentManager.beginTransaction()
                                .remove(fragment)
                                .commitAllowingStateLoss()
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        FragmentContainerView(ctx).apply {
                            id = confettiContainerId
                            setBackgroundColor(0)
                            background = null
                            isClickable = false
                            isFocusable = false
                            isEnabled = false

                            val fragmentActivity = ctx as? FragmentActivity
                            fragmentActivity?.supportFragmentManager?.beginTransaction()
                                ?.replace(id, ConfettiAnimationFragment())
                                ?.commitAllowingStateLoss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f),
                    update = { view ->
                        view.setBackgroundColor(0)
                        view.background = null
                    }
                )
            }
        }

        TicketPanel(
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(1f)
                .then(
                    if (mode == AuthMode.Welcome) {
                        val ticketProgress by animateFloatAsState(
                            targetValue = if (welcomeAnimationStarted) 1f else 0f,
                            animationSpec = tween(durationMillis = 520),
                            label = "welcomeTicket"
                        )
                        Modifier.graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            scaleY = 0.46f + (0.54f * ticketProgress)
                            alpha = 0.68f + (0.32f * ticketProgress)
                        }
                    } else {
                        Modifier
                    }
                )
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = authLayout.ticketHorizontalInset,
                    end = authLayout.ticketHorizontalInset,
                    top = authLayout.ticketTopInset,
                    bottom = authLayout.ticketBottomInset
                )
        )

        if (mode == AuthMode.Welcome) {
            Box(Modifier.fillMaxSize().zIndex(2f)) {
                WelcomeTicketContent(
                    logoBitmap = logoBitmap,
                    layout = authLayout,
                    animationStarted = welcomeAnimationStarted,
                    onSignIn = { mode = AuthMode.Login }
                )
            }
        } else {
            Box(Modifier.fillMaxSize().zIndex(2f)) {
                LoginTicketContent(
                    logoBitmap = logoBitmap,
                    email = email,
                    password = password,
                    error = error,
                    loading = loading,
                    mode = mode,
                    layout = authLayout,
                    onEmailChange = { email = it },
                    onPasswordChange = { password = it },
                    onBack = {
                        error = null
                        mode = AuthMode.Welcome
                    },
                    onSubmit = {
                        loading = true
                        error = null
                        if (mode == AuthMode.Login) {
                            authViewModel.signIn(email, password) { ok, msg ->
                                loading = false
                                if (!ok) error = msg
                            }
                        } else {
                            authViewModel.signUp(email, password) { ok, msg ->
                                loading = false
                                if (!ok) error = msg
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeTicketContent(
    logoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    layout: AuthLayoutMetrics,
    animationStarted: Boolean,
    onSignIn: () -> Unit
) {
    val logoProgress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 135f),
        label = "welcomeLogo"
    )
    val buttonProgress by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 420, delayMillis = 260),
        label = "welcomeButton"
    )

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
                .padding(top = layout.welcomeLogoTop)
                .size(layout.logoSize)
                .graphicsLayer {
                    alpha = logoProgress
                    scaleX = 0.82f + (0.18f * logoProgress)
                    scaleY = 0.82f + (0.18f * logoProgress)
                    translationY = (1f - logoProgress) * 36f
                }
        )

        Button(
            onClick = onSignIn,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.welcomeButtonTop)
                .width(layout.welcomeButtonWidth)
                .height(layout.welcomeButtonHeight)
                .graphicsLayer {
                    alpha = buttonProgress
                    scaleX = 0.88f + (0.12f * buttonProgress)
                    scaleY = 0.88f + (0.12f * buttonProgress)
                    translationY = (1f - buttonProgress) * 28f
                },
            shape = RoundedCornerShape(layout.welcomeButtonHeight / 2f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp)
        ) {
            Text("Sign In", fontWeight = FontWeight.Black, fontSize = layout.welcomeButtonTextSize.sp)
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
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier
                .padding(start = layout.backButtonStart, top = layout.backButtonTop)
                .size(layout.backButtonSize)
                .shadow(24.dp, RoundedCornerShape(layout.backButtonSize / 2f)),
            shape = RoundedCornerShape(layout.backButtonSize / 2f),
            color = Color.White.copy(alpha = 0.96f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(layout.backIconSize)
                )
            }
        }

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
private fun TicketPanel(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val panelColor = Color(0xFFF84C86)
        val dotColor = Color.White.copy(alpha = 0.18f)
        val corner = 22.dp.toPx()
        val bottomRadius = 14.dp.toPx()
        val bottomStep = 42.dp.toPx()

        drawRoundRect(
            color = panelColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(corner, corner)
        )

        val dotRadius = 5.5.dp.toPx()
        val dotGap = 21.dp.toPx()
        var y = 38.dp.toPx()
        while (y < size.height - 80.dp.toPx()) {
            var x = 38.dp.toPx()
            while (x < size.width - 38.dp.toPx()) {
                drawCircle(dotColor, dotRadius, Offset(x, y))
                x += dotGap
            }
            y += dotGap
        }

        var scallopX = bottomStep * 0.5f
        while (scallopX < size.width) {
            drawCircle(
                color = Color.White,
                radius = bottomRadius,
                center = Offset(scallopX, size.height + bottomRadius * 0.15f)
            )
            scallopX += bottomStep
        }

        drawRoundRect(
            color = Color.White.copy(alpha = 0.16f),
            topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
            size = Size(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1.5.dp.toPx())
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
        error?.let { Text(it, color = Color.White, modifier = Modifier.padding(top = 10.dp), fontWeight = FontWeight.Bold) }
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
    val ticketHorizontalInset = (screenWidth * 0.074f).coerceIn(24.dp, 44.dp)
    val ticketTopInset = if (mode == AuthMode.Welcome) (screenHeight * 0.123f).coerceIn(88.dp, 132.dp) else (screenHeight * 0.171f).coerceIn(150.dp, 188.dp)
    val ticketBottomInset = if (mode == AuthMode.Welcome) (screenHeight * 0.123f).coerceIn(88.dp, 132.dp) else (screenHeight * 0.096f).coerceIn(72.dp, 108.dp)
    val logoSize = (screenWidth * 0.46f).coerceIn(186.dp, 232.dp)
    val ticketHeight = (screenHeight - ticketTopInset - ticketBottomInset).coerceAtLeast(1.dp)
    val welcomeLogoTop = (ticketTopInset + (ticketHeight * 0.145f)).safeCoerceIn(ticketTopInset + 38.dp, ticketTopInset + 82.dp)
    val welcomeButtonTop = (ticketTopInset + (ticketHeight * 0.655f)).safeCoerceIn(ticketTopInset + (ticketHeight * 0.54f), ticketTopInset + ticketHeight - 146.dp)
    val welcomeButtonWidth = (screenWidth * 0.34f).coerceIn(172.dp, 206.dp)
    val welcomeButtonHeight = (64.dp * scale).coerceIn(58.dp, 70.dp)
    val welcomeButtonTextSize = (22f * scale).coerceIn(20f, 24f)
    val backButtonStart = (screenWidth * 0.068f).coerceIn(24.dp, 34.dp); val backButtonTop = (screenHeight * 0.073f).coerceIn(66.dp, 84.dp); val backButtonSize = (68.dp * scale).coerceIn(60.dp, 74.dp); val backIconSize = (38.dp * scale).coerceIn(34.dp, 42.dp)
    val formHorizontalInset = (screenWidth * 0.122f).coerceIn(44.dp, 58.dp); val loginLogoTop = (screenHeight * 0.202f).coerceIn(184.dp, 218.dp); val submitButtonHeight = (screenHeight * 0.102f).coerceIn(88.dp, 106.dp); val fieldGap = (screenHeight * 0.028f).coerceIn(22.dp, 30.dp); val fieldHeight = (58.dp * scale).coerceIn(52.dp, 64.dp)
    val submitButtonTop = (ticketTopInset + ticketHeight - submitButtonHeight - (ticketHeight * 0.075f)).safeCoerceIn(ticketTopInset + (ticketHeight * 0.68f), ticketTopInset + ticketHeight - submitButtonHeight - 34.dp)
    val loginFieldsTop = (submitButtonTop - (fieldHeight * 2f) - fieldGap - (ticketHeight * 0.06f)).safeCoerceIn(ticketTopInset + (ticketHeight * 0.52f), submitButtonTop - (fieldHeight * 2f) - fieldGap - 22.dp)
    val submitButtonTextSize = (30f * scale).coerceIn(28f, 34f)
    val fieldLeadingWidth = (56.dp * scale).coerceIn(50.dp, 62.dp); val fieldIconSize = (34.dp * scale).coerceIn(30.dp, 38.dp); val fieldTextSize = (34f * scale).coerceIn(30f, 38f); val forgotTextSize = (17f * scale).coerceIn(15f, 19f)
}

@Composable
private fun WelcomeLogo(logoBitmap: androidx.compose.ui.graphics.ImageBitmap?, modifier: Modifier) { if (logoBitmap != null) Image(bitmap = logoBitmap, contentDescription = "Poplur", modifier = modifier) else LogoFallback(modifier) }

@Composable
private fun LogoFallback(modifier: Modifier) { Surface(modifier = modifier.clip(RoundedCornerShape(42.dp)), color = Color(0xFF2A1238)) { Box(contentAlignment = Alignment.Center) { Text(text = "P", color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Bold) } } }

private enum class AuthMode { Welcome, Login, SignUp }

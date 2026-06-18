package com.apptists.poplur2025.ui.screens

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.R
import com.apptists.poplur2025.call.CallSessionManager
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.components.CubeMenuWheelController
import com.apptists.poplur2025.ui.components.CubeMenuWheelItem
import com.apptists.poplur2025.ui.components.CubeMenuWheelOverlay
import com.apptists.poplur2025.ui.components.ShaderNumberText
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PopularPhoneCallScreen(
    userName: String,
    onHangUp: () -> Unit,
    userProfile: UserProfile? = null,
    onVisibilityChanged: (Boolean) -> Unit = {},
    onMiddleButtonClick: () -> Unit = {},
    onColorwaySelected: (Color) -> Unit = {},
    forcedColorway: Color? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val colorWheelScope = rememberCoroutineScope()
    var isBooting by remember { mutableStateOf(true) }
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            null
        }
    }
    val colorWheelController = remember(colorWheelScope) {
        CubeMenuWheelController(scope = colorWheelScope) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 65)
            } catch (e: Exception) {
                // Ignore if the generator is unavailable.
            }
        }
    }
    var selectedColorway by remember { mutableStateOf(PHONE_COLORWAYS.first()) }

    LaunchedEffect(forcedColorway) {
        forcedColorway?.let { color ->
            val match = PHONE_COLORWAYS.firstOrNull { it.swatch == color }
            if (match != null) selectedColorway = match
        }
    }
    var isPhoneButtonActive by remember { mutableStateOf(true) }
    var dialedNumber by remember { mutableStateOf("") }
    var snakeGame by remember { mutableStateOf(SnakeGameState.initial()) }
    var isSnakeActive by remember { mutableStateOf(false) }
    var isDialerActive by remember { mutableStateOf(false) }
    var isScreenLit by remember { mutableStateOf(false) }
    var menuScreen by remember { mutableStateOf(PhoneMenuScreen.Main) }
    var selectedMenuIndex by remember { mutableStateOf(0) }
    var menuMessage by remember { mutableStateOf<String?>(null) }
    var idleSignalFrameIndex by remember(selectedColorway) { mutableStateOf(PHONE_SIGNAL_FRAME_COUNT - 1) }
    var pressNonce by remember { mutableStateOf(0) }
    var pressedButtonFraction by remember { mutableStateOf<Offset?>(null) }
    val pressGlowAlpha = remember { Animatable(0f) }
    val glistenPhase = remember { Animatable(0f) }
    val signalSheetBitmap = remember(selectedColorway.signalSheetRes) {
        BitmapFactory.decodeResource(context.resources, selectedColorway.signalSheetRes).asImageBitmap()
    }
    val batteryLevel = remember(context) {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100) / 100f
    }
    val callState by CallSessionManager.callState.collectAsState()
    val elapsedSeconds by CallSessionManager.elapsedSeconds.collectAsState()
    val visibleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 180),
        label = "phone-call-alpha"
    )
    val screenAlpha by animateFloatAsState(
        targetValue = if (isScreenLit && !isBooting) 1f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "phone-screen-alpha"
    )

    DisposableEffect(toneGenerator) {
        onVisibilityChanged(true)
        onDispose {
            onVisibilityChanged(false)
            toneGenerator?.release()
        }
    }

    LaunchedEffect(Unit) {
        playPhoneBootSound(context)
        repeat(12) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(80L)
        }
        delay(1200)
        isBooting = false
    }

    val ringtone = remember(context) {
        runCatching {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
        }.getOrNull()
    }

    LaunchedEffect(callState) {
        when (callState) {
            is CallSessionManager.CallState.Outgoing,
            is CallSessionManager.CallState.Incoming -> ringtone?.play()
            else -> ringtone?.stop()
        }
    }

    DisposableEffect(ringtone) {
        onDispose { ringtone?.stop() }
    }

    LaunchedEffect(isSnakeActive, snakeGame.direction) {
        while (isSnakeActive) {
            delay(180)
            snakeGame = snakeGame.advance()
        }
    }

    LaunchedEffect(menuMessage) {
        if (menuMessage != null) {
            delay(950)
            menuMessage = null
        }
    }

    LaunchedEffect(pressNonce) {
        if (pressNonce == 0) return@LaunchedEffect
        pressGlowAlpha.snapTo(1f)
        pressGlowAlpha.animateTo(0f, animationSpec = tween(durationMillis = 180))
    }

    LaunchedEffect(Unit) {
        while (true) {
            glistenPhase.snapTo(0f)
            glistenPhase.animateTo(1f, animationSpec = tween(durationMillis = 4800, easing = LinearEasing))
        }
    }

    fun currentMenuItems(): List<PhoneMenuItem> = phoneMenuItems(menuScreen, userName)

    fun returnToMenu(screen: PhoneMenuScreen = PhoneMenuScreen.Main) {
        isDialerActive = false
        isSnakeActive = false
        menuScreen = screen
        selectedMenuIndex = 0
    }

    fun flashMenuDetail(message: String) {
        menuMessage = message
    }

    fun colorWheelItems(): List<CubeMenuWheelItem> {
        return PHONE_COLORWAYS.map { colorway ->
            CubeMenuWheelItem(
                label = colorway.label,
                icon = Icons.Filled.Circle,
                accent = colorway.swatch
            ) {
                selectedColorway = colorway
                onColorwaySelected(colorway.swatch)
                flashMenuDetail(colorway.label.uppercase())
            }
        }
    }

    fun togglePhoneButtonActive() {
        isPhoneButtonActive = !isPhoneButtonActive
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        flashMenuDetail(if (isPhoneButtonActive) "BUTTON ACTIVE" else "BUTTON LOCKED")
    }

    fun triggerButtonGlow(control: String) {
        pressedButtonFraction = buttonPressFraction(control) ?: return
        pressNonce += 1
    }

    fun placeCurrentProfileCall() {
        val profile = userProfile
        if (profile != null && callState is CallSessionManager.CallState.Idle) {
            isDialerActive = false
            CallSessionManager.placeCall(profile)
        } else {
            flashMenuDetail("CONTACT UNAVAILABLE")
        }
    }

    fun activateMenuItem(index: Int = selectedMenuIndex) {
        val item = currentMenuItems().getOrNull(index) ?: return
        when (menuScreen) {
            PhoneMenuScreen.Main -> {
                menuScreen = when (item.label) {
                    "Calls" -> PhoneMenuScreen.Calls
                    "Contacts" -> PhoneMenuScreen.Contacts
                    "Texts" -> PhoneMenuScreen.Texts
                    "Games" -> PhoneMenuScreen.Games
                    else -> PhoneMenuScreen.Settings
                }
                selectedMenuIndex = 0
            }
            PhoneMenuScreen.Calls -> when (item.label) {
                "Call user" -> placeCurrentProfileCall()
                "Dial number" -> {
                    dialedNumber = ""
                    isDialerActive = true
                    menuMessage = null
                }
                else -> flashMenuDetail("NO RECENTS YET")
            }
            PhoneMenuScreen.Contacts -> when (item.label) {
                userName -> flashMenuDetail(userName.uppercase())
                else -> flashMenuDetail(item.detail.uppercase())
            }
            PhoneMenuScreen.Texts -> flashMenuDetail("${item.label.uppercase()} EMPTY")
            PhoneMenuScreen.Games -> when (item.label) {
                "Snake" -> {
                    snakeGame = SnakeGameState.initial()
                    isSnakeActive = true
                    menuMessage = null
                }
                else -> flashMenuDetail("${item.label.uppercase()} SOON")
            }
            PhoneMenuScreen.Settings -> flashMenuDetail(item.detail.uppercase())
        }
    }

    fun handleMenuDigit(digit: String) {
        val items = currentMenuItems()
        when (digit) {
            "2" -> selectedMenuIndex = (selectedMenuIndex - 1 + items.size) % items.size
            "8" -> selectedMenuIndex = (selectedMenuIndex + 1) % items.size
            "5", "#" -> activateMenuItem()
            "*" -> {
                if (menuScreen == PhoneMenuScreen.Main) {
                    selectedMenuIndex = 0
                } else {
                    menuScreen = PhoneMenuScreen.Main
                    selectedMenuIndex = 0
                    menuMessage = null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .alpha(visibleAlpha)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val phoneAspect = PHONE_IMAGE_WIDTH / PHONE_IMAGE_HEIGHT
            val widthFromHeight = maxHeight * phoneAspect
            val phoneWidth = maxOf(maxWidth, widthFromHeight)
            val phoneHeight = phoneWidth / phoneAspect
            val callStatusText = when (val s = callState) {
                is CallSessionManager.CallState.Outgoing ->
                    "CALLING\n${s.calleeName.uppercase()}"
                is CallSessionManager.CallState.Incoming ->
                    "INCOMING\n${s.callerName.uppercase()}"
                is CallSessionManager.CallState.Connected ->
                    "${s.calleeName.uppercase()}\n${CallSessionManager.formattedDuration(elapsedSeconds)}"
                is CallSessionManager.CallState.Ended ->
                    when (s.reason) {
                        CallSessionManager.EndReason.Declined -> "DECLINED"
                        CallSessionManager.EndReason.NoAnswer -> "NO ANSWER"
                        else -> "CALL ENDED"
                    }
                else -> null
            }
            val showPhoneMenu = !isSnakeActive && !isDialerActive && callStatusText == null

            LaunchedEffect(selectedColorway, showPhoneMenu, isScreenLit) {
                if (!showPhoneMenu || !isScreenLit || isBooting) {
                    idleSignalFrameIndex = PHONE_SIGNAL_FRAME_COUNT - 1
                    return@LaunchedEffect
                }

                while (true) {
                    delay(Random.nextLong(1_200L, 4_200L))
                    val frameOrder = if (Random.nextBoolean()) {
                        0 until PHONE_SIGNAL_FRAME_COUNT
                    } else {
                        (PHONE_SIGNAL_FRAME_COUNT - 1 downTo 0)
                    }
                    for (frame in frameOrder) {
                        idleSignalFrameIndex = frame
                        delay(Random.nextLong(55L, 95L))
                    }
                    idleSignalFrameIndex = PHONE_SIGNAL_FRAME_COUNT - 1
                }
            }

            Box(
                modifier = Modifier.size(phoneWidth, phoneHeight),
                contentAlignment = Alignment.TopStart
            ) {
                Image(
                    painter = painterResource(selectedColorway.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Glistening diagonal specular sweep
                    val phase = glistenPhase.value
                    val sweep = phase * 2.4f - 0.1f
                    val cx = size.width * sweep * 0.55f
                    val cy = size.height * (1f - sweep * 0.55f)
                    val spanX = size.width * 0.28f
                    val spanY = size.height * 0.40f
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.45f to Color.White.copy(alpha = 0.14f),
                                0.55f to Color.White.copy(alpha = 0.14f),
                                1f to Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(cx - spanX, cy + spanY),
                            end = androidx.compose.ui.geometry.Offset(cx + spanX, cy - spanY)
                        ),
                        blendMode = BlendMode.Screen
                    )
                    // Button press glow
                    pressedButtonFraction?.let { frac ->
                        val alpha = pressGlowAlpha.value
                        if (alpha > 0f) {
                            val bx = size.width * frac.x
                            val by = size.height * frac.y
                            val radius = size.width * 0.14f
                            drawCircle(
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = alpha * 0.60f),
                                        Color.Transparent
                                    ),
                                    center = androidx.compose.ui.geometry.Offset(bx, by),
                                    radius = radius
                                ),
                                radius = radius,
                                center = androidx.compose.ui.geometry.Offset(bx, by),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isBooting) {
                        drawPhoneScreenFill(alpha = screenAlpha)
                    } else if (isSnakeActive) {
                        drawPhoneScreenFill(alpha = screenAlpha)
                        drawSnakeGame(snakeGame)
                    } else {
                        drawPhoneScreenFill(alpha = screenAlpha)
                    }
                }
                if (isBooting) {
                    val screenModifier = Modifier
                        .alpha(screenAlpha)
                        .offset(
                            x = phoneWidth * PHONE_SCREEN_LEFT_FRACTION,
                            y = phoneHeight * PHONE_SCREEN_TOP_FRACTION
                        )
                        .size(
                            width = phoneWidth * PHONE_SCREEN_WIDTH_FRACTION,
                            height = phoneHeight * PHONE_SCREEN_HEIGHT_FRACTION
                        )
                        .clipToBounds()
                    PhoneBootupOverlay(modifier = screenModifier)
                } else if (!isSnakeActive) {
                    val screenModifier = Modifier
                        .alpha(screenAlpha)
                        .offset(
                            x = phoneWidth * PHONE_SCREEN_LEFT_FRACTION,
                            y = phoneHeight * PHONE_SCREEN_TOP_FRACTION
                        )
                        .size(
                            width = phoneWidth * PHONE_SCREEN_WIDTH_FRACTION,
                            height = phoneHeight * PHONE_SCREEN_HEIGHT_FRACTION
                        )
                        .clipToBounds()
                    if (showPhoneMenu) {
                        PhoneMenuDisplay(
                            title = phoneMenuTitle(menuScreen),
                            items = currentMenuItems(),
                            selectedIndex = selectedMenuIndex,
                            message = menuMessage,
                            modifier = screenModifier
                        )
                    } else {
                        PhoneScreenText(
                            dialedNumber = callStatusText ?: dialedNumber.ifBlank { "DIAL NUMBER" },
                            modifier = screenModifier
                        )
                    }
                }
                if (showPhoneMenu && !isBooting && isScreenLit) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPhoneSignalFrame(signalSheetBitmap, idleSignalFrameIndex)
                        drawPhoneBatteryIndicator(batteryLevel)
                    }
                }
                PhoneKeypadTouchLayer(
                    modifier = Modifier.fillMaxSize(),
                    onTopButtonTap = {
                        triggerButtonGlow("call")
                        if (!isBooting) togglePhoneButtonActive()
                    },
                    onTopButtonHoldStart = {
                        triggerButtonGlow("call")
                        if (!isBooting) colorWheelController.beginHold(colorWheelItems())
                    },
                    onTopButtonHoldEnd = { activateSelected ->
                        if (!isBooting) colorWheelController.endHold(activateSelected)
                    },
                    onMiddleButtonClick = {
                        triggerButtonGlow("navUp")
                        if (!isBooting) {
                            if (!isScreenLit) isScreenLit = true else onMiddleButtonClick()
                        }
                    },
                    onDigitPressed = { digit ->
                        triggerButtonGlow(digit)
                        if (isBooting || !isScreenLit) return@PhoneKeypadTouchLayer
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            toneGenerator?.startTone(toneForDigit(digit), 140)
                        } catch (e: Exception) {
                            // Ignore if released
                        }
                        if (!isPhoneButtonActive) {
                            flashMenuDetail("BUTTON LOCKED")
                        } else if (isSnakeActive) {
                            when (digit) {
                                "2" -> snakeGame = snakeGame.turn(GridPoint(0, 1))
                                "8" -> snakeGame = snakeGame.turn(GridPoint(0, -1))
                                "4" -> snakeGame = snakeGame.turn(GridPoint(-1, 0))
                                "6" -> snakeGame = snakeGame.turn(GridPoint(1, 0))
                                "5" -> snakeGame = SnakeGameState.initial()
                                "*" -> returnToMenu(PhoneMenuScreen.Games)
                            }
                        } else if (showPhoneMenu) {
                            handleMenuDigit(digit)
                        } else if (isDialerActive && digit == "*") {
                            returnToMenu()
                        } else if (isDialerActive && digit == "#") {
                            launchDialer(context, dialedNumber)
                        } else if (dialedNumber.length < 24) {
                            dialedNumber += digit
                        }
                    }
                )
            }
        }

        CubeMenuWheelOverlay(
            controller = colorWheelController,
            modifier = Modifier.zIndex(40f)
        )
    }
}

@Composable
private fun PhoneBootupOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "POPLUR",
                color = Color(0xFF0F1800),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "SYSTEMS",
                color = Color(0xFF0F1800),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PhoneMenuDisplay(
    title: String,
    items: List<PhoneMenuItem>,
    selectedIndex: Int,
    message: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1C350D),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(
                            color = if (selected) Color(0xFF243D13) else Color.Transparent,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "${index + 1}. ${item.label}",
                        color = if (selected) Color(0xFFEFF100) else Color(0xFF20330F),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFF36FF2F).copy(alpha = if (selected) 0.8f else 0.25f),
                                offset = Offset.Zero,
                                blurRadius = if (selected) 7f else 2f
                            )
                        )
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = message ?: items.getOrNull(selectedIndex)?.detail.orEmpty(),
                color = Color(0xFF1C350D),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "2/8 NAV   5 OK   * BACK",
                color = Color(0xFF1A2C0D),
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun PhoneKeypadLabels(modifier: Modifier = Modifier) {
    val keyRows = listOf(
        listOf("1" to "",     "2" to "abc",  "3" to "def"),
        listOf("4" to "ghi",  "5" to "jkl",  "6" to "mno"),
        listOf("7" to "pqrs", "8" to "tuv",  "9" to "wxyz"),
        listOf("*" to "+",    "0" to "−",    "#" to "⇧")
    )
    val xFracs = listOf(0.28f, 0.50f, 0.72f)
    val yFracs = listOf(0.683f, 0.758f, 0.833f, 0.908f)

    BoxWithConstraints(modifier = modifier) {
        val btnW = maxWidth  * 0.162f
        val btnH = maxHeight * 0.058f
        val digitGreen = Color(0xFF4EFF48)
        val letterGreen = Color(0xFFA6FF89)

        keyRows.forEachIndexed { row, cols ->
            cols.forEachIndexed { col, (main, sub) ->
                val cx = maxWidth  * xFracs[col]
                val cy = maxHeight * yFracs[row]
                Box(
                    modifier = Modifier
                        .offset(x = cx - btnW / 2f, y = cy - btnH / 2f)
                        .size(btnW, btnH),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ShaderNumberText(
                            text = main,
                            color = digitGreen,
                            fontSize = (btnH.value * 0.50f).sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = (btnH.value * 0.52f).sp,
                            intensity = 1.45f,
                            glowColor = digitGreen,
                            autoSize = true,
                            forceShader = false
                        )
                        if (sub.isNotEmpty()) {
                            ShaderNumberText(
                                text = sub,
                                color = letterGreen,
                                fontSize = (btnH.value * 0.24f).sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = (btnH.value * 0.28f).sp,
                                intensity = 0.75f,
                                glowColor = digitGreen,
                                autoSize = true,
                                forceShader = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneScreenText(
    dialedNumber: String,
    modifier: Modifier = Modifier
) {
    if (dialedNumber.isBlank()) return

    // Chunk into lines of max 12 chars so nothing wraps mid-word
    val lines = dialedNumber.chunked(12).take(2)
    val text  = lines.joinToString("\n")
    val baseFontSize = if (dialedNumber.length > 12) 24.sp else 32.sp
    var fontSize by remember(text, baseFontSize) { mutableStateOf(baseFontSize) }

    Box(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF36FF2F),
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            lineHeight = fontSize * 1.15f,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp,
            maxLines = 2,
            softWrap = false,
            overflow = TextOverflow.Visible,
            onTextLayout = { result ->
                if (result.didOverflowWidth || result.didOverflowHeight) {
                    fontSize = fontSize * 0.90f
                }
            },
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFF36FF2F).copy(alpha = 0.9f),
                    offset = Offset.Zero,
                    blurRadius = 12f
                )
            )
        )
    }
}

private fun DrawScope.drawSnakeGame(game: SnakeGameState) {
    val left = size.width * PHONE_SCREEN_LEFT_FRACTION
    val top = size.height * PHONE_SCREEN_TOP_FRACTION
    val width = size.width * PHONE_SCREEN_WIDTH_FRACTION
    val height = size.height * PHONE_SCREEN_HEIGHT_FRACTION
    val cellWidth = width / SNAKE_COLUMNS
    val cellHeight = height / SNAKE_ROWS

    drawRoundRect(
        color = PHONE_LCD_COLOR,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(width * PHONE_SCREEN_CORNER_RADIUS_FRACTION)
    )

    fun topLeftFor(point: GridPoint): Offset {
        return Offset(
            x = left + point.x * cellWidth + cellWidth * 0.09f,
            y = top + (SNAKE_ROWS - point.y - 1) * cellHeight + cellHeight * 0.09f
        )
    }

    drawRect(
        color = Color(0xFF1F2E14),
        topLeft = topLeftFor(game.food),
        size = Size(cellWidth * 0.78f, cellHeight * 0.78f)
    )

    game.snake.forEachIndexed { index, point ->
        drawRect(
            color = if (index == 0) Color(0xFF0D1809) else Color(0xFF17280F),
            topLeft = topLeftFor(point),
            size = Size(cellWidth * 0.82f, cellHeight * 0.82f)
        )
    }
}

private fun DrawScope.drawPhoneScreenFill(alpha: Float = 1f) {
    val screenWidth = size.width * PHONE_SCREEN_WIDTH_FRACTION
    val screenHeight = size.height * PHONE_SCREEN_HEIGHT_FRACTION
    drawRoundRect(
        color = PHONE_LCD_COLOR,
        topLeft = Offset(
            x = size.width * PHONE_SCREEN_LEFT_FRACTION,
            y = size.height * PHONE_SCREEN_TOP_FRACTION
        ),
        size = Size(
            width = screenWidth,
            height = screenHeight
        ),
        alpha = alpha,
        cornerRadius = CornerRadius(screenWidth * PHONE_SCREEN_CORNER_RADIUS_FRACTION)
    )
}

private fun DrawScope.drawPhoneSignalFrame(spriteSheet: ImageBitmap, frameIndex: Int) {
    val safeIndex = frameIndex.coerceIn(0, PHONE_SIGNAL_FRAME_COUNT - 1)
    val cellWidth = spriteSheet.width / PHONE_SIGNAL_COLUMNS
    val cellHeight = spriteSheet.height / PHONE_SIGNAL_ROWS
    val column = safeIndex % PHONE_SIGNAL_COLUMNS
    val row = safeIndex / PHONE_SIGNAL_COLUMNS

    val sourceLeft = column * cellWidth + (cellWidth * PHONE_SIGNAL_PATCH_LEFT_FRACTION).roundToInt()
    val sourceTop = row * cellHeight + (cellHeight * PHONE_SIGNAL_PATCH_TOP_FRACTION).roundToInt()
    val sourceWidth = (cellWidth * PHONE_SIGNAL_PATCH_WIDTH_FRACTION).roundToInt()
    val sourceHeight = (cellHeight * PHONE_SIGNAL_PATCH_HEIGHT_FRACTION).roundToInt()

    val dstLeft = size.width * PHONE_SIGNAL_PATCH_LEFT_FRACTION
    val dstTop = size.height * PHONE_SIGNAL_PATCH_TOP_FRACTION
    val dstW = size.width * PHONE_SIGNAL_PATCH_WIDTH_FRACTION
    val dstH = size.height * PHONE_SIGNAL_PATCH_HEIGHT_FRACTION

    // Exact-colour background so any image compression in the sprite blends cleanly
    drawRect(
        color = PHONE_LCD_COLOR,
        topLeft = Offset(dstLeft, dstTop),
        size = Size(dstW, dstH)
    )

    drawImage(
        image = spriteSheet,
        srcOffset = IntOffset(sourceLeft, sourceTop),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
        dstSize = IntSize(dstW.roundToInt(), dstH.roundToInt()),
        blendMode = BlendMode.Darken
    )
}

private fun DrawScope.drawPhoneBatteryIndicator(batteryLevel: Float) {
    val left = size.width * PHONE_BATTERY_LEFT_FRACTION
    val top = size.height * PHONE_BATTERY_TOP_FRACTION
    val width = size.width * PHONE_BATTERY_WIDTH_FRACTION
    val height = size.height * PHONE_BATTERY_HEIGHT_FRACTION

    val padX = width * 0.05f
    val padY = height * 0.18f
    val nubW = width * 0.08f
    val lineW = maxOf(1f, height * 0.08f)

    val bodyW = width - padX * 2f - nubW
    val bodyH = height - padY * 2f
    val bodyLeft = left + padX
    val bodyTop = top + padY

    // Exact LCD background so no colour bleed at edges
    drawRect(color = PHONE_LCD_COLOR, topLeft = Offset(left, top), size = Size(width, height))

    // Battery body: filled with ink then interior cleared (outline without Stroke import)
    drawRect(color = PHONE_LCD_INK_COLOR, topLeft = Offset(bodyLeft, bodyTop), size = Size(bodyW, bodyH))
    drawRect(color = PHONE_LCD_COLOR, topLeft = Offset(bodyLeft + lineW, bodyTop + lineW),
        size = Size(bodyW - lineW * 2f, bodyH - lineW * 2f))

    // Fill segments (4 Nokia-style bars)
    val bars = (batteryLevel * 4f + 0.5f).toInt().coerceIn(0, 4)
    if (bars > 0) {
        val segW = (bodyW - lineW * 2f) / 4f
        drawRect(color = PHONE_LCD_INK_COLOR,
            topLeft = Offset(bodyLeft + lineW, bodyTop + lineW),
            size = Size(segW * bars.toFloat(), bodyH - lineW * 2f))
    }

    // Positive terminal nub on right side
    val nubH = bodyH * 0.50f
    drawRect(color = PHONE_LCD_INK_COLOR,
        topLeft = Offset(bodyLeft + bodyW, bodyTop + (bodyH - nubH) / 2f),
        size = Size(nubW * 0.75f, nubH))
}

@Composable
private fun PhoneKeypadTouchLayer(
    modifier: Modifier,
    onTopButtonTap: () -> Unit,
    onTopButtonHoldStart: () -> Unit,
    onTopButtonHoldEnd: (Boolean) -> Unit,
    onMiddleButtonClick: () -> Unit,
    onDigitPressed: (String) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val layerWidth = maxWidth
        val layerHeight = maxHeight
        val buttonWidth = maxWidth * 0.18f
        val buttonHeight = maxHeight * 0.055f
        val callButtonWidth = maxWidth * 0.33f
        val callButtonHeight = maxHeight * 0.08f
        val xCenters = listOf(0.30f, 0.50f, 0.70f)
        val yCenters = listOf(0.63f, 0.705f, 0.78f, 0.855f)
        val labels = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        Box(modifier = Modifier.fillMaxSize()) {
            InvisiblePhoneButton(
                digit = "call",
                xCenter = layerWidth * 0.50f,
                yCenter = layerHeight * (0.485f + 0.08f / 2f),
                width = callButtonWidth,
                height = callButtonHeight,
                onDigitPressed = { onTopButtonTap() },
                onHoldStart = onTopButtonHoldStart,
                onHoldEnd = onTopButtonHoldEnd
            )

            // Middle button (navUp/Down area)
            InvisiblePhoneButton(
                digit = "middle",
                xCenter = layerWidth * 0.50f,
                yCenter = layerHeight * 0.497f,
                width = buttonWidth * 1.5f,
                height = buttonHeight * 1.2f,
                onDigitPressed = { onMiddleButtonClick() }
            )

            labels.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { columnIndex, digit ->
                    InvisiblePhoneButton(
                        digit = digit,
                        xCenter = layerWidth * xCenters[columnIndex],
                        yCenter = layerHeight * yCenters[rowIndex],
                        width = buttonWidth,
                        height = buttonHeight,
                        onDigitPressed = onDigitPressed
                    )
                }
            }
        }
    }
}

@Composable
private fun InvisiblePhoneButton(
    digit: String,
    xCenter: Dp,
    yCenter: Dp,
    width: Dp,
    height: Dp,
    onDigitPressed: (String) -> Unit,
    onHoldStart: (() -> Unit)? = null,
    onHoldEnd: ((Boolean) -> Unit)? = null
) {
    val holdStart = onHoldStart
    val holdEnd = onHoldEnd
    val pressModifier = if (holdStart != null && holdEnd != null) {
        // Use a concurrent launch instead of withTimeoutOrNull so finger drift doesn't
        // prematurely cancel the hold path (waitForUpOrCancellation returns null on drift,
        // which the old code misread as "released before hold").
        Modifier.pointerInput(holdStart, holdEnd) {
            coroutineScope {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var held = false
                    val holdJob = launch {
                        delay(350L)
                        held = true
                        holdStart()
                    }
                    val up = waitForUpOrCancellation()
                    holdJob.cancel()
                    if (held) {
                        holdEnd(up != null)
                    } else if (up != null) {
                        onDigitPressed(digit)
                    }
                }
            }
        }
    } else {
        Modifier.clickable { onDigitPressed(digit) }
    }

    Box(
        modifier = Modifier
            .offset(x = xCenter - width / 2f, y = yCenter - height / 2f)
            .size(width, height)
            .then(pressModifier)
    )
}

private fun toneForDigit(digit: String): Int {
    return when (digit) {
        "1" -> ToneGenerator.TONE_DTMF_1
        "2" -> ToneGenerator.TONE_DTMF_2
        "3" -> ToneGenerator.TONE_DTMF_3
        "4" -> ToneGenerator.TONE_DTMF_4
        "5" -> ToneGenerator.TONE_DTMF_5
        "6" -> ToneGenerator.TONE_DTMF_6
        "7" -> ToneGenerator.TONE_DTMF_7
        "8" -> ToneGenerator.TONE_DTMF_8
        "9" -> ToneGenerator.TONE_DTMF_9
        "*" -> ToneGenerator.TONE_DTMF_S
        "#" -> ToneGenerator.TONE_DTMF_P
        else -> ToneGenerator.TONE_DTMF_0
    }
}

// Returns phone-image-relative (x, y) fractions (0,0 = top-left) for each button.
private fun buttonPressFraction(control: String): Offset? = when (control) {
    "1"  -> Offset(0.28f, 0.683f)
    "2"  -> Offset(0.50f, 0.683f)
    "3"  -> Offset(0.72f, 0.683f)
    "4"  -> Offset(0.28f, 0.758f)
    "5"  -> Offset(0.50f, 0.758f)
    "6"  -> Offset(0.72f, 0.758f)
    "7"  -> Offset(0.28f, 0.833f)
    "8"  -> Offset(0.50f, 0.833f)
    "9"  -> Offset(0.72f, 0.833f)
    "*"  -> Offset(0.28f, 0.908f)
    "0"  -> Offset(0.50f, 0.908f)
    "#"  -> Offset(0.72f, 0.908f)
    "call" -> Offset(0.50f, 0.525f)
    "gear" -> Offset(0.765f, 0.59f)
    "navLeft"  -> Offset(0.235f, 0.497f)
    "navRight" -> Offset(0.765f, 0.497f)
    "navUp", "navDown" -> Offset(0.50f, 0.497f)
    else -> null
}

private enum class PhoneCallState {
    Ready,
    Dialing,
    Connected
}

internal data class PhoneColorway(
    val label: String,
    val drawableRes: Int,
    val signalSheetRes: Int,
    val swatch: Color
)

internal val PHONE_COLORWAYS = listOf(
    PhoneColorway("Silver", R.drawable.poplur_bling_phone_01_silver_diamond, R.drawable.poplur_phone_signal_silver_diamond, Color(0xFFE2E2D8)),
    PhoneColorway("Gold", R.drawable.poplur_bling_phone_02_champagne_gold, R.drawable.poplur_phone_signal_champagne_gold, Color(0xFFF4B846)),
    PhoneColorway("Rose", R.drawable.poplur_bling_phone_03_rose_gold_pink, R.drawable.poplur_phone_signal_rose_gold_pink, Color(0xFFFF8CA1)),
    PhoneColorway("Ruby", R.drawable.poplur_bling_phone_04_ruby_red, R.drawable.poplur_phone_signal_ruby_red, Color(0xFFEA1010)),
    PhoneColorway("Blue", R.drawable.poplur_bling_phone_05_sapphire_blue, R.drawable.poplur_phone_signal_sapphire_blue, Color(0xFF0B54F2)),
    PhoneColorway("Green", R.drawable.poplur_bling_phone_06_emerald_green, R.drawable.poplur_phone_signal_emerald_green, Color(0xFF00BD40)),
    PhoneColorway("Purple", R.drawable.poplur_bling_phone_07_amethyst_purple, R.drawable.poplur_phone_signal_amethyst_purple, Color(0xFF8F3AEB)),
    PhoneColorway("Onyx", R.drawable.poplur_bling_phone_08_jet_black_onyx, R.drawable.poplur_phone_signal_jet_black_onyx, Color(0xFF151518)),
    PhoneColorway("Aqua", R.drawable.poplur_bling_phone_09_icy_aqua_turquoise, R.drawable.poplur_phone_signal_icy_aqua_turquoise, Color(0xFF33EBF2)),
    PhoneColorway("Holo", R.drawable.poplur_bling_phone_10_holographic_iridescent, R.drawable.poplur_phone_signal_holographic_iridescent, Color(0xFFDBA3FF))
)

private enum class PhoneMenuScreen {
    Main,
    Calls,
    Contacts,
    Texts,
    Games,
    Settings
}

private data class PhoneMenuItem(
    val label: String,
    val detail: String
)

private fun phoneMenuTitle(screen: PhoneMenuScreen): String {
    return when (screen) {
        PhoneMenuScreen.Main -> "POPLUR"
        PhoneMenuScreen.Calls -> "CALLS"
        PhoneMenuScreen.Contacts -> "CONTACTS"
        PhoneMenuScreen.Texts -> "TEXTS"
        PhoneMenuScreen.Games -> "GAMES"
        PhoneMenuScreen.Settings -> "SETTINGS"
    }
}

private fun phoneMenuItems(screen: PhoneMenuScreen, userName: String): List<PhoneMenuItem> {
    return when (screen) {
        PhoneMenuScreen.Main -> listOf(
            PhoneMenuItem("Calls", "Make and manage calls"),
            PhoneMenuItem("Contacts", "Saved Poplur friends"),
            PhoneMenuItem("Texts", "Inbox and drafts"),
            PhoneMenuItem("Games", "Classic phone games"),
            PhoneMenuItem("Settings", "Ringtone and display")
        )
        PhoneMenuScreen.Calls -> listOf(
            PhoneMenuItem("Call user", userName),
            PhoneMenuItem("Recent calls", "No recent calls"),
            PhoneMenuItem("Dial number", "Enter digits")
        )
        PhoneMenuScreen.Contacts -> listOf(
            PhoneMenuItem(userName, "Primary contact"),
            PhoneMenuItem("Poplur Support", "Online soon"),
            PhoneMenuItem("Add contact", "Coming soon")
        )
        PhoneMenuScreen.Texts -> listOf(
            PhoneMenuItem("Inbox", "No new messages"),
            PhoneMenuItem("Write message", "Composer soon"),
            PhoneMenuItem("Drafts", "No drafts")
        )
        PhoneMenuScreen.Games -> listOf(
            PhoneMenuItem("Snake", "2/4/6/8 move"),
            PhoneMenuItem("Memory", "Coming soon"),
            PhoneMenuItem("Bounce", "Coming soon")
        )
        PhoneMenuScreen.Settings -> listOf(
            PhoneMenuItem("Ring tone", "Pop tone"),
            PhoneMenuItem("Screen glow", "Maximum"),
            PhoneMenuItem("About", "Poplur phone")
        )
    }
}

private data class GridPoint(val x: Int, val y: Int)

private data class SnakeGameState(
    val snake: List<GridPoint>,
    val direction: GridPoint,
    val pendingDirection: GridPoint,
    val food: GridPoint,
    val score: Int,
    val gameOver: Boolean
) {
    fun turn(nextDirection: GridPoint): SnakeGameState {
        if (nextDirection.x + direction.x == 0 && nextDirection.y + direction.y == 0) return this
        return copy(pendingDirection = nextDirection)
    }

    fun advance(): SnakeGameState {
        if (gameOver) return this
        val head = snake.first()
        val nextDirection = pendingDirection
        val nextHead = GridPoint(head.x + nextDirection.x, head.y + nextDirection.y)
        if (
            nextHead.x !in 0 until SNAKE_COLUMNS ||
            nextHead.y !in 0 until SNAKE_ROWS ||
            snake.contains(nextHead)
        ) {
            return copy(direction = nextDirection, gameOver = true)
        }

        val ateFood = nextHead == food
        val nextSnake = if (ateFood) {
            listOf(nextHead) + snake
        } else {
            listOf(nextHead) + snake.dropLast(1)
        }

        return copy(
            snake = nextSnake,
            direction = nextDirection,
            pendingDirection = nextDirection,
            food = if (ateFood) placeFood(nextSnake) else food,
            score = if (ateFood) score + 1 else score
        )
    }

    companion object {
        fun initial(): SnakeGameState {
            val body = listOf(GridPoint(5, 5), GridPoint(4, 5), GridPoint(3, 5))
            return SnakeGameState(
                snake = body,
                direction = GridPoint(1, 0),
                pendingDirection = GridPoint(1, 0),
                food = GridPoint(10, 5),
                score = 0,
                gameOver = false
            )
        }

        private fun placeFood(snake: List<GridPoint>): GridPoint {
            val openCells = (0 until SNAKE_COLUMNS).flatMap { x ->
                (0 until SNAKE_ROWS).map { y -> GridPoint(x, y) }
            }.filterNot { snake.contains(it) }
            return openCells.randomOrNull() ?: GridPoint(10, 5)
        }
    }
}

private fun launchDialer(context: android.content.Context, dialedNumber: String) {
    val normalized = dialedNumber.filter { it in "0123456789*#+" }
    if (normalized.isBlank()) return

    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(normalized)}"))
    runCatching { context.startActivity(intent) }
}

private fun formatCallDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private fun playPhoneBootSound(context: Context) {
    try {
        val afd = context.assets.openFd("popular_phone_bootup.mp3")
        val player = MediaPlayer()
        player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        afd.close()
        player.setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        player.prepare()
        player.start()
        player.setOnCompletionListener { it.release() }
    } catch (_: Exception) {}
}

private const val PHONE_IMAGE_WIDTH = 1024f
private const val PHONE_IMAGE_HEIGHT = 1536f
private const val PHONE_SIGNAL_COLUMNS = 4
private const val PHONE_SIGNAL_ROWS = 4
private const val PHONE_SIGNAL_FRAME_COUNT = PHONE_SIGNAL_COLUMNS * PHONE_SIGNAL_ROWS
private const val PHONE_SIGNAL_PATCH_LEFT_FRACTION = 0.265f
private const val PHONE_SIGNAL_PATCH_TOP_FRACTION = 0.195f
private const val PHONE_SIGNAL_PATCH_WIDTH_FRACTION = 0.135f
private const val PHONE_SIGNAL_PATCH_HEIGHT_FRACTION = 0.058f
private const val PHONE_SCREEN_LEFT_FRACTION = 0.240f
private const val PHONE_SCREEN_TOP_FRACTION = 0.183f
private const val PHONE_SCREEN_WIDTH_FRACTION = 0.520f
private const val PHONE_SCREEN_HEIGHT_FRACTION = 0.245f
private const val PHONE_SCREEN_CORNER_RADIUS_FRACTION = 0.040f
private val PHONE_LCD_COLOR = Color(0xFFEFF100)
private val PHONE_LCD_INK_COLOR = Color(0xFF0F1800)
// Battery indicator: mirrored to top-right of LCD, symmetric with signal at top-left
private const val PHONE_BATTERY_LEFT_FRACTION = 0.600f
private const val PHONE_BATTERY_TOP_FRACTION = 0.195f
private const val PHONE_BATTERY_WIDTH_FRACTION = 0.135f
private const val PHONE_BATTERY_HEIGHT_FRACTION = 0.058f
private const val SNAKE_COLUMNS = 16
private const val SNAKE_ROWS = 10

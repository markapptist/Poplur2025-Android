package com.apptists.poplur2025.ui.screens

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun PopularPhoneCallScreen(
    userName: String,
    onHangUp: () -> Unit,
    userProfile: UserProfile? = null,
    onVisibilityChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val colorWheelScope = rememberCoroutineScope()
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
    var isPhoneButtonActive by remember { mutableStateOf(true) }
    var dialedNumber by remember { mutableStateOf("") }
    var snakeGame by remember { mutableStateOf(SnakeGameState.initial()) }
    var isSnakeActive by remember { mutableStateOf(false) }
    var isDialerActive by remember { mutableStateOf(false) }
    var isScreenLit by remember { mutableStateOf(false) }
    var menuScreen by remember { mutableStateOf(PhoneMenuScreen.Main) }
    var selectedMenuIndex by remember { mutableStateOf(0) }
    var menuMessage by remember { mutableStateOf<String?>(null) }
    var pressedFrameIndex by remember { mutableStateOf<Int?>(null) }
    var idleSignalFrameIndex by remember(selectedColorway) { mutableStateOf(PHONE_SIGNAL_FRAME_COUNT - 1) }
    var pressAnimationNonce by remember { mutableStateOf(0) }
    val pressSheetBitmap = remember(selectedColorway.pressSheetRes) {
        BitmapFactory.decodeResource(context.resources, selectedColorway.pressSheetRes).asImageBitmap()
    }
    val signalSheetBitmap = remember(selectedColorway.signalSheetRes) {
        BitmapFactory.decodeResource(context.resources, selectedColorway.signalSheetRes).asImageBitmap()
    }
    val callState by CallSessionManager.callState.collectAsState()
    val elapsedSeconds by CallSessionManager.elapsedSeconds.collectAsState()
    val visibleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 180),
        label = "phone-call-alpha"
    )
    val screenAlpha by animateFloatAsState(
        targetValue = if (isScreenLit) 1f else 0f,
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

    LaunchedEffect(isSnakeActive, snakeGame.direction) {
        while (isSnakeActive) {
            delay(180)
            snakeGame = snakeGame.advance()
        }
    }

    LaunchedEffect(Unit) {
        isScreenLit = true
    }

    LaunchedEffect(menuMessage) {
        if (menuMessage != null) {
            delay(950)
            menuMessage = null
        }
    }

    LaunchedEffect(pressAnimationNonce) {
        if (pressedFrameIndex != null) {
            delay(140)
            pressedFrameIndex = null
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
                flashMenuDetail(colorway.label.uppercase())
            }
        }
    }

    fun togglePhoneButtonActive() {
        isPhoneButtonActive = !isPhoneButtonActive
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        flashMenuDetail(if (isPhoneButtonActive) "BUTTON ACTIVE" else "BUTTON LOCKED")
    }

    fun showPhonePress(control: String) {
        pressedFrameIndex = phonePressFrameIndex(control)
        pressAnimationNonce += 1
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

            LaunchedEffect(selectedColorway, showPhoneMenu, pressedFrameIndex, isScreenLit) {
                if (!showPhoneMenu || pressedFrameIndex != null || !isScreenLit) {
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
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(selectedColorway.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                pressedFrameIndex?.let { frameIndex ->
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPhonePressFrame(pressSheetBitmap, frameIndex)
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (isSnakeActive) {
                        drawPhoneScreenFill(alpha = screenAlpha)
                        drawSnakeGame(snakeGame)
                    } else {
                        drawPhoneScreenFill(alpha = screenAlpha)
                    }
                }
                if (!isSnakeActive) {
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
                if (showPhoneMenu && pressedFrameIndex == null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawPhoneSignalFrame(signalSheetBitmap, idleSignalFrameIndex)
                    }
                }
                PhoneKeypadTouchLayer(
                    modifier = Modifier.fillMaxSize(),
                    onTopButtonTap = {
                        showPhonePress("call")
                        togglePhoneButtonActive()
                    },
                    onTopButtonHoldStart = {
                        showPhonePress("call")
                        colorWheelController.beginHold(colorWheelItems())
                    },
                    onTopButtonHoldEnd = { activateSelected ->
                        colorWheelController.endHold(activateSelected)
                    },
                    onDigitPressed = { digit ->
                        showPhonePress(digit)
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
                            autoSize = true
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
                                autoSize = true
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

private fun DrawScope.drawPhonePressFrame(spriteSheet: ImageBitmap, frameIndex: Int) {
    val safeIndex = frameIndex.coerceIn(0, PHONE_PRESS_COLUMNS * PHONE_PRESS_ROWS - 1)
    val cellWidth = spriteSheet.width / PHONE_PRESS_COLUMNS
    val cellHeight = spriteSheet.height / PHONE_PRESS_ROWS
    val column = safeIndex % PHONE_PRESS_COLUMNS
    val row = safeIndex / PHONE_PRESS_COLUMNS

    drawImage(
        image = spriteSheet,
        srcOffset = IntOffset(column * cellWidth, row * cellHeight),
        srcSize = IntSize(cellWidth, cellHeight),
        dstOffset = IntOffset(0, 0),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
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

    drawImage(
        image = spriteSheet,
        srcOffset = IntOffset(sourceLeft, sourceTop),
        srcSize = IntSize(sourceWidth, sourceHeight),
        dstOffset = IntOffset(
            x = (size.width * PHONE_SIGNAL_PATCH_LEFT_FRACTION).roundToInt(),
            y = (size.height * PHONE_SIGNAL_PATCH_TOP_FRACTION).roundToInt()
        ),
        dstSize = IntSize(
            width = (size.width * PHONE_SIGNAL_PATCH_WIDTH_FRACTION).roundToInt(),
            height = (size.height * PHONE_SIGNAL_PATCH_HEIGHT_FRACTION).roundToInt()
        )
    )
}

@Composable
private fun PhoneKeypadTouchLayer(
    modifier: Modifier,
    onTopButtonTap: () -> Unit,
    onTopButtonHoldStart: () -> Unit,
    onTopButtonHoldEnd: (Boolean) -> Unit,
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
    val viewConfiguration = LocalViewConfiguration.current
    val holdStart = onHoldStart
    val holdEnd = onHoldEnd
    val pressModifier = if (holdStart != null && holdEnd != null) {
        Modifier.pointerInput(viewConfiguration, holdStart, holdEnd) {
            awaitEachGesture {
                awaitFirstDown()
                val releasedBeforeHold = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    waitForUpOrCancellation()
                }
                if (releasedBeforeHold != null) {
                    onDigitPressed(digit)
                } else {
                    holdStart()
                    val release = waitForUpOrCancellation()
                    holdEnd(release != null)
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

private fun phonePressFrameIndex(control: String): Int {
    return when (control) {
        "gear" -> 0
        "call" -> 1
        "navUp", "navLeft" -> 2
        "navDown", "navRight" -> 3
        "1" -> 4
        "2" -> 5
        "3" -> 6
        "4" -> 7
        "5" -> 8
        "6" -> 9
        "7" -> 10
        "8" -> 11
        "9" -> 12
        "*" -> 13
        "0" -> 14
        "#" -> 15
        else -> 1
    }
}

private enum class PhoneCallState {
    Ready,
    Dialing,
    Connected
}

private data class PhoneColorway(
    val label: String,
    val drawableRes: Int,
    val pressSheetRes: Int,
    val signalSheetRes: Int,
    val swatch: Color
)

private val PHONE_COLORWAYS = listOf(
    PhoneColorway("Silver", R.drawable.poplur_bling_phone_01_silver_diamond, R.drawable.poplur_phone_press_silver_diamond, R.drawable.poplur_phone_signal_silver_diamond, Color(0xFFE2E2D8)),
    PhoneColorway("Gold", R.drawable.poplur_bling_phone_02_champagne_gold, R.drawable.poplur_phone_press_champagne_gold, R.drawable.poplur_phone_signal_champagne_gold, Color(0xFFF4B846)),
    PhoneColorway("Rose", R.drawable.poplur_bling_phone_03_rose_gold_pink, R.drawable.poplur_phone_press_rose_gold_pink, R.drawable.poplur_phone_signal_rose_gold_pink, Color(0xFFFF8CA1)),
    PhoneColorway("Ruby", R.drawable.poplur_bling_phone_04_ruby_red, R.drawable.poplur_phone_press_ruby_red, R.drawable.poplur_phone_signal_ruby_red, Color(0xFFEA1010)),
    PhoneColorway("Blue", R.drawable.poplur_bling_phone_05_sapphire_blue, R.drawable.poplur_phone_press_sapphire_blue, R.drawable.poplur_phone_signal_sapphire_blue, Color(0xFF0B54F2)),
    PhoneColorway("Green", R.drawable.poplur_bling_phone_06_emerald_green, R.drawable.poplur_phone_press_emerald_green, R.drawable.poplur_phone_signal_emerald_green, Color(0xFF00BD40)),
    PhoneColorway("Purple", R.drawable.poplur_bling_phone_07_amethyst_purple, R.drawable.poplur_phone_press_amethyst_purple, R.drawable.poplur_phone_signal_amethyst_purple, Color(0xFF8F3AEB)),
    PhoneColorway("Onyx", R.drawable.poplur_bling_phone_08_jet_black_onyx, R.drawable.poplur_phone_press_jet_black_onyx, R.drawable.poplur_phone_signal_jet_black_onyx, Color(0xFF151518)),
    PhoneColorway("Aqua", R.drawable.poplur_bling_phone_09_icy_aqua_turquoise, R.drawable.poplur_phone_press_icy_aqua_turquoise, R.drawable.poplur_phone_signal_icy_aqua_turquoise, Color(0xFF33EBF2)),
    PhoneColorway("Holo", R.drawable.poplur_bling_phone_10_holographic_iridescent, R.drawable.poplur_phone_press_holographic_iridescent, R.drawable.poplur_phone_signal_holographic_iridescent, Color(0xFFDBA3FF))
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

// Ellipse path covering all button regions — used to clip the button-restore and glitter layers.
// Y fractions here are from the TOP of the image (Android canvas origin), not GLSL UV bottom-origin.
private fun buildButtonPath(canvasSize: Size): Path {
    val bw = canvasSize.width * 0.138f
    val bh = canvasSize.height * 0.052f
    val xCenters = listOf(0.28f, 0.50f, 0.72f)
    val yCenters = listOf(0.683f, 0.758f, 0.833f, 0.908f)
    return Path().apply {
        for (yc in yCenters) {
            for (xc in xCenters) {
                addOval(Rect(
                    left   = canvasSize.width  * xc - bw,
                    top    = canvasSize.height * yc - bh,
                    right  = canvasSize.width  * xc + bw,
                    bottom = canvasSize.height * yc + bh
                ))
            }
        }
        // Nav: centre (x=0.50, y=0.497), left (x=0.235), right (x=0.765)
        val navY = canvasSize.height * 0.497f
        addOval(Rect(canvasSize.width * 0.50f  - canvasSize.width * 0.195f, navY - canvasSize.height * 0.065f,
                     canvasSize.width * 0.50f  + canvasSize.width * 0.195f, navY + canvasSize.height * 0.065f))
        addOval(Rect(canvasSize.width * 0.235f - canvasSize.width * 0.120f, navY - canvasSize.height * 0.062f,
                     canvasSize.width * 0.235f + canvasSize.width * 0.120f, navY + canvasSize.height * 0.062f))
        addOval(Rect(canvasSize.width * 0.765f - canvasSize.width * 0.120f, navY - canvasSize.height * 0.062f,
                     canvasSize.width * 0.765f + canvasSize.width * 0.120f, navY + canvasSize.height * 0.062f))
    }
}

// Generates a Nokia-era glitter texture: near-black base with purple/pink/gold flecks
// and bright white sparkle catch-lights. Screen-blending this over the red body gives
// holographic sparkle without changing the underlying hue where texture is dark.
private fun makeGlitterBitmap(): ImageBitmap {
    val sz = 512
    val bmp = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)

    canvas.drawColor(android.graphics.Color.BLACK)

    val fleckColors = listOf(
        android.graphics.Color.argb(224, 204,   0, 230),
        android.graphics.Color.argb(209, 242,  51, 153),
        android.graphics.Color.argb(199, 230, 140,  26),
        android.graphics.Color.argb(184, 179, 173, 230),
        android.graphics.Color.argb(217, 153,   0, 179)
    )
    for (i in 0 until 2800) {
        val fi = i.toFloat()
        val x = ((fi * 1.618f) % 1f) * sz
        val y = ((fi * 2.414f) % 1f) * sz
        val r = 0.9f + (fi % 6f) * 0.45f
        paint.color = fleckColors[i % fleckColors.size]
        canvas.drawCircle(x, y, r, paint)
    }
    for (i in 0 until 320) {
        val fi = i.toFloat()
        val x = ((fi * 3.14159f) % 1f) * sz
        val y = ((fi * 2.71828f) % 1f) * sz
        val r = 1.1f + (fi % 4f) * 1.0f
        paint.color = android.graphics.Color.WHITE
        paint.alpha = (220 + (i % 3) * 10).coerceAtMost(255)
        canvas.drawCircle(x, y, r, paint)
    }
    return bmp.asImageBitmap()
}

// Converts the phone body photo to vivid red plastic.
// Luma (0.2126R+0.7152G+0.0722B) is mapped to the R channel at 1.55× scale;
// G and B are zeroed so the body is purely red. +40/255 floor lifts grimy
// blacks to a deep-red rather than near-black, preserving visible form detail.
private fun plasticColorMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        0.3295f, 1.1086f, 0.1119f, 0f, 40f,
        0f,      0f,      0f,      0f,  0f,
        0f,      0f,      0f,      0f,  0f,
        0f,      0f,      0f,      1f,  0f
    )
)

// Wide studio specular (top-centre), rim catch-light (left edge), and a light
// form vignette — kept at 0.22 alpha so the brand-new look stays bright.
private fun DrawScope.drawPhonePlasticOverlay() {
    val specCenter = Offset(size.width * 0.50f, size.height * 0.18f)
    val specRadius = size.width * 0.72f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.06f),
                Color.Transparent
            ),
            center = specCenter,
            radius = specRadius
        ),
        radius = specRadius,
        center = specCenter
    )

    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
            startX = 0f,
            endX = size.width * 0.12f
        )
    )

    val vigCenter = Offset(size.width * 0.50f, size.height * 0.48f)
    val vigRadius = maxOf(size.width, size.height) * 0.85f
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.22f)),
            center = vigCenter,
            radius = vigRadius,
            tileMode = TileMode.Clamp
        )
    )
}

private fun formatCallDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

private const val PHONE_IMAGE_WIDTH = 1024f
private const val PHONE_IMAGE_HEIGHT = 1536f
private const val PHONE_PRESS_COLUMNS = 4
private const val PHONE_PRESS_ROWS = 4
private const val PHONE_SIGNAL_COLUMNS = 4
private const val PHONE_SIGNAL_ROWS = 4
private const val PHONE_SIGNAL_FRAME_COUNT = PHONE_SIGNAL_COLUMNS * PHONE_SIGNAL_ROWS
private const val PHONE_SIGNAL_PATCH_LEFT_FRACTION = 0.265f
private const val PHONE_SIGNAL_PATCH_TOP_FRACTION = 0.195f
private const val PHONE_SIGNAL_PATCH_WIDTH_FRACTION = 0.120f
private const val PHONE_SIGNAL_PATCH_HEIGHT_FRACTION = 0.045f
private const val PHONE_SCREEN_LEFT_FRACTION = 0.240f
private const val PHONE_SCREEN_TOP_FRACTION = 0.183f
private const val PHONE_SCREEN_WIDTH_FRACTION = 0.520f
private const val PHONE_SCREEN_HEIGHT_FRACTION = 0.245f
private const val PHONE_SCREEN_CORNER_RADIUS_FRACTION = 0.040f
private val PHONE_LCD_COLOR = Color(0xFFEFF100)
private const val SNAKE_COLUMNS = 16
private const val SNAKE_ROWS = 10

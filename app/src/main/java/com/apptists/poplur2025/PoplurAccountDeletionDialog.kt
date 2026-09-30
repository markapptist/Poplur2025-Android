package com.apptists.poplur2025

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlin.math.hypot

// The account-deletion dialog as a Poplur "clear window", matching iOS
// PoplurAccountDeletionDialog.swift: the canonical transient-card paint
// (poplur-clay-map/src/TransientPopupSurface.module.css) in the News pop card's
// layout (MapNewsCard.module.css). A dark glass card with a thin pink rim and a
// pink glow in its top corner, a header bar with a status pill and a round ×, a
// left-aligned heavy title, and rounded-rectangle buttons. It replaces three
// stock AlertDialogs. The password is typed and used here, in the native shell,
// never in the page.

/** The one dialog's three states. The header pill reads ACCOUNT, DELETING or DELETED. */
internal sealed interface DeletionPhase {
    /** The password step. [error] is the inline message after a refused password. */
    data class Confirm(val error: String? = null) : DeletionPhase
    object Working : DeletionPhase
    data class Result(val title: String, val message: String) : DeletionPhase
}

internal const val DELETION_DONE_TITLE = "Account deleted"

/**
 * Hosts the Compose card in a full-screen transparent window over [activity].
 * The coordinator keeps the phase; this only draws it, so the card can be
 * rebuilt on whichever activity is on screen when a deletion finishes.
 */
internal class PoplurDeletionDialog(
    val activity: Activity,
    initial: DeletionPhase,
    private val onConfirm: (String) -> Unit,
    private val onCancel: () -> Unit,
    private val onAcknowledge: () -> Unit,
) {
    var phase by mutableStateOf(initial)

    private val blurBehind = Build.VERSION.SDK_INT >= 31 && activity.windowManager.isCrossWindowBlurEnabled

    private val dialog = ComponentDialog(activity, android.R.style.Theme_Translucent_NoTitleBar).apply {
        // Modal like iOS isModalInPresentation: no tap-outside dismissal.
        setCancelable(false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (phase) {
                    is DeletionPhase.Confirm -> onCancel()
                    is DeletionPhase.Result -> onAcknowledge()
                    DeletionPhase.Working -> Unit
                }
            }
        })
        setContentView(
            ComposeView(context).apply {
                setContent {
                    PoplurDeletionDialogContent(
                        phase = phase,
                        onConfirm = onConfirm,
                        onCancel = onCancel,
                        onAcknowledge = onAcknowledge,
                        scrimAlpha = if (blurBehind) 0.52f else 0.6f,
                    )
                }
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setWindowAnimations(android.R.style.Animation_Toast)
            // The card pads itself clear of the bars, cutout and keyboard.
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            // TransientPopupSurface .scrim blurs what is behind it. Devices that
            // allow cross-window blur get it; the rest get a darker scrim.
            if (blurBehind && Build.VERSION.SDK_INT >= 31) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.also { it.blurBehindRadius = 18 }
            }
        }
    }

    fun show() {
        if (!activity.isFinishing && !activity.isDestroyed) dialog.show()
    }

    fun dismiss() {
        runCatching { if (dialog.isShowing) dialog.dismiss() }
    }
}

// Style values from TransientPopupSurface.module.css and MapNewsCard.module.css, as on iOS.
private val CardShape = RoundedCornerShape(21.dp)
private val ButtonShape = RoundedCornerShape(14.dp)
private val WellShape = RoundedCornerShape(8.dp)
private val Scrim = Color(0xFF09050B)
private val HeaderFill = Color(0xE610070F)
private val Rim = Color(0xB8FF88C3)
private val Glow = Color(0x40F93C9A)
private val PillFill = Color(0xFFE11859)
private val PillFillResult = Color(0xFFF00078)
private val Cyan = Color(0xFF71DFFF)
private val CyanFocus = Color(0xFF8DE9FF)
private val ErrorPink = Color(0xFFFF79B9)
private val BodyText = Color(0xE6FFF4FA)

/**
 * The scrim and the card. Kept separate from the window so it can be drawn on
 * its own, for example in a screenshot test.
 */
@Composable
internal fun PoplurDeletionDialogContent(
    phase: DeletionPhase,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onAcknowledge: () -> Unit,
    scrimAlpha: Float = 0.6f,
    animateEntrance: Boolean = true,
) {
    var shown by remember { mutableStateOf(!animateEntrance) }
    LaunchedEffect(Unit) { shown = true }
    // iOS: .spring(response: 0.42, dampingFraction: 0.86) from 0.94 scale, 14 pt low.
    val entrance by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 224f),
        label = "deletionDialogEntrance",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Scrim.copy(alpha = scrimAlpha))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Card(
                phase = phase,
                onConfirm = onConfirm,
                onCancel = onCancel,
                onAcknowledge = onAcknowledge,
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .graphicsLayer {
                        val scale = 0.94f + 0.06f * entrance
                        scaleX = scale
                        scaleY = scale
                        translationY = (1f - entrance) * 14.dp.toPx()
                        alpha = entrance.coerceIn(0f, 1f)
                    },
            )
        }
    }
}

@Composable
private fun Card(
    phase: DeletionPhase,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .shadow(20.dp, CardShape, clip = false, ambientColor = Color(0xFFF63B9A), spotColor = Color.Black)
            .clip(CardShape)
            .drawBehind {
                // linear-gradient(150deg, rgba(29,8,24,.97), rgba(8,7,13,.97))
                drawRect(
                    Brush.linearGradient(
                        listOf(Color(0xF71D0818), Color(0xF708070D)),
                        start = Offset(size.width * 0.25f, size.height * 0.07f),
                        end = Offset(size.width * 0.75f, size.height * 0.93f),
                    ),
                )
                // radial-gradient(circle at 88% 2%, rgba(249,60,154,.25), transparent 38%)
                drawRect(
                    Brush.radialGradient(
                        listOf(Glow, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.02f),
                        radius = hypot(size.width * 0.88f, size.height * 0.98f) * 0.38f,
                    ),
                )
                // inset 0 1px 0 rgba(255,255,255,.19)
                drawLine(Color(0x30FFFFFF), Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1.dp.toPx())
            }
            .border(1.dp, Rim, CardShape)
            .semantics { paneTitle = "Delete account" },
    ) {
        Header(phase, onCancel)
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) using SizeTransform(clip = false) },
            contentKey = { it::class },
            label = "deletionDialogPhase",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 19.dp, top = 17.dp, end = 19.dp, bottom = 19.dp),
        ) { current ->
            Column(Modifier.fillMaxWidth()) {
                when (current) {
                    is DeletionPhase.Confirm -> ConfirmBody(current.error, onConfirm, onCancel)
                    DeletionPhase.Working -> WorkingBody()
                    is DeletionPhase.Result -> ResultBody(current, onAcknowledge)
                }
            }
        }
    }
}

/** The News pop card's header: a status pill and a round ×. */
@Composable
private fun Header(phase: DeletionPhase, onCancel: () -> Unit) {
    val status = when (phase) {
        is DeletionPhase.Confirm -> "ACCOUNT"
        DeletionPhase.Working -> "DELETING"
        is DeletionPhase.Result -> if (phase.title == DELETION_DONE_TITLE) "DELETED" else "ACCOUNT"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(HeaderFill)
            .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            status,
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp,
            modifier = Modifier
                .background(if (phase is DeletionPhase.Result) PillFillResult else PillFill, CircleShape)
                .padding(vertical = 6.dp, horizontal = 10.dp),
        )
        Spacer(Modifier.weight(1f))
        if (phase is DeletionPhase.Confirm) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x17FFFFFF))
                    .border(1.dp, Color(0x6BFFCDE5), CircleShape)
                    .clickable(onClick = onCancel)
                    .clearAndSetSemantics {
                        contentDescription = "Close"
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("×", color = Color.White, fontSize = 24.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
            }
        } else {
            Spacer(Modifier.size(44.dp))
        }
    }
}

@Composable
private fun ConfirmBody(error: String?, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    // Never saved or restored: a new card always starts with an empty field.
    var password by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        val typed = password
        if (typed.isNotEmpty()) {
            password = ""
            keyboard?.hide()
            onConfirm(typed)
        }
    }
    LaunchedEffect(error) {
        delay(350)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }
    Heading("Delete your account?")
    Summary("This permanently deletes your Poplur account, profile, posts, uploaded media and the messages you sent. It can't be undone.")
    Text(
        "ENTER YOUR PASSWORD TO CONFIRM",
        color = Cyan,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
    PasswordWell(password, { password = it }, focus, submit)
    if (error != null) {
        Text(
            error,
            color = ErrorPink,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SurfaceButton("DELETE ACCOUNT", primary = true, enabled = password.isNotEmpty(), onClick = submit)
        SurfaceButton("CANCEL", primary = false, onClick = {
            keyboard?.hide()
            onCancel()
        })
    }
}

@Composable
private fun WorkingBody() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Cyan,
            strokeWidth = 2.5.dp,
            trackColor = Color(0x2671DFFF),
        )
        Heading("Deleting your account…")
    }
    Summary("Removing your account and personal data. Please keep Poplur open.")
}

@Composable
private fun ResultBody(result: DeletionPhase.Result, onAcknowledge: () -> Unit) {
    Heading(result.title)
    Summary(result.message)
    SurfaceButton("OK", primary = true, onClick = onAcknowledge, modifier = Modifier.padding(top = 6.dp))
}

/** MapNewsCard `.copy h2`: white, heavy, tight. */
@Composable
private fun Heading(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 23.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1).sp,
        modifier = Modifier.semantics { heading() },
    )
}

/** MapNewsCard `.summary`. */
@Composable
private fun Summary(text: String) {
    Text(
        text,
        color = BodyText,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 13.dp, bottom = 10.dp),
    )
}

/** The composer's text field: a dark inset well with a faint pink rim that turns cyan while focused. */
@Composable
private fun PasswordWell(value: String, onValueChange: (String) -> Unit, focus: FocusRequester, onGo: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .semantics {
                contentDescription = "Password"
                contentType = ContentType.Password
            },
        singleLine = true,
        textStyle = TextStyle(color = Color(0xFFFFF9FC), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(CyanFocus),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        interactionSource = interaction,
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .drawBehind {
                        // iOS: a #45cdef glow at 30% while focused. Drawn as rings outside
                        // the rim; a shadow would show through the translucent well.
                        if (focused) {
                            val stroke = 2.dp.toPx()
                            listOf(1.dp to 0.22f, 3.dp to 0.12f, 5.dp to 0.06f).forEach { (spread, alpha) ->
                                val out = spread.toPx()
                                drawRoundRect(
                                    color = Color(0xFF45CDEF).copy(alpha = alpha),
                                    topLeft = Offset(-out, -out),
                                    size = Size(size.width + out * 2, size.height + out * 2),
                                    cornerRadius = CornerRadius(8.dp.toPx() + out),
                                    style = Stroke(width = stroke),
                                )
                            }
                        }
                    }
                    .clip(WellShape)
                    .background(Color(0xD109080B))
                    .drawBehind {
                        // The well's inner shadow along its top edge.
                        drawRect(Brush.verticalGradient(listOf(Color(0x9E000000), Color.Transparent), startY = 0f, endY = 7.dp.toPx()))
                    }
                    .border(1.dp, if (focused) CyanFocus else Color(0x38FFD1E9), WellShape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text("Password", color = Color(0x73FFFFFF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                field()
            }
        },
    )
}

/** TransientPopupSurface `.primary` (pink) and MapNewsCard `.secondary` (cyan outline). */
@Composable
private fun SurfaceButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (primary) 44.dp else 42.dp)
            .graphicsLayer {
                val scale = if (pressed) 0.97f else 1f
                scaleX = scale
                scaleY = scale
                translationY = if (pressed) 1.dp.toPx() else 0f
                alpha = if (enabled) 1f else 0.45f
            }
            .then(
                if (primary) Modifier.shadow(8.dp, ButtonShape, clip = false, ambientColor = Color(0xFF6A0035), spotColor = Color(0xFF6A0035))
                else Modifier
            )
            .clip(ButtonShape)
            .background(
                if (primary) Brush.linearGradient(listOf(Color(0xFFF63B9A), Color(0xFFBD0F66)))
                else SolidColor(Color(0x1471DFFF))
            )
            .then(
                if (primary) Modifier.drawBehind {
                    drawLine(Color(0x47FFFFFF), Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1.dp.toPx())
                } else Modifier
            )
            .border(1.dp, if (primary) Color(0x99FFBCDE) else Color(0x7571DFFF), ButtonShape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (primary) Color.White else Color(0xFFBDEEFF),
            fontSize = if (primary) 11.sp else 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = if (primary) 1.32.sp else 1.sp,
        )
    }
}

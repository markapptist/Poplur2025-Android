package com.apptists.poplur2025.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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

    LaunchedEffect(Unit) {
        delay(900)
        showConfetti = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (showConfetti && mode == AuthMode.Welcome) {
            AndroidView(
                factory = { ctx ->
                    FragmentContainerView(ctx).apply {
                        id = confettiContainerId
                        setBackgroundColor(0)
                        background = null
                        isClickable = false
                        isFocusable = false
                        isEnabled = false

                        val activity = ctx as? FragmentActivity
                        activity?.supportFragmentManager?.beginTransaction()
                            ?.replace(id, ConfettiAnimationFragment())
                            ?.commit()
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

        TicketPanel(
            modifier = Modifier
                .align(Alignment.Center)
                .zIndex(1f)
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = if (mode == AuthMode.Welcome) 92.dp else 172.dp,
                    bottom = if (mode == AuthMode.Welcome) 92.dp else 72.dp
                )
        )

        if (mode == AuthMode.Welcome) {
            Box(Modifier.fillMaxSize().zIndex(2f)) {
                WelcomeTicketContent(
                    logoBitmap = logoBitmap,
                    onSignIn = { mode = AuthMode.Login },
                    onGoogleSignIn = {
                        authViewModel.signInWithGoogle(context) { ok, msg ->
                            if (!ok && msg != null) {
                                android.util.Log.e("Auth", "Google Sign-In failed: $msg")
                            }
                        }
                    }
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
    onSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 36.dp, vertical = 92.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        WelcomeLogo(logoBitmap, Modifier.size(214.dp))
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .width(220.dp)
                .height(66.dp),
            shape = RoundedCornerShape(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp)
        ) {
            Text("Sign In", fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onGoogleSignIn,
            modifier = Modifier
                .width(220.dp)
                .height(66.dp),
            shape = RoundedCornerShape(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4285F4),
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp)
        ) {
            Text("Google Sign In", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(Modifier.height(40.dp))
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
                .padding(start = 28.dp, top = 74.dp)
                .size(68.dp)
                .shadow(24.dp, RoundedCornerShape(34.dp)),
            shape = RoundedCornerShape(34.dp),
            color = Color.White.copy(alpha = 0.96f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(38.dp))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 46.dp, end = 46.dp, top = 208.dp, bottom = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WelcomeLogo(logoBitmap, Modifier.size(218.dp))
            Spacer(Modifier.weight(1f))
            LoginFields(
                email = email,
                password = password,
                error = error,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange
            )
            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onSubmit,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                shape = RoundedCornerShape(58.dp),
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
                    fontSize = 30.sp
                )
            }
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
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        LoginLineField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "E-Mail",
            leading = { Text("@", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black) }
        )
        Spacer(Modifier.height(26.dp))
        LoginLineField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Password",
            visualTransformation = PasswordVisualTransformation(),
            leading = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)) },
            trailing = {
                Text(
                    "FORGOT",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        )
        error?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoginLineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.width(56.dp), contentAlignment = Alignment.CenterStart) { leading() }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = visualTransformation,
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = Color.White.copy(alpha = 0.36f),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        innerTextField()
                    }
                }
            )
            trailing?.let {
                Spacer(Modifier.width(12.dp))
                Box(contentAlignment = Alignment.CenterEnd) { it() }
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.86f), thickness = 2.dp)
    }
}

@Composable
private fun WelcomeLogo(
    logoBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier
) {
    if (logoBitmap != null) {
        Image(bitmap = logoBitmap, contentDescription = "Poplur", modifier = modifier)
    } else {
        LogoFallback(modifier)
    }
}

@Composable
private fun LogoFallback() {
    LogoFallback(Modifier.size(186.dp))
}

@Composable
private fun LogoFallback(modifier: Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(42.dp)),
        color = Color(0xFF2A1238)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("P", color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Black)
        }
    }
}

private enum class AuthMode { Welcome, Login, SignUp }

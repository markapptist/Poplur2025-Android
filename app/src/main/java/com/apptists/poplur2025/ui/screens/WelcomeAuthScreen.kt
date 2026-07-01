package com.apptists.poplur2025.ui.screens

import android.graphics.BitmapFactory
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.tooling.preview.Preview
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AccountType
import com.apptists.poplur2025.data.BusinessProfile
import com.apptists.poplur2025.data.BusinessVerificationMethod
import com.apptists.poplur2025.data.BusinessVerificationStatus
import com.apptists.poplur2025.ui.ConfettiAnimationFragment
import com.apptists.poplur2025.ui.GdxState
import com.apptists.poplur2025.ui.components.PersistentCubeOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Composable
fun WelcomeAuthScreen(authViewModel: AuthViewModel) {
    WelcomeAuthScreenContent(
        onSignIn = { email, password, onResult ->
            authViewModel.signIn(email, password, onResult)
        },
        onSignUp = { email, password, accountType, businessProfile, onResult ->
            authViewModel.signUp(email, password, accountType, businessProfile, onResult)
        }
    )
}

@Composable
internal fun WelcomeAuthScreenContent(
    initialMode: AuthMode = AuthMode.Welcome,
    onSignIn: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onSignUp: (String, String, AccountType, BusinessProfile?, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(initialMode) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var logoBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    
    LaunchedEffect(Unit) {
        logoBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("logo.png").use { BitmapFactory.decodeStream(it).asImageBitmap() }
            }.getOrNull()
        }
    }

    var welcomeAnimationStarted by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        if (mode == AuthMode.Welcome) {
            welcomeAnimationStarted = false
            delay(100)
            welcomeAnimationStarted = true
        } else {
            welcomeAnimationStarted = false
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

        // Confetti Layer - Only active on Welcome screen
        if (mode == AuthMode.Welcome) {
            val confettiContainerId = remember { android.view.View.generateViewId() }
            AndroidView(
                factory = { ctx ->
                    android.util.Log.d("WelcomeAuthScreen", "Creating confetti layer wrapper")
                    android.widget.FrameLayout(ctx).apply {
                        id = confettiContainerId
                        setBackgroundColor(0)
                        background = null
                        isClickable = false
                        isFocusable = false

                        val fragmentActivity = ctx as? FragmentActivity
                        fragmentActivity?.supportFragmentManager?.beginTransaction()
                            ?.replace(id, ConfettiAnimationFragment())
                            ?.commitAllowingStateLoss()
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.5f),
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
                    onSignIn = { mode = AuthMode.Login },
                    onCreateAccount = { mode = AuthMode.SignUp }
                )
            }
        } else if (mode == AuthMode.Login) {
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
                        onSignIn(email, password) { ok, msg ->
                            loading = false
                            if (!ok) error = msg
                        }
                    }
                )
            }
        } else {
            Box(Modifier.fillMaxSize().zIndex(2f)) {
                BusinessSignupTicketContent(
                    logoBitmap = logoBitmap,
                    error = error,
                    loading = loading,
                    layout = authLayout,
                    onBack = {
                        error = null
                        mode = AuthMode.Welcome
                    },
                    onSubmit = { signupEmail, signupPassword, signupAccountType, businessProfile ->
                        loading = true
                        error = null
                        onSignUp(signupEmail, signupPassword, signupAccountType, businessProfile) { ok, msg ->
                            loading = false
                            if (!ok) error = msg
                        }
                    },
                    onValidationError = { error = it }
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
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit
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

        AuthCubeOverlay(
            label = "Welcome",
            selectedTabIndex = 0,
            enabled = false,
            onTap = {}
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.welcomeButtonTop)
                .graphicsLayer {
                    alpha = buttonProgress
                    scaleX = 0.88f + (0.12f * buttonProgress)
                    scaleY = 0.88f + (0.12f * buttonProgress)
                    translationY = (1f - buttonProgress) * 28f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSignIn,
                modifier = Modifier
                    .width(layout.welcomeButtonWidth)
                    .height(layout.welcomeButtonHeight),
                shape = RoundedCornerShape(layout.welcomeButtonHeight / 2f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp)
            ) {
                Text("Sign In", fontWeight = FontWeight.Black, fontSize = layout.welcomeButtonTextSize.sp)
            }
            OutlinedButton(
                onClick = onCreateAccount,
                modifier = Modifier
                    .width(layout.welcomeButtonWidth)
                    .height((layout.welcomeButtonHeight * 0.86f).coerceAtLeast(50.dp)),
                shape = RoundedCornerShape(layout.welcomeButtonHeight / 2f),
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.86f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Sign Up",
                    fontWeight = FontWeight.Black,
                    fontSize = (layout.welcomeButtonTextSize * 0.82f).sp
                )
            }
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
        WelcomeLogo(
            logoBitmap = logoBitmap,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = layout.loginLogoTop)
                .size(layout.logoSize)
        )

        AuthCubeOverlay(
            label = "Login",
            selectedTabIndex = 0,
            enabled = false,
            onTap = {}
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
    onValidationError: (String?) -> Unit
) {
    var signupStep by rememberSaveable { mutableStateOf(0) }
    var cubeSpinToken by rememberSaveable { mutableStateOf(0) }
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

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = layout.formHorizontalInset)
                .padding(top = 24.dp, bottom = 190.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    if (signupStep == 0) onBack() else stepBack()
                }) {
                    Text("Back", color = Color.White, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.weight(1f))
            }

            WelcomeLogo(
                logoBitmap = logoBitmap,
                modifier = Modifier.size((layout.logoSize * 0.58f).coerceIn(116.dp, 150.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                signupStepTitle(signupStep),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                signupStepSubtitle(signupStep, accountType),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(18.dp))

            if (signupStep == 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SignupAccountTypeCard(
                    title = "User",
                    selected = accountType == AccountType.User,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedAccountType = AccountType.User.wireValue
                    moveToStep(1)
                    onValidationError(null)
                }
                SignupAccountTypeCard(
                    title = "Business",
                    selected = accountType == AccountType.Business,
                    modifier = Modifier.weight(1f)
                ) {
                    selectedAccountType = AccountType.Business.wireValue
                    if (contactEmail.isBlank()) contactEmail = email
                    moveToStep(1)
                    onValidationError(null)
                }
            }

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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text(
                            if (verificationRequested) "Verification Requested" else "Verify My Business",
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (verificationRequested) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                            Text(
                                "I agree that major changes to name, address, or ownership can trigger re-verification.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Choose proof methods",
                            color = Color.White.copy(alpha = 0.78f),
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        BusinessVerificationMethod.entries.forEach { method ->
                            val checked = selectedMethods.contains(method.wireValue)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        setVerificationMethod(method, !checked)
                                    }
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { setVerificationMethod(method, it) }
                                )
                                Text(
                                    method.label,
                                    color = Color.White,
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
                                color = if (active) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (active) Color.White else Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Text(
                        "Approved businesses show a verified badge and unlock business-only features. Rejected businesses can see the reason and resubmit proof.",
                        color = Color.White.copy(alpha = 0.74f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color.White, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !loading,
                onClick = { advanceSignup() },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.72f),
                    disabledContentColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    signupActionTitle(signupStep, accountType, loading),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }

        AuthCubeOverlay(
            label = signupStepTitle(signupStep),
            selectedTabIndex = cubeSpinToken % 4,
            enabled = !loading,
            onTap = { advanceSignup() }
        )
    }
}

@Composable
private fun AuthCubeOverlay(
    label: String,
    selectedTabIndex: Int,
    enabled: Boolean,
    onTap: () -> Unit
) {
    PersistentCubeOverlay(
        selectedTabIndex = selectedTabIndex.coerceIn(0, 3),
        mapCubeFaceTitle = label,
        cameraCubeFaceTitle = label,
        feedCubeFaceTitle = label,
        profileCubeIconTitle = label,
        cubeSkin = GdxState.selectedCubeSkin,
        isInteractive = enabled,
        onTap = onTap,
        onTabSelected = {},
        onVerticalSwipe = {},
        onLongPress = {},
        onLongPressMove = { _, _ -> },
        onLongPressEnd = {}
    )
}

@Composable
private fun SignupAccountTypeCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (selected) Color.Black else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun SignupSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.16f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
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
            Text(placeholder, color = Color.White.copy(alpha = 0.54f), fontWeight = FontWeight.Bold)
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = LocalTextStyle.current.copy(color = Color.White, fontWeight = FontWeight.Bold),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.32f),
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            cursorColor = Color.White
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
            .background(Color.White.copy(alpha = 0.12f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Map location", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
        Text(businessName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(address.ifBlank { "Add an address so Poplur can place this business on the map." }, color = Color.White.copy(alpha = 0.74f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("${latitude.ifBlank { "lat" }}, ${longitude.ifBlank { "lng" }}", color = Color.White.copy(alpha = 0.74f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

internal enum class AuthMode { Welcome, Login, SignUp }

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

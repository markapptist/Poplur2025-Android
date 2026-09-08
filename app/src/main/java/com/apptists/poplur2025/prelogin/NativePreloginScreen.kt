package com.apptists.poplur2025.prelogin

import android.app.DatePickerDialog
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.apptists.poplur2025.PoplurPermissionPrimer
import com.apptists.poplur2025.data.AccountType
import com.apptists.poplur2025.ui.components.PreLoginMusicPlayer
import kotlinx.coroutines.delay
import java.time.LocalDate

private val Cream = Color(0xFFFFF4E3)
private val Ink = Color(0xFF500D2E)
private val Pink = Color(0xFFFF2D96)

@Composable
fun NativePreloginScreen(model: NativePreloginModel, music: PreLoginMusicPlayer, foreground: Boolean, onSignIn: (Boolean) -> Unit, onBiometric: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    var hue by remember { mutableIntStateOf(0) }
    val colors = listOf(Pink, Color(0xFFF53DA1), Color(0xFFE6297D))
    val background = colors[hue]
    var lastInteraction by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var awake by remember { mutableStateOf(true) }
    var primer by remember { mutableStateOf<String?>(null) }
    var enableBiometric by remember { mutableStateOf(false) }
    val formScroll = rememberScrollState()
    fun wake() { lastInteraction = SystemClock.elapsedRealtime(); awake = true }
    fun answerPrimer(kind: String) {
        PoplurPermissionPrimer.markHandled(context, kind)
        primer = if (kind == PoplurPermissionPrimer.LOCATION && !PoplurPermissionPrimer.wasHandled(context, PoplurPermissionPrimer.PHOTOS)) PoplurPermissionPrimer.PHOTOS else null
        wake()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) model.photo = uri }
    LaunchedEffect(model.step) { formScroll.scrollTo(0); wake() }
    LaunchedEffect(Unit) { while (true) { delay(1000); if (SystemClock.elapsedRealtime() - lastInteraction >= 20_000) awake = false } }
    LaunchedEffect(Unit) {
        delay(3500)
        primer = if (!PoplurPermissionPrimer.wasHandled(context, PoplurPermissionPrimer.LOCATION)) PoplurPermissionPrimer.LOCATION else if (!PoplurPermissionPrimer.wasHandled(context, PoplurPermissionPrimer.PHOTOS)) PoplurPermissionPrimer.PHOTOS else null
    }
    BackHandler { if (primer != null) answerPrimer(primer!!) else if (model.step == PreloginStep.Welcome) onExit() else model.back() }
    BoxWithConstraints(Modifier.fillMaxSize().imePadding().testTag("nativePrelogin").pointerInput(Unit) {
        awaitPointerEventScope { while (true) { awaitPointerEvent(); wake() } }
    }) {
        PreloginBackdrop(background, awake && foreground)
        if (model.step == PreloginStep.Welcome) {
            val logoTop = (maxHeight.value * .08f).coerceIn(58f, 92f).dp
            val cubeSize = if (maxHeight > 760.dp) 132.dp else 103.dp
            val bottom = (maxHeight.value * .12f).coerceIn(60f, 132f).dp
            val buttonsTop = maxHeight - bottom - 144.dp
            val gap = ((buttonsTop - logoTop - 132.dp - cubeSize - 133.dp) / 3).coerceAtLeast(14.dp)
            AsyncImage("file:///android_asset/native-prelogin/NativePreloginLogo.png", "Poplur", Modifier.size(132.dp).align(Alignment.TopCenter).offset(y = logoTop))
            PreloginModelArt("poplur_icon_cube", Modifier.size(cubeSize).align(Alignment.TopCenter).offset(y = logoTop + 132.dp + gap), background, welcome = true, awake = awake && foreground, onTouch = { wake() })
            Column(Modifier.align(Alignment.TopCenter).offset(y = logoTop + 132.dp + gap * 2 + cubeSize).widthIn(max = 380.dp).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Heading("Welcome to Poplur!")
                Text("Post. Explore. Connect.\nExpand your world.", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Cream, textAlign = TextAlign.Center)
                Text(music.currentAIScript, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cream, textAlign = TextAlign.Center)
            }
            Column(Modifier.align(Alignment.TopCenter).offset(y = buttonsTop), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                ClayButton("Sign In", "welcomeSignIn") { model.go(PreloginStep.SignIn) }
                ClayButton("Sign Up", "welcomeSignUp") { model.go(PreloginStep.Account) }
            }
        } else {
            Column(Modifier.fillMaxWidth().verticalScroll(formScroll).padding(top = 72.dp, bottom = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (model.step != PreloginStep.Intro) AsyncImage("file:///android_asset/native-prelogin/NativePreloginLogo.png", null, Modifier.size(112.dp))
                PreloginModelArt("poplur_icon_cube", Modifier.size(if (model.step == PreloginStep.Intro) 74.dp else 103.dp).semantics { contentDescription = "Back" }.testTag("preloginBack"), background, awake = awake && foreground, onBack = model::back, onTouch = { wake() })
                Column(Modifier.widthIn(max = 378.dp).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val editable = !model.busy && model.createdUID == null
                    when (model.step) {
                        PreloginStep.Welcome -> Unit
                        PreloginStep.SignIn -> {
                            Heading("Sign in")
                            ClayButton("Sign in with biometrics", "biometricSignIn", editable, onBiometric)
                            Subtitle("or sign in with email")
                            Field("E-Mail", model.email, editable, KeyboardType.Email, tag = "preloginEmail") { model.email = it }
                            Field("Password", model.password, editable, KeyboardType.Password, tag = "signInPassword") { model.password = it }
                            TextButton(onClick = model::resetPassword, enabled = editable) { Text("FORGOT", color = Cream, fontWeight = FontWeight.Black) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(enableBiometric, { enableBiometric = it }, enabled = editable); Text("Enable biometric sign-in on this device", color = Cream, fontSize = 13.sp) }
                            ClayButton("Sign In", "submitSignIn", editable) { onSignIn(enableBiometric) }
                            Link("New here? Create an account", editable) { model.go(PreloginStep.Account) }
                        }
                        PreloginStep.Account -> {
                            Heading("Create your Poplur"); Subtitle("Choose your account type.")
                            Choice("Personal", "Share with friends inside your radius", "MarkHead_TalkingRig", model.accountType == AccountType.User, editable, foreground) { if (model.accountType != AccountType.User) { model.photo = null; model.gender = "" }; model.accountType = AccountType.User }
                            Choice("Business", "Reach locals and promote your spot", "zoom_buildings", model.accountType == AccountType.Business, editable, foreground) { if (model.accountType != AccountType.Business) { model.photo = null; model.gender = "" }; model.accountType = AccountType.Business }
                            ClayButton("Create account", "chooseAccount", editable) { model.go(PreloginStep.Intro) }
                            Link("Have an account? Sign in", editable) { model.go(PreloginStep.SignIn) }
                        }
                        PreloginStep.Intro -> {
                            PreloginModelArt(if (model.accountType == AccountType.Business) "zoom_buildings" else "MarkHead_TalkingRig", Modifier.size(96.dp).clip(RoundedCornerShape(13.dp)), Cream, thumbnail = true, awake = foreground)
                            Heading(if (model.accountType == AccountType.Business) "Business" else "Personal")
                            Subtitle("What a ${if (model.accountType == AccountType.Business) "business" else "personal"} account does.")
                            val lines = if (model.accountType == AccountType.Business) listOf("Your radius is anchored to your address, not your phone — it stays where your door is.", "Your doorstep stays covered even on a quiet week, so people outside can always find you.", "Reach locals actually within range, and post offers and bookings straight from your spot.", "You see who you reached as numbers, never as individuals.") else listOf("Your radius follows you. Everything you post radiates out from wherever you were standing.", "Radius decays. Keep posting and it grows; go quiet and it shrinks back.", "Take over ground on the map, one pop at a time — the goal is a radius the size of the world.", "You are discovered by interception: people inside your circle see you, and you see theirs.")
                            lines.forEach { Text("•  $it", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Cream, modifier = Modifier.fillMaxWidth()) }
                            ClayButton("Next", "introNext", editable) { model.go(PreloginStep.Credentials) }
                        }
                        PreloginStep.Credentials -> {
                            Heading("Create login"); Subtitle("Then continue with onboarding.")
                            Field("E-Mail", model.email, editable, KeyboardType.Email, tag = "preloginEmail") { model.email = it }
                            Field("Password", model.password, editable, KeyboardType.Password, tag = "signupPassword") { model.password = it }
                            Field("Confirm password", model.confirmation, editable, KeyboardType.Password, tag = "confirmPassword") { model.confirmation = it }
                            OutlinedButton(onClick = {
                                val date = model.birthday ?: LocalDate.now().minusYears(18)
                                DatePickerDialog(context, { _, y, m, d -> model.birthday = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                            }, enabled = editable) { Text(model.birthday?.toString() ?: "Choose date of birth", color = Cream) }
                            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(model.agreed, { model.agreed = it }, enabled = editable, modifier = Modifier.testTag("agreeTerms")); Text("I agree to the Terms of Service", color = Cream, fontSize = 13.sp) }
                            ClayButton("Create account", "credentialsNext", editable, model::advanceCredentials)
                        }
                        PreloginStep.Photo -> {
                            Heading("Add a profile photo"); Subtitle("Show your radius who you are.")
                            Box(Modifier.size(120.dp).clip(CircleShape).background(Cream).clickable(enabled = editable) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                                if (model.photo != null) AsyncImage(model.photo, "Selected profile photo", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) else Text("+", color = Pink, fontSize = 44.sp, fontWeight = FontWeight.Black)
                            }
                            Link("Upload photo", editable) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            ClayButton("Continue", "photoNext", editable) { if (model.photo == null) model.error = "Add a profile photo to continue." else model.go(PreloginStep.Gender) }
                        }
                        PreloginStep.Gender -> {
                            Heading("Is your cube male or female?"); Subtitle("Pick the clay head your profile cube wears.")
                            Choice("Male", "The original mark head", "poplur_mark_head_navigator", model.gender == "male", editable, foreground) { model.gender = "male"; model.submit() }
                            Choice("Female", "The female head", "poplur_female_head_navigator", model.gender == "female", editable, foreground) { model.gender = "female"; model.submit() }
                            Subtitle("You can change this any time in Settings · Profile.")
                        }
                        PreloginStep.Business -> {
                            Heading("Find your business"); Subtitle("Search the map and select your spot.")
                            Field("Business name, address and city", model.query, editable) { model.query = it }
                            Link("Search", editable, model::search)
                            if (model.searching) CircularProgressIndicator(color = Cream)
                            model.places.forEach { place -> TextButton(onClick = { model.place = place; model.places = emptyList() }, enabled = editable) { Text(place.address, color = Cream) } }
                            model.place?.let { Text("${it.name}\n${it.address}", color = Cream) }
                            model.photo?.let { AsyncImage(it, "Business logo", Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))) }
                            Link(if (model.photo == null) "Upload logo (optional)" else "Change logo", editable) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            Field("Category", model.category, editable) { model.category = it }
                            ClayButton("Continue", "businessNext", editable) { if (model.place == null || model.category.isBlank()) model.error = "Select your business and enter its category." else model.go(PreloginStep.Verification) }
                        }
                        PreloginStep.Verification -> {
                            Heading("Verify business"); Subtitle("Provide your business contact and ownership details for review.")
                            Field("Business phone (optional)", model.phone, editable, KeyboardType.Phone) { model.phone = it }
                            Field("Your role and proof of ownership", model.proof, editable, multiline = true) { model.proof = it }
                            Subtitle("Verification is pending until your submission is reviewed.")
                            ClayButton("Continue", "verificationNext", editable) { if (model.proof.isBlank()) model.error = "Add your ownership details to continue." else model.go(PreloginStep.Review) }
                        }
                        PreloginStep.Review -> {
                            Heading("Review & submit"); Subtitle("Confirm before submitting for review.")
                            listOf(model.place?.name.orEmpty(), model.category, model.place?.address.orEmpty(), model.email).forEach { Text(it, color = Ink, modifier = Modifier.fillMaxWidth().background(Cream, RoundedCornerShape(14.dp)).padding(14.dp)) }
                            ClayButton("Submit for review", "submitBusiness", editable, model::submit)
                        }
                    }
                    model.error?.let { Text(it, color = Color.White, fontSize = 14.sp, modifier = Modifier.testTag("preloginError").background(Color.Black.copy(alpha = .65f), RoundedCornerShape(16.dp)).padding(12.dp)) }
                    if (model.busy) CircularProgressIndicator(color = Cream)
                    if (model.createdUID != null && !model.busy) ClayButton("Retry", "retryOnboarding", action = model::submit)
                }
            }
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = music::toggleMute, modifier = Modifier.semantics { contentDescription = if (music.isMuted) "Unmute local radio" else "Mute local radio" }) { Icon(if (music.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, contentDescription = null, tint = Cream, modifier = Modifier.size(28.dp)) }
            Box(Modifier.size(34.dp).shadow(4.dp, CircleShape).background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)), CircleShape).border(2.dp, Cream, CircleShape).clickable { hue = (hue + 1) % colors.size }.padding(8.dp).background(background, CircleShape).semantics { contentDescription = "Change background colour" })
        }
        primer?.let { kind ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .25f)).clickable {}, contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = 310.dp).shadow(20.dp, RoundedCornerShape(28.dp)).background(Cream, RoundedCornerShape(28.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(if (kind == PoplurPermissionPrimer.LOCATION) "Allow “Poplur” to use your location?" else "Allow “Poplur” to access your camera roll?", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink, textAlign = TextAlign.Center)
                    Text(if (kind == PoplurPermissionPrimer.LOCATION) "Your location powers nearby posts, map positioning, and the radius around you." else "Share your favourite moments with people inside your radius.", fontSize = 14.sp, color = Ink, textAlign = TextAlign.Center)
                    if (kind == PoplurPermissionPrimer.LOCATION) { TextButton(onClick = { answerPrimer(kind) }) { Text("Allow Once") }; TextButton(onClick = { answerPrimer(kind) }) { Text("Allow While Using App") } } else TextButton(onClick = { answerPrimer(kind) }) { Text("Allow") }
                    TextButton(onClick = { answerPrimer(kind) }) { Text("Don’t Allow") }
                }
            }
        }
    }
    model.notice?.let { message -> AlertDialog(onDismissRequest = { model.notice = null }, title = { Text("Poplur") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { model.notice = null }) { Text("OK") } }) }
}

@Composable private fun Heading(text: String) { Text(text, color = Cream, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = .3f), androidx.compose.ui.geometry.Offset(0f, 3f), 2f))) }
@Composable private fun Subtitle(text: String) { Text(text, color = Cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
@Composable private fun Link(text: String, enabled: Boolean, action: () -> Unit) { TextButton(onClick = action, enabled = enabled) { Text(text, color = Cream, fontWeight = FontWeight.Bold, fontSize = 14.sp) } }
@Composable private fun ClayButton(text: String, tag: String, enabled: Boolean = true, action: () -> Unit) {
    Box(Modifier.size(250.dp, 58.dp).testTag(tag).shadow(6.dp, CircleShape).background(Brush.verticalGradient(listOf(Color.White, Color.White, Color(0xFFFFEEF8), Color(0xFFE2E0E7))), CircleShape).border(2.5.dp, Pink, CircleShape).clip(CircleShape).clickable(enabled = enabled, onClick = action), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, style = TextStyle(brush = Brush.verticalGradient(listOf(Color.Black, Ink, Pink, Ink, Color.Black))))
    }
}
@Composable private fun Field(label: String, value: String, enabled: Boolean, keyboard: KeyboardType = KeyboardType.Text, tag: String = label, multiline: Boolean = false, onValue: (String) -> Unit) {
    TextField(value, onValue, enabled = enabled, placeholder = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag(tag).shadow(3.dp, RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard), singleLine = !multiline, minLines = if (multiline) 3 else 1, visualTransformation = if (keyboard == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None, colors = TextFieldDefaults.colors(focusedContainerColor = Cream, unfocusedContainerColor = Cream, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Ink, unfocusedTextColor = Ink))
}
@Composable private fun Choice(title: String, detail: String, asset: String, selected: Boolean, enabled: Boolean, foreground: Boolean, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)).background(Cream, RoundedCornerShape(20.dp)).border(3.dp, if (selected) Color.White else Pink.copy(alpha = .5f), RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable(enabled = enabled, onClick = action).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PreloginModelArt(asset, Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)), Cream, thumbnail = true, awake = foreground)
        Column(Modifier.weight(1f)) { Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 20.sp); Text(detail, color = Ink, fontSize = 12.sp) }
        if (selected) Text("✓", color = Pink, fontWeight = FontWeight.Black)
    }
}

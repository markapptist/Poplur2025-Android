package com.apptists.poplur2025.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.R
import com.apptists.poplur2025.PoplurLiteTier
import com.apptists.poplur2025.data.FeedAdProbability
import com.apptists.poplur2025.data.FeedAdProbabilityConfig
import com.apptists.poplur2025.data.PoplurLanguage
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.SoftPink
import com.apptists.poplur2025.ui.appIconResId
import com.apptists.poplur2025.ui.components.CubeMenuLayout
import com.apptists.poplur2025.ui.profile.ProfileTheme
import com.apptists.poplur2025.ui.profile.ProfileThemes
import com.apptists.poplur2025.ui.rememberDrawableBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Settings — matches the supplied Settings mockup (01_settings.png): soft-pink page,
 * sectioned white cards with the 3D pink icon set, cube-skin picker and a pill Sign Out.
 * Android-only preference controls that have no mockup equivalent live under a PREFERENCES
 * card in the same visual language so nothing functional is lost.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    onOpenCubeDebug: () -> Unit = {},
    preferredLanguage: PoplurLanguage = PoplurLanguage.English,
    closedCaptionsEnabled: Boolean = true,
    onPreferredLanguageChange: (PoplurLanguage) -> Unit = {},
    onClosedCaptionsChange: (Boolean) -> Unit = {},
    selectedCubeSkin: CubeSkin = CubeSkin.Default,
    onCubeSkinChange: (CubeSkin) -> Unit = {},
    selectedProfileTheme: ProfileTheme = ProfileThemes.Default,
    onProfileThemeChange: (ProfileTheme) -> Unit = {},
    cubeMenuLayout: CubeMenuLayout = CubeMenuLayout.Vertical,
    onCubeMenuLayoutChange: (CubeMenuLayout) -> Unit = {},
    isDarkMode: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {},
    profileAvatarIs3D: Boolean = false,
    onProfileAvatarIs3DChange: (Boolean) -> Unit = {},
    tiltControlEnabled: Boolean = false,
    onTiltControlChange: (Boolean) -> Unit = {}
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("poplur_settings", Context.MODE_PRIVATE) }

    var limitVideos by remember { mutableStateOf(prefs.getBoolean("poplur_limitVideos10s", false)) }
    var phoneXLMode by remember { mutableStateOf(prefs.getBoolean("poplur_phoneXLMode", true)) }
    var tvGuideFlipAnimation by remember { mutableStateOf(prefs.getBoolean("tv_guide_flip_animation", true)) }
    var customMapTheme by remember { mutableStateOf(prefs.getBoolean("poplur_customMapTheme", false)) }
    var use3DLoadingScreen by remember { mutableStateOf(prefs.getBoolean("poplur_use3DLoadingScreen", true)) }
    var use3DMap by remember {
        mutableStateOf(
            if (prefs.contains("poplur_use3DMap")) {
                prefs.getBoolean("poplur_use3DMap", !PoplurLiteTier.isLowTier(ctx))
            } else {
                !PoplurLiteTier.isLowTier(ctx)
            }
        )
    }
    var cacheText by remember { mutableStateOf("—") }
    var adProbabilityConfig by remember { mutableStateOf(FeedAdProbability.currentConfig(ctx)) }

    var infoTitle by remember { mutableStateOf("") }
    var infoBody by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val bytes = runCatching { ctx.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() }.getOrDefault(0L)
        val mb = bytes / (1024.0 * 1024.0)
        cacheText = if (mb < 0.1) "0 MB" else String.format("%.1f MB", mb)
    }

    val displayedSkins = listOf(
        CubeSkin.Default to "Default",
        CubeSkin.Rainbow to "Rainbow",
        CubeSkin.Carbon to "Sparkle Pink",
        CubeSkin.Gold to "Gold"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(SoftPink.pageBG)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)) {
                Text(
                    "Settings",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = SoftPink.title,
                    modifier = Modifier.align(Alignment.Center)
                )
                Image(
                    painter = painterResource(R.drawable.settings_ic_bell),
                    contentDescription = "Notifications",
                    modifier = Modifier.align(Alignment.CenterEnd).size(58.dp)
                )
            }

            SectionLabel("VIDEO SETTINGS")
            SettingsCard {
                IconRow(
                    icon = R.drawable.settings_ic_film,
                    title = "Limit videos to 10 seconds",
                    subtitle = "Current limit: ${if (limitVideos) 10 else 15}s"
                ) {
                    Switch(
                        checked = limitVideos,
                        onCheckedChange = {
                            limitVideos = it
                            prefs.edit().putBoolean("poplur_limitVideos10s", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SoftPink.accentSoft,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE7DCE3)
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("SIMULATOR")
            SettingsCard {
                NavRow(icon = R.drawable.settings_ic_flask, title = "Seed NA (1000)") {}
                RowDivider()
                NavRow(
                    icon = R.drawable.settings_ic_play,
                    title = "Start Simulator",
                    subtitle = "Dev mode: 120s/day"
                ) {}
            }
            Spacer(Modifier.height(12.dp))
            AccentPillButton(text = "Stop Simulator", leadingSquare = true) {}

            Spacer(Modifier.height(24.dp))
            SectionLabel("ACCOUNT & DATA")
            SettingsCard {
                NavRow(icon = R.drawable.settings_ic_wrench, title = "Fix Empty UserIDs") {}
                RowDivider()
                NavRow(icon = R.drawable.settings_ic_cloud, title = "Back Up Data") {}
                RowDivider()
                NavRow(
                    icon = R.drawable.settings_ic_trash,
                    title = "Clear Cache",
                    trailingText = cacheText
                ) {
                    runCatching { ctx.cacheDir.deleteRecursively() }
                    cacheText = "0 MB"
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("CUBE SKIN")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                displayedSkins.forEach { (skin, label) ->
                    SkinTile(
                        skin = skin,
                        label = label,
                        isSelected = selectedCubeSkin == skin,
                        modifier = Modifier.weight(1f)
                    ) {
                        onCubeSkinChange(skin)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("PROFILE BACKGROUND")
            Text(
                "Choose a fixed background for your profile. This setting is separate from Cube Skin.",
                color = SoftPink.subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 12.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
            ) {
                items(ProfileThemes.All, key = ProfileTheme::id) { profileTheme ->
                    ProfileThemeTile(
                        theme = profileTheme,
                        isSelected = selectedProfileTheme.id == profileTheme.id,
                        onClick = { onProfileThemeChange(profileTheme) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("ABOUT")
            SettingsCard {
                NavRow(icon = R.drawable.settings_ic_info, title = "About Poplur") {
                    infoTitle = "About Poplur"
                    infoBody = "Poplur — share moments pinned to the world around you."
                    showInfo = true
                }
                RowDivider()
                NavRow(icon = R.drawable.settings_ic_doc, title = "Terms of Service") {
                    infoTitle = "Terms of Service"
                    infoBody = "By using Poplur you agree to our Terms of Service and Privacy Policy."
                    showInfo = true
                }
            }

            // Android-only functional controls, same visual language.
            Spacer(Modifier.height(24.dp))
            SectionLabel("PREFERENCES")
            SettingsCard {
                ToggleRow("XL Phone", phoneXLMode) {
                    phoneXLMode = it; prefs.edit().putBoolean("poplur_phoneXLMode", it).apply()
                }
                RowDivider()
                ToggleRow("TV Guide Flip Animation", tvGuideFlipAnimation) {
                    tvGuideFlipAnimation = it; prefs.edit().putBoolean("tv_guide_flip_animation", it).apply()
                }
                RowDivider()
                ToggleRow("Custom Map Theme", customMapTheme) {
                    customMapTheme = it; prefs.edit().putBoolean("poplur_customMapTheme", it).apply()
                }
                RowDivider()
                ToggleRow("3D Poplur Map", use3DMap) {
                    use3DMap = it; prefs.edit().putBoolean("poplur_use3DMap", it).apply()
                }
                RowDivider()
                ToggleRow("3D Loading Screen", use3DLoadingScreen) {
                    use3DLoadingScreen = it; prefs.edit().putBoolean("poplur_use3DLoadingScreen", it).apply()
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingsCard {
                LanguagePickerRow(preferredLanguage, onPreferredLanguageChange)
            }
            Spacer(Modifier.height(14.dp))
            SettingsCard {
                FeedAdControls(adProbabilityConfig) { config ->
                    val normalized = config.normalized
                    adProbabilityConfig = normalized
                    FeedAdProbability.saveConfig(ctx, normalized)
                }
            }
            Spacer(Modifier.height(14.dp))
            SettingsCard {
                NavRow(icon = R.drawable.settings_ic_wrench, title = "Launch Cube Debug") { onOpenCubeDebug() }
            }

            Spacer(Modifier.height(22.dp))
            SignOutButton { onSignOut?.invoke() }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Close", color = SoftPink.accent, fontWeight = FontWeight.Black)
                }
            },
            title = { Text(infoTitle, fontWeight = FontWeight.Black, color = SoftPink.title) },
            text = { Text(infoBody, color = SoftPink.rowTitle) },
            containerColor = SoftPink.card
        )
    }
}

// MARK: - Building blocks

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        color = SoftPink.sectionLbl,
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 12.dp),
        textAlign = TextAlign.Start
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(SoftPink.card),
        content = content
    )
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .padding(start = 84.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(SoftPink.divider)
    )
}

@Composable
private fun IconRow(
    icon: Int,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = SoftPink.rowTitle)
            if (subtitle != null) {
                Text(subtitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SoftPink.subtitle)
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun NavRow(
    icon: Int,
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth().clickable { onClick() }) {
        IconRow(icon = icon, title = title, subtitle = subtitle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailingText != null) {
                    Text(trailingText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SoftPink.subtitle)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SoftPink.chevron,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SoftPink.rowTitle, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SoftPink.accentSoft,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE7DCE3)
            )
        )
    }
}

@Composable
private fun AccentPillButton(text: String, leadingSquare: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SoftPink.accent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), clip = false)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingSquare) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
                Spacer(Modifier.width(12.dp))
            }
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}

@Composable
private fun SkinTile(
    skin: CubeSkin,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(3.dp)
                .shadow(8.dp, RoundedCornerShape(21.dp), clip = false)
                .clip(RoundedCornerShape(21.dp))
                .background(SoftPink.card)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        if (isSelected) 3.dp else 0.dp,
                        if (isSelected) SoftPink.accent else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                rememberDrawableBitmap(skin.appIconResId)?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = "$label skin",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
                    )
                }
            }
            if (isSelected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SoftPink.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) SoftPink.accent else SoftPink.subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileThemeTile(
    theme: ProfileTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val previewBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = theme.backgroundResId
    ) {
        value = withContext(Dispatchers.IO) {
            theme.backgroundResId?.let { resourceId ->
                runCatching {
                    BitmapFactory.decodeResource(
                        context.resources,
                        resourceId,
                        BitmapFactory.Options().apply { inSampleSize = 8 }
                    )?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    val tileShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .width(92.dp)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = "${theme.displayName} profile background"
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(116.dp)
                .shadow(8.dp, tileShape, clip = false)
                .clip(tileShape)
                .background(
                    if (theme.isDefault) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.White, SoftPink.pageBG)
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(theme.primaryAccent, theme.secondaryAccent)
                        )
                    }
                )
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) SoftPink.accent else Color.White.copy(alpha = 0.7f),
                    shape = tileShape
                ),
            contentAlignment = Alignment.Center
        ) {
            previewBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (theme.isDefault) {
                Text("P", color = SoftPink.accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            if (isSelected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SoftPink.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            theme.displayName,
            color = if (isSelected) SoftPink.accent else SoftPink.subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SoftPink.signOutBG,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(22.dp), clip = false)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.settings_ic_signout),
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text("Sign Out", fontSize = 20.sp, fontWeight = FontWeight.Black, color = SoftPink.accent)
        }
    }
}

@Composable
private fun LanguagePickerRow(selected: PoplurLanguage, onSelected: (PoplurLanguage) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Preferred Language", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SoftPink.rowTitle)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SoftPink.pageBG).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PoplurLanguage.entries.forEach { lang ->
                val isSel = lang == selected
                Surface(
                    onClick = { onSelected(lang) },
                    color = if (isSel) SoftPink.accent else Color.Transparent,
                    contentColor = if (isSel) Color.White else SoftPink.subtitle,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "${lang.flag} ${lang.shortLabel}",
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedAdControls(config: FeedAdProbabilityConfig, onChange: (FeedAdProbabilityConfig) -> Unit) {
    val n = config.normalized
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Feed Ads", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SoftPink.rowTitle)
        AdSlider("Poplur Ads", n.poplurAdPercent, SoftPink.accent) {
            onChange(FeedAdProbabilityConfig(poplurAdPercent = it, googleMobileAdPercent = n.googleMobileAdPercent))
        }
        AdSlider("Google Mobile Ads", n.googleMobileAdPercent, Color(0xFF68C34F)) {
            onChange(FeedAdProbabilityConfig(poplurAdPercent = n.poplurAdPercent, googleMobileAdPercent = it))
        }
        Text(
            "Normal feed content: ${n.contentPercent.roundToInt()}%",
            color = SoftPink.subtitle, fontSize = 12.sp, fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun AdSlider(label: String, value: Float, accent: Color, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Black, fontSize = 14.sp, color = SoftPink.rowTitle)
            Text("${value.roundToInt()}%", fontWeight = FontWeight.Black, fontSize = 14.sp, color = accent)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = SoftPink.divider
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme { SettingsScreen() }
}

package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.apptists.poplur2025.data.PoplurLanguage
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import coil.compose.AsyncImage
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.components.CubeMenuLayout
import com.apptists.poplur2025.ui.skinBackdropBrush
import com.apptists.poplur2025.ui.uiTheme

private val LocalSettingsTheme = staticCompositionLocalOf { CubeSkin.Default.uiTheme() }

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
    cubeMenuLayout: CubeMenuLayout = CubeMenuLayout.Horizontal,
    onCubeMenuLayoutChange: (CubeMenuLayout) -> Unit = {},
    isDarkMode: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {},
    profileAvatarIs3D: Boolean = false,
    onProfileAvatarIs3DChange: (Boolean) -> Unit = {},
    tiltControlEnabled: Boolean = false,
    onTiltControlChange: (Boolean) -> Unit = {}
) {
    val cubeDebugCallback by rememberUpdatedState(onOpenCubeDebug)
    var previewSkin by remember { mutableStateOf(selectedCubeSkin) }
    val theme = selectedCubeSkin.uiTheme()

    LaunchedEffect(selectedCubeSkin) {
        previewSkin = selectedCubeSkin
    }

    CompositionLocalProvider(LocalSettingsTheme provides theme) {
        Surface(
            color = theme.background,
            contentColor = theme.primaryText,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(skinBackdropBrush(selectedCubeSkin))
            ) {
                Column(Modifier.statusBarsPadding()) {
                    Text(
                        "Settings",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = theme.primaryText,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 22.dp, bottom = 4.dp)
                    )
                    SettingsTab(
                        onSignOut = onSignOut,
                        onOpenCubeDebug = cubeDebugCallback,
                        preferredLanguage = preferredLanguage,
                        closedCaptionsEnabled = closedCaptionsEnabled,
                        onPreferredLanguageChange = onPreferredLanguageChange,
                        onClosedCaptionsChange = onClosedCaptionsChange,
                        selectedCubeSkin = selectedCubeSkin,
                        previewSkin = previewSkin,
                        onPreviewSkinChange = { previewSkin = it },
                        onCubeSkinChange = onCubeSkinChange,
                        cubeMenuLayout = cubeMenuLayout,
                        onCubeMenuLayoutChange = onCubeMenuLayoutChange,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        profileAvatarIs3D = profileAvatarIs3D,
                        onProfileAvatarIs3DChange = onProfileAvatarIs3DChange,
                        tiltControlEnabled = tiltControlEnabled,
                        onTiltControlChange = onTiltControlChange
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    onSignOut: (() -> Unit)?,
    onOpenCubeDebug: () -> Unit,
    preferredLanguage: PoplurLanguage,
    closedCaptionsEnabled: Boolean,
    onPreferredLanguageChange: (PoplurLanguage) -> Unit,
    onClosedCaptionsChange: (Boolean) -> Unit,
    selectedCubeSkin: CubeSkin,
    previewSkin: CubeSkin,
    onPreviewSkinChange: (CubeSkin) -> Unit,
    onCubeSkinChange: (CubeSkin) -> Unit,
    cubeMenuLayout: CubeMenuLayout,
    onCubeMenuLayoutChange: (CubeMenuLayout) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    profileAvatarIs3D: Boolean = false,
    onProfileAvatarIs3DChange: (Boolean) -> Unit = {},
    tiltControlEnabled: Boolean = false,
    onTiltControlChange: (Boolean) -> Unit = {}
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("poplur_settings", Context.MODE_PRIVATE) }
    var phoneXLMode by remember { mutableStateOf(prefs.getBoolean("poplur_phoneXLMode", true)) }
    var tvGuideFlipAnimation by remember { mutableStateOf(prefs.getBoolean("tv_guide_flip_animation", true)) }
    val theme = LocalSettingsTheme.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Account Options",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = theme.primaryText,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        SettingsToggleRow("Dark Mode", isDarkMode, onDarkModeChange)
        SettingsToggleRow("Private Account", false) {}
        SettingsToggleRow("Notifications", true) {}
        SettingsToggleRow("Closed Captions", closedCaptionsEnabled, onClosedCaptionsChange)
        SettingsToggleRow("XL Phone", phoneXLMode) { value ->
            phoneXLMode = value
            prefs.edit().putBoolean("poplur_phoneXLMode", value).apply()
        }
        SettingsToggleRow("TV Guide Flip Animation", tvGuideFlipAnimation) { value ->
            tvGuideFlipAnimation = value
            prefs.edit().putBoolean("tv_guide_flip_animation", value).apply()
        }
        SettingsToggleRow("Tilt Control", tiltControlEnabled, onTiltControlChange)
        SettingsProfileHeadModePicker(
            is3D = profileAvatarIs3D,
            onModeSelected = onProfileAvatarIs3DChange
        )

        SettingsLanguagePicker(
            selectedLanguage = preferredLanguage,
            onLanguageSelected = onPreferredLanguageChange
        )

        SettingsHoldMenuLayoutPicker(
            selectedLayout = cubeMenuLayout,
            onLayoutSelected = onCubeMenuLayoutChange
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Skin Wrapper",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = theme.primaryText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        SkinPreviewPanel(previewSkin = previewSkin)

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(CubeSkin.entries) { skin ->
                SkinOption(
                    skin = skin,
                    isSelected = previewSkin == skin,
                    onClick = { onPreviewSkinChange(skin) }
                )
            }
        }

        DesignButton(
            text = "Apply ${previewSkin.label} Skin",
            containerColor = if (selectedCubeSkin == previewSkin) theme.secondaryText.copy(alpha = 0.42f) else previewSkin.uiTheme().accent,
            onClick = { onCubeSkinChange(previewSkin) }
        )

        SettingsActionRow("Email Address") {}
        SettingsActionRow("Password") {}

        Spacer(Modifier.height(32.dp))

        DesignButton("Launch Cube Debug", containerColor = theme.accent) {
            onOpenCubeDebug()
        }

        DesignButton("Limit videos to 10 seconds", subtext = "Current limit: 15s", containerColor = theme.controlFill) {}
        DesignButton("Seed NA (1000)", containerColor = theme.controlFill) {}
        DesignButton("Start Simulator (dev: 120s/day)", containerColor = theme.controlFill) {}
        
        DesignButton(
            "Fix Empty UserIDs",
            icon = { Icon(Icons.Default.Build, null, Modifier.size(18.dp)) },
            containerColor = Color(0xFFF09E5A)
        ) {}
        
        DesignButton(
            "Stop Simulator",
            containerColor = Color(0xFFEB5757)
        ) {}

        if (onSignOut != null) {
            TextButton(onClick = onSignOut, modifier = Modifier.padding(vertical = 16.dp)) {
                Text("Log Out", color = theme.secondaryText, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsProfileHeadModePicker(
    is3D: Boolean,
    onModeSelected: (Boolean) -> Unit
) {
    val theme = LocalSettingsTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Profile Head", fontWeight = FontWeight.Black, fontSize = 18.sp, color = theme.primaryText)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.controlFill)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(false to "2D", true to "3D").forEach { (modeIs3D, label) ->
                val isSelected = modeIs3D == is3D
                Surface(
                    onClick = { onModeSelected(modeIs3D) },
                    color = if (isSelected) theme.surfaceStrong else Color.Transparent,
                    contentColor = if (isSelected) theme.primaryText else theme.secondaryText,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 10.dp),
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            "Choose whether your profile uses the 2D speaking head or the 3D head.",
            color = theme.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsLanguagePicker(
    selectedLanguage: PoplurLanguage,
    onLanguageSelected: (PoplurLanguage) -> Unit
) {
    val theme = LocalSettingsTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Preferred Language", fontWeight = FontWeight.Black, fontSize = 18.sp, color = theme.primaryText)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.controlFill)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PoplurLanguage.entries.forEach { language ->
                val isSelected = language == selectedLanguage
                Surface(
                    onClick = { onLanguageSelected(language) },
                    color = if (isSelected) theme.surfaceStrong else Color.Transparent,
                    contentColor = if (isSelected) theme.primaryText else theme.secondaryText,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${language.flag} ${language.shortLabel}",
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
private fun SettingsHoldMenuLayoutPicker(
    selectedLayout: CubeMenuLayout,
    onLayoutSelected: (CubeMenuLayout) -> Unit
) {
    val theme = LocalSettingsTheme.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Hold Menu", fontWeight = FontWeight.Black, fontSize = 18.sp, color = theme.primaryText)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(theme.controlFill)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CubeMenuLayout.entries.forEach { layout ->
                val isSelected = layout == selectedLayout
                Surface(
                    onClick = { onLayoutSelected(layout) },
                    color = if (isSelected) theme.surfaceStrong else Color.Transparent,
                    contentColor = if (isSelected) theme.primaryText else theme.secondaryText,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = layout.name,
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
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val theme = LocalSettingsTheme.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 18.sp, color = theme.primaryText)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = theme.accent
            )
        )
    }
}

@Composable
private fun SettingsActionRow(label: String, onClick: () -> Unit) {
    val theme = LocalSettingsTheme.current
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 18.sp, color = theme.primaryText)
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = theme.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DesignButton(
    text: String,
    subtext: String? = null,
    containerColor: Color = Color.White,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val theme = LocalSettingsTheme.current
    val resolvedTextColor = if (containerColor == theme.controlFill || containerColor == Color.White) {
        theme.primaryText
    } else {
        theme.badgeText
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
        Surface(
            onClick = onClick,
            color = containerColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(2.dp, RoundedCornerShape(24.dp))
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = resolvedTextColor
                )
            }
        }
        if (subtext != null) {
            Text(
                text = subtext,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = theme.secondaryText,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun SkinPreviewPanel(previewSkin: CubeSkin) {
    val previewTheme = previewSkin.uiTheme()
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = 12.dp)
            .shadow(14.dp, shape, clip = false)
            .clip(shape)
            .background(skinBackdropBrush(previewSkin))
            .border(1.5.dp, previewTheme.border, shape),
        contentAlignment = Alignment.Center
    ) {
        previewSkin.assetPath?.let { assetPath ->
            AsyncImage(
                model = "file:///android_asset/$assetPath",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .padding(28.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (previewTheme.isLight) 0.12f else 0.06f),
                            previewTheme.backgroundDeep.copy(alpha = if (previewTheme.isLight) 0.04f else 0.44f)
                        )
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
                    .clip(RoundedCornerShape(24.dp))
                    .background(previewTheme.surfaceStrong)
                    .border(2.5.dp, previewTheme.accent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewSkin.assetPath?.let { "file:///android_asset/$it" },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                previewSkin.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = previewTheme.primaryText
            )
        }
    }
}

@Composable
private fun SkinOption(
    skin: CubeSkin,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val theme = LocalSettingsTheme.current
    val skinTheme = skin.uiTheme()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(skinBackdropBrush(skin))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) skinTheme.accent else skinTheme.border.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            skin.assetPath?.let { assetPath ->
                AsyncImage(
                    model = "file:///android_asset/$assetPath",
                    contentDescription = skin.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isSelected) {
                Surface(shape = CircleShape, color = skinTheme.badgeFill) {
                    Text(
                        "✓",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = skinTheme.badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            skin.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) skinTheme.accent else theme.secondaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}

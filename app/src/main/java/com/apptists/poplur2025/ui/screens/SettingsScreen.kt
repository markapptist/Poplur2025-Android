package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import com.apptists.poplur2025.data.PoplurLanguage
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.components.CubeMenuLayout
import com.apptists.poplur2025.ui.components.PersistentCubeOverlay

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
    onCubeMenuLayoutChange: (CubeMenuLayout) -> Unit = {}
) {
    val cubeDebugCallback by rememberUpdatedState(onOpenCubeDebug)
    var previewSkin by remember { mutableStateOf(selectedCubeSkin) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .statusBarsPadding()
    ) {
        Text(
            "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
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
            onCubeMenuLayoutChange = onCubeMenuLayoutChange
        )
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
    onCubeMenuLayoutChange: (CubeMenuLayout) -> Unit
) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("poplur_settings", Context.MODE_PRIVATE) }
    var phoneXLMode by remember { mutableStateOf(prefs.getBoolean("poplur_phoneXLMode", true)) }
    var tvGuideFlipAnimation by remember { mutableStateOf(prefs.getBoolean("tv_guide_flip_animation", true)) }

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
            modifier = Modifier.padding(vertical = 16.dp)
        )

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
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            PersistentCubeOverlay(
                modifier = Modifier.fillMaxSize(),
                cubeSkin = previewSkin,
                isInteractive = true
            )
        }

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
            containerColor = if (selectedCubeSkin == previewSkin) Color.Gray else Color(0xFF7B78FF),
            onClick = { onCubeSkinChange(previewSkin) }
        )

        SettingsActionRow("Email Address") {}
        SettingsActionRow("Password") {}

        Spacer(Modifier.height(32.dp))

        DesignButton("Launch Cube Debug", containerColor = Color(0xFF7B78FF)) {
            onOpenCubeDebug()
        }

        DesignButton("Limit videos to 10 seconds", subtext = "Current limit: 15s") {}
        DesignButton("Seed NA (1000)") {}
        DesignButton("Start Simulator (dev: 120s/day)") {}
        
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
                Text("Log Out", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsLanguagePicker(
    selectedLanguage: PoplurLanguage,
    onLanguageSelected: (PoplurLanguage) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Preferred Language", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFEDEDF2))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PoplurLanguage.entries.forEach { language ->
                val isSelected = language == selectedLanguage
                Surface(
                    onClick = { onLanguageSelected(language) },
                    color = if (isSelected) Color.White else Color.Transparent,
                    contentColor = if (isSelected) Color.Black else Color.Gray,
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
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Hold Menu", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFEDEDF2))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CubeMenuLayout.entries.forEach { layout ->
                val isSelected = layout == selectedLayout
                Surface(
                    onClick = { onLayoutSelected(layout) },
                    color = if (isSelected) Color.White else Color.Transparent,
                    contentColor = if (isSelected) Color.Black else Color.Gray,
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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF7B78FF) // Lavender/Purple from screenshot
            )
        )
    }
}

@Composable
private fun SettingsActionRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color(0xFF7B78FF), modifier = Modifier.size(18.dp))
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
                    color = if (containerColor == Color.White) Color.Black else Color.White
                )
            }
        }
        if (subtext != null) {
            Text(
                text = subtext,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
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
                .background(if (isSelected) Color(0xFF7B78FF) else Color(0xFFEDEDF2))
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFF7B78FF) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                skin.label.take(1),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else Color.Gray
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            skin.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF7B78FF) else Color.Gray,
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

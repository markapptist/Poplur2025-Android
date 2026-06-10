package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    onOpenCubeDebug: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // Default to Statistics
    val cubeDebugCallback by rememberUpdatedState(onOpenCubeDebug)

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .statusBarsPadding()
    ) {
        // Header with Back Button
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, CircleShape),
                shape = CircleShape,
                color = Color.White,
                onClick = onBack
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp).padding(end = 2.dp),
                        tint = Color.Black
                    )
                }
            }
        }

        // Tab Switcher
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFEEEEEE))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabPill("Statistics", selectedTab == 0) { selectedTab = 0 }
            TabPill("Settings", selectedTab == 1) { selectedTab = 1 }
        }

        // Content
        Box(Modifier.fillMaxSize()) {
            if (selectedTab == 0) {
                StatisticsTab()
            } else {
                SettingsTab(onSignOut, cubeDebugCallback)
            }
        }
    }
}

@Composable
private fun TabPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) Color.White else Color.Transparent)
    val textColor by animateColorAsState(if (isSelected) Color.Black else Color.LightGray)

    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp),
        modifier = if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(20.dp)) else Modifier
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun StatisticsTab() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Average Radius",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                drawArc(
                    color = Color(0xFFE0E0E0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.Black,
                    startAngle = -90f,
                    sweepAngle = 110f, // Example progress
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("5", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.9f))
                Text("KM", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
            }
        }

        Text(
            "Posts counted: 37",
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 48.dp)
        )

        Text("Accumulated Radius", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("You've reached", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        
        Text(
            "186 KM",
            fontSize = 58.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text("in total", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun SettingsTab(onSignOut: (() -> Unit)?, onOpenCubeDebug: () -> Unit) {
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen()
    }
}

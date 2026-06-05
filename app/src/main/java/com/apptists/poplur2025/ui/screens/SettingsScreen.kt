package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onSignOut: (() -> Unit)? = null) {
    var dailyQuota by remember { mutableStateOf(true) }
    var mlOverlay by remember { mutableStateOf(true) }
    var cameraPrewarm by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().background(Color.White).statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        SettingsRow("Daily quota overlay", dailyQuota) { dailyQuota = it }
        SettingsRow("Live ML detection overlay", mlOverlay) { mlOverlay = it }
        SettingsRow("Prewarm camera/map", cameraPrewarm) { cameraPrewarm = it }
        Surface(color = Color(0xFFF3F1F7), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                "Android equivalents are wired for Firebase Auth/Firestore/Storage, Google Maps, CameraX, ML Kit, Media3, AdMob, Play Billing, and LibGDX in place of SwiftUI, MapKit, AVFoundation, CoreML, SceneKit, and SpriteKit.",
                modifier = Modifier.padding(16.dp),
                color = Color.DarkGray
            )
        }
        Spacer(Modifier.weight(1f))
        if (onSignOut != null) {
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Log Out") }
        }
    }
}

@Composable
private fun SettingsRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(color = Color(0xFFF8F8F8), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.Bold)
            Switch(checked, onCheckedChange)
        }
    }
}

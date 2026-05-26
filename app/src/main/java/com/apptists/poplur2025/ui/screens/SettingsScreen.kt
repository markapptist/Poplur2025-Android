package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun SettingsScreen() { var dailyQuota by remember { mutableStateOf(true) }; Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Settings", style = MaterialTheme.typography.headlineMedium); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Daily quota overlay"); Switch(dailyQuota, { dailyQuota = it }) }; Text("Snap CameraKit and Bitmoji require Android SDK setup and keys in the README.") } }

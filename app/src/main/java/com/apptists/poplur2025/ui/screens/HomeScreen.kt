package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun HomeScreen(onOpenCamera: () -> Unit, onOpenMap: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(18.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Poplur", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text(
            "Android conversion: SwiftUI -> Jetpack Compose\nNavigation: 3D Cube (Swipe to switch tabs)",
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(Modifier.weight(1f))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onOpenCamera) { Text("Open Camera") }
            OutlinedButton(onClick = onOpenMap) { Text("Open Map") }
        }
        AssistChip(
            onClick = {}, 
            label = { Text("AI recommends posts by place + scene context") },
            colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
        )
        Spacer(Modifier.height(100.dp))
    }
}

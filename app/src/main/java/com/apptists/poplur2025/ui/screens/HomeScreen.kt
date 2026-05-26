package com.apptists.poplur2025.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apptists.poplur2025.gdx.LibGdxSceneView

@Composable fun HomeScreen(onOpenCamera: () -> Unit, onOpenMap: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Poplur", style = MaterialTheme.typography.displaySmall)
        Text("Android conversion: SwiftUI -> Jetpack Compose, SceneKit/SpriteKit -> LibGDX")
        Card(Modifier.fillMaxWidth().height(240.dp)) { LibGdxSceneView(scene = "radius") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onOpenCamera) { Text("Open Camera") }
            OutlinedButton(onClick = onOpenMap) { Text("Open Map") }
        }
        AssistChip(onClick = {}, label = { Text("AI recommends posts by place + scene context") })
    }
}

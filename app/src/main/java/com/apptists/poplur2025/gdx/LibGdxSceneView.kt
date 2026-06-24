package com.apptists.poplur2025.gdx

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box

@Composable
fun LibGdxSceneView(scene: String, modifier: Modifier = Modifier) {
    // Disabled to prevent conflicts with the background navigation cube
    Box(modifier = modifier)
}

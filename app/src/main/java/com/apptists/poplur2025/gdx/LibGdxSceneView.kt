package com.apptists.poplur2025.gdx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import com.apptists.poplur2025.ui.GdxState

@Composable
fun LibGdxSceneView(scene: String, modifier: Modifier = Modifier) {
    // Instead of starting a new libGDX instance, we just toggle the global state
    DisposableEffect(scene) {
        GdxState.isSceneActive = true
        GdxState.currentScene = scene
        onDispose {
            GdxState.isSceneActive = false
        }
    }
    
    // Empty Box as a placeholder in the Compose layout
    Box(modifier = modifier)
}

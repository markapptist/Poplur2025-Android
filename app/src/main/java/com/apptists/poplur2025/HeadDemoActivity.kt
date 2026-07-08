package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.ui.components.MarkHeadSceneView

/**
 * DEBUG-only entry point that renders just the 3D Mark head (SceneView/Filament),
 * bypassing login — used to verify the head in isolation:
 *   adb shell am start -n com.apptists.poplur2025/.HeadDemoActivity
 */
class HeadDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF1A0030)),
                contentAlignment = Alignment.Center
            ) {
                MarkHeadSceneView(isSpeaking = true, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

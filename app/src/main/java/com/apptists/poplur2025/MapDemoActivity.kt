package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apptists.poplur2025.ui.MapViewModel
import com.apptists.poplur2025.ui.screens.MapScreen

/**
 * DEBUG-only entry point that renders the real MapScreen, bypassing login/cube nav —
 * used to verify map style/overlays:
 *   adb shell am start -n com.apptists.poplur2025/.MapDemoActivity
 */
class MapDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MapScreen(mapViewModel = viewModel())
            }
        }
    }
}

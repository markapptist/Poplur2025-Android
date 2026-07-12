package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.ui.screens.WelcomeAuthScreenContent

/**
 * DEBUG-only harness for the redesigned Welcome/Auth carousel (welcome↔login↔signup slide):
 *   adb shell am start -n com.apptists.poplur2025/.WelcomeDemoActivity
 */
class WelcomeDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WelcomeAuthScreenContent(
                    onSignIn = { _, _, onResult -> onResult(false, "Demo mode") },
                    onSignUp = { _, _, _, _, onResult -> onResult(false, "Demo mode") }
                )
            }
        }
    }
}

package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.screens.ProfileScreen

/**
 * DEBUG-only entry point that renders the real ProfileScreen (with the 3D head) using
 * default mock data, bypassing login/cube navigation — used to verify profile design:
 *   adb shell am start -n com.apptists.poplur2025/.ProfileDemoActivity
 */
class ProfileDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color.White) {
                    ProfileScreen(
                        profile = UserProfile(),
                        avatarIs3D = true
                    )
                }
            }
        }
    }
}

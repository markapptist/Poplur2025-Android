package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.screens.ProfileStatsScreen

/**
 * DEBUG-only harness for the redesigned Profile "Stats" sub-screen:
 *   adb shell am start -n com.apptists.poplur2025/.StatsDemoActivity
 */
class StatsDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProfileStatsScreen(profile = UserProfile())
            }
        }
    }
}

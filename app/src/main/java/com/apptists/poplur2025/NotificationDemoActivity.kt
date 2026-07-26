package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * DEBUG verification entry (mirrors the other *DemoActivity hooks): posts a
 * sample Poplur system notification and immediately finishes, so the shade /
 * lock-screen notification — clay-P small icon + clay large icon + clay-pink
 * accent — can be checked outside the app with:
 *
 *   adb shell pm grant com.apptists.poplur2025 android.permission.POST_NOTIFICATIONS
 *   adb shell am start -n com.apptists.poplur2025/.NotificationDemoActivity
 */
class NotificationDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PoplurNotifications.post(
            context = this,
            id = 4201,
            title = "Poplur",
            body = "3 new posts are trending in your radius right now."
        )
        finish()
    }
}

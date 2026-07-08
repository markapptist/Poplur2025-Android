package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.ui.screens.MessagesScreen

/**
 * DEBUG-only harness for the redesigned DM/Messages screen (pink ChatC theme):
 *   adb shell am start -n com.apptists.poplur2025/.MessagesDemoActivity
 */
class MessagesDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MessagesScreen()
            }
        }
    }
}

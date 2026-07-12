package com.apptists.poplur2025

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.apptists.poplur2025.ui.components.CubeMenuWheelController
import com.apptists.poplur2025.ui.components.CubeMenuWheelItem
import com.apptists.poplur2025.ui.components.CubeMenuWheelOverlay

/**
 * DEBUG-only harness that shows the cube hold menu (grid-card redesign) over a mock
 * background, bypassing the cube-hold gesture:
 *   adb shell am start -n com.apptists.poplur2025/.HoldMenuDemoActivity
 */
class HoldMenuDemoActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { CubeMenuWheelController(scope) {} }
            LaunchedEffect(Unit) {
                controller.beginHold(
                    listOf(
                        CubeMenuWheelItem("HOME", "hold_menu_icons/home.png", onSelected = {}),
                        CubeMenuWheelItem("CHAT", "hold_menu_icons/chat.png", onSelected = {}),
                        CubeMenuWheelItem("MUSIC", "hold_menu_icons/music.png", onSelected = {}),
                        CubeMenuWheelItem("GIFTS", "hold_menu_icons/gifts.png", onSelected = {}),
                        CubeMenuWheelItem("STORE", "hold_menu_icons/store.png", onSelected = {}),
                        CubeMenuWheelItem("PHONE", "hold_menu_icons/phone.png", onSelected = {}),
                        CubeMenuWheelItem("SETTINGS", "hold_menu_icons/settings.png", onSelected = {}),
                    )
                )
            }
            Box(Modifier.fillMaxSize().background(Color(0xFF2A0033))) {
                CubeMenuWheelOverlay(controller)
            }
        }
    }
}

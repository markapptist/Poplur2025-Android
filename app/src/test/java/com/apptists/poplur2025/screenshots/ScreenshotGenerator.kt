package com.apptists.poplur2025.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.apptists.poplur2025.ui.screens.HomeScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi") // Standard Phone
class ScreenshotGenerator {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureHomeScreen() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212) // Dark background for white text
                ) {
                    HomeScreen(onOpenCamera = {}, onOpenMap = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage("screenshots/home_screen.png")
    }
}

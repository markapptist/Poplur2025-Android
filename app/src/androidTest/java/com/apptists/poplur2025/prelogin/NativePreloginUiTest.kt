package com.apptists.poplur2025.prelogin

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.apptists.poplur2025.NativePreloginActivity
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

/** Exercises the shipping native activity without signing out, creating an
 * account, or sending credentials. Screenshot evidence includes the real GPU. */
@RunWith(AndroidJUnit4::class)
class NativePreloginUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<NativePreloginActivity>
    private lateinit var context: Context
    private var previousPrimers = emptyMap<String, Any?>()
    private var previousMusic = emptyMap<String, Any?>()
    @Before fun launch() {
        context = ApplicationProvider.getApplicationContext()
        val primers = context.getSharedPreferences("poplur_permission_primers", 0)
        val music = context.getSharedPreferences("poplur_prelogin_music", 0)
        previousPrimers = primers.all; previousMusic = music.all
        primers.edit().putBoolean("location", true).putBoolean("photos", true).commit()
        music.edit().putBoolean("muted", true).commit()
        scenario = ActivityScenario.launch(NativePreloginActivity::class.java)
    }
    @After fun close() {
        scenario.close()
        fun restore(name: String, previous: Map<String, *>) {
            val editor = context.getSharedPreferences(name, 0).edit().clear()
            previous.forEach { (key, value) -> when (value) { is Boolean -> editor.putBoolean(key, value); is String -> editor.putString(key, value); is Int -> editor.putInt(key, value); is Long -> editor.putLong(key, value); is Float -> editor.putFloat(key, value) } }; editor.commit()
        }
        restore("poplur_permission_primers", previousPrimers); restore("poplur_prelogin_music", previousMusic)
    }
    @Test fun welcomeSignInAccountSelectionUseNoWebViews() {
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("welcomeSignIn").fetchSemanticsNodes().isNotEmpty() }
        noWebViews(); screenshot("android-native-welcome")
        compose.onNodeWithTag("welcomeSignIn").performClick()
        compose.onNodeWithTag("submitSignIn").performScrollTo().performClick()
        compose.onNodeWithTag("preloginError").assertExists(); noWebViews()
        screenshot("android-native-sign-in")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.onNodeWithTag("welcomeSignUp").performClick()
        compose.onNodeWithText("Business").performScrollTo().performClick()
        screenshot("android-native-accounts")
        compose.onNodeWithTag("chooseAccount").performScrollTo().performClick()
        compose.onNodeWithTag("introNext").performScrollTo().performClick()
        compose.onNodeWithTag("credentialsNext").performScrollTo().performClick()
        compose.onNodeWithTag("preloginError").assertExists(); noWebViews()
        screenshot("android-native-create-login")
    }
    private fun noWebViews() { scenario.onActivity { activity ->
        fun count(view: View): Int = (if (view is WebView) 1 else 0) + if (view is ViewGroup) (0 until view.childCount).sumOf { count(view.getChildAt(it)) } else 0
        Assert.assertEquals("Signed-out activity must contain no WebViews", 0, count(activity.window.decorView))
    } }
    private fun screenshot(name: String) {
        val directory = File(context.getExternalFilesDir(null), "prelogin-test-evidence").apply { mkdirs() }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(directory, "$name.png"))
    }
}

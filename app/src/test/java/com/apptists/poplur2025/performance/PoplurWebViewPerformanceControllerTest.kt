package com.apptists.poplur2025.performance

import android.app.Application
import android.content.Context
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class PoplurWebViewPerformanceControllerTest {
    private class CountingWebView(context: Context) : WebView(context) {
        var stops = 0
        override fun stopLoading() {
            stops += 1
        }
    }

    // A5: an outside-scope station plays in Home's WebView, so hiding Home must not pause
    // that view or cancel its loads until the radio stops.
    @Test fun aHeldViewKeepsItsMediaWhileInactive() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val view = CountingWebView(context)
        val controller = PoplurWebViewPerformanceController(context, "RadioHoldTest")
        try {
            controller.holdMedia(true)
            controller.attach(view)
            controller.setVisible(false)
            controller.releaseInactiveResources()
            assertFalse(shadowOf(view).wasOnPauseCalled())
            assertEquals(0, view.stops)

            controller.holdMedia(false)
            assertTrue(shadowOf(view).wasOnPauseCalled())
            controller.releaseInactiveResources()
            assertEquals(1, view.stops)
        } finally {
            controller.close()
        }
    }
}

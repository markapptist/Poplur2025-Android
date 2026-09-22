package com.apptists.poplur2025.ui

import android.app.Application
import com.apptists.poplur2025.data.mapillary.MapillaryError
import com.apptists.poplur2025.data.mapillary.MapillaryImage
import com.apptists.poplur2025.data.mapillary.MapillaryImageProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Verifies the Street View button state machine on [MapViewModel]:
 * idle -> checking -> (available | unavailable | error), and reset back to idle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class MapViewModelStreetViewTest {

    private fun idle() {
        ShadowLooper.idleMainLooper()
    }

    private class FakeProvider(
        private val result: (Double, Double) -> MapillaryImage?,
        private val error: Throwable? = null
    ) : MapillaryImageProvider {
        override suspend fun nearestImage(latitude: Double, longitude: Double): MapillaryImage? {
            error?.let { throw it }
            return result(latitude, longitude)
        }
    }

    private fun sampleImage(id: String = "123") = MapillaryImage(
        id = id,
        latitude = 40.7484,
        longitude = -73.9857,
        compassAngle = 123.4,
        isPanoramic = true,
        thumb256Url = null,
        thumb1024Url = null
    )

    @Test
    fun `initial state is idle`() {
        val viewModel = MapViewModel(FakeProvider({ _, _ -> null }))
        assertTrue(viewModel.streetViewState.value is StreetViewUiState.Idle)
    }

    @Test
    fun `checkStreetView transitions through checking to available when an image exists`() {
        val viewModel = MapViewModel(FakeProvider({ _, _ -> sampleImage() }))
        viewModel.checkStreetView(40.7484, -73.9857)

        idle()
        idle()

        val state = viewModel.streetViewState.value
        assertTrue("Expected Available but was $state", state is StreetViewUiState.Available)
        assertEquals("123", (state as StreetViewUiState.Available).image.id)
    }

    @Test
    fun `checkStreetView resolves to unavailable when no image is found`() {
        val viewModel = MapViewModel(FakeProvider({ _, _ -> null }))
        viewModel.checkStreetView(0.0, 0.0)
        idle()
        assertTrue(viewModel.streetViewState.value is StreetViewUiState.Unavailable)
    }

    @Test
    fun `checkStreetView resolves to error when the provider throws`() {
        val viewModel = MapViewModel(FakeProvider({ _, _ -> null }, error = MapillaryError.Network))
        viewModel.checkStreetView(0.0, 0.0)
        idle()

        val state = viewModel.streetViewState.value
        assertTrue(state is StreetViewUiState.Error)
        assertEquals("Street View could not be loaded. Try again.", (state as StreetViewUiState.Error).message)
    }

    @Test
    fun `resetStreetView returns to idle`() {
        val viewModel = MapViewModel(FakeProvider({ _, _ -> sampleImage() }))
        viewModel.checkStreetView(0.0, 0.0)
        idle()
        assertTrue(viewModel.streetViewState.value is StreetViewUiState.Available)

        viewModel.resetStreetView()
        assertTrue(viewModel.streetViewState.value is StreetViewUiState.Idle)
    }
}

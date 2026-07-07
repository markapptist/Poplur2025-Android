package com.apptists.poplur2025.ui

import com.apptists.poplur2025.data.mapillary.MapillaryImage

/** State machine for the "Street View" button/flow on the Android map screen. */
sealed class StreetViewUiState {
    object Idle : StreetViewUiState()
    object Checking : StreetViewUiState()
    data class Available(val image: MapillaryImage) : StreetViewUiState()
    object Unavailable : StreetViewUiState()
    data class Error(val message: String) : StreetViewUiState()
}

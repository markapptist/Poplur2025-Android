package com.apptists.poplur2025.data.mapillary

/** Outcome of parsing a raw Mapillary images response, kept separate from network I/O for easy testing. */
sealed class MapillaryResult {
    data class Success(val image: MapillaryImage?) : MapillaryResult()
    data class Failure(val error: MapillaryError) : MapillaryResult()
}

package com.apptists.poplur2025.data.mapillary

/** Abstraction over [MapillaryService] so view models can be unit tested with a fake. */
interface MapillaryImageProvider {
    suspend fun nearestImage(latitude: Double, longitude: Double): MapillaryImage?
}

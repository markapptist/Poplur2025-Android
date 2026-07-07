package com.apptists.poplur2025.data.mapillary

/** A single street-level image returned by the Mapillary images endpoint. */
data class MapillaryImage(
    val id: String,
    val latitude: Double?,
    val longitude: Double?,
    val compassAngle: Double?,
    val isPanoramic: Boolean,
    val thumb256Url: String?,
    val thumb1024Url: String?
)

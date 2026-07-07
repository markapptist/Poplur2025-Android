package com.apptists.poplur2025.data.mapillary

/** Failure modes for [MapillaryService]. Never carries the access token. */
sealed class MapillaryError(message: String) : Exception(message) {
    object MissingAccessToken : MapillaryError("Mapillary access token is not configured.")
    object Network : MapillaryError("Could not reach Mapillary. Check your connection.")
    data class Api(val statusCode: Int) : MapillaryError("Mapillary API returned an error (HTTP $statusCode).")
    data class Malformed(val reason: String) : MapillaryError("Mapillary response could not be parsed: $reason")
}

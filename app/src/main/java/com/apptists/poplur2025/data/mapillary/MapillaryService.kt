package com.apptists.poplur2025.data.mapillary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Looks up the nearest Mapillary street-level image for a coordinate.
 * Android-only: Mapbox provides the map, Mapillary provides the street-level imagery.
 */
class MapillaryService(private val accessToken: String) : MapillaryImageProvider {

    /** Returns the nearest image within [RADIUS_METERS], or null if Mapillary has no coverage there. */
    override suspend fun nearestImage(latitude: Double, longitude: Double): MapillaryImage? {
        if (accessToken.isBlank()) throw MapillaryError.MissingAccessToken

        val rawJson = withContext(Dispatchers.IO) {
            fetchRaw(buildUrl(latitude, longitude))
        }

        return when (val result = parseNearestImage(rawJson)) {
            is MapillaryResult.Success -> result.image
            is MapillaryResult.Failure -> throw result.error
        }
    }

    private fun fetchRaw(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code !in 200..299) {
                throw MapillaryError.Api(code)
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (error: MapillaryError) {
            throw error
        } catch (error: IOException) {
            throw MapillaryError.Network
        } finally {
            connection.disconnect()
        }
    }

    /** Exposed for testing URL construction without making a network call. */
    internal fun buildUrl(latitude: Double, longitude: Double): String {
        val encodedToken = URLEncoder.encode(accessToken, "UTF-8")
        return "$BASE_URL?fields=$FIELDS&lat=$latitude&lng=$longitude&radius=$RADIUS_METERS&limit=$LIMIT&access_token=$encodedToken"
    }

    companion object {
        private const val BASE_URL = "https://graph.mapillary.com/images"
        private const val FIELDS = "id,computed_geometry,computed_compass_angle,is_pano,thumb_256_url,thumb_1024_url"
        private const val RADIUS_METERS = 50
        private const val LIMIT = 1
        private const val TIMEOUT_MS = 10_000

        /** Pure JSON parsing, split out from network I/O so it can be unit tested directly. */
        fun parseNearestImage(rawJson: String): MapillaryResult {
            return try {
                val json = JSONObject(rawJson)
                val data = json.optJSONArray("data")
                if (data == null || data.length() == 0) {
                    return MapillaryResult.Success(null)
                }

                val item = data.getJSONObject(0)
                val id = item.optString("id").takeIf { it.isNotBlank() }
                    ?: return MapillaryResult.Failure(MapillaryError.Malformed("missing id"))

                val geometry = item.optJSONObject("computed_geometry")
                val coordinates = geometry?.optJSONArray("coordinates")
                val longitude = coordinates?.optDouble(0)?.takeIf { !it.isNaN() }
                val latitude = coordinates?.optDouble(1)?.takeIf { !it.isNaN() }
                val compassAngle = item.optDouble("computed_compass_angle").takeIf { !it.isNaN() }

                MapillaryResult.Success(
                    MapillaryImage(
                        id = id,
                        latitude = latitude,
                        longitude = longitude,
                        compassAngle = compassAngle,
                        isPanoramic = item.optBoolean("is_pano", false),
                        thumb256Url = item.optString("thumb_256_url").takeIf { it.isNotBlank() },
                        thumb1024Url = item.optString("thumb_1024_url").takeIf { it.isNotBlank() }
                    )
                )
            } catch (error: Exception) {
                MapillaryResult.Failure(MapillaryError.Malformed(error.message ?: "unknown parse error"))
            }
        }
    }
}

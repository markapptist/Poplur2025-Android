package com.apptists.poplur2025.data.mapillary

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MapillaryServiceTest {

    @Test
    fun `buildUrl includes required query parameters and endpoint`() {
        val service = MapillaryService("test-token")
        val url = service.buildUrl(40.7484, -73.9857)

        assertTrue(url.startsWith("https://graph.mapillary.com/images?"))
        assertTrue(url.contains("fields=id,computed_geometry,computed_compass_angle,is_pano,thumb_256_url,thumb_1024_url"))
        assertTrue(url.contains("lat=40.7484"))
        assertTrue(url.contains("lng=-73.9857"))
        assertTrue(url.contains("radius=50"))
        assertTrue(url.contains("limit=1"))
        assertTrue(url.contains("access_token=test-token"))
    }

    @Test
    fun `parseNearestImage returns null image for empty data array`() {
        val result = MapillaryService.parseNearestImage("""{"data": []}""")
        assertTrue(result is MapillaryResult.Success)
        assertNull((result as MapillaryResult.Success).image)
    }

    @Test
    fun `parseNearestImage returns null image when data field missing`() {
        val result = MapillaryService.parseNearestImage("""{}""")
        assertTrue(result is MapillaryResult.Success)
        assertNull((result as MapillaryResult.Success).image)
    }

    @Test
    fun `parseNearestImage parses a valid single-image response`() {
        val json = """
            {
              "data": [
                {
                  "id": "123",
                  "computed_geometry": {
                    "type": "Point",
                    "coordinates": [-73.9857, 40.7484]
                  },
                  "computed_compass_angle": 123.4,
                  "is_pano": true
                }
              ]
            }
        """.trimIndent()

        val result = MapillaryService.parseNearestImage(json)
        assertTrue(result is MapillaryResult.Success)
        val image = (result as MapillaryResult.Success).image
        assertEquals("123", image?.id)
        assertEquals(-73.9857, image?.longitude!!, 0.0001)
        assertEquals(40.7484, image.latitude!!, 0.0001)
        assertEquals(123.4, image.compassAngle!!, 0.0001)
        assertTrue(image.isPanoramic)
    }

    @Test
    fun `parseNearestImage tolerates missing optional fields`() {
        val json = """{"data": [{"id": "456"}]}"""
        val result = MapillaryService.parseNearestImage(json)
        assertTrue(result is MapillaryResult.Success)
        val image = (result as MapillaryResult.Success).image
        assertEquals("456", image?.id)
        assertNull(image?.latitude)
        assertNull(image?.longitude)
        assertNull(image?.compassAngle)
        assertEquals(false, image?.isPanoramic)
    }

    @Test
    fun `parseNearestImage fails gracefully on malformed json`() {
        val result = MapillaryService.parseNearestImage("not json at all")
        assertTrue(result is MapillaryResult.Failure)
        assertTrue((result as MapillaryResult.Failure).error is MapillaryError.Malformed)
    }

    @Test
    fun `parseNearestImage fails when an image entry is missing its id`() {
        val json = """{"data": [{"is_pano": false}]}"""
        val result = MapillaryService.parseNearestImage(json)
        assertTrue(result is MapillaryResult.Failure)
        assertTrue((result as MapillaryResult.Failure).error is MapillaryError.Malformed)
    }

    @Test
    fun `nearestImage throws MissingAccessToken when token is blank`() {
        val service = MapillaryService("")
        runBlocking {
            try {
                service.nearestImage(40.7484, -73.9857)
                fail("Expected MapillaryError.MissingAccessToken")
            } catch (error: MapillaryError.MissingAccessToken) {
                // expected
            }
        }
    }
}

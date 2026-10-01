package com.apptists.poplur2025.ml

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class CameraSceneLabelsTest {
    @Test fun labelsReadLikeIosStrongestFirstWithoutRepeatsOrMoods() {
        val labels = CameraSceneLabels.from(
            listOf(
                "Table" to 0.61f,
                "Coffee_Mug" to 0.83f,
                "Fun" to 0.9f,
                "table" to 0.55f,
                " " to 0.99f,
                "Selfie" to 0.7f,
            ),
        )
        assertEquals(listOf("coffee mug", "table"), labels.map { it.text })
        assertEquals(0.83f, labels.first().confidence, 0.0001f)
    }

    @Test fun keepsAtMostSix() {
        val labels = CameraSceneLabels.from((1..9).map { "thing $it" to it / 10f })
        assertEquals(CameraSceneLabels.MAX_LABELS, labels.size)
        assertEquals("thing 9", labels.first().text)
    }

    @Test fun entriesAreWholeFrameWithOnePrimaryAndSkipLocatedLabels() {
        val entries = CameraSceneLabels.entries(
            listOf(
                CameraSceneLabels.Label("person", 0.9f),
                CameraSceneLabels.Label("cup", 0.8f),
                CameraSceneLabels.Label("desk", 0.6f),
            ),
            taken = setOf("Person"),
        )
        assertEquals(listOf("cup", "desk"), entries.map { it.getString("label") })
        entries.forEach { entry ->
            assertTrue(entry.getBoolean("whole"))
            assertEquals(0.0, entry.getDouble("x"), 0.0)
            assertEquals(1.0, entry.getDouble("w"), 0.0)
        }
        assertTrue(entries[0].getBoolean("primary"))
        assertFalse(entries[1].getBoolean("primary"))
        assertEquals(0.8, entries[0].getDouble("confidence"), 0.0001)
    }
}

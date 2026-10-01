package com.apptists.poplur2025.ml

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
class CameraFacePoseTest {
    @Test fun rollIsInRadiansWithVisionsSign() {
        // ML Kit: a counter-clockwise tilt is positive. Vision, which the page reads: negative.
        assertEquals(-Math.toRadians(20.0), CameraFacePose.roll(20f, mirrored = false), 1e-9)
        assertEquals(Math.toRadians(20.0), CameraFacePose.roll(20f, mirrored = true), 1e-9)
    }

    @Test fun yawKeepsItsSignUnlessMirrored() {
        assertEquals(Math.toRadians(15.0), CameraFacePose.yaw(15f, mirrored = false), 1e-9)
        assertEquals(-Math.toRadians(15.0), CameraFacePose.yaw(15f, mirrored = true), 1e-9)
    }

    @Test fun landmarksAreFractionsOfTheFrameMirroredLikeTheBox() {
        val point = CameraFacePose.point(80f, 60f, 320.0, 240.0, mirrored = false)!!
        assertEquals(0.25, point.getDouble("x"), 1e-9)
        assertEquals(0.25, point.getDouble("y"), 1e-9)
        assertEquals(0.75, CameraFacePose.point(80f, 60f, 320.0, 240.0, mirrored = true)!!.getDouble("x"), 1e-9)
        assertNull(CameraFacePose.point(-4f, 60f, 320.0, 240.0, mirrored = false))
        assertNull(CameraFacePose.point(80f, 260f, 320.0, 240.0, mirrored = false))
        assertNull(CameraFacePose.point(80f, 60f, 0.0, 240.0, mirrored = false))
    }
}

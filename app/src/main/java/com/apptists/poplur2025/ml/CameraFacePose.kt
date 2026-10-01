package com.apptists.poplur2025.ml

import org.json.JSONObject

/**
 * Face pose for the page's 3D costumes on Android (A5). iOS sends Vision's roll and yaw
 * in radians and the eye and nose centres in top-left frame coordinates
 * (WebHomeCameraLayer.publishCombinedDetections); FaceAR3D turns the costume with them
 * and anchors it on the eye line. Android sent only the box, so costumes sat on faces
 * like stickers. These put ML Kit's readings in iOS's terms.
 */
object CameraFacePose {
    /** ML Kit counts a counter-clockwise head tilt as positive; the page reads Vision's
     *  sign (FaceAR3D negates it), where a clockwise tilt is positive. */
    fun roll(eulerZDegrees: Float, mirrored: Boolean): Double {
        val roll = -Math.toRadians(eulerZDegrees.toDouble())
        return if (mirrored) -roll else roll
    }

    /** Positive when the face turns towards the right of the frame, in both. */
    fun yaw(eulerYDegrees: Float, mirrored: Boolean): Double {
        val yaw = Math.toRadians(eulerYDegrees.toDouble())
        return if (mirrored) -yaw else yaw
    }

    /** A landmark in 0…1 of the frame, mirrored like the box; null when off the frame. */
    fun point(x: Float, y: Float, width: Double, height: Double, mirrored: Boolean): JSONObject? {
        if (width <= 0.0 || height <= 0.0) return null
        val nx = x / width
        val ny = y / height
        if (!nx.isFinite() || !ny.isFinite() || nx !in 0.0..1.0 || ny !in 0.0..1.0) return null
        return JSONObject().put("x", if (mirrored) 1.0 - nx else nx).put("y", ny)
    }
}

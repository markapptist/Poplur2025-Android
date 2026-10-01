package com.apptists.poplur2025.ml

import org.json.JSONObject
import java.util.Locale

/**
 * SCAN's whole-frame labels on Android (A5). iOS names what is in the frame with
 * VNClassifyImageRequest and hands the page up to six labels marked `whole`, the first
 * one `primary` (WebHomeCameraLayer.publishCombinedDetections). Android's camera frames
 * now go through ML Kit image labeling as well, shaped the same way, so the readout, the
 * scan radius and the spoken "I see …" work unchanged.
 */
object CameraSceneLabels {
    data class Label(val text: String, val confidence: Float)

    const val MAX_LABELS = 6

    // ML Kit's base model also tags the photo rather than a thing in it. The scanner
    // counts objects, and "I see fun." is not a sighting.
    private val NOT_OBJECTS = setOf("cool", "fun", "event", "sitting", "standing", "selfie")

    /** iOS's label style: lower case, underscores as spaces. Strongest first, no repeats. */
    fun from(raw: List<Pair<String, Float>>): List<Label> = raw.asSequence()
        .map { (text, confidence) -> Label(text.replace('_', ' ').trim().lowercase(Locale.ROOT), confidence) }
        .filter { it.text.isNotEmpty() && it.text !in NOT_OBJECTS }
        .sortedByDescending { it.confidence }
        .distinctBy { it.text }
        .take(MAX_LABELS)
        .toList()

    /** Whole-frame entries for `poplur-native-detections`. A classification has no
     *  position, so its bounds are the whole frame; labels the located entries already
     *  carry (`taken`) are not repeated, as on iOS. */
    fun entries(labels: List<Label>, taken: Set<String>): List<JSONObject> {
        val seen = taken.mapTo(HashSet()) { it.lowercase(Locale.ROOT) }
        var primaryGiven = false
        return labels.mapIndexedNotNull { index, label ->
            if (!seen.add(label.text)) return@mapIndexedNotNull null
            JSONObject().apply {
                put("id", "classification-$index")
                put("x", 0.0)
                put("y", 0.0)
                put("w", 1.0)
                put("h", 1.0)
                put("whole", true)
                put("primary", !primaryGiven)
                put("label", label.text)
                put("confidence", label.confidence.toDouble())
            }.also { primaryGiven = true }
        }
    }
}

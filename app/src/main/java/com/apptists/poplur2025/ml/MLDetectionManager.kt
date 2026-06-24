package com.apptists.poplur2025.ml

import android.media.Image
import com.apptists.poplur2025.data.DetectedObject
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class MLDetectionManager {
    private val detector = ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE).enableClassification().build())
    fun analyze(mediaImage: Image, rotation: Int, onResult: (List<DetectedObject>) -> Unit) {
        detector.process(InputImage.fromMediaImage(mediaImage, rotation)).addOnSuccessListener { objects -> onResult(objects.flatMap { it.labels }.map { DetectedObject(it.text, it.confidence) }) }.addOnFailureListener { onResult(emptyList()) }
    }
}

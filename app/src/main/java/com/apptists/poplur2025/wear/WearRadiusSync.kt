package com.apptists.poplur2025.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object WearRadiusSync {
    private const val TAG = "WearRadiusSync"
    private const val RADIUS_PATH = "/poplur/radius"
    private const val KEY_OVERALL_RADIUS_METERS = "overallRadiusMeters"
    private const val KEY_POST_COUNT = "postCount"
    private const val KEY_UPDATED_AT = "updatedAt"

    private var started = false
    private lateinit var appContext: Context

    fun start(context: Context) {
        if (started) return
        started = true
        appContext = context.applicationContext

        FirebaseAuth.getInstance().addAuthStateListener {
            refreshAndSendRadius()
        }
        refreshAndSendRadius()
    }

    fun refreshAndSendRadius() {
        if (!::appContext.isInitialized) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrBlank()) {
            sendRadius(radiusMeters = 0, postCount = 0)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val uniqueDocuments = snapshot.documents.distinctBy { it.id }
                val totalRadius = uniqueDocuments.sumOf { document ->
                    document.getLong("radius")?.toInt()
                        ?: document.getDouble("radius")?.toInt()
                        ?: 0
                }
                sendRadius(
                    radiusMeters = totalRadius.coerceAtLeast(0),
                    postCount = uniqueDocuments.size
                )
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Could not fetch radius for Wear sync", error)
            }
    }

    private fun sendRadius(radiusMeters: Int, postCount: Int) {
        val request = PutDataMapRequest.create(RADIUS_PATH).apply {
            dataMap.putInt(KEY_OVERALL_RADIUS_METERS, radiusMeters.coerceAtLeast(0))
            dataMap.putInt(KEY_POST_COUNT, postCount.coerceAtLeast(0))
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(appContext)
            .putDataItem(request)
            .addOnFailureListener { error ->
                Log.w(TAG, "Could not send radius to Wear device", error)
            }
    }

    fun DataMap.overallRadiusMeters(): Int = getInt(KEY_OVERALL_RADIUS_METERS, 0)
    fun DataMap.postCount(): Int = getInt(KEY_POST_COUNT, 0)
    fun DataMap.updatedAtMillis(): Long = getLong(KEY_UPDATED_AT, 0L)
}

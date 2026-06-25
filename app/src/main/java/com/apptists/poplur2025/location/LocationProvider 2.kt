package com.apptists.poplur2025.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationProvider(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    
    @SuppressLint("MissingPermission") 
    fun lastLocation(onResult: (Double?, Double?) -> Unit) { 
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            } else {
                // If last location is null, request current location
                val cts = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { curLoc ->
                        onResult(curLoc?.latitude, curLoc?.longitude)
                    }
                    .addOnFailureListener { onResult(null, null) }
            }
        }.addOnFailureListener { onResult(null, null) } 
    }
}

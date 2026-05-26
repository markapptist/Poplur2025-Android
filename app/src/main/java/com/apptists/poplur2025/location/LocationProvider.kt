package com.apptists.poplur2025.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices

class LocationProvider(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    @SuppressLint("MissingPermission") fun lastLocation(onResult: (Double?, Double?) -> Unit) { client.lastLocation.addOnSuccessListener { onResult(it?.latitude, it?.longitude) }.addOnFailureListener { onResult(null, null) } }
}

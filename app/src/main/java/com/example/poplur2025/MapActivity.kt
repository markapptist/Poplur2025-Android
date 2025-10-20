package com.example.poplur2025

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback // <-- 1. IMPORT THIS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// --- 2. IMPLEMENT THE INTERFACE ---
class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null // It's good practice to hold a reference
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // User login check is correct
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return // IMPORTANT: Return here to stop execution
        }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // --- 3. REQUEST THE MAP ---
        // This tells the MapView to prepare the map and call onMapReady when it's done.
        mapView.getMapAsync(this)
    }

    // --- 4. IMPLEMENT THE CALLBACK METHOD ---
    override fun onMapReady(map: GoogleMap) {
        // This is called when the map is ready to be used.
        // You can now safely interact with the map object.
        googleMap = map

        // Example: Set the camera to a default location
        // val sydney = LatLng(-34.0, 151.0)
        // googleMap?.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        // googleMap?.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        // TODO: Add your map logic here (e.g., loading posts, setting up listeners)
    }

    // --- The rest of your lifecycle methods are CORRECT ---
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Check if mapView is initialized before calling onDestroy on it
        if(this::mapView.isInitialized) {
            mapView.onDestroy()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}

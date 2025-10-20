package com.example.poplur2025

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration // For managing listener
import com.google.firebase.firestore.Query
import androidx.fragment.app.activityViewModels // Make sure you have this import

// Assume MediaType is defined in your Post.kt or imported
// enum class MediaType { NONE, IMAGE, VIDEO }

class MapFragment : Fragment(), OnMapReadyCallback, CreatePostDialogFragment.PostCreationListener {

   // private lateinit var sharedViewModel: SharedMapPostsViewModel
    private lateinit var mMap: GoogleMap
    private lateinit var mapView: MapView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var postsListener: ListenerRegistration? = null // To remove listener later

    private val displayedCircles: MutableMap<String, Circle> = mutableMapOf()
    private val displayedMarkers: MutableMap<String, Marker> = mutableMapOf()

    private val sharedViewModel: SharedMapPostsViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
     //   mapView.getMapAsync(this)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Call getMapAsync here instead. It's the safer place.
        mapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // ... (default location, camera move)

        mMap.setOnMapClickListener { latLng ->
            // When the map is clicked, open the post creation dialog
            Log.d("MapClick", "Map clicked at: $latLng. Opening post creation dialog.")
            val createPostDialog = CreatePostDialogFragment.newInstance(latLng)
            // Set this fragment as the listener for dialog callbacks
            createPostDialog.setPostCreationListener(this)
            createPostDialog.show(parentFragmentManager, CreatePostDialogFragment.TAG)
        }

        mMap.setOnInfoWindowClickListener { marker ->
            val post = marker.tag as? Post
            post?.let {
                displayMediaForPost(it)
            }
        }

        startListeningForPosts()
    }

    // This method will be called by CreatePostDialogFragment when a post is ready
    override fun onPostCreated(
        latLng: LatLng,
        textContent: String?,
        mediaType: MediaType, // Assuming MediaType is defined
        mediaUrl: String?,
        radius: Double
    ) {
        Log.d("MapFragment", "Creating post from dialog: $latLng, Text: $textContent, Media: $mediaUrl, Radius: $radius")
        createAndAddPostToFirestore(latLng, textContent, mediaType, mediaUrl, radius)
    }



    // ... (startListeningForPosts, updateMapWithPosts, displayMediaForPost, lifecycle methods)

    private fun startListeningForPosts() {
        postsListener?.remove() // Remove any existing listener

        postsListener = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Good to order by time
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("MapFragment", "Listen failed.", e)
                    // Consider showing an error to the user
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val posts = snapshots.toObjects(Post::class.java)
                    Log.d("MapFragment", "Firestore updated with ${posts.size} posts.")
                    updateMapWithPosts(posts) // Updates the map UI
                    sharedViewModel.updatePosts(posts) // <--- UPDATE SHARED VIEWMODEL HERE
                }
            }
    }

    // Inside MapFragment.kt

// ... (other properties like mMap, displayedCircles, displayedMarkers)

    /**
     * Clears all markers and circles from the map.
     * Also clears local collections tracking these map objects.
     */
    fun clearAllPostsFromMap() {
        if (::mMap.isInitialized) { // Check if mMap is initialized
            mMap.clear() // This removes all markers, circles, polylines, etc. from the map
            displayedCircles.clear() // Clear your local collection of Circle objects
            displayedMarkers.clear() // Clear your local collection of Marker objects
            Log.d("MapFragment", "All posts cleared from the map.")

            // Optionally, if your SharedMapPostsViewModel should also reflect that no posts are shown:
            // sharedViewModel.updateMapPosts(emptyList())
            // However, this might conflict if your posts are fetched automatically.
            // Usually, clearing the map is a UI-only action unless it means posts are truly deleted.
        } else {
            Log.w("MapFragment", "Map not initialized. Cannot clear posts.")
        }
    }

// Example of where you might want to call this:
// 1. In a button click listener (e.g., a "Clear Map" button)
// binding.clearMapButton.setOnClickListener {
//     clearAllPostsFromMap()
// }


    private fun updateMapWithPosts(posts: List<Post>) {
        mMap.clear() // Clear previous markers and circles
        displayedCircles.clear()
        displayedMarkers.clear()

        posts.forEach { post ->
            val position = LatLng(post.latitude, post.longitude)
            val markerOptions = MarkerOptions()
                .position(position)
                .title(post.textContent ?: "Post by ${post.username}")
                .snippet("Radius: ${post.radius}m | Media: ${post.mediaType}")
            // TODO: Optionally change marker icon based on post.mediaType

            val marker = mMap.addMarker(markerOptions)
            marker?.tag = post // Attach the full Post object to the marker
            if (marker != null) {
                displayedMarkers[post.postId] = marker
            }


            if (post.radius > 0) {
                val circleOptions = CircleOptions()
                    .center(position)
                    .radius(post.radius) // Radius in meters
                    .strokeColor(Color.BLUE)
                    .strokeWidth(2f)
                    .fillColor(Color.parseColor("#330000FF")) // Semi-transparent blue
                val circle = mMap.addCircle(circleOptions)
                displayedCircles[post.postId] = circle
            }
        }
        Log.d("MapFragment", "Map updated with ${posts.size} posts.")
    }

    // This function will be called AFTER the user has provided all details
    // (text, media, radius) through a separate UI (e.g., a DialogFragment).
    fun createAndAddPostToFirestore(
        latLng: LatLng,
        textContent: String?,
        mediaType: MediaType,
        mediaUrl: String?,
        radius: Double
    ) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "You need to be logged in to post.", Toast.LENGTH_SHORT).show()
            return
        }

        val postCollection = db.collection("posts")
        val newPostId = postCollection.document().id

        val post = Post(
            postId = newPostId,
            userId = currentUserId,
            username = auth.currentUser?.displayName ?: "Anonymous",
            textContent = textContent,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            latitude = latLng.latitude,
            longitude = latLng.longitude,
            locationName = "Fetched Location Name", // TODO: Implement reverse geocoding if needed
            radius = radius,
            timestamp = System.currentTimeMillis()
        )

        postCollection.document(newPostId).set(post)
            .addOnSuccessListener {
                Log.d("MapFragment", "Post added successfully: $newPostId")
                Toast.makeText(requireContext(), "Post added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("MapFragment", "Error adding post", e)
                Toast.makeText(requireContext(), "Error adding post: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayMediaForPost(post: Post) {
        when (post.mediaType) {
            MediaType.IMAGE -> {
                if (post.mediaUrl != null) {
                    Log.d("MapFragment", "Display Image: ${post.mediaUrl}")
                    Toast.makeText(requireContext(), "Show Image: ${post.mediaUrl}", Toast.LENGTH_SHORT).show()
                    // TODO: Implement image viewing (e.g., new Activity or DialogFragment)
                } else {
                    Toast.makeText(requireContext(), "No image URL", Toast.LENGTH_SHORT).show()
                }
            }
            MediaType.VIDEO -> {
                if (post.mediaUrl != null) {
                    Log.d("MapFragment", "Display Video: ${post.mediaUrl}")
                    Toast.makeText(requireContext(), "Play Video: ${post.mediaUrl}", Toast.LENGTH_SHORT).show()
                    // TODO: Implement video playing (e.g., new Activity or DialogFragment)
                } else {
                    Toast.makeText(requireContext(), "No video URL", Toast.LENGTH_SHORT).show()
                }
            }
            MediaType.NONE -> {
                Toast.makeText(requireContext(), "No media for this post.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // --- MapView lifecycle methods ---
// MapFragment.kt
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
   //     postsListener?.remove() // You have this, which is good
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        // This is the correct place to destroy the MapView, as the fragment
        // itself is being destroyed.
        if (::mapView.isInitialized) {
            mapView.onDestroy()
        }
        super.onDestroy()
    }

    // In MapFragment.kt


    override fun onDestroyView() {
        // This is the most critical place to clean up anything related to the view.
        // It's called whenever the fragment's UI is being destroyed.

        // 1. Stop listening for Firestore updates to prevent callbacks on an invalid map object.
        postsListener?.remove()
        postsListener = null // Set to null to avoid reusing a removed listener
        Log.d("MapFragment", "Firestore listener removed in onDestroyView.")

        // 2. Clean up the map object.
        if (::mMap.isInitialized) {
            mMap.clear()
            mMap.setOnMapClickListener(null)
            mMap.setOnInfoWindowClickListener(null)
        }

        // 3. Let the system handle the rest of the view destruction.
        super.onDestroyView()
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

// Dummy extension function for sharedViewModel.updateMapPosts - ensure SharedMapPostsViewModel has this method
// This should be in SharedMapPostsViewModel.kt or a common place if it's an extension
// fun SharedMapPostsViewModel.updateMapPosts(currentDisplayedMapPosts: List<Post>) {
//     // Implementation in SharedMapPostsViewModel:
//     // _mapPosts.value = currentDisplayedMapPosts
// }



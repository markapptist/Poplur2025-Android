package com.example.poplur2025
// SharedMapPostsViewModel.kt
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// SharedMapPostsViewModel.kt
class SharedMapPostsViewModel(application: Application) : ViewModel() {
    private val _allPosts = MutableLiveData<List<Post>>()
    val allPosts: LiveData<List<Post>> = _allPosts
    // Option 1: ViewModel is primarily a holder for posts updated by MapFragment

    fun updateMapPosts(newPosts: List<Post>) {
        _allPosts.value = newPosts
        Log.d("SharedVM", "Updated mapPosts with ${newPosts.size} posts")
    }
    private val firestore = FirebaseFirestore.getInstance()

    init {
        fetchAllPosts()
    }

    private fun fetchAllPosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Good for timeline
            .addSnapshotListener { snapshots, e ->
                if (e != null) { /* Handle error */ return@addSnapshotListener }
                if (snapshots != null) {
                    _allPosts.value = snapshots.toObjects(Post::class.java)
                }
            }
    }

    fun clearPosts() {
        _allPosts.value = emptyList()
    }

    fun updatePosts(newPosts: List<Post>) {
        _allPosts.value = newPosts // This triggers observers
        Log.d("SharedViewModel", "Updated with ${newPosts.size} posts.")
    }
    // Add methods here if MapFragment needs to apply specific filters
    // that the ViewModel should be aware of, or provide filtered LiveData.
}
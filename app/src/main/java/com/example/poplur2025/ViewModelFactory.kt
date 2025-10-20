package com.example.poplur2025

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Add this class inside your project, for example in a file called ViewModelFactory.kt
// Or, for a quick fix, you can place it inside MapFragment.kt for now.
class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedMapPostsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedMapPostsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
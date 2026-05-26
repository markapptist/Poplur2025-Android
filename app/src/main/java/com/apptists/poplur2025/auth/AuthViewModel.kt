package com.apptists.poplur2025.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _isSignedIn = MutableStateFlow(auth.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn
    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task -> _isSignedIn.value = task.isSuccessful; onResult(task.isSuccessful, task.exception?.localizedMessage) }
    }
    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task -> _isSignedIn.value = task.isSuccessful; onResult(task.isSuccessful, task.exception?.localizedMessage) }
    }
    fun signOut() { auth.signOut(); _isSignedIn.value = false }
}

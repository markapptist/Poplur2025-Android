package com.apptists.poplur2025.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {
    private var auth: FirebaseAuth? = null
    private var listener: FirebaseAuth.AuthStateListener? = null
    private val _isSignedIn = MutableStateFlow<Boolean?>(null)
    val isSignedIn: StateFlow<Boolean?> = _isSignedIn
    val currentUserId: String? get() = auth?.currentUser?.uid
    val currentEmail: String? get() = auth?.currentUser?.email

    init {
        viewModelScope.launch {
            val firebaseAuth = withContext(Dispatchers.IO) { FirebaseAuth.getInstance() }
            auth = firebaseAuth
            _isSignedIn.value = firebaseAuth.currentUser != null
            listener = FirebaseAuth.AuthStateListener { updatedAuth ->
                _isSignedIn.value = updatedAuth.currentUser != null
            }.also(firebaseAuth::addAuthStateListener)
        }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance().also { auth = it }
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _isSignedIn.value = task.isSuccessful
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance().also { auth = it }
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                _isSignedIn.value = task.isSuccessful
                onResult(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun signOut() {
        auth?.signOut()
        _isSignedIn.value = false
    }

    override fun onCleared() {
        val firebaseAuth = auth
        val authListener = listener
        if (firebaseAuth != null && authListener != null) {
            firebaseAuth.removeAuthStateListener(authListener)
        }
        super.onCleared()
    }
}

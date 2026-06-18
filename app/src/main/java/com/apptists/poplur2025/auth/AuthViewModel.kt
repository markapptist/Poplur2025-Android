package com.apptists.poplur2025.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private var auth: FirebaseAuth? = null
    private var listener: FirebaseAuth.AuthStateListener? = null
    private val _isSignedIn = MutableStateFlow<Boolean?>(null)
    val isSignedIn: StateFlow<Boolean?> = _isSignedIn
    val currentUserId: String? get() = auth?.currentUser?.uid
    val currentEmail: String? get() = auth?.currentUser?.email

    init {
        try {
            val firebaseAuth = FirebaseAuth.getInstance()
            auth = firebaseAuth

            // Set initial auth state synchronously — no coroutine needed since
            // FirebaseAuth.getInstance() returns an existing singleton immediately.
            _isSignedIn.value = firebaseAuth.currentUser != null

            // Firebase always fires the listener once immediately after addAuthStateListener,
            // duplicating our synchronous check above. We skip that first callback and only
            // act on genuine future state changes (sign-in, sign-out, token events).
            var skipFirst = true
            listener = FirebaseAuth.AuthStateListener { updatedAuth ->
                if (skipFirst) {
                    skipFirst = false
                    return@AuthStateListener
                }
                val signedIn = updatedAuth.currentUser != null
                if (signedIn) {
                    // Sign-in: accept immediately.
                    _isSignedIn.value = true
                } else {
                    // Sign-out: debounce by 1 s to ignore transient null states Firebase can
                    // emit during token refresh at startup (would flash the auth screen).
                    viewModelScope.launch {
                        delay(1_000)
                        if (auth?.currentUser == null) {
                            _isSignedIn.value = false
                        }
                    }
                }
            }.also(firebaseAuth::addAuthStateListener)
        } catch (e: Exception) {
            _isSignedIn.value = false
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

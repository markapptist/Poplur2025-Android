package com.apptists.poplur2025.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context

class AuthViewModel : ViewModel() {
    private var auth: FirebaseAuth? = null
    private var listener: FirebaseAuth.AuthStateListener? = null
    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn
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

    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit) {
        val credentialManager = CredentialManager.create(context)

        // This is a placeholder for the Web Client ID.
        // In a real app, this should come from strings.xml or google-services.json
        val webClientId = "440480901104-placeholder.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(context = context, request = request)
                handleGoogleSignIn(result, onResult)
            } catch (e: GetCredentialException) {
                onResult(false, e.localizedMessage)
            }
        }
    }

    private fun handleGoogleSignIn(result: GetCredentialResponse, onResult: (Boolean, String?) -> Unit) {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val googleIdToken = credential.idToken
            val firebaseAuth = auth ?: FirebaseAuth.getInstance().also { auth = it }
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            firebaseAuth.signInWithCredential(authCredential)
                .addOnCompleteListener { task ->
                    _isSignedIn.value = task.isSuccessful
                    onResult(task.isSuccessful, task.exception?.localizedMessage)
                }
        } else {
            onResult(false, "Unexpected credential type")
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

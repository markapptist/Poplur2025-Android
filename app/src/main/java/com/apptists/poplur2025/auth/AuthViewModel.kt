package com.apptists.poplur2025.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apptists.poplur2025.data.AccountType
import com.apptists.poplur2025.data.BusinessAccountService
import com.apptists.poplur2025.data.BusinessProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        if (com.apptists.poplur2025.UITestFlags.authBypass) {
            // UI-test only (see UITestFlags): skip real Firebase sign-in entirely so navigation
            // tests don't depend on account creation succeeding.
            _isSignedIn.value = true
        } else {
            viewModelScope.launch(Dispatchers.Main) {
                try {
                    // Try getting instance on IO thread to prevent potential Main thread stalls
                    val firebaseAuth = withContext(Dispatchers.IO) { FirebaseAuth.getInstance() }
                    auth = firebaseAuth
                    _isSignedIn.value = firebaseAuth.currentUser != null

                    listener = FirebaseAuth.AuthStateListener { updatedAuth ->
                        _isSignedIn.value = updatedAuth.currentUser != null
                    }.also(firebaseAuth::addAuthStateListener)
                } catch (e: Exception) {
                    _isSignedIn.value = false
                }
            }
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

    fun signUp(
        email: String,
        password: String,
        accountType: AccountType = AccountType.User,
        businessProfile: BusinessProfile? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        val firebaseAuth = auth ?: FirebaseAuth.getInstance().also { auth = it }
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _isSignedIn.value = false
                    onResult(false, task.exception?.localizedMessage)
                    return@addOnCompleteListener
                }

                val uid = task.result?.user?.uid
                if (uid == null) {
                    _isSignedIn.value = false
                    onResult(false, "Could not create your Poplur account.")
                    return@addOnCompleteListener
                }

                BusinessAccountService.saveAccount(
                    uid = uid,
                    email = email,
                    accountType = accountType,
                    businessProfile = businessProfile
                ) { ok, message ->
                    _isSignedIn.value = ok
                    onResult(ok, message)
                }
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

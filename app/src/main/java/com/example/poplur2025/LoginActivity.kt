package com.example.poplur2025

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.text
import com.google.firebase.auth.FirebaseAuth
import com.example.poplur2025.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Listener for the existing Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            // Navigate to your main activity or dashboard
                            val intent = Intent(this, MainActivity::class.java) // Or your main content activity
                            startActivity(intent)
                            finish() // Finish LoginActivity so user can't go back to it
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- NEW: Listener for the "Go to Sign Up" button ---
        binding.btnGoToSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            // Optionally, you can finish LoginActivity if you don't want the user to go back to it
            // after navigating to sign up, but usually, it's kept.
            // finish()
        }

        // If you used a TextView instead of a Button:
        // binding.tvGoToSignup.setOnClickListener {
        //     val intent = Intent(this, SignUpActivity::class.java)
        //     startActivity(intent)
        // }
    }


    // Optional: Check if user is already logged in on start
    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // User is already logged in, navigate to main activity
            // val intent = Intent(this, MainActivity::class.java)
            // startActivity(intent)
            // finish()
        }
    }

    private fun loginUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login success
                    val user = firebaseAuth.currentUser
                    Toast.makeText(baseContext, "Login successful", Toast.LENGTH_SHORT).show()
                    // Navigate to MapActivity or HomeActivity
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    finish() // Close the LoginActivity
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

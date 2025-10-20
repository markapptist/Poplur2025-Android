package com.example.poplur2025 // Replace with your actual package name

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
// Remove EditText imports if they were only for the inline login form
// import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible // For easy visibility toggling
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.poplur2025.databinding.FragmentProfileBinding // Import ViewBinding class
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var currentUser: FirebaseUser? = null
    private val sharedViewModel: SharedMapPostsViewModel by activityViewModels()
    private lateinit var profilePostsAdapter: TimelineAdapter // Reuse or create a similar adapter
    private lateinit var recyclerViewProfilePosts: RecyclerView

    // UI Elements are now accessed via binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root // Get the root view        recyclerViewProfilePosts = view.findViewById(R.id.rv_profile_posts) // Ensure this ID exists
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        setupAuthStateListener()
        setupClickListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       super.onViewCreated(view, savedInstanceState)

        if (currentUser == null) {
            // Handle case where user is not logged in (e.g., show message, navigate to login)
           Log.w("ProfileFragment", "User is not logged in.")
           // binding.tvProfileMessage.text = "Please log in to see your posts."
           // binding.tvProfileMessage.visibility = View.VISIBLE
//            recyclerViewProfilePosts.visibility = View.GONE
           return
        }
        recyclerViewProfilePosts = view.findViewById(R.id.rv_profile_posts) // Replace with your actual ID
       setupRecyclerView()
       observeViewModel()


    }

    private fun setupRecyclerView() {
        profilePostsAdapter = TimelineAdapter { post ->
            // Handle post click on profile page, e.g., open post detail
            Log.d("ProfileFragment", "Clicked on post: ${post.postId}")
        }
        binding.rvProfilePosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = profilePostsAdapter
        }
    }


    private fun observeViewModel() {
        sharedViewModel.allPosts.observe(viewLifecycleOwner) { allPosts ->
            if (currentUser != null) {
                val myPosts = allPosts.filter { post ->
                    post.userId == currentUser!!.uid
                }.sortedByDescending { it.timestamp } // Show newest first

                Log.d("ProfileFragment", "Displaying ${myPosts.size} posts for user ${currentUser!!.uid}.")
                profilePostsAdapter.submitList(myPosts)

                if (myPosts.isEmpty()) {
                    // binding.tvProfileMessage.text = "You haven't made any posts yet."
                    // binding.tvProfileMessage.visibility = View.VISIBLE
                } else {
                    // binding.tvProfileMessage.visibility = View.GONE
                }
            } else {
                profilePostsAdapter.submitList(emptyList()) // Clear posts if user logs out or issue
            }
        }
    }


    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            updateUI(currentUser)
        }
    }


    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener { // Assuming btn_logout is in profile_section_logged_in
            signOutUser()
        }

        // --- NEW: Listener for the "Go to Login" button ---
        binding.btnGoToLogin.setOnClickListener { // Assuming btn_go_to_login is in profile_section_logged_out
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            // Optional: If LoginActivity is part of a different navigation graph flow,
            // you might want to clear the task stack or use specific launch modes.
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }


    private fun signOutUser() {
        firebaseAuth.signOut()
        Log.d("ProfileFragment", "User signed out.")
        Toast.makeText(requireContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show()
        // AuthStateListener will handle UI update
    }



    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // User is signed in
            binding.profileSectionLoggedIn.isVisible = true
            binding.profileSectionLoggedOut.isVisible = false // Hide the logged out section

            binding.tvUserEmail.text = "Email: ${user.email}"
            binding.tvUserDisplayName.text = if (user.displayName.isNullOrEmpty()) {
                "Display Name: Not set"
            } else {
                "Display Name: ${user.displayName}"
            }
        } else {
            // User is signed out
            binding.profileSectionLoggedIn.isVisible = false
            binding.profileSectionLoggedOut.isVisible = true // Show the logged out section with Sign In button

            // If you had an inline login form here before, ensure its elements are cleared or hidden.
            // For example, if emailEditText and passwordEditText were part of the old login_section:
            // binding.emailEditText.text.clear()
            // binding.passwordEditText.text.clear()
        }
    }


    override fun onStart() {
        super.onStart()
        authStateListener?.let {
            firebaseAuth.addAuthStateListener(it)
        }
    }

    override fun onStop() {
        super.onStop()
        authStateListener?.let {
            firebaseAuth.removeAuthStateListener(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to clear binding reference
    }

    companion object {
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }


}


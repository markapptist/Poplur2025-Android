import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isEmpty
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poplur2025.Post // Your Post data class
import com.example.poplur2025.TimelineAdapter // Your Adapter
import com.example.poplur2025.databinding.FragmentHomeBinding // Assuming ViewBinding for fragment_home
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query // For ordering
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import com.example.poplur2025.SharedMapPostsViewModel
import com.example.poplur2025.ViewModelFactory
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    // private lateinit var homeViewModel: HomeViewModel // If using ViewModel
    private val sharedViewModel: SharedMapPostsViewModel by activityViewModels {
        ViewModelFactory(requireActivity().application)
    }
    private lateinit var timelineAdapter: TimelineAdapter // Initialize this in onViewCreated/onCreateView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // --- FIX: ONLY return the view here. Do not set up listeners or load data. ---
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- FIX: Move all setup logic here. ---
        // It's safe to access 'binding' and 'requireContext()' here.
        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        observeSharedViewModel() // Use this if you want timeline to show posts from the map
        // loadPosts() // Or use this if you want the timeline to have its own independent posts
    }

    private fun setupRecyclerView() {
        timelineAdapter = TimelineAdapter { post ->
            // Handle post click
            Toast.makeText(requireContext(), "Clicked: ${post.textContent}", Toast.LENGTH_SHORT).show()
        }
        binding.rvTimeline.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = timelineAdapter
        }
    }

    private fun observeSharedViewModel() {
        sharedViewModel.allPosts.observe(viewLifecycleOwner) { postsFromMap ->
            Log.d("HomeFragment", "Timeline received ${postsFromMap.size} posts")
            val sortedTimelinePosts = postsFromMap.sortedByDescending { it.timestamp }
            timelineAdapter.submitList(sortedTimelinePosts)

            if (sortedTimelinePosts.isEmpty()) {
                binding.homeTitle.text = "Timeline (No posts from map yet)"
            } else {
                binding.homeTitle.text = "Timeline"
            }
        }
    }





    private fun loadPosts() {
        // Fetch posts from Firestore (or your data source)
        // Order by timestamp in descending order for a typical timeline
        firestore.collection("posts") // Assuming your collection is named "posts"
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Optional: Add a limit
            .addSnapshotListener { snapshots, e -> // Use addSnapshotListener for real-time updates
                if (e != null) {
                    Log.w("HomeFragment", "Listen failed.", e)
                    Toast.makeText(requireContext(), "Failed to load posts.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val postsList = snapshots.toObjects(Post::class.java)
                    timelineAdapter.submitList(postsList)
                    if (postsList.isEmpty()) {
                        binding.homeTitle.text = "Timeline (No posts yet)" // Update title if empty
                    } else {
                        binding.homeTitle.text = "Timeline"
                    }
                }
            }
    }

    // Example if using ViewModel and LiveData
    /*
    private fun observeViewModel() {
        homeViewModel.posts.observe(viewLifecycleOwner) { posts ->
            timelineAdapter.submitList(posts)
             if (posts.isEmpty()) {
                binding.homeTitle.text = "Timeline (No posts yet)"
            } else {
                binding.homeTitle.text = "Timeline"
            }
        }
        homeViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }
    */


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important for Fragments with ViewBinding
    }
}


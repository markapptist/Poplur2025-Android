package com.example.poplur2025

// Make sure your imports are correct for your project structure
import HomeFragment
import com.example.poplur2025.MapFragment
import com.example.poplur2025.ProfileFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // --- FIX: Create and hold instances of your fragments ---
    private val fragments: List<Fragment> = listOf(
        HomeFragment(),
        MapFragment(),
        ProfileFragment()
    )

    // The number of tabs is now derived from the size of the fragments list.
    override fun getItemCount(): Int {
        return fragments.size
    }

    // --- FIX: Return the existing fragment instance ---
    override fun createFragment(position: Int): Fragment {
        // Return the pre-created fragment from the list.
        return fragments[position]
    }
}

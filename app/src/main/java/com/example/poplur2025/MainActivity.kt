package com.example.poplur2025

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide();

        // ... inside onCreate or onCreateView
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)

        val adapter = ViewPagerAdapter(this) // Your FragmentStateAdapter
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Home" // Or set an icon, or both
                1 -> tab.text = "Map"   // Or whatever your second tab is
                2 -> tab.text = "Profile"// Or whatever your third tab is
                // Add more cases if you have more tabs
            }
        }.attach()

        // Crucially, your ViewPagerAdapter must be set up to return fragments
        // and a correct item count.
    }
    }

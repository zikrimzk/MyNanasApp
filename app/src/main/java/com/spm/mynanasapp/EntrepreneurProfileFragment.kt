package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class EntrepreneurProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup ViewPager and Tabs
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)

        // Set the adapter
        val adapter = ProfilePagerAdapter(this)
        viewPager.adapter = adapter

        // Link Tabs to ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Posts" // Optional: Remove text if you only want icons
                    tab.icon = requireContext().getDrawable(R.drawable.ic_tab_posts)
                }
                1 -> {
                    tab.text = "Products"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_tab_products)
                }
                2 -> {
                    tab.text = "Farm"
                    tab.icon = requireContext().getDrawable(R.drawable.ic_tab_farm)
                }
            }
        }.attach()
    }

    // --- INNER ADAPTER FOR TABS ---
    // This switches the content at the bottom based on the tab selected
    inner class ProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            // In a real app, you would return specific fragments:
            // 0 -> ProfilePostsFragment()
            // 1 -> ProfileProductsFragment()
            // 2 -> ProfileFarmFragment()

            // For now, we return a reusable Placeholder Fragment
            return PlaceholderTabFragment.newInstance(position)
        }
    }
}

// --- PLACEHOLDER FRAGMENT CLASS ---
// (Normally this would be in a separate file, but kept here for simplicity)
class PlaceholderTabFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.item_tab_placeholder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Customize text based on which tab we are in
        val pos = arguments?.getInt("POS") ?: 0
        val tvTitle = view.findViewById<TextView>(R.id.tv_placeholder_title)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_placeholder_icon)

        when(pos) {
            0 -> {
                tvTitle.text = "No Posts Yet"
                ivIcon.setImageResource(R.drawable.ic_tab_posts)
            }
            1 -> {
                tvTitle.text = "No Products"
                ivIcon.setImageResource(R.drawable.ic_tab_products)
            }
            2 -> {
                tvTitle.text = "No Farm Data"
                ivIcon.setImageResource(R.drawable.ic_tab_farm)
            }
        }
    }

    companion object {
        fun newInstance(position: Int) = PlaceholderTabFragment().apply {
            arguments = Bundle().apply { putInt("POS", position) }
        }
    }
}
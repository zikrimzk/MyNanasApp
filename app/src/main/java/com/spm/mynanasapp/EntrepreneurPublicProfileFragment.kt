package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class EntrepreneurPublicProfileFragment : Fragment() {

    // Data Variables
    private var userId: Long = 0
    private var username: String? = null
    private var fullname: String? = null
    private var bio: String? = null
    private var role: String? = "Entrepreneur"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getLong("USER_ID")
            username = it.getString("USERNAME")
            fullname = it.getString("FULLNAME")
            bio = it.getString("BIO")
            role = it.getString("ROLE")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_public_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        val tvToolbarUsername = view.findViewById<TextView>(R.id.tv_toolbar_username)
        val tvFullName = view.findViewById<TextView>(R.id.tv_fullname)
        val tvBio = view.findViewById<TextView>(R.id.tv_bio)
        val tvCategory = view.findViewById<TextView>(R.id.tv_category) // The Role
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)

        // 2. Bind Data
        tvToolbarUsername.text = username ?: "User"
        tvFullName.text = fullname ?: "Entrepreneur"
        tvBio.text = bio ?: "No bio available."
        tvCategory.text = role ?: "Entrepreneur"

        // 3. Back Action
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 4. Setup Tabs
        setupTabs(view)
    }

    private fun setupTabs(view: View) {
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)

        // Pass 'false' for isEditable so buttons are hidden
        val adapter = PublicProfilePagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            try {
                when (position) {
                    0 -> tab.icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_tab_posts)
                    1 -> tab.icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_tab_products)
                    2 -> tab.icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_tab_farm)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.attach()
    }

    // --- HIDE BOTTOM NAV LOGIC ---
    override fun onResume() {
        super.onResume()
        // Always hide bottom bar when viewing a public profile
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    // --- ADAPTER ---
    inner class PublicProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            // isEditable = false -> Hides "Create" buttons
            return PlaceholderTabFragment.newInstance(position, isEditable = false)
        }
    }

    companion object {
        fun newInstance(userId: Long, username: String, fullname: String, bio: String = "", role: String = "Entrepreneur") =
            EntrepreneurPublicProfileFragment().apply {
                arguments = Bundle().apply {
                    putLong("USER_ID", userId)
                    putString("USERNAME", username)
                    putString("FULLNAME", fullname)
                    putString("BIO", bio)
                    putString("ROLE", role)
                }
            }
    }
}
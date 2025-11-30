package com.spm.mynanasapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

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

        // Value Initialization
        val currentUser = SessionManager.getUser(requireContext())
        val tvUsername = view.findViewById<TextView>(R.id.tv_toolbar_usernames)
        val tvFullName = view.findViewById<TextView>(R.id.tv_fullname)
        val btnSetting = view.findViewById<ImageView>(R.id.btn_settings)

        tvUsername.text = "@" + currentUser?.ent_username
        tvFullName.text = currentUser?.ent_fullname

        btnSetting.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    // 2. Perform Logout Logic
                    performLogout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
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

    private fun performLogout() {
        // Show a loading state if you want (e.g., disable button)
        // btnSetting.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Get the current token
                val token = SessionManager.getToken(requireContext())

                // If no token exists locally, just force clear and leave
                if (token == null) {
                    clearSessionAndRedirect()
                    return@launch
                }

                // 2. Call the API
                // Note: We manually add "Bearer " because your @Header parameter expects the full string
                val response = RetrofitClient.instance.logout("Bearer $token")

                if (response.isSuccessful) {
                    // === STATUS 200 OK ===
                    val baseResponse = response.body()

                    // Check the API "status" field (true/false)
                    if (baseResponse != null && baseResponse.status) {
                        // Show the message from the server: "Logged out successfully"
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                    } else {
                        // API connected, but logic failed (unlikely for logout, but good safety)
                        val msg = baseResponse?.message ?: "Logout failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // === STATUS 401, 500, etc ===
                    // If token is expired (401), the server rejects it.
                    // We just log it for debugging.
                    Log.e("Logout", "Server failed: ${response.code()}")
                }

            } catch (e: Exception) {
                // Network failure (No internet)
                Log.e("Logout", "Network error: ${e.message}")
                Toast.makeText(context, "Offline logout", Toast.LENGTH_SHORT).show()
            } finally {
                // 3. CRITICAL: Always clear local session!
                // Whether the server said "Success", "Error 500", or "No Internet",
                // the user WANTS to leave. We must delete their local data.
                clearSessionAndRedirect()
            }
        }
    }

    private fun clearSessionAndRedirect() {
        // 1. Clear SharedPreferences
        SessionManager.clearSession(requireContext()) // Ensure this clears User AND Token

        // 2. Clear Retrofit Memory
        RetrofitClient.setToken(null)

        // 3. Navigate back to Main Activity
        // 3. Navigate back to Main Activity
        val mainIntent = Intent(requireActivity(), MainActivity::class.java)
        // Clear the back stack so pressing "Back" doesn't return to the portal
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(mainIntent)
        requireActivity().finish()
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
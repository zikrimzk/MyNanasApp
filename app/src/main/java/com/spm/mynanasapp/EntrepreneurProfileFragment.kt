package com.spm.mynanasapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup ViewPager and Tabs
        setupTabs(view)

        // 2. Initialize UI with Session Data
        setupProfileData(view)

        // 3. Setup Click Listeners (Search, Logout, Edit)
        setupClickListeners(view)
    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(true)
    }

    private fun setupTabs(view: View) {
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)

        // Set the adapter
        val adapter = ProfilePagerAdapter(this)
        viewPager.adapter = adapter

        // Link Tabs to ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    // Posts Tab
                    tab.icon = requireContext().getDrawable(R.drawable.ic_tab_posts)
                }

                1 -> {
                    // Products Tab
                    tab.icon = requireContext().getDrawable(R.drawable.ic_tab_products)
                }

                2 -> {
                    // Premise/Farm Tab
                    tab.icon = requireContext().getDrawable(R.drawable.ic_tab_farm)
                }
            }
        }.attach()
    }

    @SuppressLint("SetTextI18n")
    private fun setupProfileData(view: View) {
        // Get User Session
        val currentUser = SessionManager.getUser(requireContext())

        // Find Views (Based on new XML Layout)
        val tvToolbarUsername = view.findViewById<TextView>(R.id.tv_toolbar_username)
        val tvFullName = view.findViewById<TextView>(R.id.tv_fullname)
        val tvBio = view.findViewById<TextView>(R.id.tv_bio)
        val ivProfile = view.findViewById<ImageView>(R.id.iv_profile)

        // Stats Views
        val tvStatPosts = view.findViewById<TextView>(R.id.tv_stat_posts)
        val tvStatProducts = view.findViewById<TextView>(R.id.tv_stat_products)
        val tvStatPineapples = view.findViewById<TextView>(R.id.tv_stat_pineapples)

        // Populate Data
        // If username is null, fallback to "Entrepreneur"
        tvToolbarUsername.text = currentUser?.ent_username ?: "Entrepreneur"
        tvFullName.text = currentUser?.ent_fullname ?: "MyNanas User"
        tvBio.text = currentUser?.ent_bio ?: "No bio available."
        if (!currentUser?.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + currentUser?.ent_profilePhoto
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_launcher_background) // Replace with your default avatar
                .into(ivProfile)
        }

        tvStatPosts.text = currentUser?.total_posts.toString()
        tvStatProducts.text = currentUser?.total_products.toString()
        tvStatPineapples.text = currentUser?.total_likes.toString()
    }

    private fun setupClickListeners(view: View) {
        val btnLogout = view.findViewById<ImageView>(R.id.btn_logout)
        val btnSearch = view.findViewById<ImageView>(R.id.btn_search)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btn_edit_profile)

        // Search Action
        btnSearch.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, SearchEntrepreneurFragment())
                .addToBackStack(null)
                .commit()
        }

        // Edit Profile Action
        btnEditProfile.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_up,
                    R.anim.stay_still,
                    R.anim.stay_still,
                    R.anim.slide_out_down
                )
                .replace(R.id.nav_host_fragment, EntrepreneurEditProfileFragment())
                .addToBackStack(null)
                .commit()
        }


        // Logout Action
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // --- TABS ADAPTER ---
    class ProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ProfilePostsFragment()
                1 -> ProfileProductFragment()
                2 -> ProfilePremiseFragment()
                else -> PlaceholderTabFragment.newInstance(position, isEditable = true)
            }
        }
    }

    // --- LOGOUT LOGIC ---
    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(requireContext())

                if (token == null) {
                    clearSessionAndRedirect()
                    return@launch
                }

                val response = RetrofitClient.instance.logout("Bearer $token")

                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    if (baseResponse != null && baseResponse.status) {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = baseResponse?.message ?: "Logout failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("Logout", "Server failed: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e("Logout", "Network error: ${e.message}")
                Toast.makeText(context, "Offline logout", Toast.LENGTH_SHORT).show()
            } finally {
                clearSessionAndRedirect()
            }
        }
    }

    private fun clearSessionAndRedirect() {
        SessionManager.clearSession(requireContext())
        RetrofitClient.setToken(null)

        val mainIntent = Intent(requireActivity(), MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(mainIntent)
        requireActivity().finish()
    }
}
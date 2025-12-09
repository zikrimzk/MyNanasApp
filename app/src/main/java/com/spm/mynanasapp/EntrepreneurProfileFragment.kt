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
import com.spm.mynanasapp.data.model.request.GetUsersRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class EntrepreneurProfileFragment : Fragment() {

    private lateinit var tvStatPosts: TextView
    private lateinit var tvStatProducts: TextView
    private lateinit var tvStatPineapples: TextView
    private lateinit var tvToolbarUsername: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvBio: TextView
    private lateinit var ivProfile: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_profile, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Initialize View References
        initViews(view)

        // 2. Setup ViewPager and Tabs
        setupTabs(view)

        // 3. Populate Initial Data from Local Session (Fast)
        displayLocalData()

        // 4. Setup Click Listeners (Search, Logout, Edit)
        setupClickListeners(view)
    }

    private fun initViews(view: View) {
        tvToolbarUsername = view.findViewById(R.id.tv_toolbar_username)
        tvFullName = view.findViewById(R.id.tv_fullname)
        tvBio = view.findViewById(R.id.tv_bio)
        ivProfile = view.findViewById(R.id.iv_profile)
        tvStatPosts = view.findViewById(R.id.tv_stat_posts)
        tvStatProducts = view.findViewById(R.id.tv_stat_products)
        tvStatPineapples = view.findViewById(R.id.tv_stat_pineapples)
    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(true)

        refreshProfileData()
    }

    @SuppressLint("SetTextI18n")
    private fun displayLocalData() {
        val currentUser = SessionManager.getUser(requireContext()) ?: return

        tvToolbarUsername.text = "@${currentUser.ent_username ?: "User"}"
        tvFullName.text = currentUser.ent_fullname
        tvBio.text = currentUser.ent_bio ?: "No bio available."

        tvStatPosts.text = currentUser.total_posts.toString()
        tvStatProducts.text = currentUser.total_products.toString()
        tvStatPineapples.text = currentUser.total_likes.toString()

        if (!currentUser.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + currentUser.ent_profilePhoto
            Glide.with(this).
                load(fullUrl).
                placeholder(R.drawable.placeholder_versatile).
                into(ivProfile)
        }
    }

    private fun refreshProfileData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = context ?: return@launch
            val token = SessionManager.getToken(context) ?: return@launch
            val currentUser = SessionManager.getUser(context) ?: return@launch

            // Request logic: "Get Specific User" -> "Me"
            val request = GetUsersRequest(specific_user = true, entID = currentUser.entID)

            try {
                // We reuse the getSpecificUser endpoint we made for the Public Profile
                val response = RetrofitClient.instance.getSpecificUser("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    val freshUser = response.body()!!.data

                    if (freshUser != null) {
                        // 1. Update Session Manager (So other fragments get new data too)
                        SessionManager.saveUser(context, freshUser)

                        // 2. Update UI
                        displayLocalData()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Failed to refresh stats", e)
            }
        }
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
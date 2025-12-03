package com.spm.mynanasapp

import android.os.Bundle
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
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.spm.mynanasapp.data.model.request.GetUsersRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class EntrepreneurPublicProfileFragment : Fragment() {

    // Data Variables
    private var userId: Long = 0
    private var username: String? = null
    private var fullname: String? = null

    // Views
    private lateinit var tvToolbarUsername: TextView
    private lateinit var tvFullName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvCategory: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var tvStatPosts: TextView
    private lateinit var tvStatProducts: TextView
    private lateinit var tvStatPineapples: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getLong("USER_ID")
            username = it.getString("USERNAME")
            fullname = it.getString("FULLNAME")
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
        tvToolbarUsername = view.findViewById(R.id.tv_toolbar_username)
        tvFullName = view.findViewById(R.id.tv_fullname)
        tvBio = view.findViewById(R.id.tv_bio)
        tvCategory = view.findViewById(R.id.tv_category)
        ivProfile = view.findViewById(R.id.iv_profile)
        tvStatPosts = view.findViewById(R.id.tv_stat_posts)
        tvStatProducts = view.findViewById(R.id.tv_stat_products)
        tvStatPineapples = view.findViewById(R.id.tv_stat_pineapples)
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)

        // 2. Bind Data
        tvToolbarUsername.text = "@${username ?: "User"}"
        tvFullName.text = fullname ?: "Entrepreneur"
        tvCategory.text = "Entrepreneur" // Default until loaded
        loadUserProfile()

        // 3. Back Action
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 4. Setup Tabs
        setupTabs(view)
    }

    private fun loadUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // Fetch Specific User
            val request = GetUsersRequest(specific_user = true, entID = userId)

            try {
                // Call the endpoint that returns Single User
                val response = RetrofitClient.instance.getSpecificUser("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    val user = response.body()!!.data

                    if (user != null) {
                        tvToolbarUsername.text = "@${user.ent_username}"
                        tvFullName.text = user.ent_fullname
                        tvBio.text = user.ent_bio ?: "No bio available."

                        // Load Profile Photo
                        if (!user.ent_profilePhoto.isNullOrEmpty()) {
                            Glide.with(this@EntrepreneurPublicProfileFragment)
                                .load(RetrofitClient.SERVER_IMAGE_URL + user.ent_profilePhoto)
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(ivProfile)
                        }

                        tvStatPosts.text = user?.total_posts.toString()
                        tvStatProducts.text = user?.total_products.toString()
                        tvStatPineapples.text = user?.total_likes.toString()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
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
package com.spm.mynanasapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class EntrepreneurFeedFragment : Fragment() {

    // Define Adapter and List at the class level so we can update them when API responds
    private lateinit var feedAdapter: FeedAdapter
    private val postsList = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_entrepreneur_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        setupRecyclerView(view)

        // 2. Setup Swipe Refresh Logic
        setupSwipeRefresh(view)

        // 3. Setup Navigation (Add Post)
        setupNavigation(view)

        // 4. Initial Data Load
        loadPostsFromApi()
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_feed)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize Adapter with empty list first, or existing data
        feedAdapter = FeedAdapter(postsList)
        recyclerView.adapter = feedAdapter
    }

    private fun setupSwipeRefresh(view: View) {
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)

        swipeRefresh.setOnRefreshListener {
            // Trigger API reload when user pulls down
            loadPostsFromApi()
        }
    }

    private fun setupNavigation(view: View) {
        view.findViewById<View>(R.id.btn_add_post).setOnClickListener {
            parentFragmentManager.beginTransaction()
                // (enter, exit, popEnter, popExit)
                .setCustomAnimations(
                    R.anim.slide_in_up,
                    R.anim.stay_still,
                    R.anim.stay_still,
                    R.anim.slide_out_down
                )
                .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    /**
     * TODO: BACKEND TEAM - API INTEGRATION GUIDE
     * 1. This function is called when the screen loads or when the user swipes to refresh.
     * 2. Replace the 'Dummy Data' block below with your Retrofit/API call.
     * 3. Endpoint suggestion: GET /api/v1/feed
     */
    private fun loadPostsFromApi() {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        // --- START OF MOCK DATA (Replace this block with API Call) ---
        Handler(Looper.getMainLooper()).postDelayed({

            // Clear old data to avoid duplicates
            postsList.clear()

            // Add new data (In real app, this comes from response.body())
            postsList.add(
                Post(1, "mof_malaysia", "Putrajaya, MY", "2h ago",
                    "Budget 2025 allocations for SME digitalization grants are now open for application.",
                    listOf(R.drawable.pineapple_1), "1.2k", 450)
            )
            postsList.add(
                Post(2, "zikrimzk", null, "5h ago",
                    "Just registered my pineapple farm on MyNanas! The process was super smooth.",
                    null, "850", 230)
            )
            postsList.add(
                Post(3, "mynanas_official", "Kuala Lumpur", "1d ago",
                    "System maintenance scheduled for Sunday 2 AM to 4 AM.",
                    listOf(R.drawable.pineapple_2), "3.1k", 120)
            )
            postsList.add(
                Post(4, "awani501", "Johor Bahru", "2d ago",
                    "Breaking: New export incentives announced for agricultural sector.",
                    listOf(R.drawable.pineapple_1, R.drawable.pineapple_2), "2.2k", 560)
            )

            // CRITICAL: Tell the adapter the data changed so it refreshes the UI
            feedAdapter.notifyDataSetChanged()

            // Stop the loading spinner
            swipeRefresh?.isRefreshing = false

        }, 300)
        // --- END OF MOCK DATA ---

        /* TODO: EXAMPLE RETROFIT IMPLEMENTATION

           lifecycleScope.launch {
               try {
                   val response = apiService.getFeedPosts()
                   if (response.isSuccessful && response.body() != null) {
                       postsList.clear()
                       postsList.addAll(response.body()!!)
                       feedAdapter.notifyDataSetChanged()
                   }
               } catch (e: Exception) {
                   Toast.makeText(context, "Error loading feed", Toast.LENGTH_SHORT).show()
               } finally {
                   swipeRefresh?.isRefreshing = false
               }
           }
        */
    }
}
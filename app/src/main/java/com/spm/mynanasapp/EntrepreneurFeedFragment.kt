package com.spm.mynanasapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.ChipGroup

class EntrepreneurFeedFragment : Fragment() {

    private lateinit var feedAdapter: FeedAdapter

    // Master list holds ALL data fetched from API
    private val masterPostList = mutableListOf<Post>()

    // Display list is what the adapter shows (filtered)
    private val displayPostList = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_feed)
        recyclerView.layoutManager = LinearLayoutManager(context)
        feedAdapter = FeedAdapter(displayPostList)
        recyclerView.adapter = feedAdapter

        // 2. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        swipeRefresh.setOnRefreshListener { loadPostsFromApi() }

        // 3. Setup Filter Logic
        setupFilterChips(view)

        // 4. Setup Navigation
        view.findViewById<View>(R.id.btn_add_post).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                .addToBackStack(null)
                .commit()
        }

        // 5. Load Data
        loadPostsFromApi()
    }

    private fun setupFilterChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filter)

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chip_all -> filterList("All")
                    R.id.chip_announcement -> filterList("Announcement")
                    R.id.chip_community -> filterList("Community")
                }
            }
        }
    }

    private fun filterList(type: String) {
        displayPostList.clear()

        if (type == "All") {
            displayPostList.addAll(masterPostList)
        } else {
            // Filter logic
            val filtered = masterPostList.filter { it.type == type }
            displayPostList.addAll(filtered)
        }

        feedAdapter.notifyDataSetChanged()

        // Handle Empty State
        val emptyState = view?.findViewById<View>(R.id.layout_empty_state)
        val tvEmpty = view?.findViewById<TextView>(R.id.tv_empty_title)

        if (displayPostList.isEmpty()) {
            emptyState?.visibility = View.VISIBLE
            tvEmpty?.text = "No $type posts yet"
        } else {
            emptyState?.visibility = View.GONE
        }
    }

    private fun loadPostsFromApi() {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)

        Handler(Looper.getMainLooper()).postDelayed({
            masterPostList.clear()

            // Added 'type' to the dummy data
            masterPostList.add(Post(1, "Announcement", "mof_malaysia", "Putrajaya", "2h ago", "Budget 2025 allocations open.", listOf(R.drawable.pineapple_1), "1.2k", 450))
            masterPostList.add(Post(2, "Community", "zikrimzk", null, "5h ago", "Just registered my pineapple farm!", null, "850", 230))
            masterPostList.add(Post(3, "Announcement", "mynanas_official", "KL", "1d ago", "System maintenance notice.", listOf(R.drawable.pineapple_2), "3.1k", 120))
            masterPostList.add(Post(4, "Community", "awani501", "Johor", "2d ago", "Export incentives announced.", listOf(R.drawable.pineapple_1, R.drawable.pineapple_2), "2.2k", 560))

            // After loading, apply the current filter (Default: All)
            val chipGroup = view?.findViewById<ChipGroup>(R.id.chip_group_filter)
            when (chipGroup?.checkedChipId) {
                R.id.chip_announcement -> filterList("Announcement")
                R.id.chip_community -> filterList("Community")
                else -> filterList("All")
            }

            swipeRefresh?.isRefreshing = false
        }, 1000)
    }
}
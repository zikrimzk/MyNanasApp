package com.spm.mynanasapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.request.GetPostRequest
import com.spm.mynanasapp.data.model.request.LoginRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import com.spm.mynanasapp.data.model.entity.Post

class EntrepreneurFeedFragment : Fragment() {

    private lateinit var feedAdapter: FeedAdapter

//    // Master list holds ALL data fetched from API
////    private val masterPostList = mutableListOf<Post>()
//    private var masterPostList: List<Post> = emptyList()
//
//    // Display list is what the adapter shows (filtered)
//    private var displayPostList = mutableListOf<Post>()

    // This is the SINGLE list that connects to the Recycler View
    private val postsForAdapter = mutableListOf<Post>()

    // Track current filter to handle SwipeRefresh correctly
    private var currentFilterType = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView (FIXED HERE)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_feed)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter with the list we defined at the top
        feedAdapter = FeedAdapter(postsForAdapter)

        // Attach the adapter to the RecyclerView
        recyclerView.adapter = feedAdapter

        // 2. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        // On refresh, reload using the CURRENT filter
        swipeRefresh.setOnRefreshListener { loadPostsFromApi(currentFilterType) }

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
        // Now this is safe to call because feedAdapter is initialized above
        loadPostsFromApi("All")
    }

    private fun setupFilterChips(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filter)

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                // When chip changes, update current type and CALL API
                when (checkedIds[0]) {
                    R.id.chip_all -> loadPostsFromApi("All")
                    R.id.chip_announcement -> loadPostsFromApi("Announcement")
                    R.id.chip_community -> loadPostsFromApi("Community")
                }
            } else {
                // Fallback if user unchecks a chip
                loadPostsFromApi("All")
            }
        }
    }

//    private fun filterList(type: String) {
//        displayPostList.clear()
//
//        if (type == "All") {
//            displayPostList.addAll(masterPostList)
//        } else {
//            // Filter logic
//            val filtered = masterPostList.filter { it.post_type == type }
//            displayPostList.addAll(filtered)
//        }
//
//        feedAdapter.notifyDataSetChanged()
//
//        // Handle Empty State
//        val emptyState = view?.findViewById<View>(R.id.layout_empty_state)
//        val tvEmpty = view?.findViewById<TextView>(R.id.tv_empty_title)
//
//        if (displayPostList.isEmpty()) {
//            emptyState?.visibility = View.VISIBLE
//            tvEmpty?.text = "No $type posts yet"
//        } else {
//            emptyState?.visibility = View.GONE
//        }
//    }

    private fun loadPostsFromApi(postType: String) {
        currentFilterType = postType // Save state for SwipeRefresh

        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val emptyState = view?.findViewById<View>(R.id.layout_empty_state)
        val tvEmpty = view?.findViewById<TextView>(R.id.tv_empty_title)

        // Show loading if not using swipe refresh
        if (swipeRefresh?.isRefreshing == false) {
            swipeRefresh.isRefreshing = true
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    clearSessionAndRedirect()
                    return@launch
                }

                // 1. Create Request with the specific type
                val getPostRequest = GetPostRequest(post_type = postType)

                // 2. Call API
                val response = RetrofitClient.instance.getPosts("Bearer $token", getPostRequest)

                if (response.isSuccessful && response.body() != null) {
                    val baseResponse = response.body()!!

                    if (baseResponse.status) {
                        // === SUCCESS ===
                        val apiList = baseResponse.data ?: emptyList()

                        // Clear current UI List
                        postsForAdapter.clear()

                        // Add new data from API (Sorted by ID descending to show newest first)
                        postsForAdapter.addAll(apiList.sortedByDescending { it.postID })

                        // Update Adapter
                        feedAdapter.notifyDataSetChanged()

                        // Handle Empty State
                        if (postsForAdapter.isEmpty()) {
                            emptyState?.visibility = View.VISIBLE
                            tvEmpty?.text = "No $postType posts yet"
                        } else {
                            emptyState?.visibility = View.GONE
                        }

                    } else {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                    }

                } else {
                    // === API ERROR ===
                    handleApiError(response)
                }
            } catch (e: Exception) {
                // === NETWORK ERROR ===
                Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("EntrepreneurFeedFragment", "Network Error: ${e.message}", e)
            } finally {
                // Stop the loading spinner
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun handleApiError(response: retrofit2.Response<BaseResponse<List<Post>>>) {
        val errorBody = response.errorBody()
        if (errorBody != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<BaseResponse<LoginResponse>>() {}.type
                val errorResponse: BaseResponse<LoginResponse>? = gson.fromJson(errorBody.charStream(), type)
                val cleanMessage = errorResponse?.message ?: "Request Failed"
                Toast.makeText(context, cleanMessage, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Unknown Error Occurred", Toast.LENGTH_SHORT).show()
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
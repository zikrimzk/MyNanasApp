package com.spm.mynanasapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
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
import com.spm.mynanasapp.data.model.request.LikePostRequest
import com.spm.mynanasapp.data.model.request.ViewPostRequest

class EntrepreneurFeedFragment : Fragment() {

    private lateinit var feedAdapter: FeedAdapter

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

        setupProfileData(view)

        // 1. Setup RecyclerView (FIXED HERE)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_feed)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter with the list we defined at the top
        feedAdapter = FeedAdapter(
            posts = postsForAdapter,
            onLikeClicked = { post -> togglePostLikeApi(post) },
            onPostViewed = { post -> incrementViewCountApi(post) } // New Callback
        )

        // Attach the adapter to the RecyclerView
        recyclerView.adapter = feedAdapter

        // 2. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        // On refresh, reload using the CURRENT filter
        swipeRefresh.setOnRefreshListener { loadPostsFromApi(currentFilterType) }

        // 3. Setup Filter Logic
        setupTabs(view)

        // 4. Setup Navigation
        view.findViewById<View>(R.id.btn_add_post).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.cv_feed_avatar).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // 5. Load Data
        // Now this is safe to call because feedAdapter is initialized above
        loadPostsFromApi("All")
    }

    private fun setupProfileData(view: View) {
        // Get User Session
        val currentUser = SessionManager.getUser(requireContext())

        // Find Views (Based on new XML Layout)
        val ivHeaderAvatar = view.findViewById<ImageView>(R.id.iv_header_avatar)

        // Populate Data
        if (!currentUser?.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + currentUser?.ent_profilePhoto
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.placeholder_versatile)
                .into(ivHeaderAvatar)
        }
    }

    private fun setupTabs(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout_feed)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_feed)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // When tab changes, reload data with new filter
                when (tab?.position) {
                    0 -> loadPostsFromApi("All")
                    1 -> loadPostsFromApi("Announcement")
                    2 -> loadPostsFromApi("Community")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Do nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // UX Feature: Scroll to top if user taps the active tab again (like Twitter/Insta)
                recyclerView.smoothScrollToPosition(0)
            }
        })
    }


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
                val getPostRequest = GetPostRequest(post_type = postType, specific_user = null)

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
//                Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                Log.e("EntrepreneurFeedFragment", "Network Error: ${e.message}", e)
            } finally {
                // Stop the loading spinner
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun togglePostLikeApi(post: Post) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext())
            if (token == null) return@launch

            // We send the NEW state.
            // If the user just clicked "Like", post.is_liked is now TRUE.
            val request = LikePostRequest(
                postID = post.postID,
                is_liked = post.is_liked
            )

            try {
                // Call the API defined in your ApiService
                val response = RetrofitClient.instance.likePost("Bearer $token", request)

                if (!response.isSuccessful || response.body()?.status == false) {
                    // API FAILED
                    revertLikeState(post)
                    Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show()
                }
                // If SUCCESS, do nothing. The UI is already correct from the Adapter.

            } catch (e: Exception) {
                // NETWORK ERROR
                revertLikeState(post)
                Log.e("FeedFragment", "Like Error", e)
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun incrementViewCountApi(post: Post) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext())
            if (token == null) return@launch

            val request = ViewPostRequest(postID = post.postID)

            try {
                // Fire and Forget
                // We don't need to wait for the result or update the UI immediately
                // The view count will update naturally the next time the user refreshes.
                RetrofitClient.instance.viewPost("Bearer $token", request)
            } catch (e: Exception) {
                // Silent failure is okay for view counts.
                // We don't want to annoy the user with Toasts for background stats.
                Log.e("FeedFragment", "Failed to count view", e)
            }
        }
    }

    // Helper to revert UI if API fails
    private fun revertLikeState(post: Post) {
        // 1. Revert Boolean
        post.is_liked = !post.is_liked

        // 2. Revert Count
        if (post.is_liked) {
            post.post_likes_count += 1
        } else {
            post.post_likes_count = maxOf(0, post.post_likes_count - 1)
        }

        // 3. Find the index and notify adapter to refresh just that one item
        val index = postsForAdapter.indexOfFirst { it.postID == post.postID }
        if (index != -1) {
            feedAdapter.notifyItemChanged(index)
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
package com.spm.mynanasapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.entity.Post
import com.spm.mynanasapp.data.model.request.GetPostRequest
import com.spm.mynanasapp.data.model.request.LikePostRequest
import com.spm.mynanasapp.data.model.request.ViewPostRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class PublicProfilePostsFragment : Fragment() {

    private var userId: Long = 0
    private lateinit var adapter: FeedAdapter
    private val postsForAdapter = mutableListOf<Post>()

    companion object {
        fun newInstance(userId: Long) = PublicProfilePostsFragment().apply {
            arguments = Bundle().apply { putLong("USER_ID", userId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the ID passed from newInstance
        arguments?.let {
            userId = it.getLong("USER_ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_public_profile_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_profile_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = FeedAdapter(
            posts = postsForAdapter,
            onLikeClicked = { post -> togglePostLikeApi(post) },
            onPostViewed = { post -> incrementViewCountApi(post) } // New Callback
        )
        recyclerView.adapter = adapter

        // 3. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        swipeRefresh.setOnRefreshListener {
            loadPostsFromApi("All")
        }

        // 4. Load Data
        loadPostsFromApi("All")
    }

    private fun loadPostsFromApi(postType: String) {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)

        // Show Progress Bar only if NOT pulling to refresh
        if (swipeRefresh?.isRefreshing == false) {
            progressBar?.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Safety Check: If user left the screen, stop.
                if (!isAdded || context == null) return@launch

                val token = SessionManager.getToken(requireContext())
                if (token == null) return@launch

                val request = GetPostRequest(post_type = postType, specific_user = true, entID = userId)
                val response = RetrofitClient.instance.getPosts("Bearer $token", request)

                // CRASH FIX: Use safe call '?' instead of '!!'
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()

                    if (body?.status == true) {
                        val apiList = body.data ?: emptyList()

                        postsForAdapter.clear()
                        postsForAdapter.addAll(apiList.sortedByDescending { it.postID })
                        adapter.notifyDataSetChanged()

                        // Update UI state
                        checkEmptyState()
                    } else {
                        Toast.makeText(context, body?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Reuse generic error handler
                    handleApiError(response)
                }
            } catch (e: Exception) {
                if (isAdded) { // Only show toast if user is still here
                    Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // Stop animations
                progressBar?.visibility = View.GONE
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun checkEmptyState() {
        if (!isAdded || view == null) return
        val recyclerView = view?.findViewById<View>(R.id.recycler_profile_posts)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_placeholder)
        val btnaction = view?.findViewById<MaterialButton>(R.id.btn_tab_action)

        if (postsForAdapter.isEmpty()) {
            recyclerView?.visibility = View.GONE
            emptyLayout?.visibility = View.VISIBLE
            btnaction?.visibility = View.GONE

            // Configure Placeholder Content
            val tvTitle = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_title)
            val tvDesc = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_desc)
            val ivIcon = emptyLayout?.findViewById<ImageView>(R.id.iv_placeholder_icon)

            tvTitle?.text = "No Posts Yet"
            tvDesc?.text = "There are no post shared by this user."
            ivIcon?.setImageResource(R.drawable.ic_tab_posts)

        } else {
            recyclerView?.visibility = View.VISIBLE
            emptyLayout?.visibility = View.GONE
            btnaction?.visibility = View.GONE
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
            adapter.notifyItemChanged(index)
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
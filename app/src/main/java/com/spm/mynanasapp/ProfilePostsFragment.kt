package com.spm.mynanasapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import com.spm.mynanasapp.data.model.request.UpdatePostRequest
import com.spm.mynanasapp.data.model.request.ViewPostRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import kotlin.math.max

class ProfilePostsFragment : Fragment() {

    private lateinit var adapter: ProfilePostsAdapter
    private val postsForAdapter = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure this layout file exists and has no errors
        return inflater.inflate(R.layout.fragment_profile_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_profile_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ProfilePostsAdapter(
            posts = postsForAdapter,
            onEditClick = { post -> showEditDialog(post) },
            onDeleteClick = { post -> confirmDelete(post) },
            // New: Like Logic
            onLikeClick = { post -> togglePostLikeApi(post) },
            // New: View Logic
            onViewPost = { post -> incrementViewCountApi(post) }
        )
        recyclerView.adapter = adapter

        // 2. Setup Create Button
        val btnCreate = view.findViewById<Button>(R.id.btn_create_post)
        btnCreate.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                .addToBackStack(null)
                .commit()
        }

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

                val request = GetPostRequest(post_type = postType, specific_user = true)
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
                    handleError(response)
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

        val containerPinnedBtn = view?.findViewById<View>(R.id.container_pinned_button)
        val recyclerView = view?.findViewById<View>(R.id.recycler_profile_posts)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_placeholder)

        if (postsForAdapter.isEmpty()) {
            // === CASE: NO POSTS ===
            // Hide the list and the pinned button (to avoid duplicates)
            recyclerView?.visibility = View.GONE
            containerPinnedBtn?.visibility = View.GONE

            // Show the placeholder
            emptyLayout?.visibility = View.VISIBLE

            // Configure Placeholder Content
            val tvTitle = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_title)
            val tvDesc = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_desc)
            val ivIcon = emptyLayout?.findViewById<ImageView>(R.id.iv_placeholder_icon)
            val btnPlaceholderAction = emptyLayout?.findViewById<MaterialButton>(R.id.btn_tab_action)

            tvTitle?.text = "No Posts Yet"
            tvDesc?.text = "Share your updates with the community."
            ivIcon?.setImageResource(R.drawable.ic_tab_posts)

            // Setup the button INSIDE the placeholder
            btnPlaceholderAction?.text = "+ Create New Post"
            btnPlaceholderAction?.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                    .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                    .addToBackStack(null)
                    .commit()
            }

        } else {
            // === CASE: HAS POSTS ===
            // Show list and pinned button
            recyclerView?.visibility = View.VISIBLE
            containerPinnedBtn?.visibility = View.VISIBLE

            // Hide placeholder
            emptyLayout?.visibility = View.GONE
        }
    }

    private fun performPostUpdate(post: Post, isDelete: Boolean) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)
        progressBar?.visibility = View.VISIBLE // Show loading during update

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(requireContext()) ?: return@launch

                val updatePostRequest = UpdatePostRequest(
                    postID = post.postID,
                    post_caption = post.post_caption,
                    post_location = post.post_location,
                    is_delete = isDelete
                )

                val response = RetrofitClient.instance.updatePost("Bearer $token", updatePostRequest)

                if (response.isSuccessful && response.body()?.status == true) {
                    Toast.makeText(context, response.body()!!.message, Toast.LENGTH_SHORT).show()
                    handleLocalListUpdate(post, isDelete)
                } else {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun handleLocalListUpdate(post: Post, isDelete: Boolean) {
        val index = postsForAdapter.indexOfFirst { it.postID == post.postID }
        if (index != -1) {
            if (isDelete) {
                postsForAdapter.removeAt(index)
                adapter.notifyItemRemoved(index)
                checkEmptyState() // Re-check empty state after deletion
            } else {
                postsForAdapter[index] = post
                adapter.notifyItemChanged(index)
            }
        } else {
            loadPostsFromApi("All")
        }
    }

    private fun togglePostLikeApi(post: Post) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            val request = LikePostRequest(
                postID = post.postID,
                is_liked = post.is_liked
            )

            try {
                val response = RetrofitClient.instance.likePost("Bearer $token", request)

                if (!response.isSuccessful || response.body()?.status == false) {
                    // API Failed: Revert UI
                    revertLikeState(post)
                    Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Network Error: Revert UI
                revertLikeState(post)
                Toast.makeText(context, "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun revertLikeState(post: Post) {
        // Revert data
        post.is_liked = !post.is_liked
        if (post.is_liked) {
            post.post_likes_count += 1
        } else {
            post.post_likes_count = max(0, post.post_likes_count - 1)
        }

        // Notify Adapter
        val index = postsForAdapter.indexOfFirst { it.postID == post.postID }
        if (index != -1) {
            adapter.notifyItemChanged(index)
        }
    }

    private fun incrementViewCountApi(post: Post) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch
            val request = ViewPostRequest(postID = post.postID)

            try {
                // Fire and forget - silent update
                RetrofitClient.instance.viewPost("Bearer $token", request)
            } catch (e: Exception) {
                Log.e("ProfileFragment", "View count error", e)
            }
        }
    }

    private fun showEditDialog(post: Post) {
        val input = EditText(requireContext())
        input.setText(post.post_caption)

        // Add padding to edit text
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = (20 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin, margin, 0)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Caption")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newCaption = input.text.toString()
                if (newCaption.isNotEmpty()) {
                    val updatedPost = post.copy(post_caption = newCaption)
                    performPostUpdate(updatedPost, isDelete = false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performPostUpdate(post, isDelete = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Generic Error Handler
    private fun <T> handleError(response: retrofit2.Response<BaseResponse<T>>) {
        try {
            val errorBody = response.errorBody()
            if (errorBody != null) {
                val gson = Gson()
                val type = object : TypeToken<BaseResponse<LoginResponse>>() {}.type
                val errorResponse: BaseResponse<LoginResponse>? = gson.fromJson(errorBody.charStream(), type)
                Toast.makeText(context, errorResponse?.message ?: "Error", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }
}
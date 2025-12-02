package com.spm.mynanasapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.SignUpEntrepreneurFragment.LaravelValidationError
import com.spm.mynanasapp.data.model.entity.Post
import com.spm.mynanasapp.data.model.request.GetPostRequest
import com.spm.mynanasapp.data.model.request.RegisterRequest
import com.spm.mynanasapp.data.model.request.UpdatePostRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class ProfilePostsFragment : Fragment() {

    private lateinit var adapter: ProfilePostsAdapter
    private val postsForAdapter = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ERROR CHECK: Make sure 'fragment_profile_posts' matches your XML filename exactly
        return inflater.inflate(R.layout.fragment_profile_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        // ERROR CHECK: Make sure 'recycler_profile_posts' ID matches your XML
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_profile_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter with the list we defined at the top
        adapter = ProfilePostsAdapter(postsForAdapter,
            onEditClick = { post ->
                // Handle Edit: Show a dialog to modify text
                showEditDialog(post)
            },
            onDeleteClick = { post ->
                // Handle Delete: Show confirmation
                confirmDelete(post)
            }
        )
        recyclerView.adapter = adapter

        // 2. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        // On refresh, reload using the CURRENT filter
        swipeRefresh.setOnRefreshListener { loadPostsFromApi("All") }

        // 3. Setup Create Button
        // ERROR CHECK: Make sure 'btn_create_post' ID matches your XML
        val btnCreate = view.findViewById<Button>(R.id.btn_create_post)

        btnCreate.setOnClickListener {
            // Navigate to the "Add Post" screen using the Main Activity's container
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                .addToBackStack(null)
                .commit()
        }

        // 4. Load Data
        //loadMyPosts()
        loadPostsFromApi("All")

        // 4. Initialize Adapter
        // ERROR CHECK: Make sure ProfilePostsAdapter class exists!
//        adapter = ProfilePostsAdapter(myPosts,
//            onEditClick = { post ->
//                Toast.makeText(context, "Edit Post: ${post.id}", Toast.LENGTH_SHORT).show()
//            },
//            onDeleteClick = { post ->
//                confirmDelete(post)
//            }
//        )
//        recyclerView.adapter = adapter
    }

    private fun loadMyPosts() {
        // Mock Data
//        postsForAdapter.clear()
        // Note: Ensure your Post data class handles the 'location' and 'images' fields we added recently
        // Post(id, type, username, location, timestamp, caption, images, views, likes)
//        myPosts.add(Post(101, "Community", "zikrimzk", "Johor", "1h ago", "My new harvest is ready!", null, "120", 45))
//        myPosts.add(Post(102, "Community", "zikrimzk", "Johor", "2d ago", "Shipping to KL tomorrow.", listOf(android.R.drawable.ic_menu_gallery), "300", 80))
    }

    private fun showEditDialog(post: Post) {
        // 1. Create the EditText programmatically
        val input = EditText(requireContext())
        input.setText(post.post_caption)
        input.hint = "Edit your caption..."

        // 2. Add margins using a container (FrameLayout)
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        // Convert 24dp to pixels for margins
        val margin = (24 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin/2, margin, 0)
        input.layoutParams = params
        container.addView(input)

        // 3. Show Dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(container) // Pass the container view here
            .setPositiveButton("Update") { _, _ ->
                val newCaption = input.text.toString()
                if (newCaption.isNotEmpty()) {
                    // Create a copy of the post with new data
                    val updatedPost = post.copy(post_caption = newCaption)
                    // Call API with isDelete = false
                    performPostUpdate(updatedPost, isDelete = false)
                } else {
                    Toast.makeText(context, "Caption cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // Call API with isDelete = true
                performPostUpdate(post, isDelete = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performPostUpdate(post: Post, isDelete: Boolean) {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh?.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    swipeRefresh?.isRefreshing = false
                    clearSessionAndRedirect()
                    return@launch
                }

                // Prepare Request
                val updatePostRequest = UpdatePostRequest(
                    postID = post.postID,
                    post_caption = post.post_caption,
                    post_location = post.post_location, // Keep existing location
                    is_delete = isDelete
                )

                // API Call
                val response = RetrofitClient.instance.updatePost("Bearer $token", updatePostRequest)

                if (response.isSuccessful && response.body() != null) {
                    val baseResponse = response.body()!!

                    if (baseResponse.status) {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()

                        // === LOCAL LIST UPDATE ===
                        handleLocalListUpdate(post, isDelete)

                    } else {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleError(response)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun handleLocalListUpdate(post: Post, isDelete: Boolean) {
        // Find the index of the post in our current list
        val index = postsForAdapter.indexOfFirst { it.postID == post.postID }

        if (index != -1) {
            if (isDelete) {
                // DELETE: Remove from list and notify adapter
                postsForAdapter.removeAt(index)
                adapter.notifyItemRemoved(index)

                // Check if list is empty now
                checkEmptyState()
            } else {
                // UPDATE: Update the item in the list and notify adapter
                postsForAdapter[index] = post
                adapter.notifyItemChanged(index)
            }
        } else {
            // Fallback: If we couldn't find the index (rare), reload all
            loadPostsFromApi("All")
        }
    }

    private fun loadPostsFromApi(postType: String) {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh?.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    clearSessionAndRedirect()
                    return@launch
                }

                val request = GetPostRequest(post_type = postType, specific_user = true)
                val response = RetrofitClient.instance.getPosts("Bearer $token", request)

                if (response.isSuccessful && response.body() != null) {
                    if (response.body()!!.status) {
                        val apiList = response.body()!!.data ?: emptyList()
                        postsForAdapter.clear()
                        postsForAdapter.addAll(apiList.sortedByDescending { it.postID })
                        adapter.notifyDataSetChanged()
                        checkEmptyState()
                    } else {
                        Toast.makeText(context, response.body()!!.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Reuse generic error handler
                    handleError(response)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun checkEmptyState() {
        val emptyState = view?.findViewById<View>(R.id.layout_empty_state)
        val tvEmpty = view?.findViewById<TextView>(R.id.tv_empty_title)

        if (postsForAdapter.isEmpty()) {
            emptyState?.visibility = View.VISIBLE
            tvEmpty?.text = "No posts found."
        } else {
            emptyState?.visibility = View.GONE
        }
    }

    // Generic error handler to reduce code duplication
    private fun <T> handleError(response: retrofit2.Response<BaseResponse<T>>) {
        val errorBody = response.errorBody()
        if (errorBody != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<BaseResponse<LoginResponse>>() {}.type
                val errorResponse: BaseResponse<LoginResponse>? = gson.fromJson(errorBody.charStream(), type)
                Toast.makeText(context, errorResponse?.message ?: "Request Failed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
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
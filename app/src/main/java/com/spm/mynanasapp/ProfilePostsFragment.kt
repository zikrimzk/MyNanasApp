package com.spm.mynanasapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProfilePostsFragment : Fragment() {

    private lateinit var adapter: ProfilePostsAdapter
    private val myPosts = mutableListOf<Post>()

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

        // 2. Setup Create Button
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

        // 3. Load Dummy Data
        loadMyPosts()

        // 4. Initialize Adapter
        // ERROR CHECK: Make sure ProfilePostsAdapter class exists!
        adapter = ProfilePostsAdapter(myPosts,
            onEditClick = { post ->
                Toast.makeText(context, "Edit Post: ${post.id}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { post ->
                confirmDelete(post)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadMyPosts() {
        // Mock Data
        myPosts.clear()
        // Note: Ensure your Post data class handles the 'location' and 'images' fields we added recently
        // Post(id, type, username, location, timestamp, caption, images, views, likes)
        myPosts.add(Post(101, "Community", "zikrimzk", "Johor", "1h ago", "My new harvest is ready!", null, "120", 45))
        myPosts.add(Post(102, "Community", "zikrimzk", "Johor", "2d ago", "Shipping to KL tomorrow.", listOf(android.R.drawable.ic_menu_gallery), "300", 80))
    }

    private fun confirmDelete(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                val index = myPosts.indexOf(post)
                if (index != -1) {
                    myPosts.removeAt(index)
                    adapter.notifyItemRemoved(index)
                    Toast.makeText(context, "Post Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
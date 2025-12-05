package com.spm.mynanasapp

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.entity.Post
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.TimeUtils

class FeedAdapter(
    private val posts: List<Post>,
    private val onLikeClicked: (Post) -> Unit, // Callback for the Fragment
    private val onPostViewed: (Post) -> Unit
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    private val SERVER_IMAGE_URL = RetrofitClient.SERVER_IMAGE_URL
    // Helper to prevent spamming the API when scrolling up/down
    private val viewedPostIds = HashSet<Long>()

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header Info
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTime: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location) // New Dynamic Field
        val tvAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)


        // Content
        val tvCaption: TextView = itemView.findViewById(R.id.tv_caption)

        // Media Container (For Multiple Photos)
        val scrollImagesContainer: View = itemView.findViewById(R.id.scroll_images_container)
        val linearImagesLayout: LinearLayout = itemView.findViewById(R.id.layout_images_linear)

        // Interactions
        val btnLikeContainer: LinearLayout = itemView.findViewById(R.id.btn_like_container)
        val btnLike: ImageView = itemView.findViewById(R.id.btn_like) // The Pineapple Icon
        val tvLikes: TextView = itemView.findViewById(R.id.tv_like_count)
        val tvViews: TextView = itemView.findViewById(R.id.tv_view_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val context = holder.itemView.context

        // ==========================================
        // 1. BIND BASIC TEXT DATA
        // ==========================================
        holder.tvUsername.text = post.user.ent_username
        //        holder.tvTime.text = post.created_at
        holder.tvTime.text = TimeUtils.getTimeAgo(post.created_at)
        holder.tvCaption.text = post.post_caption
        holder.tvViews.text = "${post.post_views_count} views"
        holder.tvLikes.text = post.post_likes_count.toString()

        if (!post.user.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + post.user.ent_profilePhoto
            Glide.with(context)
                .load(fullUrl)
                .placeholder(R.drawable.placeholder_versatile)
                .into(holder.tvAvatar)
        } else {
            Glide.with(context)
                .load(R.drawable.ic_avatar_placeholder)
                .placeholder(R.drawable.placeholder_versatile)
                .into(holder.tvAvatar)
        }

        // ==========================================
        // 2. LOCATION LOGIC (Dynamic Visibility)
        // ==========================================
        if (!post.post_location.isNullOrEmpty()) {
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = post.post_location
        } else {
            // If no location provided, hide the view to keep the layout compact
            holder.tvLocation.visibility = View.GONE
        }

        // ==========================================
        // 3. MULTIPLE IMAGE RENDERING (UPDATED)
        // ==========================================

        // A. Clear previous views (Crucial for RecyclerView recycling)
        holder.linearImagesLayout.removeAllViews()

        // B. Parse the JSON String to a List
        val imageList: List<String> = try {
            if (post.post_images.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<String>>() {}.type
                Gson().fromJson(post.post_images, type)
            }
        } catch (e: Exception) {
            emptyList()
        }

        // C. Render Images
        if (imageList.isNotEmpty()) {
            holder.scrollImagesContainer.visibility = View.VISIBLE

            for (relativePath in imageList) {
                // 1. Create CardWrapper
                val cardWrapper = CardView(context)
                val cardParams = LinearLayout.LayoutParams(
                    (280 * context.resources.displayMetrics.density).toInt(), // 280dp Width
                    (200 * context.resources.displayMetrics.density).toInt()  // 200dp Height
                )
                cardParams.setMargins(0, 0, 24, 0)
                cardWrapper.layoutParams = cardParams
                cardWrapper.radius = 12 * context.resources.displayMetrics.density
                cardWrapper.cardElevation = 0f

                // 2. Create ImageView
                val imageView = ImageView(context)
                imageView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                // 3. Construct Full URL
                // relativePath is like "posts/abc.jpg"
                // fullUrl becomes "http://192.168.0.221/storage/posts/abc.jpg"
                val fullUrl = SERVER_IMAGE_URL + relativePath

                // 4. Load with Glide
                Glide.with(context)
                    .load(fullUrl)
                    .placeholder(R.drawable.placeholder_versatile) // Add a placeholder drawable
                    .error(android.R.drawable.stat_notify_error) // Error icon
                    .into(imageView)

                // 5. Add to Layout
                cardWrapper.addView(imageView)
                holder.linearImagesLayout.addView(cardWrapper)

                // 6. Add Click Listener for Preview
                imageView.setOnClickListener {
                    showImagePreview(context, fullUrl)
                }
            }
        } else {
            holder.scrollImagesContainer.visibility = View.GONE
        }

        // ==========================================
        // 4. INTERACTION LOGIC (LIKES)
        // ==========================================
        // Set visual state based on data (Grey Outline vs Orange Filled)
//        holder.btnLike.isSelected = post.isLiked
        holder.btnLike.isSelected = post.is_liked

        holder.btnLikeContainer.setOnClickListener {

            // 1. OPTIMISTIC UI UPDATE
            // Flip the boolean
            post.is_liked = !post.is_liked

            // Update the count locally
            if (post.is_liked) {
                post.post_likes_count += 1
            } else {
                post.post_likes_count = maxOf(0, post.post_likes_count - 1)
            }

            // 2. Refresh the specific views immediately
            holder.tvLikes.text = post.post_likes_count.toString()
            holder.btnLike.isSelected = post.is_liked

            // 3. Notify the Fragment to make the API call
            onLikeClicked(post)
        }

        // ==========================================
        // 5. VIEW COUNT LOGIC
        // ==========================================
        // Check if we have already counted this post in this session
        if (!viewedPostIds.contains(post.postID)) {

            // 1. Add to local set so we don't count it again immediately
            viewedPostIds.add(post.postID)

            // 2. Trigger the API call via Fragment
            onPostViewed(post)
        }
    }

//    fun updateList(newPosts: List<Post>) {
//        // You would typically use DiffUtil for better performance,
//        // but for now, this simple method works.
//        (posts as MutableList).clear()
//        posts.addAll(newPosts)
//        notifyDataSetChanged()
//    }

    private fun showImagePreview(context: Context, imageUrl: String) {
        // Use a full-screen theme
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_image_preview)

        val ivFull = dialog.findViewById<ImageView>(R.id.iv_full_image)
        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_preview)

        // Load the image using Glide
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_versatile) // Use a loading placeholder
            .error(android.R.drawable.stat_notify_error) // Use an error icon
            .into(ivFull)

        // Close Action
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    override fun getItemCount() = posts.size
}
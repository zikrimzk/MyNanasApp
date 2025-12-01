package com.spm.mynanasapp

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

class FeedAdapter(private val posts: List<Post>) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    /**
     * ViewHolder Pattern:
     * Holds references to the UI elements to improve scrolling performance.
     */
    private val SERVER_IMAGE_URL = RetrofitClient.SERVER_IMAGE_URL

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Header Info
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTime: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location) // New Dynamic Field

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
        // TODO: BACKEND - Ensure JSON response maps 'username', 'created_at', and 'caption' correctly.
        holder.tvUsername.text = post.user.ent_username
//        holder.tvTime.text = post.created_at
        holder.tvTime.text = TimeUtils.getTimeAgo(post.created_at)
        holder.tvCaption.text = post.post_caption
        holder.tvViews.text = "${post.post_views_count} views"
        holder.tvLikes.text = post.post_likes_count.toString()

        // ==========================================
        // 2. LOCATION LOGIC (Dynamic Visibility)
        // ==========================================
        // TODO: DATABASE - The 'location' field should be Nullable in the DB.
        if (!post.post_location.isNullOrEmpty()) {
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = post.post_location
        } else {
            // If no location provided, hide the view to keep the layout compact
            holder.tvLocation.visibility = View.GONE
        }

        // ==========================================
        // 3. MULTIPLE IMAGE RENDERING
        // ==========================================
        // TODO: FRONTEND - We use a HorizontalScrollView for simplicity.
        // TODO: BACKEND - The 'images' field should be a List/Array of URLs (e.g., ["url1.jpg", "url2.jpg"])

        // A. Clear previous views (Crucial because RecyclerView reuses layouts)
//        holder.linearImagesLayout.removeAllViews()
//
//        if (!post.post_images.isNullOrEmpty()) {
//            holder.scrollImagesContainer.visibility = View.VISIBLE
//
//            // Loop through the list of images and add them dynamically
//            for (imageResId in post.post_images) {
//
//                // Create a CardView to hold the image (Gives us rounded corners)
//                val cardWrapper = CardView(context)
//                val cardParams = LinearLayout.LayoutParams(
//                    (280 * context.resources.displayMetrics.density).toInt(), // Width: 280dp
//                    (200 * context.resources.displayMetrics.density).toInt()  // Height: 200dp
//                )
//                cardParams.setMargins(0, 0, 24, 0) // Margin right between photos
//                cardWrapper.layoutParams = cardParams
//                cardWrapper.radius = 12 * context.resources.displayMetrics.density // Corner Radius: 12dp
//                cardWrapper.cardElevation = 0f
//
//                // Create the ImageView
//                val imageView = ImageView(context)
//                imageView.layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//                )
//                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
//
//                // TODO: IMAGE LOADING - Use Glide or Coil here
//                // Example: Glide.with(context).load(imageUrl).into(imageView)
////                imageView.setImageResource(imageResId) // Using dummy ID for now
//
//                // Add Image to Card, then Card to Layout
//                cardWrapper.addView(imageView)
//                holder.linearImagesLayout.addView(cardWrapper)
//            }
//
//        } else {
//            // Hide the scroll container if there are no images (Text-only post)
//            holder.scrollImagesContainer.visibility = View.GONE
//        }

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
                    .placeholder(R.drawable.ic_launcher_background) // Add a placeholder drawable
                    .error(android.R.drawable.stat_notify_error) // Error icon
                    .into(imageView)

                // 5. Add to Layout
                cardWrapper.addView(imageView)
                holder.linearImagesLayout.addView(cardWrapper)
            }
        } else {
            holder.scrollImagesContainer.visibility = View.GONE
        }

        // ==========================================
        // 4. INTERACTION LOGIC (LIKES)
        // ==========================================
        // Set visual state based on data (Grey Outline vs Orange Filled)
//        holder.btnLike.isSelected = post.isLiked
        holder.btnLike.isSelected = false

//        holder.btnLikeContainer.setOnClickListener {
//            // --- OPTIMISTIC UI UPDATE ---
//            // Update the UI immediately so the user feels zero lag.
//            post.isLiked = !post.isLiked
//
//            if (post.isLiked) {
//                post.likes++
//                // TODO: API - Fire a background POST request to /api/posts/{id}/like
//            } else {
//                post.likes--
//                // TODO: API - Fire a background DELETE request to /api/posts/{id}/like
//            }
//
//            // Update Text & Icon
//            holder.tvLikes.text = post.likes.toString()
//            holder.btnLike.isSelected = post.isLiked
//
//            // TODO: ERROR HANDLING - If the API call fails, revert these changes and show a Toast.
//        }
    }

//    fun updateList(newPosts: List<Post>) {
//        // You would typically use DiffUtil for better performance,
//        // but for now, this simple method works.
//        (posts as MutableList).clear()
//        posts.addAll(newPosts)
//        notifyDataSetChanged()
//    }


    override fun getItemCount() = posts.size
}
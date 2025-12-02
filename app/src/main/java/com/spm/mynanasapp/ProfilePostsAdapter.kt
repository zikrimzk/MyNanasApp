package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.entity.Post
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.TimeUtils

class ProfilePostsAdapter(
    private val posts: MutableList<Post>,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>() {

    private val SERVER_IMAGE_URL = RetrofitClient.SERVER_IMAGE_URL

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvCaption: TextView = itemView.findViewById(R.id.tv_caption)

        val scrollImagesContainer: View = itemView.findViewById(R.id.scroll_images_container)
        val linearImagesLayout: LinearLayout = itemView.findViewById(R.id.layout_images_linear)

        val btnLikeContainer: View = itemView.findViewById(R.id.btn_like_container)
        val btnLike: ImageView = itemView.findViewById(R.id.btn_like)
        val tvLikes: TextView = itemView.findViewById(R.id.tv_like_count)
        val tvViews: TextView = itemView.findViewById(R.id.tv_view_count)

        val btnMore: ImageView = itemView.findViewById(R.id.btn_more_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        val context = holder.itemView.context

        holder.tvUsername.text = post.user.ent_username
        holder.tvTimestamp.text = TimeUtils.getTimeAgo(post.created_at)
        holder.tvCaption.text = post.post_caption
        holder.tvViews.text = "${post.post_views_count} views"
        holder.tvLikes.text = post.post_likes_count.toString()

        // Location Logic
        if (!post.post_location.isNullOrEmpty()) {
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = post.post_location
        } else {
            holder.tvLocation.visibility = View.GONE
        }

//        // Image Logic (Simplified for brevity)
//        holder.linearImagesLayout.removeAllViews()
//        if (!post.images.isNullOrEmpty()) {
//            holder.scrollImagesContainer.visibility = View.VISIBLE
//            // ... (Insert Image rendering code from FeedAdapter here) ...
//            // Ideally, extract image rendering to a Helper function to avoid code duplication
//            for (imageResId in post.images) {
//                val imageView = ImageView(context)
//                val params = LinearLayout.LayoutParams(
//                    (280 * context.resources.displayMetrics.density).toInt(),
//                    (200 * context.resources.displayMetrics.density).toInt()
//                )
//                params.setMargins(0, 0, 24, 0)
//                imageView.layoutParams = params
//                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
//                imageView.setImageResource(imageResId)
//                // You might want to wrap this in a CardView for rounded corners like FeedAdapter
//                holder.linearImagesLayout.addView(imageView)
//            }
//        } else {
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

        // Like Logic
        holder.btnLike.isSelected = false
//        holder.btnLikeContainer.setOnClickListener {
//            post.isLiked = !post.isLiked
//            if (post.isLiked) post.likes++ else post.likes--
//            holder.tvLikes.text = post.likes.toString()
//            holder.btnLike.isSelected = post.isLiked
//        }

        // --- THE POPUP MENU (Three Dots) ---
        holder.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Edit Post")
            popup.menu.add("Delete Post")

            popup.setOnMenuItemClickListener { item ->
                when(item.title) {
                    "Edit Post" -> {
                        // Pass the current post object back to Fragment
                        onEditClick(post)
                    }
                    "Delete Post" -> {
                        // Pass the current post object back to Fragment
                        onDeleteClick(post)
                    }
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = posts.size
}
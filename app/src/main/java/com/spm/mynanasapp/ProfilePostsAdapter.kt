package com.spm.mynanasapp

import android.app.Dialog
import android.content.Context
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
import kotlin.math.max

class ProfilePostsAdapter(
    private val posts: MutableList<Post>,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onViewPost: (Post) -> Unit
) : RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>() {

    private val SERVER_IMAGE_URL = RetrofitClient.SERVER_IMAGE_URL
    private val viewedPostIds = HashSet<Long>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvCaption: TextView = itemView.findViewById(R.id.tv_caption)
        val tvAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)


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

        // Location Logic
        if (!post.post_location.isNullOrEmpty()) {
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = post.post_location
        } else {
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
                val fullUrl = SERVER_IMAGE_URL + relativePath

                // 4. Load with Glide
                Glide.with(context)
                    .load(fullUrl)
                    .placeholder(R.drawable.placeholder_versatile)
                    .error(android.R.drawable.stat_notify_error)
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
        // 4. NEW: VIEW COUNT LOGIC
        // ==========================================
        holder.tvViews.text = "${post.post_views_count} views"

        // Only count view if not already counted in this session
        if (!viewedPostIds.contains(post.postID)) {
            viewedPostIds.add(post.postID)
            onViewPost(post)
        }

        // ==========================================
        // 5. NEW: LIKE LOGIC
        // ==========================================
        holder.tvLikes.text = post.post_likes_count.toString()
        holder.btnLike.isSelected = post.is_liked

        holder.btnLikeContainer.setOnClickListener {
            // Optimistic Update
            post.is_liked = !post.is_liked

            if (post.is_liked) {
                post.post_likes_count += 1
            } else {
                post.post_likes_count = max(0, post.post_likes_count - 1)
            }

            // Update UI immediately
            holder.tvLikes.text = post.post_likes_count.toString()
            holder.btnLike.isSelected = post.is_liked

            // Call API via callback
            onLikeClick(post)
        }

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

    private fun showImagePreview(context: Context, imageUrl: String) {
        // Use a full-screen theme
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_image_preview)

        val ivFull = dialog.findViewById<ImageView>(R.id.iv_full_image)
        val btnClose = dialog.findViewById<ImageView>(R.id.btn_close_preview)

        // Load the image using Glide
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_versatile)
            .error(android.R.drawable.stat_notify_error)
            .into(ivFull)

        // Close Action
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun getItemCount() = posts.size
}
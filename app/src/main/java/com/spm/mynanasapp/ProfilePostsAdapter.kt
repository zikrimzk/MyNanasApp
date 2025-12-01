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

class ProfilePostsAdapter(
    private val posts: MutableList<Post>,
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>() {

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

        holder.tvUsername.text = post.username
        holder.tvTimestamp.text = post.timestamp
        holder.tvCaption.text = post.caption
        holder.tvViews.text = "${post.views} views"
        holder.tvLikes.text = post.likes.toString()

        // Location Logic
        if (!post.location.isNullOrEmpty()) {
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = post.location
        } else {
            holder.tvLocation.visibility = View.GONE
        }

        // Image Logic (Simplified for brevity)
        holder.linearImagesLayout.removeAllViews()
        if (!post.images.isNullOrEmpty()) {
            holder.scrollImagesContainer.visibility = View.VISIBLE
            // ... (Insert Image rendering code from FeedAdapter here) ...
            // Ideally, extract image rendering to a Helper function to avoid code duplication
            for (imageResId in post.images) {
                val imageView = ImageView(context)
                val params = LinearLayout.LayoutParams(
                    (280 * context.resources.displayMetrics.density).toInt(),
                    (200 * context.resources.displayMetrics.density).toInt()
                )
                params.setMargins(0, 0, 24, 0)
                imageView.layoutParams = params
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                imageView.setImageResource(imageResId)
                // You might want to wrap this in a CardView for rounded corners like FeedAdapter
                holder.linearImagesLayout.addView(imageView)
            }
        } else {
            holder.scrollImagesContainer.visibility = View.GONE
        }

        // Like Logic
        holder.btnLike.isSelected = post.isLiked
        holder.btnLikeContainer.setOnClickListener {
            post.isLiked = !post.isLiked
            if (post.isLiked) post.likes++ else post.likes--
            holder.tvLikes.text = post.likes.toString()
            holder.btnLike.isSelected = post.isLiked
        }

        // --- THE POPUP MENU (Three Dots) ---
        holder.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Edit Post")
            popup.menu.add("Delete Post")

            popup.setOnMenuItemClickListener { item ->
                when(item.title) {
                    "Edit Post" -> onEditClick(post)
                    "Delete Post" -> onDeleteClick(post)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = posts.size
}
package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.spm.mynanasapp.data.model.entity.User
import com.spm.mynanasapp.data.network.RetrofitClient

// Simple data model for search results
data class SearchUser(
    val id: Long,
    val username: String,
    val fullname: String,
    val avatarUrl: String? = null
)

class SearchUserAdapter(
    private var users: List<User>,
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvFullname: TextView = itemView.findViewById(R.id.tv_fullname)
        val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvUsername.text = "@${user.ent_username}"
        holder.tvFullname.text = user.ent_fullname

        if (!user.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + user.ent_profilePhoto
            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.ic_launcher_background) // Add your placeholder
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder) // Default
        }

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size

    // Helper to update list
    fun updateList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
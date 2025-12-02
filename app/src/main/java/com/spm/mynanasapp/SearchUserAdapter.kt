package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Simple data model for search results
data class SearchUser(
    val id: Long,
    val username: String,
    val fullname: String,
    val avatarUrl: String? = null
)

class SearchUserAdapter(
    private var users: List<SearchUser>,
    private val onClick: (SearchUser) -> Unit
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
        holder.tvUsername.text = user.username
        holder.tvFullname.text = user.fullname

        // TODO: Load image with Glide/Coil
        // Glide.with(holder.itemView).load(user.avatarUrl).into(holder.ivAvatar)

        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = users.size

    // Helper to update list
    fun updateList(newUsers: List<SearchUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
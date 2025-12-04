package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.TimeUtils.getTimeAgo
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val products: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        val tvName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_product_location)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = products[position]

        // 1. Format Price to RM
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        holder.tvPrice.text = format.format(item.product_price)

        // 2. Set Text
        holder.tvName.text = item.product_name

        // 3. Location Logic
        // Access the nested 'premise' object (Make sure your Product entity has 'val premise: Premise?')
        val city = item.premise?.premise_city ?: "Unknown"
        val state = item.premise?.premise_state ?: "Location"
        val timeAgo = getTimeAgo(item.created_at)

        holder.tvLocation.text = "$city, $state • $timeAgo"

        // 4. Load Image (Placeholder logic)
        // if (item.product_image != null) Glide...
        holder.ivImage.setImageResource(R.drawable.pineapple_1)
        if (!item.product_image.isNullOrEmpty()) {
            try {
                // Parse JSON String to List
                val type = object : TypeToken<List<String>>() {}.type
                val imageList: List<String> = Gson().fromJson(item.product_image, type)

                if (imageList.isNotEmpty()) {
                    // Get the first image
                    val fullUrl = RetrofitClient.SERVER_IMAGE_URL + imageList[0]

                    Glide.with(holder.itemView)
                        .load(fullUrl)
                        .placeholder(R.drawable.pineapple_1) // Loading placeholder
                        .error(R.drawable.pineapple_1) // Error placeholder
                        .centerCrop()
                        .into(holder.ivImage)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Click
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = products.size
}
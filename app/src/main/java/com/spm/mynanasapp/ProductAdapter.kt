package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spm.mynanasapp.data.model.entity.Product
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

        // Mocking location logic (since Product model uses premiseID, we assume location for now)
        // In real app, you would map premiseID to a City name
        holder.tvLocation.text = "Johor • ${getTimeAgo(item.created_at)}"

        // 3. Load Image (Placeholder logic)
        // if (item.product_image != null) Glide...
        holder.ivImage.setImageResource(R.drawable.pineapple_1)

        // 4. Click
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = products.size

    // Simple helper for "2h ago" (Placeholder logic)
    private fun getTimeAgo(dateString: String): String {
        return "2h ago" // TODO: Implement real Date parsing
    }
}
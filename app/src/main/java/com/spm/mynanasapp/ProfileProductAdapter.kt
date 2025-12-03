package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spm.mynanasapp.data.model.entity.Product
import java.text.NumberFormat
import java.util.Locale

class ProfileProductAdapter(
    private val products: MutableList<Product>,
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit
) : RecyclerView.Adapter<ProfileProductAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        val tvName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_product_status)
        val btnMore: ImageView = itemView.findViewById(R.id.btn_more_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = products[position]

        // 1. Data Binding
        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        holder.tvPrice.text = format.format(item.product_price)
        holder.tvName.text = item.product_name

        // 2. Status Logic (Mock)
        if (item.product_status == 1) {
            holder.tvStatus.text = "Active"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(R.color.gov_orange_primary))
        } else {
            holder.tvStatus.text = "Pending"
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }

        // 3. Image
        // if (item.product_image != null) Glide...
        holder.ivImage.setImageResource(R.drawable.pineapple_2)

        // 4. Menu Action
        holder.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Edit Product")
            popup.menu.add("Delete Product")

            popup.setOnMenuItemClickListener { menuItem ->
                when(menuItem.title) {
                    "Edit Product" -> onEdit(item)
                    "Delete Product" -> onDelete(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = products.size
}
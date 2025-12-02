package com.spm.mynanasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.spm.mynanasapp.data.model.entity.Premise

class ProfilePremiseAdapter(
    private val premises: MutableList<Premise>,
    private val onEdit: (Premise) -> Unit,
    private val onDelete: (Premise) -> Unit
) : RecyclerView.Adapter<ProfilePremiseAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_premise_name)
        val tvType: TextView = itemView.findViewById(R.id.tv_premise_type)

        val tvSize: TextView = itemView.findViewById(R.id.tv_premise_size)

        val tvAddress: TextView = itemView.findViewById(R.id.tv_premise_address)
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_premise_type)
        val btnMore: ImageView = itemView.findViewById(R.id.btn_more_options)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_premise, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = premises[position]

        holder.tvName.text = item.premise_name

        // Handle Address (Shorten for card view)
        val shortAddress = "${item.premise_city ?: ""}, ${item.premise_state ?: ""}".trim(',',' ')
        holder.tvAddress.text = if(shortAddress.isNotEmpty()) shortAddress else "Address not set"

        // Handle Type Badge
        holder.tvType.text = item.premise_type

        // Handle Size Badge (Only for Farms)
        if (!item.premise_landsize.isNullOrEmpty() && item.premise_type.contains("Farm", true)) {
            holder.tvSize.visibility = View.VISIBLE
            holder.tvSize.text = "${item.premise_landsize} Acres"
            holder.ivIcon.setImageResource(R.drawable.ic_tab_farm)
        } else {
            holder.tvSize.visibility = View.GONE
            // Use Shop Icon if not farm
            holder.ivIcon.setImageResource(R.drawable.ic_tab_kiosk)
        }

        // 4. Popup Menu
        holder.btnMore.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Edit Details")
            popup.menu.add("Delete Premise")

            popup.setOnMenuItemClickListener { menuItem ->
                when(menuItem.title) {
                    "Edit Details" -> onEdit(item)
                    "Delete Premise" -> onDelete(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount() = premises.size
}
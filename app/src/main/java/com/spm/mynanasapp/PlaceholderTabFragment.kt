package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class PlaceholderTabFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.item_tab_placeholder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pos = arguments?.getInt("POS") ?: 0
        val isEditable = arguments?.getBoolean("IS_EDITABLE") ?: false // Default to false (Safe)

        // Find Views
        val tvTitle = view.findViewById<TextView>(R.id.tv_placeholder_title)
        val tvDesc = view.findViewById<TextView>(R.id.tv_placeholder_desc)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_placeholder_icon)
        val btnAction = view.findViewById<MaterialButton>(R.id.btn_tab_action)

        // 1. Handle Button Visibility
        if (isEditable) {
            btnAction.visibility = View.VISIBLE
        } else {
            btnAction.visibility = View.GONE
        }

        // 2. Customize Content
        when(pos) {
            0 -> {
                // Posts
                tvTitle.text = if (isEditable) "No Posts Yet" else "No Posts"
                tvDesc.text = if (isEditable) "Share your updates with the community." else "This user hasn't posted anything yet."
                ivIcon.setImageResource(R.drawable.ic_tab_posts)
                btnAction.text = "+ Create New Post"

                // Only set listener if editable
                if (isEditable) {
                    btnAction.setOnClickListener {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                            .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
            1 -> {
                // Products
                tvTitle.text = if (isEditable) "No Products" else "No Products Listed"
                tvDesc.text = if (isEditable) "Add your pineapple products here." else "This user has no products listed."
                ivIcon.setImageResource(R.drawable.ic_tab_products)
                btnAction.text = "+ Add Product"
            }
            2 -> {
                // Premise
                tvTitle.text = if (isEditable) "No Premises" else "No Premises Found"
                tvDesc.text = if (isEditable) "Register your farm or shop." else "No registered premises found."
                ivIcon.setImageResource(R.drawable.ic_tab_farm)
                btnAction.text = "+ Register Premise"

                if (isEditable) {
                    btnAction.setOnClickListener {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                            .replace(R.id.nav_host_fragment, EntrepreneurRegisterPremiseFragment())
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }
    }

    companion object {
        /**
         * @param position 0=Posts, 1=Products, 2=Premise
         * @param isEditable TRUE if this is MY profile (Show Button), FALSE if Public (Hide Button)
         */
        fun newInstance(position: Int, isEditable: Boolean) = PlaceholderTabFragment().apply {
            arguments = Bundle().apply {
                putInt("POS", position)
                putBoolean("IS_EDITABLE", isEditable)
            }
        }
    }
}
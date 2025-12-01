package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class PlaceholderTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout with the button and empty state
        return inflater.inflate(R.layout.item_tab_placeholder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Get the Tab Position (0, 1, or 2)
        val pos = arguments?.getInt("POS") ?: 0

        // 2. Find Views
        val tvTitle = view.findViewById<TextView>(R.id.tv_placeholder_title)
        val tvDesc = view.findViewById<TextView>(R.id.tv_placeholder_desc)
        val ivIcon = view.findViewById<ImageView>(R.id.iv_placeholder_icon)
        val btnAction = view.findViewById<MaterialButton>(R.id.btn_tab_action)

        // 3. Customize Content based on Tab
        when (pos) {
            0 -> {
                // === TAB: POSTS ===
                tvTitle.text = "No Posts Yet"
                tvDesc.text = "Share your updates with the community."
                ivIcon.setImageResource(R.drawable.ic_tab_posts)
                btnAction.text = "+ Create New Post"

                // Navigation: Go to Add Post Page
                btnAction.setOnClickListener {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .setCustomAnimations(
                            R.anim.slide_in_up,
                            R.anim.stay_still,
                            R.anim.stay_still,
                            R.anim.slide_out_down
                        )
                        .replace(R.id.nav_host_fragment, EntrepreneurFeedPostFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
            1 -> {
                // === TAB: PRODUCTS ===
                tvTitle.text = "No Products Listed"
                tvDesc.text = "Add your pineapple products here."
                ivIcon.setImageResource(R.drawable.ic_tab_products)
                btnAction.text = "+ Add Product"

                btnAction.setOnClickListener {
                    Toast.makeText(context, "Add Product Feature Coming Soon", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to AddProductFragment()
                }
            }
            2 -> {
                // === TAB: PREMISE ===
                tvTitle.text = "No Premise Info"
                tvDesc.text = "Register your farm or processing center."
                ivIcon.setImageResource(R.drawable.ic_tab_farm)
                btnAction.text = "+ Register Premise"

                btnAction.setOnClickListener {
                    Toast.makeText(context, "Register Premise Feature Coming Soon", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to AddPremiseFragment()
                }
            }
        }
    }

    companion object {
        /**
         * Factory method to create a new instance of this fragment
         * @param position 0=Posts, 1=Products, 2=Premise
         */
        fun newInstance(position: Int) = PlaceholderTabFragment().apply {
            arguments = Bundle().apply {
                putInt("POS", position)
            }
        }
    }
}
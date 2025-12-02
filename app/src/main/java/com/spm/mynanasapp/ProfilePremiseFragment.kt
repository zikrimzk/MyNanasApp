package com.spm.mynanasapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.spm.mynanasapp.data.model.entity.Premise

class ProfilePremiseFragment : Fragment() {

    private lateinit var adapter: ProfilePremiseAdapter
    private val premiseList = mutableListOf<Premise>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile_premise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_premises)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = ProfilePremiseAdapter(premiseList,
            onEdit = { premise ->
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                    .replace(R.id.nav_host_fragment, EntrepreneurEditPremiseFragment().apply {
                        arguments = Bundle().apply {
                            putLong("ID", premise.premiseID)
                            // Pass other fields here to pre-fill real data
                        }
                    })
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { premise -> confirmDelete(premise) }
        )
        recyclerView.adapter = adapter

        // 2. Setup Add Button
        val btnAddPinned = view.findViewById<Button>(R.id.btn_add_premise)
        btnAddPinned.setOnClickListener { navigateToRegister() }

        // 3. Load Data
        loadData()
    }

    private fun navigateToRegister() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
            .replace(R.id.nav_host_fragment, EntrepreneurRegisterPremiseFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun loadData() {
        premiseList.clear()

        premiseList.add(
            Premise(
                premiseID = 1,
                premise_type = "Farm",
                premise_name = "MZK Farm",
                premise_address = "Lot 45, Mukim Ayer Hitam",
                premise_state = "Johor",
                premise_city = "Batu Pahat",
                premise_postcode = "86100",
                premise_landsize = "12",
                premise_status = 1,
                premise_coordinates = null,
                entID = 101,
                created_at = "2025-01-01",
                updated_at = "2025-01-01"
            )
        )

        premiseList.add(
            Premise(
                premiseID = 2,
                premise_type = "Shop/Kiosk",
                premise_name = "Zikri Fresh Mart",
                premise_address = "No 12, Jalan Besar",
                premise_state = "Johor",
                premise_city = "Pontian",
                premise_postcode = "82000",
                premise_landsize = null, // Shops usually don't have land size
                premise_status = 1,
                premise_coordinates = null,
                entID = 101,
                created_at = "2025-02-01",
                updated_at = "2025-02-01"
            )
        )

        adapter.notifyDataSetChanged()

        // CALL THE CHECK FUNCTION
        checkEmptyState()
    }

    private fun confirmDelete(premise: Premise) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Premise")
            .setMessage("Remove ${premise.premise_name} from your list?")
            .setPositiveButton("Delete") { _, _ ->
                val index = premiseList.indexOf(premise)
                if (index != -1) {
                    premiseList.removeAt(index)
                    adapter.notifyItemRemoved(index)

                    // Re-check state after deletion
                    checkEmptyState()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- THE EMPTY STATE LOGIC (Same as Post Fragment) ---
    private fun checkEmptyState() {
        if (!isAdded || view == null) return

        val containerPinned = view?.findViewById<View>(R.id.container_pinned_button)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_premises)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_placeholder)

        if (premiseList.isEmpty()) {
            // === STATE: EMPTY ===
            recyclerView?.visibility = View.GONE
            containerPinned?.visibility = View.GONE
            emptyLayout?.visibility = View.VISIBLE

            // Customize Placeholder Content
            val tvTitle = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_title)
            val tvDesc = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_desc)
            val ivIcon = emptyLayout?.findViewById<ImageView>(R.id.iv_placeholder_icon)
            val btnPlaceholder = emptyLayout?.findViewById<MaterialButton>(R.id.btn_tab_action)

            tvTitle?.text = "No Premises Registered"
            tvDesc?.text = "Register your farm or shop to get started."
            ivIcon?.setImageResource(R.drawable.ic_tab_farm)

            // Setup the button INSIDE the placeholder
            btnPlaceholder?.visibility = View.VISIBLE
            btnPlaceholder?.text = "+ Register New Premise"
            btnPlaceholder?.setOnClickListener {
                navigateToRegister()
            }

        } else {
            // === STATE: HAS DATA ===
            emptyLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            containerPinned?.visibility = View.VISIBLE
        }
    }
}
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.spm.mynanasapp.data.model.entity.Premise
import com.spm.mynanasapp.data.model.request.GetPremiseRequest
import com.spm.mynanasapp.data.model.request.UpdatePremiseRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

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
            onEdit = { premise -> navigateToEdit(premise) },
            onDelete = { premise -> confirmDelete(premise) }
        )
        recyclerView.adapter = adapter

        // 2. Setup Add Button
        val btnAddPinned = view.findViewById<Button>(R.id.btn_add_premise)
        btnAddPinned.setOnClickListener { navigateToRegister() }

        // 3. Load Data
        loadPremisesFromApi()
    }

    private fun loadPremisesFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // Get All premises for specific user
            val request = GetPremiseRequest(premise_type = "All", specific_user = true)

            try {
                val response = RetrofitClient.instance.getPremises("Bearer $token", request)
                if (response.isSuccessful && response.body()?.status == true) {
                    val data = response.body()?.data ?: emptyList()
                    premiseList.clear()
                    premiseList.addAll(data)
                    adapter.notifyDataSetChanged()
                    checkEmptyState()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load premises", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(premise: Premise) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Premise")
            .setMessage("Remove ${premise.premise_name}?")
            .setPositiveButton("Delete") { _, _ -> deletePremiseApi(premise) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePremiseApi(premise: Premise) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // Use Update Request with is_delete = true
            val request = UpdatePremiseRequest(
                premiseID = premise.premiseID,
                premise_type = premise.premise_type,
                premise_name = premise.premise_name,
                premise_address = premise.premise_address,
                premise_city = premise.premise_city,
                premise_state = premise.premise_state,
                premise_postcode = premise.premise_postcode,
                premise_landsize = premise.premise_landsize,
                is_delete = true
            )

            try {
                val response = RetrofitClient.instance.updatePremise("Bearer $token", request)
                if (response.isSuccessful && response.body()?.status == true) {
                    premiseList.remove(premise)
                    adapter.notifyDataSetChanged()
                    checkEmptyState()
                    Toast.makeText(context, "Premise Deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToRegister() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
            .replace(R.id.nav_host_fragment, EntrepreneurRegisterPremiseFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToEdit(premise: Premise) {
        // Serialize object to JSON to pass it cleanly
        val premiseJson = Gson().toJson(premise)

        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
            .replace(R.id.nav_host_fragment, EntrepreneurEditPremiseFragment().apply {
                arguments = Bundle().apply {
                    putString("PREMISE_DATA", premiseJson)
                }
            })
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

    // --- THE EMPTY STATE LOGIC (Same as Post Fragment) ---
    private fun checkEmptyState() {
        if (!isAdded || view == null) return

        val containerPinned = view?.findViewById<View>(R.id.container_pinned_button)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_premises)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_view)

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
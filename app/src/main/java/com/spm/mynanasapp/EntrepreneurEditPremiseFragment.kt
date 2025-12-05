package com.spm.mynanasapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.spm.mynanasapp.data.model.entity.Premise
import com.spm.mynanasapp.data.model.request.UpdatePremiseRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class EntrepreneurEditPremiseFragment : Fragment() {

    // UI References
    private lateinit var etType: TextInputEditText // Read-only
    private lateinit var etName: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var actvState: AutoCompleteTextView
    private lateinit var actvCity: AutoCompleteTextView
    private lateinit var actvPostcode: AutoCompleteTextView
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilPostcode: TextInputLayout
    private lateinit var containerLandSize: LinearLayout
    private lateinit var etLandSize: TextInputEditText
    private lateinit var loadingOverlay: View

    private var currentPremise: Premise? = null

    // Location Data
    private var stateList: List<StateItem> = emptyList()
    private var selectedState: StateItem? = null
    private var selectedCity: CityItem? = null

    // Passed Data
    private var premiseId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("PREMISE_DATA")?.let { json ->
            currentPremise = Gson().fromJson(json, Premise::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_edit_premise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        etType = view.findViewById(R.id.et_premise_type) // This is the disabled EditText
        etName = view.findViewById(R.id.et_premise_name)
        etAddress = view.findViewById(R.id.et_address)
        actvState = view.findViewById(R.id.actv_state)
        actvCity = view.findViewById(R.id.actv_city)
        actvPostcode = view.findViewById(R.id.actv_postcode)
        tilCity = view.findViewById(R.id.til_city)
        tilPostcode = view.findViewById(R.id.til_postcode)
        containerLandSize = view.findViewById(R.id.container_land_size)
        etLandSize = view.findViewById(R.id.et_land_size)
        loadingOverlay = view.findViewById(R.id.layout_loading_overlay)

        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val btnSave = view.findViewById<ImageView>(R.id.btn_save)
        val btnDelete = view.findViewById<TextView>(R.id.btn_delete_premise)

        // 2. Load Location Data (JSON)
        loadJsonData()

        // 3. Populate Existing Data (Mock or from Arguments)
        populateData()

        // 4. Actions
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSave.setOnClickListener {
            if (validateInput()) {
                performUpdate(isDelete = false)
            }
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingOverlay.visibility = View.VISIBLE
        } else {
            loadingOverlay.visibility = View.GONE
        }
    }

    private fun populateData() {
        val premise = currentPremise ?: return

        etType.setText(premise.premise_type)
        etName.setText(premise.premise_name)
        etAddress.setText(premise.premise_address)

        if (premise.premise_type == "Farm") {
            containerLandSize.visibility = View.VISIBLE
            etLandSize.setText(premise.premise_landsize)
        } else {
            containerLandSize.visibility = View.GONE
        }

        // Set Locations (Use false to prevent auto-filter dropdown trigger)
        actvState.setText(premise.premise_state, false)
        actvCity.setText(premise.premise_city, false)
        actvPostcode.setText(premise.premise_postcode, false)

        // Re-enable fields
        tilCity.isEnabled = true
        tilPostcode.isEnabled = true

        // Try to find the objects in the list to enable correct dropdown behavior if user changes them
        selectedState = stateList.find { it.name == premise.premise_state }
        selectedCity = selectedState?.cityList?.find { it.name == premise.premise_city }
    }

    private fun performUpdate(isDelete: Boolean) {
        setLoading(true)

        val premise = currentPremise ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            val request = UpdatePremiseRequest(
                premiseID = premise.premiseID,
                premise_type = etType.text.toString(),
                premise_name = etName.text.toString(),
                premise_address = etAddress.text.toString(),
                premise_state = actvState.text.toString(),
                premise_city = actvCity.text.toString(),
                premise_postcode = actvPostcode.text.toString(),
                premise_landsize = if (containerLandSize.visibility == View.VISIBLE) etLandSize.text.toString() else null,
                premise_coordinates = null,
                is_delete = isDelete
            )

            try {
                val response = RetrofitClient.instance.updatePremise("Bearer $token", request)
                if (response.isSuccessful && response.body()?.status == true) {
                    val msg = if (isDelete) "Premise Deleted" else "Premise Updated"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, response.body()?.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun validateInput(): Boolean {
        if (etName.text.isNullOrBlank()) return false
        if (actvState.text.isNullOrBlank()) return false
        if (containerLandSize.visibility == View.VISIBLE && etLandSize.text.isNullOrBlank()) {
            Toast.makeText(context, "Enter land size", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Premise")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performUpdate(isDelete = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==========================================
    // LOCATION JSON LOGIC
    // ==========================================
    private fun loadJsonData() {
        try {
            val inputStream = requireContext().assets.open("state-city.json")
            val reader = InputStreamReader(inputStream)
            val root = Gson().fromJson(reader, LocationRoot::class.java)
            stateList = root.stateList
            setupDropdowns()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupDropdowns() {
        // A. Setup State Adapter
        val stateNames = stateList.map { it.name }
        val stateAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stateNames)
        actvState.setAdapter(stateAdapter)

        // B. On State Selected
        actvState.setOnItemClickListener { _, _, position, _ ->
            val selection = actvState.adapter.getItem(position).toString()
            selectedState = stateList.find { it.name == selection }

            // Reset Sub-fields
            actvCity.text = null
            actvPostcode.text = null
            tilPostcode.isEnabled = false

            if (selectedState != null) {
                tilCity.isEnabled = true
                val cityNames = selectedState!!.cityList.map { it.name }
                actvCity.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cityNames))
            }
        }

        // C. On City Selected
        actvCity.setOnItemClickListener { _, _, position, _ ->
            val selection = actvCity.adapter.getItem(position).toString()
            selectedCity = selectedState?.cityList?.find { it.name == selection }

            // Reset Postcode
            actvPostcode.text = null

            if (selectedCity != null) {
                tilPostcode.isEnabled = true
                actvPostcode.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, selectedCity!!.postcodes))
            }
        }
    }

    // --- HIDE BOTTOM NAV ---
    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }
}
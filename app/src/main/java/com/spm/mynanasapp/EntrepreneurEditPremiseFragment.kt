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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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

    // Location Data
    private var stateList: List<StateItem> = emptyList()
    private var selectedState: StateItem? = null
    private var selectedCity: CityItem? = null

    // Passed Data
    private var premiseId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            premiseId = it.getLong("ID")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure this matches your XML filename exactly
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
                // TODO: API Call Update
                Toast.makeText(context, "Premise Updated!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun populateData() {
        // --- MOCK DATA (Replace with real data passed via arguments or API) ---
        // Simulating loading a "Farm"
        val mockType = "Farm"
        val mockName = "Mazlan Pineapple Valley"
        val mockAddress = "Lot 45, Mukim Ayer Hitam"
        val mockState = "Johor"
        val mockCity = "Batu Pahat"
        val mockPostcode = "86100"
        val mockSize = "12"

        // Set Fields
        etType.setText(mockType) // Read-only field
        etName.setText(mockName)
        etAddress.setText(mockAddress)

        // Handle Conditional Logic based on Type
        if (mockType.contains("Farm", ignoreCase = true)) {
            containerLandSize.visibility = View.VISIBLE
            etLandSize.setText(mockSize)
        } else {
            containerLandSize.visibility = View.GONE
        }

        // Set Location (Note: We use setText(..., false) to avoid triggering filter)
        actvState.setText(mockState, false)
        actvCity.setText(mockCity, false)
        actvPostcode.setText(mockPostcode, false)

        // Enable dependent dropdowns since data exists
        tilCity.isEnabled = true
        tilPostcode.isEnabled = true
    }

    private fun validateInput(): Boolean {
        if (etName.text.isNullOrBlank()) {
            Toast.makeText(context, "Please enter premise name", Toast.LENGTH_SHORT).show()
            return false
        }
        if (actvState.text.isNullOrBlank() || actvCity.text.isNullOrBlank()) {
            Toast.makeText(context, "Please select complete location", Toast.LENGTH_SHORT).show()
            return false
        }
        // If it's a farm, land size is required
        if (containerLandSize.visibility == View.VISIBLE && etLandSize.text.isNullOrBlank()) {
            Toast.makeText(context, "Please enter land size", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Premise")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // TODO: API Delete Call
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
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
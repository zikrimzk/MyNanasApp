package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.spm.mynanasapp.data.model.request.AddPremiseRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class EntrepreneurRegisterPremiseFragment : Fragment() {

    // UI References
    private lateinit var tilCity: TextInputLayout
    private lateinit var tilPostcode: TextInputLayout
    private lateinit var actvState: AutoCompleteTextView
    private lateinit var actvCity: AutoCompleteTextView
    private lateinit var actvPostcode: AutoCompleteTextView
    private lateinit var containerLandSize: LinearLayout

    // DATA HOLDERS
    private var stateList: List<StateItem> = emptyList()
    private var selectedState: StateItem? = null
    private var selectedCity: CityItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_register_premise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnSave = view.findViewById<ImageView>(R.id.btn_save)
        val actvType = view.findViewById<AutoCompleteTextView>(R.id.actv_premise_type)
        val etName = view.findViewById<TextInputEditText>(R.id.et_premise_name)
        val etAddress = view.findViewById<TextInputEditText>(R.id.et_address)
        val etLandSize = view.findViewById<TextInputEditText>(R.id.et_land_size)

        tilCity = view.findViewById(R.id.til_city)
        tilPostcode = view.findViewById(R.id.til_postcode)
        actvState = view.findViewById(R.id.actv_state)
        actvCity = view.findViewById(R.id.actv_city)
        actvPostcode = view.findViewById(R.id.actv_postcode)
        containerLandSize = view.findViewById(R.id.container_land_size)

        // 2. Setup Premise Type (Farm vs Shop)
        val types = listOf("Farm", "Shop/Kiosk")
        actvType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types))

        actvType.setOnItemClickListener { _, _, position, _ ->
            if (types[position] == "Farm") {
                containerLandSize.visibility = View.VISIBLE
            } else {
                containerLandSize.visibility = View.GONE
                etLandSize.text = null
            }
        }

        // 3. LOAD JSON DATA (The Easy Way)
        loadJsonData()

        // 4. Actions
        btnClose.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSave.setOnClickListener {
            val type = actvType.text.toString()
            val name = etName.text.toString()
            val address = etAddress.text.toString()
            val state = actvState.text.toString()
            val city = actvCity.text.toString()
            val postcode = actvPostcode.text.toString()
            val landsize = etLandSize.text.toString()

            // Basic Validation
            if (type.isEmpty() || name.isEmpty() || state.isEmpty()) {
                Toast.makeText(context, "Please fill in required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performAddPremise(type, name, address, state, city, postcode, landsize)
        }
    }

    private fun performAddPremise(
        type: String, name: String, address: String,
        state: String, city: String, postcode: String, landsize: String
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            val request = AddPremiseRequest(
                premise_type = type,
                premise_name = name,
                premise_address = address,
                premise_city = city,
                premise_state = state,
                premise_postcode = postcode,
                premise_landsize = if (type == "Farm") landsize else null,
                premise_coordinates = null
            )

            try {
                val response = RetrofitClient.instance.addPremise("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    Toast.makeText(context, "Premise Added Successfully!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, response.body()?.message ?: "Failed to add", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- THE EASY PARSING LOGIC ---
    private fun loadJsonData() {
        try {
            // 1. Open the file directly
            val inputStream = requireContext().assets.open("state-city.json")
            val reader = InputStreamReader(inputStream)

            // 2. Parse using Gson into our internal class
            val root = Gson().fromJson(reader, LocationRoot::class.java)
            stateList = root.stateList

            // 3. Setup Dropdowns immediately
            setupDropdowns()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error loading locations", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDropdowns() {
        // A. Setup State Adapter
        val stateNames = stateList.map { it.name }
        actvState.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stateNames))

        // B. On State Selected
        actvState.setOnItemClickListener { _, _, position, _ ->
            // Find the state object based on the name selected
            val selection = actvState.adapter.getItem(position).toString()
            selectedState = stateList.find { it.name == selection }

            // Reset Sub-fields
            actvCity.text = null
            actvPostcode.text = null
            tilPostcode.isEnabled = false

            // Populate City
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

            // Populate Postcode
            if (selectedCity != null) {
                tilPostcode.isEnabled = true
                actvPostcode.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, selectedCity!!.postcodes))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }
}

// ==========================================
// INTERNAL DATA CLASSES (No extra files needed)
// ==========================================

data class LocationRoot(
    @SerializedName("state") val stateList: List<StateItem>
)

data class StateItem(
    @SerializedName("name") val name: String,
    @SerializedName("city") val cityList: List<CityItem>
)

data class CityItem(
    @SerializedName("name") val name: String,
    @SerializedName("postcode") val postcodes: List<String>
)
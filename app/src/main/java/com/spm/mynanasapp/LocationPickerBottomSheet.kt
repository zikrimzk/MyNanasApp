package com.spm.mynanasapp

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Interface for map location selection result
interface MapLocationResult {
    fun onMapLocationSelected(locationName: String?, latitude: Double?, longitude: Double?)
}

class LocationPickerBottomSheet(
    private val onLocationSelected: (String?) -> Unit,
    private val mapLocationResult: MapLocationResult? = null // Optional callback for map coordinates
) : BottomSheetDialogFragment() {

    // Activity result launcher for MapPickerActivity
    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val locationName = data?.getStringExtra(EXTRA_LOCATION_NAME)
            val latitude = data?.getDoubleExtra(EXTRA_LATITUDE, 0.0)
            val longitude = data?.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

            // Notify about map selection with coordinates
            mapLocationResult?.onMapLocationSelected(locationName, latitude, longitude)

            // Pass location name to original callback
            onLocationSelected(locationName)
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_location_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSearch = view.findViewById<EditText>(R.id.et_search_location)
        val progressBar = view.findViewById<ProgressBar>(R.id.progress_search)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_search_results)
        val btnCurrent = view.findViewById<LinearLayout>(R.id.item_current_location)
        val btnMap = view.findViewById<LinearLayout>(R.id.item_map_location)

        // 1. Handle "Use Current Location" Click
        btnCurrent.setOnClickListener {
            // If mapLocationResult is provided, we can also pass coordinates for current location
            // You would need to get current location coordinates here
            onLocationSelected(null)  // null indicates current location
            dismiss()
        }

        // 2. Handle "Choose from Map" Click
        btnMap.setOnClickListener {
            openMapPicker()
        }

        // 3. Setup Search Input Listener
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {

                val query = etSearch.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query, progressBar, recyclerView)
                }
                true
            } else {
                false
            }
        }
    }

    private fun openMapPicker() {
        try {
            val intent = Intent(requireContext(), MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Map feature not available", Toast.LENGTH_SHORT).show()
            // Fallback: Show a dialog explaining to install Google Maps
            showMapUnavailableDialog()
        }
    }

    private fun showMapUnavailableDialog() {
        // You can implement a simple alert dialog here
        Toast.makeText(context, "Google Maps is required for this feature", Toast.LENGTH_LONG).show()
    }

    private fun performSearch(query: String, progressBar: ProgressBar, recyclerView: RecyclerView) {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val results = geocoder.getFromLocationName(query, 10)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (!results.isNullOrEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutManager = LinearLayoutManager(context)
                        recyclerView.adapter = SearchResultsAdapter(results) { selectedAddress ->
                            // Get coordinates from the selected address
                            val latitude = selectedAddress.latitude
                            val longitude = selectedAddress.longitude

                            // Notify about coordinates if listener exists
                            val locationName = getAddressString(selectedAddress)
                            mapLocationResult?.onMapLocationSelected(locationName, latitude, longitude)

                            // Pass to original callback
                            onLocationSelected(locationName)
                            dismiss()
                        }
                    } else {
                        Toast.makeText(context, "No location found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        context,
                        "Search failed: ${e.localizedMessage ?: "Check internet connection"}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getAddressString(address: android.location.Address): String {
        val sb = StringBuilder()
        if (address.featureName != null && address.featureName.isNotEmpty()) {
            sb.append(address.featureName)
        }
        if (address.locality != null && address.locality.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(address.locality)
        }
        if (address.adminArea != null && address.adminArea.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(address.adminArea)
        }
        if (address.countryName != null && address.countryName.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(address.countryName)
        }

        return if (sb.isEmpty()) "Unknown Location" else sb.toString()
    }

    // Inner adapter class for search results
    inner class SearchResultsAdapter(
        private val addresses: List<android.location.Address>,
        private val onClick: (android.location.Address) -> Unit
    ) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val address = addresses[position]
            val locationName = getAddressString(address)

            holder.tvName.text = locationName
            holder.tvName.setTextColor(resources.getColor(R.color.text_header, null))
            holder.tvName.textSize = 16f

            holder.itemView.setOnClickListener {
                onClick(address)
            }

            // Add ripple effect
            holder.itemView.background = resources.getDrawable(
                android.R.drawable.list_selector_background,
                null
            )
        }

        override fun getItemCount() = addresses.size
    }

    companion object {
        const val REQUEST_MAP_PICKER = 1001
        const val EXTRA_LOCATION_NAME = "location_name"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"

        // Helper function to create LocationPickerBottomSheet with full configuration
        fun create(
            onLocationSelected: (String?) -> Unit,
            onMapLocationSelected: ((String?, Double?, Double?) -> Unit)? = null
        ): LocationPickerBottomSheet {
            val mapResult = if (onMapLocationSelected != null) {
                object : MapLocationResult {
                    override fun onMapLocationSelected(
                        locationName: String?,
                        latitude: Double?,
                        longitude: Double?
                    ) {
                        onMapLocationSelected(locationName, latitude, longitude)
                    }
                }
            } else {
                null
            }

            return LocationPickerBottomSheet(onLocationSelected, mapResult)
        }
    }
}

// Extension function to use LocationPickerBottomSheet easily
fun showLocationPicker(
    fragmentManager: androidx.fragment.app.FragmentManager,
    onLocationSelected: (String?) -> Unit,
    onMapLocationSelected: ((String?, Double?, Double?) -> Unit)? = null
) {
    val bottomSheet = LocationPickerBottomSheet.create(
        onLocationSelected = onLocationSelected,
        onMapLocationSelected = onMapLocationSelected
    )

    bottomSheet.show(fragmentManager, "LocationPickerBottomSheet")
}
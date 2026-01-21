package com.spm.mynanasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var etSearch: EditText
    private lateinit var btnSelect: Button
    private lateinit var btnBack: Button
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup OpenStreetMap
        Configuration.getInstance().load(this, getSharedPreferences("osm", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.map_view)
        etSearch = findViewById(R.id.et_search)
        btnSelect = findViewById(R.id.btn_select)
        btnBack = findViewById(R.id.btn_back)

        setupMap()
        setupClickListeners()

        // Move to default location
        moveToDefaultLocation()
    }

    private fun setupMap() {
        // Use OpenStreetMap tiles (FREE)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        // Click anywhere to add marker at map center - FIXED WITH CAST
        mapView.setOnClickListener {
            val mapCenter = mapView.mapCenter as GeoPoint
            addMarker(mapCenter)
        }
    }

    private fun addMarker(geoPoint: GeoPoint) {
        // Remove old marker
        selectedMarker?.let {
            mapView.overlays.remove(it)
        }

        // Add new marker
        selectedMarker = Marker(mapView).apply {
            position = geoPoint
            title = "Selected Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(selectedMarker)

        // Center map on marker
        mapView.controller.animateTo(geoPoint)

        // Get address for this location
        getAddressFromCoordinates(geoPoint)
    }

    private fun setupClickListeners() {
        // Search button
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString()
                if (query.isNotEmpty()) {
                    searchLocation(query)
                }
                true
            } else {
                false
            }
        }

        // Select button
        btnSelect.setOnClickListener {
            if (selectedMarker != null) {
                val point = selectedMarker!!.position
                val intent = Intent().apply {
                    putExtra(LocationPickerBottomSheet.EXTRA_LOCATION_NAME, etSearch.text.toString())
                    putExtra(LocationPickerBottomSheet.EXTRA_LATITUDE, point.latitude)
                    putExtra(LocationPickerBottomSheet.EXTRA_LONGITUDE, point.longitude)
                }
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Please tap on map to select location", Toast.LENGTH_SHORT).show()
            }
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun searchLocation(query: String) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = android.location.Geocoder(this@MapPickerActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(query, 1)

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val geoPoint = GeoPoint(address.latitude, address.longitude)

                        // Add marker
                        addMarker(geoPoint)

                        // Show address
                        etSearch.setText(formatAddress(address))
                    } else {
                        Toast.makeText(this@MapPickerActivity, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapPickerActivity, "Search failed. Check internet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getAddressFromCoordinates(geoPoint: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = android.location.Geocoder(this@MapPickerActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        etSearch.setText(formatAddress(addresses[0]))
                    } else {
                        etSearch.setText(String.format("%.4f, %.4f", geoPoint.latitude, geoPoint.longitude))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    etSearch.setText(String.format("%.4f, %.4f", geoPoint.latitude, geoPoint.longitude))
                }
            }
        }
    }

    private fun formatAddress(address: android.location.Address): String {
        val parts = mutableListOf<String>()
        if (address.featureName != null) parts.add(address.featureName)
        if (address.locality != null) parts.add(address.locality)
        if (address.adminArea != null) parts.add(address.adminArea)
        if (address.countryName != null) parts.add(address.countryName)
        return if (parts.isEmpty()) "Unknown Location" else parts.joinToString(", ")
    }

    private fun moveToDefaultLocation() {
        // Default to Kuala Lumpur coordinates
        val defaultLocation = GeoPoint(3.1390, 101.6869)
        mapView.controller.setCenter(defaultLocation)
        addMarker(defaultLocation)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
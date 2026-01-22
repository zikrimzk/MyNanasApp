package com.spm.mynanasapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var etSearch: EditText
    private lateinit var btnSelect: Button
    private lateinit var btnBack: TextView
    private lateinit var btnCurrentLocation: ImageButton
    private lateinit var recyclerSuggestions: RecyclerView
    private var selectedMarker: Marker? = null
    private var searchJob: Job? = null
    private val searchDelay = 300L // milliseconds 
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var myLocationOverlay: MyLocationNewOverlay

    // Store the default hint text 
    private val defaultSearchHint = "Search location..."

    // Flag to track if we should show coordinates or not 
    private var shouldShowCoordinates = false

    private companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup OpenStreetMap 
        Configuration.getInstance().load(
            this, getSharedPreferences(
                "osm",
                MODE_PRIVATE
            )
        )
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.map_view)
        etSearch = findViewById(R.id.et_search)
        btnSelect = findViewById(R.id.btn_select)
        btnBack = findViewById(R.id.btn_back)
        btnCurrentLocation = findViewById(R.id.btn_current_location)
        recyclerSuggestions = findViewById(R.id.recycler_suggestions)

        // Initialize location services 
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupClickListeners()
        setupSearchWithSuggestions()

        // Set default hint in search bar - FIXED 
        etSearch.hint = defaultSearchHint
        etSearch.text.clear() // Clear any text 

        // Move to default location (without showing coordinates) 
        shouldShowCoordinates = false
        moveToDefaultLocation()

        // Set a delay before allowing coordinates to show 
        etSearch.postDelayed({
            shouldShowCoordinates = true
        }, 1000)
    }

    private fun setupMap() {
        // Use OpenStreetMap tiles (FREE) 
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        // Setup location overlay 
        myLocationOverlay =
            MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        mapView.overlays.add(myLocationOverlay)

        // Enable touch events for exact tap location 
        mapView.isClickable = true

        // Handle exact tap location 
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                e: MotionEvent?, mapView:
                MapView?
            ): Boolean {
                e?.let { motionEvent ->
                    val projection = mapView?.projection
                    projection?.let {
                        val geoPoint = it.fromPixels(
                            motionEvent.x.toInt(),
                            motionEvent.y.toInt()
                        ) as GeoPoint
                        addMarker(geoPoint)
                    }
                }
                return true
            }
        })
    }

    private fun addMarker(geoPoint: GeoPoint) {
        // Remove old marker 
        selectedMarker?.let {
            mapView.overlays.remove(it)
        }

        // Add new marker at exact tapped location 
        selectedMarker = Marker(mapView).apply {
            position = geoPoint
            title = "Selected Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(selectedMarker)

        // Center map on marker 
        mapView.controller.animateTo(geoPoint)

        // Get address for this location (only if shouldShowCoordinates is true)
        if (shouldShowCoordinates) {
            getAddressFromCoordinates(geoPoint)
        }

        // Hide suggestions when tapping map 
        recyclerSuggestions.visibility = View.GONE
    }

    private fun setupClickListeners() {
        // Select button 
        btnSelect.setOnClickListener {
            if (selectedMarker != null) {
                val point = selectedMarker!!.position
                val locationName = etSearch.text.toString()

                // FIX: If search box is empty or shows hint, use coordinates
                val finalLocationName = if (locationName.isBlank() ||
                    locationName == defaultSearchHint
                ) {
                    String.format(
                        "%.4f, %.4f", point.latitude,
                        point.longitude
                    )
                } else {
                    locationName
                }

                val intent = Intent().apply {
                    putExtra(
                        LocationPickerBottomSheet.EXTRA_LOCATION_NAME,
                        finalLocationName
                    )
                    putExtra(
                        LocationPickerBottomSheet.EXTRA_LATITUDE,
                        point.latitude
                    )
                    putExtra(
                        LocationPickerBottomSheet.EXTRA_LONGITUDE,
                        point.longitude

                    )
                }
                setResult(RESULT_OK, intent)
                finish()
            } else {
                showToast("Please tap on map to select location")
            }
        }

        // Back button 
        btnBack.setOnClickListener {
            finish()
        }

        // Current location button - FIXED: Force get address 
        btnCurrentLocation.setOnClickListener {
            moveToCurrentLocation()
        }
    }

    private fun setupSearchWithSuggestions() {
        // Setup suggestions recycler view 
        recyclerSuggestions.layoutManager = LinearLayoutManager(this)
        recyclerSuggestions.visibility = View.GONE

        // Real-time search with suggestions 
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?, start: Int, before:
                Int, count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()
                if (query.isNullOrEmpty() || query.length < 2) {
                    recyclerSuggestions.visibility = View.GONE
                    // FIX: Restore hint when empty 
                    if (query.isNullOrEmpty()) {
                        etSearch.hint = defaultSearchHint
                        etSearch.text.clear()
                    }
                    return
                }

                // Cancel previous search job 
                searchJob?.cancel()

                // Start new search with delay (debounce) 
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(searchDelay)
                    performSearchWithSuggestions(query)
                }
            }
        })

        // Clear search and hide suggestions when user clicks elsewhere 
        mapView.setOnTouchListener { _, _ ->
            recyclerSuggestions.visibility = View.GONE
            false
        }
    }

    private fun performSearchWithSuggestions(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder =
                    android.location.Geocoder(this@MapPickerActivity, Locale.getDefault())

                // Try multiple search strategies to get more results 
                val allAddresses = mutableListOf<android.location.Address>()

                // Strategy 1: Search with Malaysia focus (more results) 
                val addresses1 = geocoder.getFromLocationName("$query,Malaysia", 10)
                if (!addresses1.isNullOrEmpty()) {
                    allAddresses.addAll(addresses1)
                }

                // Strategy 2: Search without country filter (global)
                if (allAddresses.size < 8) {
                    val addresses2 = geocoder.getFromLocationName(query, 10)
                    if (!addresses2.isNullOrEmpty()) {
                        // Add only unique addresses
                        addresses2.forEach { address2 ->
                            val isDuplicate = allAddresses.any { address1 ->
                                formatAddress(address1) ==
                                        formatAddress(address2)
                            }
                            if (!isDuplicate) {
                                allAddresses.add(address2)
                            }
                        }
                    }
                }

                // Strategy 3: For Malaysian locations, add "kuala" as prefix
                if (query.lowercase().contains("kuala") && allAddresses.size
                    < 5
                ) {
                    val enhancedQuery = if
                                                (!query.lowercase().startsWith("kuala ")) {
                        "kuala $query"
                    } else {
                        query
                    }
                    val addresses3 =
                        geocoder.getFromLocationName("$enhancedQuery, Malaysia", 5)
                    if (!addresses3.isNullOrEmpty()) {
                        addresses3.forEach { address3 ->
                            val isDuplicate = allAddresses.any { address1 ->
                                formatAddress(address1) ==
                                        formatAddress(address3)
                            }
                            if (!isDuplicate) {
                                allAddresses.add(address3)
                            }
                        }
                    }
                }

                // Remove duplicates and limit to 10 results
                val uniqueAddresses = allAddresses
                    .distinctBy { formatAddress(it) }
                    .take(10)
                    .sortedBy { address ->
                        // Sort by: Malaysia first, then completeness 
                        when {
                            address.countryName.equals(
                                "Malaysia",
                                ignoreCase = true
                            ) -> 0

                            address.locality != null && address.adminArea !=
                                    null -> 1

                            address.locality != null -> 2
                            else -> 3
                        }
                    }

                withContext(Dispatchers.Main) {
                    if (uniqueAddresses.isNotEmpty()) {
                        // Show suggestions 
                        recyclerSuggestions.adapter =
                            SuggestionsAdapter(uniqueAddresses) { address ->
                                // User selected a suggestion
                                val geoPoint = GeoPoint(
                                    address.latitude,
                                    address.longitude
                                )
                                mapView.controller.animateTo(geoPoint)
                                addMarker(geoPoint)
                                etSearch.setText(formatAddress(address))
                                recyclerSuggestions.visibility = View.GONE
                            }
                        recyclerSuggestions.visibility = View.VISIBLE
                    } else {
                        recyclerSuggestions.visibility = View.GONE
                        if (query.length >= 3) {
                            Toast.makeText(
                                this@MapPickerActivity,
                                "No locations found for '$query'",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    recyclerSuggestions.visibility = View.GONE
                    if (query.length >= 3) {
                        Toast.makeText(
                            this@MapPickerActivity,
                            "Search failed. Check internet connection.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun getAddressFromCoordinates(geoPoint: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder =
                    android.location.Geocoder(this@MapPickerActivity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(
                    geoPoint.latitude,
                    geoPoint.longitude, 1
                )

                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        // FIX: Set the actual address text, not just hint 
                        val address = addresses[0]
                        val addressText = formatAddress(address)
                        etSearch.setText(addressText)  // SET AS TEXT, NOT HINT
                        etSearch.hint = ""
                    } else {
                        // If no address found, show coordinates as TEXT 
                        val coordinates = String.format(
                            "%.4f, %.4f",
                            geoPoint.latitude, geoPoint.longitude
                        )
                        etSearch.setText(coordinates)  // SET AS TEXT 
                        etSearch.hint = ""
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // If geocoding fails, show coordinates as TEXT 
                    val coordinates = String.format(
                        "%.4f, %.4f",
                        geoPoint.latitude, geoPoint.longitude
                    )
                    etSearch.setText(coordinates)  // SET AS TEXT 
                    etSearch.hint = ""
                }
            }
        }
    }

    private fun formatAddress(address: android.location.Address): String {
        val parts = mutableListOf<String>()
        if (address.featureName != null && address.featureName.isNotBlank()) {
            parts.add(address.featureName)
        }
        if (address.locality != null && address.locality.isNotBlank()) {
            parts.add(address.locality)
        }
        if (address.adminArea != null && address.adminArea.isNotBlank()) {
            parts.add(address.adminArea)
        }
        if (address.countryName != null && address.countryName.isNotBlank()) {
            parts.add(address.countryName)
        }
        return if (parts.isEmpty()) {
            // If no proper address, return coordinates 
            String.format("%.4f, %.4f", address.latitude, address.longitude)
        } else {
            parts.joinToString(", ")
        }
    }

    private fun moveToDefaultLocation() {
        // Default to Kuala Lumpur coordinates 
        val defaultLocation = GeoPoint(3.1390, 101.6869)
        mapView.controller.setCenter(defaultLocation)

        // Add marker but DON'T call getAddressFromCoordinates() initially 
        selectedMarker?.let {
            mapView.overlays.remove(it)
        }

        selectedMarker = Marker(mapView).apply {
            position = defaultLocation
            title = "Selected Location"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(selectedMarker)

        // Clear text and show hint for default location 
        etSearch.setText("")
        etSearch.hint = defaultSearchHint
    }

    private fun moveToCurrentLocation() {
        if (checkLocationPermission()) {
            showToast("Getting your location...")
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            val geoPoint = GeoPoint(
                                it.latitude,
                                it.longitude
                            )
                            mapView.controller.animateTo(geoPoint)

                            // FIX: Add marker and force address lookup 
                            shouldShowCoordinates = true  // Force enable coordinates
                            addMarker(geoPoint)

                            showToast("Location found!")
                        } ?: run {
                            showToast("Unable to get current location")
                        }
                    }
            } catch (e: SecurityException) {
                showToast("Location permission needed")
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            // Request permission 
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST
            )
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions,
            grantResults
        )
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty()
        ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                moveToCurrentLocation()
            } else {
                showToast("Location permission denied")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        // Enable my location 
        try {
            myLocationOverlay.enableMyLocation()
        } catch (e: SecurityException) {
            // Permission not granted yet 
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        myLocationOverlay.disableMyLocation()
    }

    // Suggestions Adapter 
    inner class SuggestionsAdapter(
        private val addresses: List<android.location.Address>,
        private val onItemClick: (android.location.Address) -> Unit
    ) : RecyclerView.Adapter<SuggestionsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {
            val tvSuggestion: TextView =
                itemView.findViewById(R.id.tv_suggestion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
                ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_suggestion, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val address = addresses[position]
            val suggestionText = formatAddress(address)

            holder.tvSuggestion.text = suggestionText
            holder.itemView.setOnClickListener {
                onItemClick(address)
            }

            // Optional: Style Malaysian addresses differently 
            if (address.countryName?.equals("Malaysia", ignoreCase = true)
                == true
            ) {

                holder.tvSuggestion.setTextColor(
                    resources.getColor(
                        android.R.color.holo_blue_dark,
                        null
                    )
                )
                holder.tvSuggestion.text = "🇲🇾 $suggestionText"
            }
        }

        override fun getItemCount() = addresses.size
    }
} 
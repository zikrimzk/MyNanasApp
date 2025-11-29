package com.spm.mynanasapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EntrepreneurFeedPostFragment : Fragment() {

    // Data
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var mediaAdapter: MediaPreviewAdapter

    // Location Client
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationString: String? = null

    // UI References
    private lateinit var locationPreview: LinearLayout
    private lateinit var tvLocation: TextView
    private lateinit var recyclerMedia: RecyclerView

    // --- PERMISSION LAUNCHER ---
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            else -> Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // --- IMAGE PICKER LAUNCHER ---
    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateMediaPreview()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_feed_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val etCaption = view.findViewById<EditText>(R.id.et_caption)
        val btnPost = view.findViewById<MaterialButton>(R.id.btn_submit_post)
        val btnCancel = view.findViewById<TextView>(R.id.btn_cancel)
        val btnAddImage = view.findViewById<View>(R.id.btn_add_image)
        val btnAddLocation = view.findViewById<View>(R.id.btn_add_location)
        val btnRemoveLocation = view.findViewById<View>(R.id.btn_remove_location)

        locationPreview = view.findViewById(R.id.layout_location_preview)
        tvLocation = view.findViewById(R.id.tv_selected_location)
        recyclerMedia = view.findViewById(R.id.recycler_selected_media)

        mediaAdapter = MediaPreviewAdapter(selectedImageUris) { uriToRemove ->
            selectedImageUris.remove(uriToRemove)
            updateMediaPreview()
        }
        recyclerMedia.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerMedia.adapter = mediaAdapter

        // Keyboard Logic
        etCaption.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etCaption, InputMethodManager.SHOW_IMPLICIT)

        btnCancel.setOnClickListener {
            hideKeyboard()
            parentFragmentManager.popBackStack()
        }

        btnPost.setOnClickListener {
            val caption = etCaption.text.toString()
            if (caption.isBlank() && selectedImageUris.isEmpty()) {
                Toast.makeText(context, "Please share something!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Posting...", Toast.LENGTH_SHORT).show()
                // TODO: BACKEND - Send Data to API
                hideKeyboard()
                parentFragmentManager.popBackStack()
            }
        }

        btnAddImage.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        // --- LOCATION CLICK ---
        btnAddLocation.setOnClickListener {
            // Open the Bottom Modal
            val bottomSheet = LocationPickerBottomSheet { selectedCity ->
                if (selectedCity == null) {
                    checkAndRequestLocation()
                } else {
                    setLocationUI(selectedCity)
                }
            }
            bottomSheet.show(parentFragmentManager, "LocationPicker")
        }

        btnRemoveLocation.setOnClickListener {
            locationPreview.visibility = View.GONE
            currentLocationString = null
        }
    }

    private fun checkAndRequestLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }
        fetchCurrentLocation()
    }

    private fun fetchCurrentLocation() {
        Toast.makeText(context, "Locating...", Toast.LENGTH_SHORT).show()
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(requireContext(), Locale.getDefault())
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            withContext(Dispatchers.Main) {
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val city = address.locality ?: address.subAdminArea ?: "Unknown City"
                                    val country = address.countryCode ?: ""
                                    setLocationUI("$city, $country")
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Geocoding failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Turn on GPS", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLocationUI(locationName: String) {
        currentLocationString = locationName
        locationPreview.visibility = View.VISIBLE
        tvLocation.text = currentLocationString
    }

    private fun updateMediaPreview() {
        if (selectedImageUris.isNotEmpty()) {
            recyclerMedia.visibility = View.VISIBLE
            mediaAdapter.notifyDataSetChanged()
        } else {
            recyclerMedia.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottom_nav_bar)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().findViewById<View>(R.id.bottom_nav_bar)?.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        val view = activity?.currentFocus
        if (view != null) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    inner class MediaPreviewAdapter(
        private val uris: List<Uri>,
        private val onDelete: (Uri) -> Unit
    ) : RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder>() {
        inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivPreview: ImageView = itemView.findViewById(R.id.iv_preview)
            val btnRemove: ImageView = itemView.findViewById(R.id.btn_remove_image)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_image_preview, parent, false)
            return MediaViewHolder(view)
        }
        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val uri = uris[position]
            holder.ivPreview.setImageURI(uri)
            holder.btnRemove.setOnClickListener { onDelete(uri) }
        }
        override fun getItemCount() = uris.size
    }
}
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
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EntrepreneurFeedPostFragment : Fragment() {

    // --- DATA ---
    private val selectedImageUris = mutableListOf<Uri>()
    private var currentLocationString: String? = null

    // --- LOCATION CLIENT ---
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // --- UI REFERENCES ---
    private lateinit var chipLocation: Chip
    private lateinit var recyclerMedia: RecyclerView
    private lateinit var mediaAdapter: MediaPreviewAdapter

    // --- LAUNCHERS ---
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseLocation = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        if (fineLocation || coarseLocation) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateMediaPreview()
        }
    }

    // --- LIFECYCLE ---
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_feed_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // 1. Find Views
        val etCaption = view.findViewById<EditText>(R.id.et_caption)
        val btnShare = view.findViewById<TextView>(R.id.btn_submit_post)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnAddImage = view.findViewById<View>(R.id.btn_add_image)
        val btnAddLocation = view.findViewById<View>(R.id.btn_add_location)
        val chipGroupType = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_type)

        chipLocation = view.findViewById(R.id.chip_location)
        recyclerMedia = view.findViewById(R.id.recycler_selected_media)

        // 2. Setup Media RecyclerView
        mediaAdapter = MediaPreviewAdapter(selectedImageUris) { uriToRemove ->
            selectedImageUris.remove(uriToRemove)
            updateMediaPreview()
        }
        recyclerMedia.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerMedia.adapter = mediaAdapter

        // 3. Auto-Open Keyboard
        etCaption.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etCaption, InputMethodManager.SHOW_IMPLICIT)

        // 4. Click Listeners
        btnClose.setOnClickListener {
            hideKeyboard()
            parentFragmentManager.popBackStack()
        }


        btnShare.setOnClickListener {
            val caption = etCaption.text.toString()

            // Determine Post Type
            val selectedChipId = chipGroupType.checkedChipId
            val postType =
                if (selectedChipId == R.id.chip_announcement) "Announcement" else "Community"

            if (caption.isBlank() && selectedImageUris.isEmpty()) {
                Toast.makeText(context, "Please share something!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Posting to $postType...", Toast.LENGTH_SHORT).show()

                // TODO: BACKEND - Send 'postType', 'caption', 'selectedImageUris', 'currentLocationString'

                hideKeyboard()
                parentFragmentManager.popBackStack()
            }
        }


        btnAddImage.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        btnAddLocation.setOnClickListener {
            val bottomSheet = LocationPickerBottomSheet { selectedCity ->
                if (selectedCity == null) {
                    checkAndRequestLocation()
                } else {
                    setLocationUI(selectedCity)
                }
            }
            bottomSheet.show(parentFragmentManager, "LocationPicker")
        }



        // 5. Handle Chip Close (Remove Location)
        chipLocation.setOnCloseIconClickListener {
            chipLocation.visibility = View.GONE
            currentLocationString = null
        }
    }

    // --- LOCATION LOGIC ---
    private fun checkAndRequestLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

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
                    Toast.makeText(context, "GPS is off", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) { /* Handle error */ }
    }

    private fun setLocationUI(locationName: String) {
        currentLocationString = locationName
        chipLocation.visibility = View.VISIBLE
        chipLocation.text = currentLocationString
    }

    // --- MEDIA LOGIC ---
    private fun updateMediaPreview() {
        if (selectedImageUris.isNotEmpty()) {
            recyclerMedia.visibility = View.VISIBLE
            mediaAdapter.notifyDataSetChanged()
        } else {
            recyclerMedia.visibility = View.GONE
        }
    }

    // --- KEYBOARD & NAV HANDLING ---
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

    // --- ADAPTER ---
    inner class MediaPreviewAdapter(
        private val uris: List<Uri>,
        private val onDelete: (Uri) -> Unit
    ) : RecyclerView.Adapter<MediaPreviewAdapter.MediaViewHolder>() {

        inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivPreview: ImageView = itemView.findViewById(R.id.iv_preview)
            val btnRemove: View = itemView.findViewById(R.id.btn_remove_image) // Using View to support FrameLayout
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post_image_preview, parent, false)
            return MediaViewHolder(view)
        }

        override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
            val uri = uris[position]
            holder.ivPreview.setImageURI(uri) // TODO: Use Glide in production
            holder.btnRemove.setOnClickListener { onDelete(uri) }
        }

        override fun getItemCount() = uris.size
    }
}
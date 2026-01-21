package com.spm.mynanasapp

import android.Manifest
import android.annotation.SuppressLint
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
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.FileUtils
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import android.util.Log
import com.bumptech.glide.Glide
import com.spm.mynanasapp.data.model.entity.Post

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
    private lateinit var btnShare: TextView
    private lateinit var progressBar: View
    private lateinit var ivAvatar: ImageView

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
        if (uris.isEmpty()) return@registerForActivityResult

        val maxFileSize = 10 * 1024 * 1024 // 10MB in bytes
        val maxTotalImages = 4

        // 1. Calculate how many slots are left
        val currentCount = selectedImageUris.size
        val slotsLeft = maxTotalImages - currentCount

        if (slotsLeft <= 0) {
            Toast.makeText(context, "Limit reached ($maxTotalImages images max)", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        // 2. Slice the list if they picked too many (e.g., picked 5 but only 2 slots left)
        val selectionToCheck = if (uris.size > slotsLeft) {
            Toast.makeText(context, "Only first $slotsLeft images selected", Toast.LENGTH_SHORT).show()
            uris.take(slotsLeft)
        } else {
            uris
        }

        // 3. Filter by Size
        val validUris = mutableListOf<Uri>()
        var sizeErrorOccurred = false

        for (uri in selectionToCheck) {
            val size = FileUtils.getFileSize(requireContext(), uri)
            if (size <= maxFileSize) {
                validUris.add(uri)
            } else {
                sizeErrorOccurred = true
            }
        }

        if (sizeErrorOccurred) {
            Toast.makeText(context, "Some images were skipped (Max 10MB)", Toast.LENGTH_LONG).show()
        }

        // 4. Update UI
        if (validUris.isNotEmpty()) {
            selectedImageUris.addAll(validUris)
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
//        val btnShare = view.findViewById<TextView>(R.id.btn_submit_post)
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnAddImage = view.findViewById<View>(R.id.btn_add_image)
        val btnAddLocation = view.findViewById<View>(R.id.btn_add_location)
        val chipGroupType = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_type)
        btnShare = view.findViewById(R.id.btn_submit_post)
        progressBar = view.findViewById(R.id.progress_bar_loading)

        chipLocation = view.findViewById(R.id.chip_location)
        recyclerMedia = view.findViewById(R.id.recycler_selected_media)
        ivAvatar = view.findViewById(R.id.iv_avatar)

        // 2. Load local data
        displayLocalData()

        // 3. Setup Media RecyclerView
        mediaAdapter = MediaPreviewAdapter(selectedImageUris) { uriToRemove ->
            selectedImageUris.remove(uriToRemove)
            updateMediaPreview()
        }
        recyclerMedia.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerMedia.adapter = mediaAdapter

        // 4. Auto-Open Keyboard
        etCaption.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etCaption, InputMethodManager.SHOW_IMPLICIT)

        // 5. Click Listeners
        btnClose.setOnClickListener {
            hideKeyboard()
            parentFragmentManager.popBackStack()
        }

        btnAddImage.setOnClickListener {
//            pickImagesLauncher.launch("image/*")
            if (selectedImageUris.size >= 4) {
                Toast.makeText(context, "Maximum 4 images allowed", Toast.LENGTH_SHORT).show()
            } else {
                pickImagesLauncher.launch("image/*")
            }
        }

        btnShare.setOnClickListener {
            val caption = etCaption.text.toString()

            // Get Post Type
            val selectedChipId = chipGroupType.checkedChipId
            val postType = if (selectedChipId == R.id.chip_announcement) "Announcement" else "Community"

            if (caption.isBlank() && selectedImageUris.isEmpty()) {
                Toast.makeText(context, "Please share something!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            uploadPost(caption, postType, currentLocationString)
        }

        btnAddLocation.setOnClickListener {
            val bottomSheet = LocationPickerBottomSheet.create(
                onLocationSelected = { selectedCity ->
                    if (selectedCity == null) {
                        checkAndRequestLocation()
                    } else {
                        setLocationUI(selectedCity)
                    }
                }
            )
            bottomSheet.show(parentFragmentManager, "LocationPicker")
        }

        // 6. Handle Chip Close (Remove Location)
        chipLocation.setOnCloseIconClickListener {
            chipLocation.visibility = View.GONE
            currentLocationString = null
        }
    }


    private suspend fun verifyPost(post: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            try {
                val response = RetrofitClient.instance.verifyPost("Bearer $token", post)

                if (response.isSuccessful && response.body()?.status == true) {

                    Toast.makeText(context, response.body()?.message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, response.body()?.message, Toast.LENGTH_SHORT).show()
                }

                //loadPostsFromApi("All")
            } catch (e: Exception) {
                Toast.makeText(context, "Connection or Server Error during verification post", Toast.LENGTH_SHORT).show()
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun displayLocalData() {
        val currentUser = SessionManager.getUser(requireContext()) ?: return

        if (!currentUser.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + currentUser.ent_profilePhoto
            Glide.with(this).
            load(fullUrl).
            placeholder(R.drawable.ic_launcher_background).
            into(ivAvatar)
        }
    }

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

    private fun uploadPost(caption: String, postType: String, location: String?) {
        // Show a loading indicator if you have one (optional)
        // progressBar.visibility = View.VISIBLE
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getToken(requireContext())

                if (token == null) {
                    Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                    setLoading(false) // Reset if failed
                    // Handle logout/redirect if needed
                    return@launch
                }

                // 1. Prepare Text Data
                val captionPart = caption.toRequestBody("text/plain".toMediaTypeOrNull())
                val typePart = postType.toRequestBody("text/plain".toMediaTypeOrNull())

                val locationPart = if (!location.isNullOrEmpty()) {
                    location.toRequestBody("text/plain".toMediaTypeOrNull())
                } else null

                // 2. Prepare Image Data
                val imageParts = mutableListOf<MultipartBody.Part>()

                selectedImageUris.forEach { uri ->
                    val file = FileUtils.getFileFromUri(requireContext(), uri)
                    if (file != null) {
                        // Create request body for the file
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        // "post_images[]" matches the Laravel validation array
                        val body = MultipartBody.Part.createFormData("post_images[]", file.name, requestFile)
                        imageParts.add(body)
                    }
                }

                // 3. Make the API Call
                val response = RetrofitClient.instance.addPost(
                    "Bearer $token",
                    captionPart,
                    typePart,
                    locationPart,
                    imageParts
                )

                // 4. Handle Response (Same logic as your performLogout)
                if (response.isSuccessful) {
                    val baseResponse = response.body()
                    if (baseResponse != null && baseResponse.status) {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()

                        // Success! Close the screen
                        hideKeyboard()

                        val postId = baseResponse.data?.postID?:0
                        verifyPost(postId)

                        withContext(Dispatchers.Main) {
                            parentFragmentManager.popBackStack()
                            (activity as? EntrepreneurPortalActivity)?.redirectToProfile()
                        }
                    } else {
                        val msg = baseResponse?.message ?: "Upload failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                } else {
                    Log.e("UploadPost", "Server failed: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, "Server Error: $errorBody", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }

            } catch (e: Exception) {
                Log.e("UploadPost", "Network error: ${e.message}")
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoading(false)
            } finally {
                // Hide loading indicator
                // progressBar.visibility = View.GONE
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnShare.isEnabled = false  // <--- CRITICAL: Prevents double clicks
            btnShare.alpha = 0.5f       // Optional: Dim the button so it looks disabled
            btnShare.text = "Posting..."
        } else {
            progressBar.visibility = View.GONE
            btnShare.isEnabled = true   // <--- Re-enable so they can try again if it failed
            btnShare.alpha = 1.0f
            btnShare.text = "Share"
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
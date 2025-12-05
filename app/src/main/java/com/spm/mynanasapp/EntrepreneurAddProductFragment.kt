package com.spm.mynanasapp

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.spm.mynanasapp.data.model.entity.Premise
import com.spm.mynanasapp.data.model.entity.ProductCategory
import com.spm.mynanasapp.data.model.request.GetPremiseRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.FileUtils
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class EntrepreneurAddProductFragment : Fragment() {

    private lateinit var actvPremise: AutoCompleteTextView
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvUnit: AutoCompleteTextView
    private lateinit var recyclerImages: RecyclerView
    private lateinit var tvWarning: TextView
    private lateinit var imagesAdapter: SmallImagePreviewAdapter
    private lateinit var loadingOverlay: View


    // Data Holders
    private val selectedImageUris = mutableListOf<Uri>()
    private var myPremises = listOf<Premise>()
    private var selectedPremiseID: Long? = null

    // Hardcoded Categories (Map Name -> ID)
    // IMPORTANT: Check your Database 'product_categories' table for correct IDs
//    private val categoryMap = mapOf(
//        "Fresh Pineapple" to 1L,
//        "Processed Goods" to 2L,
//        "Seeds/Slips" to 3L,
//        "Fertilizer" to 4L,
//        "Tools" to 5L
//    )
    private var categoryList = listOf<ProductCategory>()
    private val unitList = listOf("Kg", "Unit", "Pcs", "Box", "Carton", "Basket", "Jar", "Bottle")

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            if (selectedImageUris.size + uris.size > 4) {
                Toast.makeText(context, "Max 4 images allowed", Toast.LENGTH_SHORT).show()
            } else {
                selectedImageUris.addAll(uris.take(4 - selectedImageUris.size))
                imagesAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_add_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnSave = view.findViewById<ImageView>(R.id.btn_save)
        val btnAddImage = view.findViewById<View>(R.id.btn_add_images)

        actvPremise = view.findViewById(R.id.actv_premise)
        actvCategory = view.findViewById(R.id.actv_category)
        actvUnit = view.findViewById(R.id.actv_unit)
        recyclerImages = view.findViewById(R.id.recycler_product_images)
        tvWarning = view.findViewById(R.id.tv_no_premise_warning)
        loadingOverlay = view.findViewById(R.id.layout_loading_overlay)

        val etName = view.findViewById<TextInputEditText>(R.id.et_product_name)
        val etPrice = view.findViewById<TextInputEditText>(R.id.et_price)
        val etQty = view.findViewById<TextInputEditText>(R.id.et_quantity)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_description)

//        setupDropdowns()

        // 1. Setup Static Dropdowns
        actvUnit.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unitList))

        // 2. Load Premises from API
        loadPremises()
        loadCategoriesFromApi()

        // Adapter for 100x100 thumbnails
        imagesAdapter = SmallImagePreviewAdapter(selectedImageUris) { uri ->
            selectedImageUris.remove(uri)
            imagesAdapter.notifyDataSetChanged()
        }
        recyclerImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerImages.adapter = imagesAdapter

        btnAddImage.setOnClickListener { pickImagesLauncher.launch("image/*") }
        btnClose.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val price = etPrice.text.toString()
            val qty = etQty.text.toString()
            val desc = etDesc.text.toString()
            val categoryName = actvCategory.text.toString()
            val unit = actvUnit.text.toString()

            if (selectedPremiseID == null) {
                Toast.makeText(context, "Please select a premise", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (name.isBlank() || price.isBlank() || qty.isBlank() || categoryName.isBlank()) {
                Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get Category ID
//            val catId = categoryMap[categoryName] ?: 1L
//
//            performAddProduct(name, desc, catId, qty, unit, price)
            val selectedCategory = categoryList.find { it.category_name == categoryName }

            if (selectedCategory == null) {
                Toast.makeText(context, "Invalid Category Selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cleanPrice = price.trim().replace(",", ".")

            performAddProduct(name, desc, selectedCategory.categoryID, qty, unit, cleanPrice)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingOverlay.visibility = View.VISIBLE
        } else {
            loadingOverlay.visibility = View.GONE
        }
    }

    private fun loadCategoriesFromApi() {
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch
            try {
                // No body needed for this POST request based on your PHP
                val response = RetrofitClient.instance.getProductCategories("Bearer $token")

                if (response.isSuccessful && response.body()?.status == true) {
                    categoryList = response.body()?.data ?: emptyList()

                    if (categoryList.isNotEmpty()) {
                        // Extract just the names for the Dropdown Adapter
                        val names = categoryList.map { it.category_name }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        actvCategory.setAdapter(adapter)
                    }
                }
            } catch (e: Exception) {
                Log.e("AddProduct", "Error loading categories", e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun loadPremises() {
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch
            try {
                // Fetch User's Premises
                val request = GetPremiseRequest("All", true)
                val response = RetrofitClient.instance.getPremises("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    myPremises = response.body()?.data ?: emptyList()

                    if (myPremises.isNotEmpty()) {
                        val names = myPremises.map { it.premise_name }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        actvPremise.setAdapter(adapter)

                        actvPremise.setOnItemClickListener { _, _, position, _ ->
                            val selectedName = adapter.getItem(position)
                            selectedPremiseID = myPremises.find { it.premise_name == selectedName }?.premiseID
                        }
                        tvWarning.visibility = View.GONE
                    } else {
                        tvWarning.visibility = View.VISIBLE
                        tvWarning.text = "You need to add a Premise (Farm/Shop) first!"
                        actvPremise.isEnabled = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AddProduct", "Error loading premises", e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun performAddProduct(name: String, desc: String, catId: Long, qty: String, unit: String, price: String) {
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            val token = SessionManager.getToken(context) ?: return@launch

            // 1. Prepare Text Parts
            val rbName = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbCat = catId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val rbQty = qty.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbUnit = unit.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbPrice = price.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbPremise = selectedPremiseID.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // 2. Prepare Images
            val imageParts = mutableListOf<MultipartBody.Part>()
            selectedImageUris.forEach { uri ->
                val file = FileUtils.getFileFromUri(context, uri)
                if (file != null) {
                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    // IMPORTANT: PHP expects array "product_image[]", so use "product_image[]" as name
                    val part = MultipartBody.Part.createFormData("product_image[]", file.name, reqFile)
                    imageParts.add(part)
                }
            }

            try {
                // 3. Call API
                val response = RetrofitClient.instance.addProduct(
                    "Bearer $token",
                    rbName, rbDesc, rbQty, rbUnit, rbPrice, rbCat, rbPremise, imageParts
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    Toast.makeText(context, "Product Added!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, response.body()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

//    private fun setupDropdowns() {
//        if (premisesList.isNotEmpty()) {
//            val premiseAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, premisesList)
//            actvPremise.setAdapter(premiseAdapter)
//            // Don't set text by default, let user choose.
//            // actvPremise.setText(premisesList[0], false)
//            tvWarning.visibility = View.GONE
//        } else {
//            actvPremise.setText("No Premises Found")
//            actvPremise.isEnabled = false
//            tvWarning.visibility = View.VISIBLE
//        }
//
//        actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList))
//        actvUnit.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unitList))
//    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    // --- ADAPTER FOR THUMBNAILS ---
    inner class SmallImagePreviewAdapter(
        private val uris: List<Uri>,
        private val onDelete: (Uri) -> Unit
    ) : RecyclerView.Adapter<SmallImagePreviewAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivImage: ImageView = itemView.findViewById(R.id.iv_preview)
            val btnRemove: View = itemView.findViewById(R.id.btn_remove_image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_image_thumb, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = uris[position]
            holder.ivImage.setImageURI(uri)
            holder.btnRemove.setOnClickListener { onDelete(uri) }
        }

        override fun getItemCount() = uris.size
    }
}
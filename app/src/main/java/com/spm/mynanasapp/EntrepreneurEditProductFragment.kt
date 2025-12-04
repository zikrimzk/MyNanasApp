package com.spm.mynanasapp

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.entity.ProductCategory
import com.spm.mynanasapp.data.model.entity.Premise
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.request.GetPremiseRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.FileUtils
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class EntrepreneurEditProductFragment : Fragment() {

    // === 1. UNIFIED IMAGE DATA CLASS ===
    // This helps us display both old (Server URL) and new (Local Uri) images in one list
    sealed class ProductImage {
        data class Remote(val path: String) : ProductImage() // e.g., "products/abc.jpg"
        data class Local(val uri: Uri) : ProductImage()      // e.g., "content://..."
    }

    // UI References
    private lateinit var actvPremise: AutoCompleteTextView
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvUnit: AutoCompleteTextView
    private lateinit var recyclerImages: RecyclerView
    private lateinit var etName: TextInputEditText
    private lateinit var etQty: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDesc: TextInputEditText

    // Data
    private var currentProduct: Product? = null
    private var categoryList = listOf<ProductCategory>()
    private var premiseList = listOf<Premise>()
    private val unitList = listOf("Kg", "Unit", "Pcs", "Box", "Carton", "Basket", "Jar", "Bottle", "Packet", "Gram")

    // Image Holders
    private val imageList = mutableListOf<ProductImage>()
    private lateinit var imagesAdapter: MixedImagePreviewAdapter

    // Selection Holders
    private var selectedPremiseID: Long? = null
    private var selectedCategoryID: Long? = null

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            if (imageList.size + uris.size > 4) {
                Toast.makeText(context, "Max 4 images allowed", Toast.LENGTH_SHORT).show()
            } else {
                uris.take(4 - imageList.size).forEach {
                    imageList.add(ProductImage.Local(it))
                }
                imagesAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("PRODUCT_DATA")?.let { json ->
            currentProduct = Gson().fromJson(json, Product::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_edit_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnSave = view.findViewById<ImageView>(R.id.btn_save)
        val btnDelete = view.findViewById<TextView>(R.id.btn_delete_product)
        val btnAddImage = view.findViewById<View>(R.id.btn_add_images)

        actvPremise = view.findViewById(R.id.actv_premise)
        actvCategory = view.findViewById(R.id.actv_category)
        actvUnit = view.findViewById(R.id.actv_unit)
        recyclerImages = view.findViewById(R.id.recycler_product_images)

        etName = view.findViewById(R.id.et_product_name)
        etQty = view.findViewById(R.id.et_quantity)
        etPrice = view.findViewById(R.id.et_price)
        etDesc = view.findViewById(R.id.et_description)

        // 2. Setup Dropdowns
        setupDropdowns()
        setupImageRecyclerView()

        // 3. Populate Data
        loadApiDataAndPopulate()

        // 4. Actions
        btnAddImage.setOnClickListener { pickImagesLauncher.launch("image/*") }
        btnClose.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSave.setOnClickListener { validateAndSave() }
        btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    private fun setupImageRecyclerView() {
        imagesAdapter = MixedImagePreviewAdapter(imageList) { item ->
            imageList.remove(item)
            imagesAdapter.notifyDataSetChanged()
        }
        recyclerImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerImages.adapter = imagesAdapter
    }

    private fun loadApiDataAndPopulate() {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch
            try {
                // 1. Load Categories
                val catResponse = RetrofitClient.instance.getProductCategories("Bearer $token")
                if (catResponse.isSuccessful && catResponse.body()?.status == true) {
                    categoryList = catResponse.body()?.data ?: emptyList()
                    val names = categoryList.map { it.category_name }
                    actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))
                }

                // 2. Load Premises
                val premResponse = RetrofitClient.instance.getPremises("Bearer $token",
                    GetPremiseRequest("All", true)
                )
                if (premResponse.isSuccessful && premResponse.body()?.status == true) {
                    premiseList = premResponse.body()?.data ?: emptyList()
                    val names = premiseList.map { it.premise_name }
                    actvPremise.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))
                }

                // 3. Populate Fields (After data loaded)
                populateExistingData()

            } catch (e: Exception) {
                Log.e("EditProduct", "Error loading data", e)
            }
        }
    }

    private fun populateExistingData() {
        val product = currentProduct ?: return

        etName.setText(product.product_name)
        etDesc.setText(product.product_desc)
        etQty.setText(product.product_qty.toString())
        etPrice.setText(String.format("%.2f", product.product_price))
        actvUnit.setText(product.product_unit, false)

        // Set Selected IDs
        selectedCategoryID = product.categoryID // ID from DB
        selectedPremiseID = product.premiseID

        // Find Names for Dropdowns (Visual only)
        val catName = categoryList.find { it.categoryID == selectedCategoryID }?.category_name
        if (catName != null) actvCategory.setText(catName, false)

        val premName = premiseList.find { it.premiseID == selectedPremiseID }?.premise_name
        if (premName != null) actvPremise.setText(premName, false)

        // Populate Images
        if (!product.product_image.isNullOrEmpty()) {
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                val serverPaths: List<String> = Gson().fromJson(product.product_image, listType)
                serverPaths.forEach { path ->
                    imageList.add(ProductImage.Remote(path))
                }
                imagesAdapter.notifyDataSetChanged()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupDropdowns() {
        actvUnit.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unitList))

        // Listeners to capture IDs when user changes selection
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            val name = actvCategory.adapter.getItem(position).toString()
            selectedCategoryID = categoryList.find { it.category_name == name }?.categoryID
        }
        actvPremise.setOnItemClickListener { _, _, position, _ ->
            val name = actvPremise.adapter.getItem(position).toString()
            selectedPremiseID = premiseList.find { it.premise_name == name }?.premiseID
        }
    }

    private fun validateAndSave() {
        val name = etName.text.toString()
        val rawPrice = etPrice.text.toString()
        val qty = etQty.text.toString()

        if (imageList.isEmpty()) {
            Toast.makeText(context, "At least 1 image required", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isBlank() || rawPrice.isBlank()) {
            Toast.makeText(context, "Fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Sanitize Price
        val cleanPrice = rawPrice.trim().replace(",", ".")

        performUpdate(name, qty, cleanPrice, isDelete = false)
    }

    private fun performUpdate(name: String, qty: String, price: String, isDelete: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            val token = SessionManager.getToken(context) ?: return@launch
            val product = currentProduct ?: return@launch

            // 1. Basic Fields
            val rbId = product.productID.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val rbIsDelete = (if(isDelete) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())
            val rbName = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbDesc = etDesc.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val rbQty = qty.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbPrice = price.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbUnit = actvUnit.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val rbCat = selectedCategoryID.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val rbPremise = selectedPremiseID.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            // 2. Separate Images into Existing (Strings) and New (Files)
            val existingImagesParts = mutableListOf<MultipartBody.Part>()
            val newImagesParts = mutableListOf<MultipartBody.Part>()

            if (!isDelete) {
                imageList.forEach { item ->
                    when (item) {
                        is ProductImage.Remote -> {
                            // PHP: existing_images[]
                            val part = MultipartBody.Part.createFormData("existing_images[]", item.path)
                            existingImagesParts.add(part)
                        }
                        is ProductImage.Local -> {
                            // PHP: new_images[]
                            val file = FileUtils.getFileFromUri(context, item.uri)
                            if (file != null) {
                                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                val part = MultipartBody.Part.createFormData("new_images[]", file.name, reqFile)
                                newImagesParts.add(part)
                            }
                        }
                    }
                }
            }

            try {
                val response = RetrofitClient.instance.updateProduct(
                    token = "Bearer $token",
                    id = rbId,
                    isDelete = rbIsDelete,
                    name = rbName,
                    desc = rbDesc,
                    categoryId = rbCat,
                    qty = rbQty,
                    unit = rbUnit,
                    price = rbPrice,
                    premiseId = rbPremise,
                    existing_images = existingImagesParts,
                    new_images = newImagesParts
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    val msg = if(isDelete) "Deleted" else "Updated"
                    Toast.makeText(context, "Product $msg!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, response.body()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performUpdate("", "0", "0", isDelete = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    // === UNIFIED ADAPTER ===
    inner class MixedImagePreviewAdapter(
        private val items: List<ProductImage>,
        private val onDelete: (ProductImage) -> Unit
    ) : RecyclerView.Adapter<MixedImagePreviewAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivImage: ImageView = itemView.findViewById(R.id.iv_preview)
            val btnRemove: View = itemView.findViewById(R.id.btn_remove_image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_image_thumb, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            // Handle Loading based on type
            when (item) {
                is ProductImage.Remote -> {
                    Glide.with(holder.itemView)
                        .load(RetrofitClient.SERVER_IMAGE_URL + item.path)
                        .placeholder(R.drawable.placeholder_versatile)
                        .into(holder.ivImage)
                }
                is ProductImage.Local -> {
                    holder.ivImage.setImageURI(item.uri)
                }
            }

            holder.btnRemove.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount() = items.size
    }
}
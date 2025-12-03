package com.spm.mynanasapp

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class EntrepreneurEditProductFragment : Fragment() {

    // UI References
    private lateinit var actvPremise: AutoCompleteTextView
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var actvUnit: AutoCompleteTextView
    private lateinit var recyclerImages: RecyclerView

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagesAdapter: SmallImagePreviewAdapter

    // Data
    private var productId: Long = 0
    private val premisesList = listOf("Mazlan Pineapple Valley", "Zikri Fresh Mart")
    private val categoryList = listOf("Fresh Pineapple", "Processed Goods", "Seeds/Slips", "Fertilizer")
    private val unitList = listOf("Kg", "Unit", "Pcs", "Box", "Carton", "Basket", "Jar", "Bottle", "Packet", "Gram")

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            imagesAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getLong("ID")
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

        val etName = view.findViewById<TextInputEditText>(R.id.et_product_name)
        val etQty = view.findViewById<TextInputEditText>(R.id.et_quantity)
        val etPrice = view.findViewById<TextInputEditText>(R.id.et_price)
        val etDesc = view.findViewById<TextInputEditText>(R.id.et_description)

        // 2. Setup Dropdowns
        setupDropdowns()

        // 3. Setup Image List
        imagesAdapter = SmallImagePreviewAdapter(selectedImageUris) { uri ->
            selectedImageUris.remove(uri)
            imagesAdapter.notifyDataSetChanged()
        }
        recyclerImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerImages.adapter = imagesAdapter

        // 4. Populate Existing Data
        populateData(etName, etQty, etPrice, etDesc)

        // 5. Actions
        btnAddImage.setOnClickListener { pickImagesLauncher.launch("image/*") }
        btnClose.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSave.setOnClickListener {
            // TODO: Validate & API Call Update
            Toast.makeText(context, "Product Updated!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    // TODO: API Delete Call
                    Toast.makeText(context, "Product Deleted", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun populateData(etName: TextInputEditText, etQty: TextInputEditText, etPrice: TextInputEditText, etDesc: TextInputEditText) {
        // Mock Data (In real app, fetch via ID or pass full object)
        etName.setText("Premium MD2 Pineapple")
        etQty.setText("100")
        etPrice.setText("12.50")
        etDesc.setText("Freshly harvested MD2 pineapples. Sweet and juicy.")

        actvPremise.setText(premisesList[0], false)
        actvCategory.setText(categoryList[0], false)
        actvUnit.setText("Kg", false)
    }

    private fun setupDropdowns() {
        actvPremise.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, premisesList))
        actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList))
        actvUnit.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unitList))
    }

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
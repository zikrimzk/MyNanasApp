package com.spm.mynanasapp

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.spm.mynanasapp.data.model.entity.Product
import java.io.InputStreamReader
import java.util.Locale

class EntrepreneurProductFragment : Fragment() {

    private lateinit var adapter: ProductAdapter

    // Master list (All Data) vs Display list (Filtered)
    private val masterProductList = mutableListOf<Product>()
    private val displayProductList = mutableListOf<Product>()

    // Location Data from JSON
    private var stateList: List<StateItem> = emptyList()

    // Filter State
    private var currentSearchQuery = ""
    private var selectedState = "All States"
    private var selectedCity = "All Cities"
    private var selectedCategory = "All Categories"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Header Style (Gradient Text)
        setupHeaderStyle(view)

        // 2. Setup RecyclerView (Grid Layout 2 Columns)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_products)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        adapter = ProductAdapter(displayProductList) { product ->
            // Navigate to Single Product
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.nav_host_fragment, SingleProductFragment.newInstance(product.productID, product.product_name, product.product_price))
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        // 3. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        swipeRefresh.setOnRefreshListener { loadProducts() }

        // 4. Load Location Data (JSON)
        loadJsonData()

        // 5. Setup UI Components
        setupSearch(view)
        setupFilters(view)

        // 6. Initial Load
        loadProducts()
    }

    private fun setupHeaderStyle(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_marketplace_title)

        // Apply Gradient (Orange -> Dark Orange)
        val paint = tvTitle.paint
        val width = paint.measureText(tvTitle.text.toString())
        val textShader = LinearGradient(0f, 0f, width, tvTitle.textSize,
            intArrayOf(
                Color.parseColor("#FF9800"), // Brighter Orange
                Color.parseColor("#E65100")  // Deep Orange
            ), null, Shader.TileMode.CLAMP)
        tvTitle.paint.shader = textShader
    }

    private fun loadJsonData() {
        try {
            // Read state-city.json from Assets
            val inputStream = requireContext().assets.open("state-city.json")
            val reader = InputStreamReader(inputStream)
            val root = Gson().fromJson(reader, LocationRoot::class.java)
            stateList = root.stateList
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback or show error
        }
    }

    private fun setupSearch(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.et_search_product)

        etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v)
                true
            } else {
                false
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s.toString().trim()
                applyFilters()
            }
        })
    }

    private fun setupFilters(view: View) {
        val actvState = view.findViewById<AutoCompleteTextView>(R.id.actv_filter_state)
        val actvCity = view.findViewById<AutoCompleteTextView>(R.id.actv_filter_city)
        val tilCity = view.findViewById<TextInputLayout>(R.id.til_filter_city)
        val actvCategory = view.findViewById<AutoCompleteTextView>(R.id.actv_filter_category)

        // --- 1. Setup State Adapter (From JSON) ---
        // Add "All States" at the beginning
        val stateNames = mutableListOf("All States")
        stateNames.addAll(stateList.map { it.name })

        actvState.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stateNames))

        // --- 2. Setup Category Adapter (Mock/Static) ---
        val categories = listOf("All Categories", "Fresh Pineapple", "Processed Goods", "Seeds/Slips", "Fertilizer")
        actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories))

        // --- 3. State Selection Logic ---
        actvState.setOnItemClickListener { _, _, position, _ ->
            // Get selected text
            selectedState = actvState.adapter.getItem(position).toString()

            // Reset City
            selectedCity = "All Cities"
            actvCity.text = null
            actvCity.setText("All Cities", false)

            if (selectedState == "All States") {
                // Disable City if no state selected
                tilCity.isEnabled = false
                actvCity.setAdapter(null)
            } else {
                // Enable City and populate based on JSON
                tilCity.isEnabled = true

                val stateObj = stateList.find { it.name == selectedState }
                val cityNames = mutableListOf("All Cities")

                if (stateObj != null) {
                    cityNames.addAll(stateObj.cityList.map { it.name })
                }

                actvCity.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cityNames))
            }
            applyFilters()
        }

        // --- 4. City Selection Logic ---
        actvCity.setOnItemClickListener { parent, _, position, _ ->
            selectedCity = parent.getItemAtPosition(position).toString()
            applyFilters()
        }

        // --- 5. Category Selection Logic ---
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            applyFilters()
        }
    }

    private fun applyFilters() {
        val filtered = masterProductList.filter { product ->
            // 1. Search Query Check
            val matchesSearch = if (currentSearchQuery.isNotEmpty()) {
                product.product_name.lowercase(Locale.getDefault()).contains(currentSearchQuery.lowercase(Locale.getDefault()))
            } else true

            // 2. Category Check
            val matchesCategory = if (selectedCategory != "All Categories") {
                // In real app: check product.category == selectedCategory
                true
            } else true

            // 3. State/City Check
            // In real app: You would check product.premise.state == selectedState
            val matchesState = if (selectedState != "All States") {
                // product.premiseState == selectedState
                true
            } else true

            val matchesCity = if (selectedCity != "All Cities") {
                // product.premiseCity == selectedCity
                true
            } else true

            matchesSearch && matchesCategory && matchesState && matchesCity
        }

        displayProductList.clear()
        displayProductList.addAll(filtered)
        adapter.notifyDataSetChanged()

        // Update Empty State
        view?.findViewById<View>(R.id.tv_empty_state)?.visibility =
            if (displayProductList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadProducts() {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh?.isRefreshing = true

        // Mock Data
        masterProductList.clear()
        masterProductList.add(Product(1, "MD2 Pineapple Grade A", "Fresh & Sweet", 100, "Kg", 12.50, 1, null, 1, 1, "2025-01-01", "2025-01-01"))
        masterProductList.add(Product(2, "Pineapple Jam (Homemade)", "No preservatives", 50, "Jar", 8.90, 1, null, 2, 1, "2025-02-01", "2025-02-01"))
        masterProductList.add(Product(3, "Josapine Slips", "Ready to plant", 500, "Unit", 1.50, 1, null, 1, 1, "2025-03-01", "2025-03-01"))
        masterProductList.add(Product(4, "Dried Pineapple Snacks", "Healthy snack", 200, "Pack", 15.00, 1, null, 2, 1, "2025-03-05", "2025-03-05"))
        masterProductList.add(Product(5, "Pineapple Juice", "100% Pure", 120, "Bottle", 5.50, 1, null, 2, 1, "2025-03-06", "2025-03-06"))
        masterProductList.add(Product(6, "NPK Fertilizer", "For Pineapples", 20, "Bag", 45.00, 1, null, 4, 1, "2025-03-07", "2025-03-07"))

        // Reset and Apply
        applyFilters()

        swipeRefresh?.isRefreshing = false
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
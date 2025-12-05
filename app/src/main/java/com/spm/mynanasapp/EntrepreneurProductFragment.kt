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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.entity.ProductCategory
import com.spm.mynanasapp.data.model.request.GetProductRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
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

    private var categoryList = listOf<ProductCategory>()

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
        loadCategoriesForFilter(view.findViewById(R.id.actv_filter_category))

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
        actvCategory.setOnItemClickListener { parent, _, position, _ ->
            selectedCategory = parent.getItemAtPosition(position).toString()
            applyFilters() // applyFilters now includes API call
        }
    }

    private fun loadCategoriesForFilter(actvCategory: AutoCompleteTextView) {
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch
            try {
                val response = RetrofitClient.instance.getProductCategories("Bearer $token")
                if (response.isSuccessful && response.body()?.status == true) {
                    categoryList = response.body()?.data ?: emptyList()
                    val names = mutableListOf("All Categories")
                    names.addAll(categoryList.map { it.category_name })
                    actvCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))
                    actvCategory.setText("All Categories", false)
                }
            } catch (e: Exception) {
                // Silent failure or log error
//                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyFilters() {
        // Find Category ID (0 = All)
        val categoryIdToFilter = if (selectedCategory == "All Categories") {
            0
        } else {
            // Find the ID based on the selected Name
            categoryList.find { it.category_name == selectedCategory }?.categoryID ?: 0
        }

        loadProductsFromApi(
            state = selectedState,
            city = selectedCity,
            categoryId = categoryIdToFilter
        )
    }

    private fun loadProducts() {
        applyFilters()
    }

    private fun loadProductsFromApi(state: String, city: String, categoryId: Long) {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh?.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            val request = GetProductRequest(
                premise_state = state,
                premise_city = city,
                categoryID = categoryId,
                specific_user = false // Marketplace view
            )

            try {
                val response = RetrofitClient.instance.getProducts("Bearer $token", request)
                if (response.isSuccessful && response.body()?.status == true) {
                    val data = response.body()?.data ?: emptyList()
                    masterProductList.clear()
                    masterProductList.addAll(data)

                    // Client-side search filtering (only for the search box)
                    val filtered = masterProductList.filter {
                        it.product_name.lowercase(Locale.getDefault()).contains(currentSearchQuery.lowercase(Locale.getDefault()))
                    }
                    displayProductList.clear()
                    displayProductList.addAll(filtered)

                    adapter.notifyDataSetChanged()
                } else {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Failed to load products", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                context?.let { ctx ->
//                    Toast.makeText(ctx, "Network Error", Toast.LENGTH_SHORT).show()
                }
            } finally {
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
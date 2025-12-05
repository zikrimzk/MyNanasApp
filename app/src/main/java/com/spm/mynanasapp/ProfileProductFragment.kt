package com.spm.mynanasapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.spm.mynanasapp.EntrepreneurEditProductFragment.ProductImage
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.request.GetProductRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.FileUtils
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileProductFragment : Fragment() {

    private lateinit var adapter: ProfileProductAdapter
    private val productList = mutableListOf<Product>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView (Grid - 2 Columns)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_profile_products)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        adapter = ProfileProductAdapter(productList,
            onEdit = { product ->
                // Serialize and pass to Edit Fragment
                val json = Gson().toJson(product)

                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                    .replace(R.id.nav_host_fragment, EntrepreneurEditProductFragment().apply {
                        arguments = Bundle().apply {
                            putString("PRODUCT_DATA", json)
                        }
                    })
                    .addToBackStack(null)
                    .commit()
            },
            onDelete = { product -> confirmDelete(product) }
        )
        recyclerView.adapter = adapter

        // 2. Setup Add Button
        val btnAdd = view.findViewById<Button>(R.id.btn_add_product)
        btnAdd.setOnClickListener { navigateToAddProduct() }

        // 3. Setup Swipe Refresh
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.gov_orange_primary)
        swipeRefresh.setOnRefreshListener {
            loadProductsFromApi()
        }

        // 4. Load Data
        loadProductsFromApi()
    }

    private fun loadProductsFromApi() {
        val swipeRefresh = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
        val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)

        // Show Progress Bar only if NOT pulling to refresh
        if (swipeRefresh?.isRefreshing == false) {
            progressBar?.visibility = View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // Fetch Specific User's Products
            val request = GetProductRequest(specific_user = true, )

            try {
                val response = RetrofitClient.instance.getProducts("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    val data = response.body()?.data ?: emptyList()
                    productList.clear()
                    productList.addAll(data)
                    adapter.notifyDataSetChanged()
                    checkEmptyState()
                } else {
                    Toast.makeText(context, "Failed to load products", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                // Stop animations
                progressBar?.visibility = View.GONE
                swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun navigateToAddProduct() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
            .replace(R.id.nav_host_fragment, EntrepreneurAddProductFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun performDelete(product: Product) {
        val progressBar = view?.findViewById<ProgressBar>(R.id.progress_bar)
        progressBar?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            val token = SessionManager.getToken(context) ?: return@launch

            // 1. Minimum Required Fields for Delete
            val rbId = product.productID.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val rbIsDelete = "1".toRequestBody("text/plain".toMediaTypeOrNull())

            // Send dummy/null for other required fields to satisfy Retrofit signature,
            // BUT your PHP validation says 'sometimes|required', so null is fine for update logic if is_delete is true.
            // However, since Retrofit @Part parameters are non-nullable in your interface (except the ones marked ?),
            // you might need to pass null if your interface allows it, or dummy RequestBody.

            // Assuming your Interface uses nullable RequestBody? for optional fields:
            try {
                val response = RetrofitClient.instance.updateProduct(
                    token = "Bearer $token",
                    id = rbId,
                    isDelete = rbIsDelete,
                    name = null, // Backend ignores these if is_delete=1
                    desc = null,
                    categoryId = null,
                    qty = null,
                    unit = null,
                    price = null,
                    premiseId = null,
                    existing_images = emptyList(),
                    new_images = emptyList()
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    Toast.makeText(context, "Product Deleted", Toast.LENGTH_SHORT).show()

                    // Update Local List
                    val index = productList.indexOfFirst { it.productID == product.productID }
                    if (index != -1) {
                        productList.removeAt(index)
                        adapter.notifyItemRemoved(index)
                        checkEmptyState()
                    }
                } else {
                    Toast.makeText(context, response.body()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Remove ${product.product_name} from your list?")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(product)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkEmptyState() {
        if (!isAdded || view == null) return

        val containerPinned = view?.findViewById<View>(R.id.container_pinned_button)
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_profile_products)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_placeholder)

        if (productList.isEmpty()) {
            // === STATE: EMPTY ===
            recyclerView?.visibility = View.GONE
            containerPinned?.visibility = View.GONE
            emptyLayout?.visibility = View.VISIBLE

            // Customize Placeholder Content
            val tvTitle = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_title)
            val tvDesc = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_desc)
            val ivIcon = emptyLayout?.findViewById<ImageView>(R.id.iv_placeholder_icon)
            val btnPlaceholder = emptyLayout?.findViewById<MaterialButton>(R.id.btn_tab_action)

            tvTitle?.text = "No Products Listed"
            tvDesc?.text = "Add your pineapple products to the marketplace."
            ivIcon?.setImageResource(R.drawable.ic_tab_products)

            // Setup the button INSIDE the placeholder
            btnPlaceholder?.visibility = View.VISIBLE
            btnPlaceholder?.text = "+ Add New Product"
            btnPlaceholder?.setOnClickListener {
                navigateToAddProduct()
            }

        } else {
            // === STATE: HAS DATA ===
            emptyLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
            containerPinned?.visibility = View.VISIBLE
        }
    }
}
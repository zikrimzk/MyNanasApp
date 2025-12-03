package com.spm.mynanasapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.request.GetProductRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

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
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                    .replace(R.id.nav_host_fragment, EntrepreneurEditProductFragment().apply {
                        arguments = Bundle().apply {
                            putLong("ID", product.productID)
                            // Pass other fields here to pre-fill real data
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

        // 3. Load Data
        loadProductsFromApi()
    }

    private fun loadProductsFromApi() {
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

    private fun loadData() {
        productList.clear()

        productList.add(Product(1, "MD2 Pineapple Grade A", "Fresh", 100, "Kg", 12.50, 1, null, 1, 1, "2025-01", "2025-01"))
        productList.add(Product(2, "Pineapple Jam", "Sweet", 50, "Jar", 8.90, 1, null, 1, 1, "2025-02", "2025-02"))
        productList.add(Product(1, "MD2 Pineapple Grade A", "Fresh", 100, "Kg", 12.50, 1, null, 1, 1, "2025-01", "2025-01"))
        productList.add(Product(2, "Pineapple Jam", "Sweet", 50, "Jar", 8.90, 1, null, 1, 1, "2025-02", "2025-02"))
        productList.add(Product(1, "MD2 Pineapple Grade A", "Fresh", 100, "Kg", 12.50, 1, null, 1, 1, "2025-01", "2025-01"))
        productList.add(Product(2, "Pineapple Jam", "Sweet", 50, "Jar", 8.90, 1, null, 1, 1, "2025-02", "2025-02"))
        productList.add(Product(1, "MD2 Pineapple Grade A", "Fresh", 100, "Kg", 12.50, 1, null, 1, 1, "2025-01", "2025-01"))
        productList.add(Product(2, "Pineapple Jam", "Sweet", 50, "Jar", 8.90, 1, null, 1, 1, "2025-02", "2025-02"))

        adapter.notifyDataSetChanged()
        checkEmptyState()
    }

    private fun confirmDelete(product: Product) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Remove ${product.product_name} from your list?")
            .setPositiveButton("Delete") { _, _ ->
                val index = productList.indexOf(product)
                if (index != -1) {
                    productList.removeAt(index)
                    adapter.notifyItemRemoved(index)
                    checkEmptyState()
                }
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
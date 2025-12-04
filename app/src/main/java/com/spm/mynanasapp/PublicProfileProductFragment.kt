package com.spm.mynanasapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.request.GetProductRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class PublicProfileProductFragment : Fragment() {

    private var userId: Long = 0
    private lateinit var adapter: ProductAdapter // Use the Market adapter (Read-only)
    private val productList = mutableListOf<Product>()

    companion object {
        fun newInstance(userId: Long) = PublicProfileProductFragment().apply {
            arguments = Bundle().apply { putLong("USER_ID", userId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong("USER_ID") ?: 0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_public_profile_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Setup RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_profile_products)
        recyclerView.layoutManager = GridLayoutManager(context, 2)

        adapter = ProductAdapter(productList) { product ->
            // Navigate to Single Product
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.nav_host_fragment, SingleProductFragment.newInstance(product.productID, product.product_name, product.product_price))
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter

        // 3. Load Data
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
            val request = GetProductRequest(specific_user = true, entID = userId)

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

    private fun checkEmptyState() {
        if (!isAdded || view == null) return
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_profile_products)
        val emptyLayout = view?.findViewById<View>(R.id.layout_empty_placeholder)

        if (productList.isEmpty()) {
            // === STATE: EMPTY ===
            recyclerView?.visibility = View.GONE
            emptyLayout?.visibility = View.VISIBLE

            // Customize Placeholder Content
            val tvTitle = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_title)
            val tvDesc = emptyLayout?.findViewById<TextView>(R.id.tv_placeholder_desc)
            val ivIcon = emptyLayout?.findViewById<ImageView>(R.id.iv_placeholder_icon)

            tvTitle?.text = "No Products Listed"
            tvDesc?.text = "There are no products sold by this user."
            ivIcon?.setImageResource(R.drawable.ic_tab_products)

        } else {
            // === STATE: HAS DATA ===
            emptyLayout?.visibility = View.GONE
            recyclerView?.visibility = View.VISIBLE
        }
    }
}
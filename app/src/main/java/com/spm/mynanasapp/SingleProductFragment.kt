package com.spm.mynanasapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.request.GetProductRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class SingleProductFragment : Fragment() {

    // Data Variables
    private var productId: Long = 0
    private var productName: String? = null
    private var productPrice: Double = 0.0

    // Mock Seller Data (In real app, fetch from API based on premiseID/entID)
//    private val sellerPhone = "60123456789" // Format: 60 + number

    private var currentProduct: Product? = null

    // UI Views
    private lateinit var tvName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvSellerName: TextView
    private lateinit var tvSellerAvatar: ImageView
    private lateinit var tvDescription: TextView
    private lateinit var tvLocation: TextView // New View
    private lateinit var tvCategory: TextView // New View
    private lateinit var tvStock: TextView    // New View
    private lateinit var vpImageGallery: ViewPager2
    private lateinit var tabIndicator: TabLayout
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getLong("ID")
            productName = it.getString("NAME")
            productPrice = it.getDouble("PRICE")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        tvName = view.findViewById(R.id.tv_product_name)
        tvPrice = view.findViewById(R.id.tv_product_price)
        tvSellerName = view.findViewById(R.id.tv_seller_name) // Assuming this ID exists
        tvSellerAvatar = view.findViewById(R.id.iv_seller_avatar) // Assuming this ID exists
        tvDescription = view.findViewById(R.id.tv_description) // Assuming this ID exists
        tvLocation = view.findViewById(R.id.tv_location)
        tvCategory = view.findViewById(R.id.tv_category)
        tvStock = view.findViewById(R.id.tv_stock)
        vpImageGallery = view.findViewById(R.id.vp_image_gallery) // NEW ViewPager
        tabIndicator = view.findViewById(R.id.tab_indicator)
        loadingOverlay = view.findViewById(R.id.layout_loading_overlay)


        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val btnCall = view.findViewById<MaterialButton>(R.id.btn_call)
        val btnWhatsapp = view.findViewById<MaterialButton>(R.id.btn_whatsapp)
        val sellerCard = view.findViewById<View>(R.id.container_seller)

        // 2. Bind Data
        loadProductDetails()

        // 3. Back Action
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingOverlay.visibility = View.VISIBLE
        } else {
            loadingOverlay.visibility = View.GONE
        }
    }

    private fun bindData() {
        val product = currentProduct ?: return
        val seller = product.premise?.user
        val premise = product.premise

        // --- 1. Bind Text Data ---
        tvName.text = product.product_name
        tvDescription.text = product.product_desc ?: "No description available."

        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        tvPrice.text = format.format(product.product_price)

        tvCategory.text = product.category?.category_name.toString() // Use category object name if available
        tvStock.text = "${product.product_qty} ${product.product_unit}"

        // Location Info
        tvLocation.text = "${premise?.premise_address}, ${premise?.premise_city}, ${premise?.premise_state}"
        tvSellerName.text = premise?.user?.ent_username ?: "Seller Unknown"
        if (!product.premise?.user?.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + product.premise.user.ent_profilePhoto
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.placeholder_versatile) // Replace with your default avatar
                .into(this.tvSellerAvatar)
        }

        // --- 2. Setup Image Gallery ---
        val imageList: List<String> = try {
            if (product.product_image.isNullOrEmpty()) emptyList()
            else Gson().fromJson(product.product_image, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) { emptyList() }

        vpImageGallery.adapter = ProductImagePagerAdapter(imageList)

        // Attach indicator dots
        TabLayoutMediator(tabIndicator, vpImageGallery) { tab, position ->
            // Use a placeholder icon for the dot style (defined in your drawable)
            tab.setIcon(R.drawable.ic_images_indicator)
        }.attach()

        // --- 3. Setup Actions (Contact Info) ---
        val sellerPhone = seller?.ent_phoneNo ?: "" // Fallback to mock

        view?.findViewById<MaterialButton>(R.id.btn_call)?.setOnClickListener {
            if (sellerPhone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$sellerPhone")
                startActivity(intent)
            } else {
                Toast.makeText(context, "Phone number unavailable", Toast.LENGTH_SHORT).show()
            }
        }

        view?.findViewById<MaterialButton>(R.id.btn_whatsapp)?.setOnClickListener {
            if (sellerPhone.isNotEmpty()) {
                val message = "Hi, I am interested in your product: ${product.product_name}"
                val url = "https://api.whatsapp.com/send?phone=$sellerPhone&text=${Uri.encode(message)}"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                try { startActivity(intent) } catch (e: Exception) {
                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Phone number unavailable", Toast.LENGTH_SHORT).show()
            }
        }

        view?.findViewById<View>(R.id.container_seller)?.setOnClickListener {
            if (seller != null) {
                // 1. Create Fragment instance
                val publicProfile = EntrepreneurPublicProfileFragment.newInstance(
                    userId = seller.entID,
                    username = seller.ent_username,
                    fullname = seller.ent_fullname
                )

                // 2. Perform Transaction with Animation
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,  // Enter
                        R.anim.slide_out_left,  // Exit
                        R.anim.slide_in_left,   // Pop Enter (Back)
                        R.anim.slide_out_right  // Pop Exit (Back)
                    )
                    .replace(R.id.nav_host_fragment, publicProfile)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Seller information unavailable", Toast.LENGTH_SHORT).show()
            }
        }
            // Navigate to Seller Profile
    }

    private fun loadProductDetails() {
        setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext()) ?: return@launch

            // Fetch single product by ID
            val request = GetProductRequest(productID = productId)

            try {
                // Note: The PHP returns an array even for single product if query isn't refined,
                // but if productID is passed, it should return one product in the 'data' field.
                val response = RetrofitClient.instance.getProducts("Bearer $token", request)

                if (response.isSuccessful && response.body()?.status == true) {
                    val productList = response.body()?.data
                    currentProduct = productList?.firstOrNull() // Get the single product object

                    bindData()
                } else {
                    Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    // --- HIDE MAIN NAV BAR ---
    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(true)
    }

    companion object {
        fun newInstance(id: Long, name: String, price: Double) =
            SingleProductFragment().apply {
                arguments = Bundle().apply {
                    putLong("ID", id)
                    putString("NAME", name)
                    putDouble("PRICE", price)
                }
            }
    }
}

// File: ImageGalleryAdapter.kt (New file, or Inner Class in SingleProductFragment)
class ProductImagePagerAdapter(private val imagePaths: List<String>) :
    RecyclerView.Adapter<ProductImagePagerAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_full_image) // Assuming your XML item layout has this ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        // You'll need a simple layout file (e.g., item_product_gallery_image.xml)
        // containing just an ImageView (iv_full_image) with layout_width="match_parent" and scaleType="centerCrop".
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_gallery_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val relativePath = imagePaths[position]
        val fullUrl = RetrofitClient.SERVER_IMAGE_URL + relativePath

        Glide.with(holder.itemView.context)
            .load(fullUrl)
            .placeholder(R.drawable.placeholder_versatile) // Use your placeholder image
            .into(holder.imageView)
    }

    override fun getItemCount() = imagePaths.size
}
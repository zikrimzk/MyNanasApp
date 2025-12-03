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
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class SingleProductFragment : Fragment() {

    // Data Variables
    private var productId: Long = 0
    private var productName: String? = null
    private var productPrice: Double = 0.0

    // Mock Seller Data (In real app, fetch from API based on premiseID/entID)
    private val sellerPhone = "60123456789" // Format: 60 + number

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
        val tvName = view.findViewById<TextView>(R.id.tv_product_name)
        val tvPrice = view.findViewById<TextView>(R.id.tv_product_price)
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val btnCall = view.findViewById<MaterialButton>(R.id.btn_call)
        val btnWhatsapp = view.findViewById<MaterialButton>(R.id.btn_whatsapp)
        val sellerCard = view.findViewById<View>(R.id.container_seller)

        // 2. Bind Data
        tvName.text = productName ?: "Product Name"

        val format = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
        tvPrice.text = format.format(productPrice)

        // 3. Back Action
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 4. Seller Profile Action
        sellerCard.setOnClickListener {
            // TODO: Navigate to PublicProfileFragment(sellerId)
            Toast.makeText(context, "View Seller Profile", Toast.LENGTH_SHORT).show()
        }

        // 5. Call Action
        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+$sellerPhone")
            startActivity(intent)
        }

        // 6. WhatsApp Action
        btnWhatsapp.setOnClickListener {
            val message = "Hi, I am interested in your product: $productName"
            val url = "https://api.whatsapp.com/send?phone=$sellerPhone&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
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
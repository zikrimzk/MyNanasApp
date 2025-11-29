package com.spm.mynanasapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class EntrepreneurPortalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entrepreneur_portal)

        setupEdgeToEdgeFix()

        if (savedInstanceState == null) {
            loadFragment(EntrepreneurFeedFragment())
            updateNavColors("home")
        }

        val btnHome = findViewById<FrameLayout>(R.id.nav_btn_home)
        val btnProducts = findViewById<FrameLayout>(R.id.nav_btn_products)
        val btnProfile = findViewById<FrameLayout>(R.id.nav_btn_profile)

        btnHome.setOnClickListener {
            loadFragment(EntrepreneurFeedFragment())
            updateNavColors("home")
        }

        btnProducts.setOnClickListener {
             loadFragment(EntrepreneurProductFragment())
            updateNavColors("products")
        }

        btnProfile.setOnClickListener {
             loadFragment(EntrepreneurProfileFragment())
            updateNavColors("profile")
        }
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
        return true
    }

    private fun setupEdgeToEdgeFix() {
        val bottomNavBar = findViewById<View>(R.id.bottom_nav_bar)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavBar) { view, insets ->

            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun updateNavColors(selected: String) {
        val iconHome = findViewById<ImageView>(R.id.nav_icon_home)
        val iconProducts = findViewById<ImageView>(R.id.nav_icon_products)
        val iconProfile = findViewById<ImageView>(R.id.nav_icon_profile)

        val orange = ContextCompat.getColor(this, R.color.gov_orange_primary)
        val grey = Color.parseColor("#BDBDBD")

        iconHome.setColorFilter(grey)
        iconProducts.setColorFilter(grey)
        iconProfile.setColorFilter(grey)

        when (selected) {
            "home" -> iconHome.setColorFilter(orange)
            "products" -> iconProducts.setColorFilter(orange)
            "profile" -> iconProfile.setColorFilter(orange)
        }
    }
}
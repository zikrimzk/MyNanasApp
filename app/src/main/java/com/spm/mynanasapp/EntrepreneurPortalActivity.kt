package com.spm.mynanasapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback

class EntrepreneurPortalActivity : AppCompatActivity() {
    private var backPressedTime: Long = 0
    private var currentTab: String = "home" // Track the current active tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entrepreneur_portal)

        setupEdgeToEdgeFix()
        setupBackPressHandler()

        if (savedInstanceState == null) {
            loadFragment(EntrepreneurFeedFragment())
            updateNavColors("home")
            currentTab = "home"
        }

        val btnHome = findViewById<FrameLayout>(R.id.nav_btn_home)
        val btnProducts = findViewById<FrameLayout>(R.id.nav_btn_products)
        val btnProfile = findViewById<FrameLayout>(R.id.nav_btn_profile)

        btnHome.setOnClickListener {
            loadFragment(EntrepreneurFeedFragment())
            updateNavColors("home")
            currentTab = "home"
        }

        btnProducts.setOnClickListener {
             loadFragment(EntrepreneurProductFragment())
            updateNavColors("products")
            currentTab = "products"
        }

        btnProfile.setOnClickListener {
             loadFragment(EntrepreneurProfileFragment())
            updateNavColors("profile")
            currentTab = "profile"
        }
    }

    fun redirectToProfile() {
        loadFragment(EntrepreneurProfileFragment())
        updateNavColors("profile")
        currentTab = "profile"
        setBottomNavVisibility(true) // Ensure nav is visible
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

    fun setBottomNavVisibility(isVisible: Boolean) {
        val bottomNav = findViewById<android.view.View>(R.id.bottom_nav_bar)
        if (bottomNav != null) {
            bottomNav.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // A. If there are fragments in the back stack (e.g., Edit Profile, Add Product), pop them first
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()

                    // If we just popped the last item, ensure Bottom Nav is visible again
                    if (supportFragmentManager.backStackEntryCount == 1) {
                        setBottomNavVisibility(true)
                    }
                    return
                }

                // B. If we are NOT on the Home Tab, go to Home Tab
                if (currentTab != "home") {
                    loadFragment(EntrepreneurFeedFragment())
                    updateNavColors("home")
                    currentTab = "home"
                    return
                }

                // C. If we ARE on Home Tab, handle Double Click to Exit
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish() // Close the app
                } else {
                    Toast.makeText(this@EntrepreneurPortalActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })
    }
}
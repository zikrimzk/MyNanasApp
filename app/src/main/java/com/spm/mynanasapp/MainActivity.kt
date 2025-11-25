package com.spm.mynanasapp

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        animateEntrance()
    }


    private fun animateEntrance() {
        // 1. Find the views you want to animate
        val logo = findViewById<View>(R.id.cv_logo_container)
        val title = findViewById<View>(R.id.tv_system_name)
        val subtitle = findViewById<View>(R.id.tv_instruction)
        val btn1 = findViewById<View>(R.id.btn_login_entrepreneur)
        val btn2 = findViewById<View>(R.id.btn_login_user)

        // 2. Set them to be invisible initially (alpha = 0)
        // and pushed down slightly (translationY = 100)
        val views = listOf(logo, title, subtitle, btn1, btn2)

        for (view in views) {
            view.alpha = 0f
            view.translationY = 100f
        }

        // 3. Animate them back to visible/original position one by one
        // Start Delay creates a "Cascade" effect
        logo.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(300).start()
        title.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(500).start()
        subtitle.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(700).start()
        btn1.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(900).start()
        btn2.animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(1100).start()
    }
}
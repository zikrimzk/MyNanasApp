package com.spm.mynanasapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // INITIALIZE
        val logo = findViewById<ImageView>(R.id.iv_splash_logo)
        val appName = findViewById<TextView>(R.id.tv_splash_app_name)
        val copyright = findViewById<TextView>(R.id.tv_copyright)

        // FUNCTION : TEXT EFFECT
        applyGradientToText(appName)

        // FUNCTION : ANIMATION
        animateEntrance(logo, appName, copyright)

        // FUNCTION : NAVIGATE
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2500)
    }

    private fun applyGradientToText(textView: TextView) {
        val paint = textView.paint
        val width = paint.measureText(textView.text.toString())
        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            intArrayOf(
                Color.parseColor("#FF9800"), // Start Color
                Color.parseColor("#E65100")  // End Color
            ), null, Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader
    }

    private fun animateEntrance(logo: View, text: View, footer: View) {
        logo.translationY = 50f
        logo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Text: Fade In (Delayed)
        text.translationY = 30f
        text.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(400) // Wait for logo to be half-way done
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Footer: Fade In (Late)
        footer.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(1000)
            .start()
    }
}
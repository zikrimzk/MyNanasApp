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
import androidx.lifecycle.lifecycleScope
import com.spm.mynanasapp.data.model.request.LoginRequest
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        handleAutoLoginOrNavigation()

        // FUNCTION : NAVIGATE
//        Handler(Looper.getMainLooper()).postDelayed({
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            finish()
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
//        }, 2500)
    }

    private fun handleAutoLoginOrNavigation() {
        lifecycleScope.launch {
            // 1. Start the minimum wait timer (so the logo doesn't flash too fast)
            // We use 'async' to let this run in the background while we check the network
            val minTimer = async { delay(2500) }

            var nextIntent: Intent? = null

            // 2. Check if "Remember Me" is checked
            if (SessionManager.isRemembered(this@SplashScreenActivity)) {
                val username = SessionManager.getSavedUsername(this@SplashScreenActivity)
                val password = SessionManager.getSavedPassword(this@SplashScreenActivity)

                if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                    // 3. Attempt Silent Login in Background
                    try {
                        val request = LoginRequest(ent_username = username, ent_password = password)
                        val response = RetrofitClient.instance.login(request)

                        if (response.isSuccessful && response.body()?.status == true) {
                            val data = response.body()!!.data

                            // Save fresh Token and User Data
                            if (data != null) {
                                if (data.token != null) {
                                    SessionManager.saveAuthToken(this@SplashScreenActivity, data.token)
                                    RetrofitClient.setToken(data.token)
                                }
                                if (data.user != null) {
                                    SessionManager.saveUser(this@SplashScreenActivity, data.user)
                                }

                                // Success! Prepare to go to Portal
                                nextIntent = Intent(this@SplashScreenActivity, EntrepreneurPortalActivity::class.java)
                            }
                        }
                    } catch (e: Exception) {
                        // If network fails or password changed, we simply stay null
                        // which means we will fall back to the Login Screen.
                        e.printStackTrace()
                    }
                }
            }

            // 4. Wait for the timer to finish (if the API was faster than 2.5s)
            minTimer.await()

            // 5. Navigate
            if (nextIntent != null) {
                startActivity(nextIntent)
            } else {
                // Fallback to Main Activity (Login)
                startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
            }

            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
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
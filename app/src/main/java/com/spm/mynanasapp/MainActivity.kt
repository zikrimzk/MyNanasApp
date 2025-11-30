package com.spm.mynanasapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Check if user is already logged in
        val savedToken = SessionManager.getToken(this)

        if (savedToken != null) {
            // "Re-inject" the token into Retrofit so API calls work
            RetrofitClient.setToken(savedToken)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StartupFragment())
                .commit()
        }
    }
}
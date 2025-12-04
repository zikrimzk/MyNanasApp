package com.spm.mynanasapp

import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.network.RetrofitClient // Import your RetrofitClient
import com.spm.mynanasapp.data.model.request.LoginRequest // Import your Request Model
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentLoginEntrepreneur.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentLoginEntrepreneur : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login_entrepreneur, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeaderStyle(view)
        animateEntrance(view)

        // Initialization Views
        val cbRememberMe = view.findViewById<CheckBox>(R.id.cb_remember_me)
        val btnLogin = view.findViewById<Button>(R.id.btn_do_login)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tv_forgot_password)
        val tvSignUpAction = view.findViewById<TextView>(R.id.tv_signup_action)
        val etUsername = view.findViewById<EditText>(R.id.et_username)
        val etPassword = view.findViewById<EditText>(R.id.et_password)

        if (SessionManager.isRemembered(requireContext())) {
            etUsername.setText(SessionManager.getSavedUsername(requireContext()))
            etPassword.setText(SessionManager.getSavedPassword(requireContext()))
            cbRememberMe.isChecked = true
        }

        // Button : Login
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validation Logic
            if (username.isEmpty()) {
                etUsername.error = "Username is required"
                etUsername.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Password is required"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "Logging in..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val loginRequest = LoginRequest(ent_username = username, ent_password = password)

                    val response = RetrofitClient.instance.login(loginRequest)

                    if (response.isSuccessful && response.body() != null) {
                        // === STATUS CODE 200 (Success) ===

                        val baseResponse = response.body()!!

                        // Check the Logical Status (from our BaseResponse)
                        if (baseResponse.status) {

                            // 1. Get Data
                            val loginResult = baseResponse.data
                            val token = loginResult?.token
                            val user = loginResult?.user

                            if (cbRememberMe.isChecked) {
                                SessionManager.saveLoginCredentials(requireContext(), username, password)
                            } else {
                                SessionManager.clearLoginCredentials(requireContext())
                            }

                            // 2. Save Token (SharedPreference inside SessionManager)
                            if (token != null) {
                                // Ensure you have a Context here (requireContext())
                                SessionManager.saveAuthToken(requireContext(), token)
                                RetrofitClient.setToken(token)
                            }

                            if (user != null) {
                                SessionManager.saveUser(requireContext(), user)
                            }

                            Toast.makeText(context, "Successfully login, ${user?.ent_fullname}!", Toast.LENGTH_LONG).show()

                            // 3. Navigate to Portal
                            val intent = Intent(requireActivity(), EntrepreneurPortalActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()

                        } else {
                            // API returned 200 OK, but logic failed (e.g. Account Locked)
                            Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        // === STATUS CODE 401, 404, 500 (Error) ===
                        // This is where "Invalid username or password" usually lands because of the 401 code

                        val errorBody = response.errorBody()

                        if (errorBody != null) {
                            try {
                                // 1. Create Gson instance
                                val gson = Gson()

                                // 2. Tell Gson what type of class to look for
                                // We use BaseResponse<LoginResponse> to match the JSON structure
                                val type = object : TypeToken<BaseResponse<LoginResponse>>() {}.type

                                // 3. Convert the Raw Error Stream into your Object
                                val errorResponse: BaseResponse<LoginResponse>? = gson.fromJson(errorBody.charStream(), type)

                                // 4. Extract the clean message
                                val cleanMessage = errorResponse?.message ?: "Request Failed"

                                Toast.makeText(context, cleanMessage, Toast.LENGTH_SHORT).show()

                            } catch (e: Exception) {
                                // Fallback: If JSON parsing fails, just show the error code
                                Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Unknown Error Occurred", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    // === NETWORK ERROR (No Internet) ===
                    Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    // Re-enable button
                    btnLogin.isEnabled = true
                    btnLogin.text = "Login"
                }
            }

            // Create an Intent to switch from the current Activity to the New Portal Activity
//            val intent =
//                android.content.Intent(requireActivity(), EntrepreneurPortalActivity::class.java)
//
//            // Start the new Activity
//            startActivity(intent)
//
//            // Close the Login Activity
//            requireActivity().finish()
        }

        // Link : Forgot Password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(context, "Forgot Password Clicked", Toast.LENGTH_SHORT).show()
        }

        // Link : Sign Up
        tvSignUpAction.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, SignUpEntrepreneurFragment())
                .addToBackStack(null)
                .commit()
        }

    }


    private fun setupHeaderStyle(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_login_title)

        // Apply Gradient (Orange -> Dark Orange)
        val paint = tvTitle.paint
        val width = paint.measureText(tvTitle.text.toString())
        val textShader = LinearGradient(0f, 0f, width, tvTitle.textSize,
            intArrayOf(
                Color.parseColor("#FF9800"), // Brighter Orange
                Color.parseColor("#E65100")  // Deep Orange
            ), null, Shader.TileMode.CLAMP)
        tvTitle.paint.shader = textShader
    }

    private fun animateEntrance(view: View) {
        val title = view.findViewById<View>(R.id.tv_login_title)
        val subtitle = view.findViewById<View>(R.id.tv_login_subtitle)
        val inputUser = view.findViewById<View>(R.id.til_username)
        val inputPass = view.findViewById<View>(R.id.til_password)
        val remember = view.findViewById<View>(R.id.cb_remember_me)
        val forgot = view.findViewById<View>(R.id.tv_forgot_password)
        val btnLogin = view.findViewById<View>(R.id.btn_do_login)
        val signUpText = view.findViewById<View>(R.id.tv_signup_text)
        val signUpAction = view.findViewById<View>(R.id.tv_signup_action)

        val views = listOf(title, subtitle, inputUser, inputPass, remember, forgot, btnLogin, signUpText, signUpAction)

        for (v in views) {
            v?.alpha = 0f
            v?.translationY = 50f
        }

        title?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()
        subtitle?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()

        inputUser?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()
        inputPass?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(500)?.start()

        remember?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(600)?.start()
        forgot?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(600)?.start()

        btnLogin?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(700)?.start()
        signUpText?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(800)?.start()
        signUpAction?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(800)?.start()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentLoginEntrepreneur.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FragmentLoginEntrepreneur().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
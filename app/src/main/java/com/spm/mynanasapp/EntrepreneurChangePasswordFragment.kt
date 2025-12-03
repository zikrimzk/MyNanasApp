package com.spm.mynanasapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.request.ChangePasswordRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch

class EntrepreneurChangePasswordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        val btnSave = view.findViewById<ImageView>(R.id.btn_save)

        val etCurrent = view.findViewById<TextInputEditText>(R.id.et_current_password)
        val etNew = view.findViewById<TextInputEditText>(R.id.et_new_password)
        val etConfirm = view.findViewById<TextInputEditText>(R.id.et_confirm_password)

        // 2. Setup Real-time Validation Logic
        setupPasswordValidation(view, etNew)

        // 3. Back Action
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 4. Save Action
        btnSave.setOnClickListener {
            val currentPass = etCurrent.text.toString()
            val newPass = etNew.text.toString()
            val confirmPass = etConfirm.text.toString()

            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordValid(newPass)) {
                Toast.makeText(context, "New password does not meet criteria", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performChangePassword(currentPass, newPass)
        }
    }

    private fun performChangePassword(currentPass: String, newPass: String) {
        // Optional: Show loading dialog here

        viewLifecycleOwner.lifecycleScope.launch {
            val token = SessionManager.getToken(requireContext())
            if (token == null) return@launch

            val request = ChangePasswordRequest(
                current_password = currentPass,
                new_password = newPass
            )

            try {
                val response = RetrofitClient.instance.changePassword("Bearer $token", request)

                if (response.isSuccessful && response.body() != null) {
                    val baseResponse = response.body()!!

                    if (baseResponse.status) {
                        // === SUCCESS ===
                        // 1. Show Success Message
                        Toast.makeText(context, "Password changed! Please login again.", Toast.LENGTH_LONG).show()

                        // 2. Perform Logout & Redirect
                        performLogoutAndRedirect()

                    } else {
                        // Logic Error (e.g. Current password wrong, or New matches Old)
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // API Error (401, 500)
                    handleError(response)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogoutAndRedirect() {
        // 1. Clear Local Session
        SessionManager.clearSession(requireContext())
        RetrofitClient.setToken(null)

        // 2. Create Intent to LoginActivity (MainActivity)
        val intent = Intent(requireActivity(), MainActivity::class.java)

        // 3. Add Flags to clear the back stack
        // This prevents the user from pressing "Back" and returning to the profile
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // 4. Start
        startActivity(intent)
        requireActivity().finish()
    }

    // Generic error handler
    private fun <T> handleError(response: retrofit2.Response<BaseResponse<T>>) {
        val errorBody = response.errorBody()
        if (errorBody != null) {
            try {
                val gson = Gson()
                val type = object : TypeToken<BaseResponse<LoginResponse>>() {}.type
                val errorResponse: BaseResponse<LoginResponse>? = gson.fromJson(errorBody.charStream(), type)
                Toast.makeText(context, errorResponse?.message ?: "Request Failed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    private fun setupPasswordValidation(view: View, etNew: TextInputEditText) {
        val ruleLength = view.findViewById<TextView>(R.id.rule_length)
        val ruleCase = view.findViewById<TextView>(R.id.rule_case)
        val ruleSpecial = view.findViewById<TextView>(R.id.rule_special)

        val successColor = Color.parseColor("#4CAF50") // Green
        val defaultColor = Color.parseColor("#9E9E9E") // Grey

        etNew.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                // Rule 1: Length
                if (password.length >= 8) {
                    ruleLength.setTextColor(successColor)
                    ruleLength.text = "✓ Minimum 8 characters"
                } else {
                    ruleLength.setTextColor(defaultColor)
                    ruleLength.text = "• Minimum 8 characters"
                }

                // Rule 2: Case
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                if (hasUpper && hasLower) {
                    ruleCase.setTextColor(successColor)
                    ruleCase.text = "✓ Upper & Lower case letters"
                } else {
                    ruleCase.setTextColor(defaultColor)
                    ruleCase.text = "• Upper & Lower case letters"
                }

                // Rule 3: Special/Number
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.matches(".*[^a-zA-Z0-9].*".toRegex())
                if (hasDigit && hasSpecial) {
                    ruleSpecial.setTextColor(successColor)
                    ruleSpecial.text = "✓ At least 1 number & 1 special char"
                } else {
                    ruleSpecial.setTextColor(defaultColor)
                    ruleSpecial.text = "• At least 1 number & 1 special char"
                }
            }
        })
    }

    // --- LOGIC: FINAL CHECK ---
    private fun isPasswordValid(password: String): Boolean {
        val hasLength = password.length >= 8
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.matches(".*[^a-zA-Z0-9].*".toRegex())

        return hasLength && hasUpper && hasLower && hasDigit && hasSpecial
    }
}
package com.spm.mynanasapp

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.request.LoginRequest
import com.spm.mynanasapp.data.model.request.RegisterRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SignUpEntrepreneurFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignUpEntrepreneurFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_sign_up_entrepreneur, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FUNCTION : ANIMATION
        animateEntrance(view)

        // FUNCTION INITIALIZATION
        setupToggleGroup(view)
        setupDatePicker(view)
        setupIcLogic(view)
        setupPhoneLogic(view)
        setupPasswordLogic(view)
        setupLoginLink(view)

        // Initialization Views
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.ent_toggle_group_type)
        val btnRegister = view.findViewById<Button>(R.id.ent_btn_register)

        val etFullName = view.findViewById<EditText>(R.id.ent_et_fullname)
        val etIC = view.findViewById<EditText>(R.id.ent_et_ic)
        val etDOB = view.findViewById<EditText>(R.id.ent_et_dob)
        val etPhone = view.findViewById<EditText>(R.id.ent_et_phone)
        val etEmail = view.findViewById<EditText>(R.id.ent_et_email)
        val etBusinessName = view.findViewById<EditText>(R.id.ent_et_business_name)
        val etSSM = view.findViewById<EditText>(R.id.ent_et_ssm)
        val etUsername = view.findViewById<EditText>(R.id.ent_et_username)
        val etPassword = view.findViewById<EditText>(R.id.ent_et_password)
        val etConfirmPassword = view.findViewById<EditText>(R.id.ent_et_confirm_password)

        btnRegister.setOnClickListener {
            if (isFieldEmpty(etFullName, "Full Name")) return@setOnClickListener
            if (isFieldEmpty(etIC, "IC Number")) return@setOnClickListener
            if (isFieldEmpty(etDOB, "Date of Birth")) return@setOnClickListener
            if (isFieldEmpty(etPhone, "Phone Number")) return@setOnClickListener
            if (isFieldEmpty(etEmail, "Email Address")) return@setOnClickListener
            if (toggleGroup.checkedButtonId == R.id.ent_btn_type_company) {
                if (isFieldEmpty(etBusinessName, "Business Name")) return@setOnClickListener
                if (isFieldEmpty(etSSM, "SSM Number")) return@setOnClickListener
            }
            if (isFieldEmpty(etUsername, "Username")) return@setOnClickListener
            if (isFieldEmpty(etPassword, "Password")) return@setOnClickListener
            if (isFieldEmpty(etConfirmPassword, "Confirm Password")) return@setOnClickListener

            if(etPassword.text.toString() != etConfirmPassword.text.toString()) {
                etConfirmPassword.error = "Password does not match"
                etConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            val fullName = etFullName.text.toString().trim()
            val ic = etIC.text.toString().trim()
            val rawDob = etDOB.text.toString().trim()
            val dob = try {
                val inputFormat = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.US)
                val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val dateObj = inputFormat.parse(rawDob)
                outputFormat.format(dateObj!!) // Result: "2003-07-23"
            } catch (e: Exception) {
                rawDob
            }
            val phone = etPhone.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val businessName = etBusinessName.text.toString().trim()
            val ssm = etSSM.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            btnRegister.isEnabled = false
            btnRegister.text = "Registering ..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val registerRequest = RegisterRequest(ent_fullname = fullName, ent_icNo = ic, ent_dob = dob, ent_phoneNo = phone, ent_email = email, ent_username = username, ent_password = password, ent_business_name = businessName, ent_business_ssmNo = ssm)

                    val response = RetrofitClient.instance.register(registerRequest)

                    if (response.isSuccessful && response.body() != null) {
                        // === STATUS CODE 200 (Success) ===

                        val baseResponse = response.body()!!

                        // Check the Logical Status (from our BaseResponse)
                        if (baseResponse.status) {

                            // 1. Get Data
                            val registerResult = baseResponse.data
                            val user = registerResult;

                            Toast.makeText(context, " Registered successfully, ${user?.ent_fullname}!", Toast.LENGTH_LONG).show()

                            // 3. Navigate to login fragment
//                            val intent = Intent(requireActivity(), EntrepreneurPortalActivity::class.java)
//                            startActivity(intent)
//                            requireActivity().finish()
                            val nextFragment = FragmentLoginEntrepreneur()

                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, nextFragment) // <--- Change this ID to match your XML
                                .addToBackStack(null) // Optional: Adds this transaction to the back button history
                                .commit()

                        } else {
                            // API returned 200 OK, but logic failed (e.g. Account Locked)
                            Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        // === STATUS CODE 401, 404, 500 (Error) ===
                        val errorBody = response.errorBody()

                        if (errorBody != null) {
                            // CASE 1: Validation Error (422)
                            if (response.code() == 422) {
                                try {
                                    val gson = Gson()
                                    val type = object : TypeToken<LaravelValidationError>() {}.type
                                    val errorResponse: LaravelValidationError = gson.fromJson(errorBody.charStream(), type)

                                    // CALL THE HELPER FUNCTION HERE
                                    handleBackendValidationErrors(errorResponse.errors, view)

                                    Toast.makeText(context, "Please check the highlighted fields", Toast.LENGTH_SHORT).show()

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Validation failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            // CASE 2: Other Errors (401, 500, etc.)
                            else {
                                try {
                                    // Try parsing as your standard BaseResponse if it's not a validation error
                                    val gson = Gson()
                                    val type = object : TypeToken<BaseResponse<Any>>() {}.type
                                    val errorResponse: BaseResponse<Any> = gson.fromJson(errorBody.charStream(), type)
                                    Toast.makeText(context, errorResponse.message, Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
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
                    btnRegister.isEnabled = true
                    btnRegister.text = "Complete Registration"
                }
            }
        }

//        // BUTTON : REGISTER ENTREPRENEUR
//        view.findViewById<Button>(R.id.ent_btn_register).setOnClickListener {
//            Toast.makeText(context, "Registering...", Toast.LENGTH_SHORT).show()
//        }
    }


    private fun setupToggleGroup(view: View) {
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.ent_toggle_group_type)
        val businessContainer = view.findViewById<LinearLayout>(R.id.ent_container_business_details)
        val btnInd = view.findViewById<MaterialButton>(R.id.ent_btn_type_individual)
        val btnComp = view.findViewById<MaterialButton>(R.id.ent_btn_type_company)

        fun updateButtonColors(isCompany: Boolean) {
            val primary = ContextCompat.getColor(requireContext(), R.color.gov_orange_primary)
            val white = ContextCompat.getColor(requireContext(), R.color.white)
            val transparent = Color.TRANSPARENT

            if (isCompany) {
                // Company is Selected (White BG, Orange Text)
                btnComp.setBackgroundColor(white)
                btnComp.setTextColor(primary)

                // Individual is Unselected (Transparent BG, White Text)
                btnInd.setBackgroundColor(transparent)
                btnInd.setTextColor(white)
            } else {
                // Individual is Selected (White BG, Orange Text)
                btnInd.setBackgroundColor(white)
                btnInd.setTextColor(primary)

                // Company is Unselected (Transparent BG, White Text)
                btnComp.setBackgroundColor(transparent)
                btnComp.setTextColor(white)
            }
        }
        updateButtonColors(false)

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.ent_btn_type_company -> {
                        businessContainer.apply {
                            alpha = 0f
                            visibility = View.VISIBLE
                            animate().alpha(1f).setDuration(300).start()
                        }
                        updateButtonColors(true)
                    }
                    R.id.ent_btn_type_individual -> {
                        businessContainer.visibility = View.GONE
                        updateButtonColors(false)
                    }
                }
            }
        }
    }

    private fun setupDatePicker(view: View) {
        val etDob = view.findViewById<EditText>(R.id.ent_et_dob)

        etDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Format: DD/MM/YYYY
                    val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    etDob.setText(date)
                },
                year, month, day
            )
            datePickerDialog.show()
        }
    }

    private fun setupIcLogic(view: View) {
        val etIc = view.findViewById<EditText>(R.id.ent_et_ic)
        val etDob = view.findViewById<EditText>(R.id.ent_et_dob)

        etIc.addTextChangedListener(object : TextWatcher {
            var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s == null) return
                isEditing = true

                var str = s.toString().replace("-", "")

                if (str.length > 12) {
                    str = str.substring(0, 12)
                }

                if (str.length > 6) {
                    str = str.substring(0, 6) + "-" + str.substring(6)
                }
                if (str.length > 9) {
                    str = str.substring(0, 9) + "-" + str.substring(9)
                }

                etIc.setText(str)
                etIc.setSelection(str.length)

                if (str.length >= 6) {

                    val rawYear = str.substring(0, 2)
                    val rawMonth = str.substring(2, 4)
                    val rawDay = str.substring(4, 6)

                    val yearInt = rawYear.toIntOrNull() ?: 0
                    val currentYearShort = Calendar.getInstance().get(Calendar.YEAR) % 100
                    val prefix = if (yearInt > currentYearShort) "19" else "20"

                    val fullYear = prefix + rawYear

                    etDob.setText("$rawDay/$rawMonth/$fullYear")
                }

                isEditing = false
            }
        })
    }

    private fun setupPhoneLogic(view: View) {
        val etPhone = view.findViewById<EditText>(R.id.ent_et_phone)

        if (etPhone.text.isEmpty()) {
            etPhone.setText("+60 ")
            etPhone.setSelection(etPhone.text.length)
        }

        etPhone.addTextChangedListener(object : TextWatcher {
            var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                val input = s.toString()
                if (!input.startsWith("+60 ")) {
                    etPhone.setText("+60 ")
                    etPhone.setSelection(etPhone.text.length)
                    isEditing = false
                    return
                }

                var digits = ""
                if (input.length > 4) {
                    digits = input.substring(4).replace(" ", "")
                }

                if (digits.length > 10) {
                    digits = digits.substring(0, 10)
                }

                val formatted = StringBuilder("+60 ")
                for (i in digits.indices) {
                    // Logic for spacing:
                    // Standard 012 (9 digits): +60 12 345 6789
                    // Standard 011 (10 digits): +60 11 1309 7546

                    // Add space after the prefix (index 2) -> "11 " or "12 "
                    if (i == 2) formatted.append(" ")

                    // Complex spacing logic for the second block
                    // If it starts with '11' (length likely 10), we space at index 6
                    // If it starts with '12'/'19' (length likely 9), we space at index 5

                    val is011 = digits.startsWith("11")

                    if (is011) {
                        // Format: 11 1309 7546 (Space after 6th digit)
                        if (i == 6) formatted.append(" ")
                    } else {
                        // Format: 12 345 6789 (Space after 5th digit)
                        if (i == 5) formatted.append(" ")
                    }

                    formatted.append(digits[i])
                }

                etPhone.setText(formatted.toString())
                etPhone.setSelection(formatted.length)

                isEditing = false
            }
        })
    }

    private fun setupPasswordLogic(view: View) {
        val etPass = view.findViewById<EditText>(R.id.ent_et_password)
        val ruleLength = view.findViewById<TextView>(R.id.ent_rule_length)
        val ruleCase = view.findViewById<TextView>(R.id.ent_rule_case)
        val ruleSpecial = view.findViewById<TextView>(R.id.ent_rule_special)

        val successColor = Color.parseColor("#4CAF50")
        val errorColor = Color.parseColor("#999999")

        etPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()

                // Rule 1: Length >= 8
                if (password.length >= 8) {
                    ruleLength.setTextColor(successColor)
                    ruleLength.text = "✓ Minimum 8 characters"
                } else {
                    ruleLength.setTextColor(errorColor)
                    ruleLength.text = "• Minimum 8 characters"
                }

                // Rule 2: Upper & Lower Case
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                if (hasUpper && hasLower) {
                    ruleCase.setTextColor(successColor)
                    ruleCase.text = "✓ Upper & Lower case letters"
                } else {
                    ruleCase.setTextColor(errorColor)
                    ruleCase.text = "• Upper & Lower case letters"
                }

                // Rule 3: Number & Special Char
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.matches(".*[^a-zA-Z0-9].*".toRegex()) // Checks for non-alphanumeric

                if (hasDigit && hasSpecial) {
                    ruleSpecial.setTextColor(successColor)
                    ruleSpecial.text = "✓ At least 1 number & 1 special char"
                } else {
                    ruleSpecial.setTextColor(errorColor)
                    ruleSpecial.text = "• At least 1 number & 1 special char"
                }
            }
        })
    }

    private fun setupLoginLink(view: View) {
        val tvLogin = view.findViewById<TextView>(R.id.ent_tv_login_link)
        val fullText = "Already have an account? Log In"
        val targetWord = "Log In"

        val spannableString = SpannableString(fullText)
        val startIndex = fullText.indexOf(targetWord)
        val endIndex = startIndex + targetWord.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                parentFragmentManager.popBackStack()
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

        val orangeColor = ContextCompat.getColor(requireContext(), R.color.gov_orange_primary)

        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(ForegroundColorSpan(orangeColor), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvLogin.text = spannableString
        tvLogin.movementMethod = LinkMovementMethod.getInstance()
        tvLogin.highlightColor = Color.TRANSPARENT
    }

    private fun animateEntrance(view: View) {
        val title = view.findViewById<View>(R.id.ent_tv_header_title)
        val scrollContent = view.findViewById<View>(R.id.ent_toggle_group_type)?.parent as? View

        title?.alpha = 0f
        title?.translationY = 50f
        scrollContent?.alpha = 0f
        scrollContent?.translationY = 100f

        title?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(200)?.start()
        scrollContent?.animate()?.alpha(1f)?.translationY(0f)?.setDuration(600)?.setStartDelay(400)?.start()
    }

    private fun isFieldEmpty(editText: EditText, label: String): Boolean {
        if (editText.text.toString().trim().isEmpty()) {
            editText.error = "$label is required"
            editText.requestFocus()
            return true
        }
        return false
    }

    data class LaravelValidationError(
        val message: String,
        val errors: Map<String, List<String>> // dynamic map: "field_name" -> ["Error message"]
    )

    private fun handleBackendValidationErrors(errorMap: Map<String, List<String>>, view: View) {
        // 1. Map Backend Field Names -> Android EditText IDs
        val fieldMap = mapOf(
            "ent_fullname" to view.findViewById<EditText>(R.id.ent_et_fullname),
            "ent_icNo" to view.findViewById<EditText>(R.id.ent_et_ic),
            "ent_dob" to view.findViewById<EditText>(R.id.ent_et_dob),
            "ent_phoneNo" to view.findViewById<EditText>(R.id.ent_et_phone),
            "ent_email" to view.findViewById<EditText>(R.id.ent_et_email),
            "ent_username" to view.findViewById<EditText>(R.id.ent_et_username),
            "ent_password" to view.findViewById<EditText>(R.id.ent_et_password),
            "ent_business_name" to view.findViewById<EditText>(R.id.ent_et_business_name),
            "ent_business_ssmNo" to view.findViewById<EditText>(R.id.ent_et_ssm)
        )

        var firstErrorView: View? = null

        // 2. Iterate through the API errors
        for ((fieldName, errorMessages) in errorMap) {
            val editText = fieldMap[fieldName]

            // If we found the EditText and there is an error message
            if (editText != null && errorMessages.isNotEmpty()) {
                // Set the error on the EditText (Android handles the red icon + message)
                editText.error = errorMessages[0]

                // Track the first field found so we can focus on it later
                if (firstErrorView == null) {
                    firstErrorView = editText
                }
            }
        }

        // 3. Scroll/Focus to the first error found
        firstErrorView?.requestFocus()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SignUpEntrepreneurFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SignUpEntrepreneurFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
package com.spm.mynanasapp

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.spm.mynanasapp.utils.SessionManager

class EntrepreneurEditProfileFragment : Fragment() {

    private lateinit var ivProfile: ImageView
    private var selectedImageUri: Uri? = null

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivProfile.setImageURI(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_entrepreneur_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find Views
        ivProfile = view.findViewById(R.id.iv_profile)
        val btnChangePhoto = view.findViewById(R.id.btn_change_photo) as TextView
        val btnBack = view.findViewById(R.id.btn_back) as ImageView
        val btnSave = view.findViewById(R.id.btn_save) as ImageView
        val btnChangePassword = view.findViewById(R.id.btn_change_password) as TextView

        val etFullname = view.findViewById<EditText>(R.id.et_fullname)
        val etUsername = view.findViewById<EditText>(R.id.et_username)
        val etBio = view.findViewById<EditText>(R.id.et_bio)
        val etPhone = view.findViewById<EditText>(R.id.et_phone)
        val etEmail = view.findViewById<EditText>(R.id.et_email)
        val etIc = view.findViewById<EditText>(R.id.et_ic)
        val etDob = view.findViewById<EditText>(R.id.et_dob)

        // 2. Pre-fill Data
        val user = SessionManager.getUser(requireContext())
        etFullname.setText(user?.ent_fullname ?: "User Fullname")
        etUsername.setText(user?.ent_username ?: "username")
        etBio.setText(user?.ent_bio ?: "")

        // Locked Fields
        etEmail.setText(user?.ent_email ?: "")
        etIc.setText(user?.ent_icNo ?: "")
        etDob.setText(user?.ent_dob ?: "")

        // 3. Attach Phone Formatting Logic
        setupPhoneLogic(etPhone)
        // Ensure formatting applies to the loaded text too if needed
        if (etPhone.text.toString().isEmpty()) {
            etPhone.setText("+60 ")
        } else {
            etPhone.setText(user?.ent_phoneNo ?: "+60")
        }

        // 4. Actions
        btnChangePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSave.setOnClickListener {
            // TODO: API Call Update
            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        btnChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? EntrepreneurPortalActivity)?.setBottomNavVisibility(false)
    }

    private fun setupPhoneLogic(etPhone: EditText) {
        // Ensure initial state
        if (etPhone.text.isEmpty()) etPhone.setText("+60 ")

        etPhone.addTextChangedListener(object : TextWatcher {
            var isEditing = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                val input = s.toString()

                // 1. Force Prefix
                if (!input.startsWith("+60 ")) {
                    etPhone.setText("+60 ")
                    etPhone.setSelection(etPhone.text.length)
                    isEditing = false
                    return
                }

                // 2. Clean digits
                var digits = ""
                if (input.length > 4) {
                    digits = input.substring(4).replace(" ", "")
                }

                // 3. Limit Length (Max 10 digits after +60)
                if (digits.length > 10) {
                    digits = digits.substring(0, 10)
                }

                // 4. Format
                val formatted = StringBuilder("+60 ")
                for (i in digits.indices) {
                    if (i == 2) formatted.append(" ")
                    val is011 = digits.startsWith("11")
                    if (is011) {
                        if (i == 6) formatted.append(" ")
                    } else {
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
}
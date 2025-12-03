package com.spm.mynanasapp

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.FileUtils
import com.spm.mynanasapp.utils.SessionManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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

        // Load existing profile photo using Glide
        if (!user?.ent_profilePhoto.isNullOrEmpty()) {
            val fullUrl = RetrofitClient.SERVER_IMAGE_URL + user?.ent_profilePhoto
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_launcher_background) // Replace with your default avatar
                .into(ivProfile)
        }

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
//            Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
//            parentFragmentManager.popBackStack()
            updateProfile(
                fullname = etFullname.text.toString(),
                username = etUsername.text.toString(),
                bio = etBio.text.toString()
            )
        }

        btnChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_out_down)
                .replace(R.id.nav_host_fragment, EntrepreneurChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateProfile(fullname: String, username: String, bio: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            val token = SessionManager.getToken(context)
            if (token == null) {
                Toast.makeText(context, "Session expired", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // 1. Prepare Text Data (Convert String to RequestBody)
            // We use "text/plain" so quotes aren't added to the database string
            val rbFullname = fullname.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbUsername = username.toRequestBody("text/plain".toMediaTypeOrNull())
            val rbBio = bio.toRequestBody("text/plain".toMediaTypeOrNull())

            // 2. Prepare Image Data (Convert Uri -> File -> MultipartBody.Part)
            var bodyImage: MultipartBody.Part? = null

            if (selectedImageUri != null) {
                val file = FileUtils.getFileFromUri(context, selectedImageUri!!)
                if (file != null) {
                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    // "ent_profilePhoto" must match the PHP $request->file('ent_profilePhoto')
                    bodyImage = MultipartBody.Part.createFormData("ent_profilePhoto", file.name, reqFile)
                }
            }

            try {
                // 3. Call API
                val response = RetrofitClient.instance.updateUserProfile(
                    "Bearer $token",
                    rbFullname,
                    rbUsername,
                    rbBio,
                    bodyImage
                )

                if (response.isSuccessful && response.body() != null) {
                    val baseResponse = response.body()!!

                    if (baseResponse.status && baseResponse.data != null) {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()

                        // 4. IMPORTANT: Update Local Session with the new User Data
                        // The API returns the updated user object. We save it so the app knows the new details.
                        val updatedUser = baseResponse.data
                        SessionManager.saveUser(context, updatedUser)

                        // 5. Return to previous screen
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(context, baseResponse.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    handleError(response)
                }

            } catch (e: Exception) {
                Log.e("EditProfile", "Error", e)
                Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ... [setupPhoneLogic remains the same] ...

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
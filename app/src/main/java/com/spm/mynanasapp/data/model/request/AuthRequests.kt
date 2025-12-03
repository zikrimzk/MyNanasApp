package com.spm.mynanasapp.data.model.request

data class LoginRequest(
    val ent_username: String,
    val ent_password: String
)

data class RegisterRequest(
    val ent_fullname: String,
    val ent_icNo: String,
    val ent_dob: String,
    val ent_phoneNo: String,
    val ent_email: String,
    val ent_username: String,
    val ent_password: String,
    val ent_business_name: String? = null,
    val ent_business_ssmNo: String? = null
)

data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)
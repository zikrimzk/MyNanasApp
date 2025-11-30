package com.spm.mynanasapp.data.model.entity

data class User(
    val entID: Long,
    val ent_fullname: String,
    val ent_email: String,
    val ent_phoneNo: String,
    val ent_icNo: String,
    val ent_dob: String, // Typically stored as a String (ISO 8601) or a specific Date object in Kotlin
    val ent_bio: String?, // Nullable field
    val ent_profilePhoto: String?, // Nullable field
    val ent_account_status: Int, // Default 1
    val ent_account_visibility: Int, // Default 1
    val ent_business_name: String?, // Nullable field
    val ent_business_ssmNo: String?, // Nullable field
    val ent_username: String,
    val ent_password: String,
    val remember_token: String?, // Corresponds to rememberToken(), nullable
    val created_at: String, // Corresponds to timestamps()
    val updated_at: String  // Corresponds to timestamps()
)
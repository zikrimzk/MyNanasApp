package com.spm.mynanasapp.data.model.entity

import java.io.Serializable

data class Premise(
    val premiseID: Long,
    val premise_type: String,
    val premise_name: String,
    val premise_address: String?, // Nullable field
    val premise_state: String?, // Nullable field
    val premise_city: String?, // Nullable field
    val premise_postcode: String?, // Nullable field
    val premise_landsize: String?, // Nullable field
    val premise_status: Int, // Default 1
    val premise_coordinates: String?, // Nullable field
    val entID: Long, // Foreign Key
    val created_at: String,
    val updated_at: String
) : Serializable
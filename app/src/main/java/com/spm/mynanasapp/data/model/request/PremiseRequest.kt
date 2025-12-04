package com.spm.mynanasapp.data.model.request

data class AddPremiseRequest(
    val premise_type: String,
    val premise_name: String,
    val premise_address: String?,
    val premise_city: String?,
    val premise_state: String?,
    val premise_postcode: String?,
    val premise_landsize: String?,
    val premise_coordinates: String? = null
)

data class UpdatePremiseRequest(
    val premiseID: Long,
    val premise_type: String,
    val premise_name: String,
    val premise_address: String?,
    val premise_city: String?,
    val premise_state: String?,
    val premise_postcode: String?,
    val premise_landsize: String?,
    val premise_coordinates: String? = null,
    val is_delete: Boolean
)

data class GetPremiseRequest(
    val premise_type: String, // "All", "Farm", "Shop"
    val specific_user: Boolean,
    val entID: Long? = null
)
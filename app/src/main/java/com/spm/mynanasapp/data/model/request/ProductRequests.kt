package com.spm.mynanasapp.data.model.request

data class GetProductRequest(
    val premise_state: String = "All",
    val premise_city: String = "All",
    val specific_user: Boolean = false,
    val productID: Long? = null,
    val categoryID: Long? = null,
    val entID: Long? = null,
)
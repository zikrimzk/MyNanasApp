package com.spm.mynanasapp.data.model.entity

import java.io.Serializable

data class Product(
    val productID: Long,
    val product_name: String,
    val product_desc: String?,
    val product_qty: Int,
    val product_unit: String?,
    val product_price: Double, // Converted from decimal(10, 2)
    val product_status: Int,
    val product_image: String?,
    // Foreign Keys
    val categoryID: Long,
    val premiseID: Long,
    val created_at: String,
    val updated_at: String,

    val premise: Premise? = null,
) : Serializable
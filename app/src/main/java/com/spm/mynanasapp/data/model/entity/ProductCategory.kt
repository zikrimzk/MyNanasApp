package com.spm.mynanasapp.data.model.entity

data class ProductCategory(
    val categoryID: Long,
    val category_name: String,
    val category_desc: String?, // Nullable field
    val category_status: Int, // Default 1
    val created_at: String,
    val updated_at: String
)

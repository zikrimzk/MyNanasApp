package com.spm.mynanasapp.data.model.entity

import com.google.gson.annotations.SerializedName

data class ProductCategory(
    @SerializedName("categoryID") val categoryID: Long,
    @SerializedName("category_name") val category_name: String,
    @SerializedName("category_desc") val category_desc: String?, // Nullable field
    @SerializedName("category_status") val category_status: Int, // Default 1
    @SerializedName("created_at") val created_at: String,
    @SerializedName("updated_at") val updated_at: String
)


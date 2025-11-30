package com.spm.mynanasapp.data.model.response

import com.google.gson.annotations.SerializedName

// This is default reponse structure from the server (API Laravel)
// T can be User, Product, or LoginResult
data class BaseResponse<T>(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
)
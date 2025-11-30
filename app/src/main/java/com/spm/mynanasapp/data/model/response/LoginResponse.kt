package com.spm.mynanasapp.data.model.response

import com.spm.mynanasapp.data.model.entity.User

// Expand the BaseResponse for login, because it return extra data which is token
data class LoginResponse(
    val token: String,
    val user: User
)
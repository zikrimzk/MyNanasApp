package com.spm.mynanasapp.data.network

import com.spm.mynanasapp.data.model.entity.User
import com.spm.mynanasapp.data.model.request.LoginRequest
import com.spm.mynanasapp.data.model.request.RegisterRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    // == START: AUTHENTICATION ==
    // Login returns "LoginResult" inside the BaseResponse
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<BaseResponse<LoginResponse>>

    // Register returns just the "User" inside the BaseResponse
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<BaseResponse<User>>

    // Logout returns nothing (Unit) in the data field
    @POST("logout")
    suspend fun logout(@Header("Authorization") token: String): Response<BaseResponse<Unit>>
    // == END: AUTHENTICATION ==


}
package com.spm.mynanasapp.data.network

import com.spm.mynanasapp.data.model.entity.Post
import com.spm.mynanasapp.data.model.entity.User
import com.spm.mynanasapp.data.model.request.GetPostRequest
import com.spm.mynanasapp.data.model.request.LoginRequest
import com.spm.mynanasapp.data.model.request.RegisterRequest
import com.spm.mynanasapp.data.model.request.UpdatePostRequest
import com.spm.mynanasapp.data.model.response.BaseResponse
import com.spm.mynanasapp.data.model.response.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

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

    // == START: POST ==
    @POST("get_posts")
    suspend fun getPosts(@Header("Authorization") token: String, @Body request: GetPostRequest): Response<BaseResponse<List<Post>>>

    @Multipart
    @POST("add_post")
    suspend fun addPost(
        @Header("Authorization") token: String,
        @Part("post_caption") caption: RequestBody,
        @Part("post_type") type: RequestBody,
        @Part("post_location") location: RequestBody?, // Nullable
        @Part images: List<MultipartBody.Part> // The array of images
    ): Response<BaseResponse<Post>>

    @POST("update_post")
    suspend fun updatePost(@Header("Authorization") token: String, @Body request: UpdatePostRequest): Response<BaseResponse<Unit>>
    // == END: POST ==
}
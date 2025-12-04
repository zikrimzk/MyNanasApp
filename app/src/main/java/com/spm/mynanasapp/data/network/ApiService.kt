package com.spm.mynanasapp.data.network

import com.spm.mynanasapp.data.model.entity.Post
import com.spm.mynanasapp.data.model.entity.Premise
import com.spm.mynanasapp.data.model.entity.Product
import com.spm.mynanasapp.data.model.entity.ProductCategory
import com.spm.mynanasapp.data.model.entity.User
import com.spm.mynanasapp.data.model.request.AddPremiseRequest
import com.spm.mynanasapp.data.model.request.ChangePasswordRequest
import com.spm.mynanasapp.data.model.request.GetPostRequest
import com.spm.mynanasapp.data.model.request.GetPremiseRequest
import com.spm.mynanasapp.data.model.request.GetProductRequest
import com.spm.mynanasapp.data.model.request.GetUsersRequest
import com.spm.mynanasapp.data.model.request.LikePostRequest
import com.spm.mynanasapp.data.model.request.LoginRequest
import com.spm.mynanasapp.data.model.request.RegisterRequest
import com.spm.mynanasapp.data.model.request.UpdatePostRequest
import com.spm.mynanasapp.data.model.request.UpdatePremiseRequest
import com.spm.mynanasapp.data.model.request.ViewPostRequest
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

    @POST("change_password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Response<BaseResponse<Unit>>
    // == END: AUTHENTICATION ==

    // == START: USER ==
    @Multipart
    @POST("update_user_profile")
    suspend fun updateUserProfile(
        @Header("Authorization") token: String,
        @Part("ent_fullname") fullname: RequestBody,
        @Part("ent_username") username: RequestBody,
        @Part("ent_bio") bio: RequestBody,
        @Part image: MultipartBody.Part? // Nullable, because user might not change photo
    ): Response<BaseResponse<User>>

    @POST("get_users")
    suspend fun getAllUsers(
        @Header("Authorization") token: String,
        @Body request: GetUsersRequest
    ): Response<BaseResponse<List<User>>>

    // 2. Get Single Public Profile
    @POST("get_users")
    suspend fun getSpecificUser(
        @Header("Authorization") token: String,
        @Body request: GetUsersRequest
    ): Response<BaseResponse<User>>
    // == END: USER ==

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

    @POST("like_post")
    suspend fun likePost(@Header("Authorization") token: String, @Body request: LikePostRequest): Response<BaseResponse<Unit>>

    @POST("view_post")
    suspend fun viewPost(
        @Header("Authorization") token: String,
        @Body request: ViewPostRequest
    ): Response<BaseResponse<Unit>>
    // == END: POST ==

    // == START: PREMISE ==
    @POST("get_premises")
    suspend fun getPremises(
        @Header("Authorization") token: String,
        @Body request: GetPremiseRequest
    ): Response<BaseResponse<List<Premise>>>

    @POST("add_premise")
    suspend fun addPremise(
        @Header("Authorization") token: String,
        @Body request: AddPremiseRequest
    ): Response<BaseResponse<Premise>>

    @POST("update_premise")
    suspend fun updatePremise(
        @Header("Authorization") token: String,
        @Body request: UpdatePremiseRequest
    ): Response<BaseResponse<Premise>>
    // == END: PREMISE ==

    // == START: PRODUCT ==
    @POST("get_product_categories")
    suspend fun getProductCategories(
        @Header("Authorization") token: String
    ): Response<BaseResponse<List<ProductCategory>>>

    @POST("get_products")
    suspend fun getProducts(
        @Header("Authorization") token: String,
        @Body request: GetProductRequest
    ): Response<BaseResponse<List<Product>>>

    @Multipart
    @POST("add_product")
    suspend fun addProduct(
        @Header("Authorization") token: String,
        @Part("product_name") name: RequestBody,
        @Part("product_desc") desc: RequestBody,
        @Part("product_qty") qty: RequestBody,
        @Part("product_unit") unit: RequestBody,
        @Part("product_price") price: RequestBody,
        @Part("categoryID") categoryId: RequestBody,
        @Part("premiseID") premiseId: RequestBody,
        @Part images: List<MultipartBody.Part> // Array of images
    ): Response<BaseResponse<Product>>

    @Multipart
    @POST("update_product")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Part("productID") id: RequestBody,
        @Part("is_delete") isDelete: RequestBody,
        // Text Fields
        @Part("product_name") name: RequestBody?,
        @Part("product_desc") desc: RequestBody?,
        @Part("categoryID") categoryId: RequestBody?,
        @Part("product_qty") qty: RequestBody?,
        @Part("product_unit") unit: RequestBody?,
        @Part("product_price") price: RequestBody?,
        @Part("premiseID") premiseId: RequestBody?,
        // Images
        @Part existing_images: List<MultipartBody.Part>, // Strings of old paths
        @Part new_images: List<MultipartBody.Part>       // New Files
    ): Response<BaseResponse<Product>>
    // == END: PRODUCT ==
}
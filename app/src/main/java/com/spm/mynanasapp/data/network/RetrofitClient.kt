package com.spm.mynanasapp.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 1. DEFINE YOUR URL
//    private const val BASE_URL = "http://192.168.0.5:8000/api/"
    private const val BASE_URL = "http://192.168.0.221/api/"
    public const val SERVER_IMAGE_URL = "http://192.168.0.221/storage/"


    // 2. VARIABLE TO HOLD THE TOKEN
    // We store the token here so the Interceptor can read it
    private var authToken: String? = null

    // 3. FUNCTION TO SET THE TOKEN
    // Call this after Login, and when the App starts
    fun setToken(token: String?) {
        this.authToken = token
    }
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .addHeader("Accept", "application/json")
//                .addHeader("Content-Type", "application/json")

            // === CRITICAL STEP: Add Bearer Token if it exists ===
            if (!authToken.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .build()

    // 3. Create the Retrofit Instance
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Converts JSON to your Objects
            .client(httpClient)
            .build()
            .create(ApiService::class.java)
    }
}
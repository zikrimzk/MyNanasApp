package com.spm.mynanasapp.data.model.entity

import java.time.LocalDateTime

data class Post(
    val postID: Long,
    val post_images: String?,
    val post_caption: String?,
    val post_location: String?,
    val post_status: Int,
    val post_views_count: Int,

    var post_likes_count: Int,
    var is_liked: Boolean, // Ensure your Laravel API returns this field!

    val post_type: String,

    // Foreign Key
    val entID: Long,

    val created_at: String,
    val updated_at: String,

    val user: User,

    val post_verified_At: String?,
    val post_verification: String?
)
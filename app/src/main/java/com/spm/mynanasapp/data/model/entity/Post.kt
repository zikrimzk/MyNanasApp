package com.spm.mynanasapp.data.model.entity

data class Post(
    val postID: Long,
    val post_images: String?,
    val post_caption: String?,
    val post_location: String?,
    val post_status: String,
    val post_views_count: Int,
    val post_likes_count: Int,
    val post_type: String,

    // Foreign Key
    val entID: Long,

    val created_at: String,
    val updated_at: String
)
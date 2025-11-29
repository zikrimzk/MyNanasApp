package com.spm.mynanasapp

data class Post(
    val id: Int,
    val username: String,
    val location: String?,
    val timestamp: String,
    val caption: String,
    val images: List<Int>?,
    val views: String,
    var likes: Int,
    var isLiked: Boolean = false
)
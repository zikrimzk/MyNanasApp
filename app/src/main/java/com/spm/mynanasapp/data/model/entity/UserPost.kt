package com.spm.mynanasapp.data.model.entity

data class UserPost (
    val userPost: Long,
    val is_liked: Boolean,
    val userpost_status: Int,

    // Foreign Key
    val entID: Long,
    val postID: Long,

    val created_at: String?,
    val updated_at: String?,

    val user: User?,
    val post: Post?
)
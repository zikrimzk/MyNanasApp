package com.spm.mynanasapp.data.model.request

data class GetPostRequest(
    val post_type: String,
    val specific_user: Boolean?,
    val entID: Long? = null
)

data class UpdatePostRequest(
    val postID: Long,
    val post_caption: String?,
    val post_location: String?,
    val is_delete: Boolean
)

data class LikePostRequest(
    val postID: Long,
    val is_liked: Boolean
)

data class ViewPostRequest(
    val postID: Long
)
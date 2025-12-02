package com.spm.mynanasapp.data.model.request

data class GetPostRequest(
    val post_type: String,
    val specific_user: Boolean?
)

data class UpdatePostRequest(
    val postID: Long,
    val post_caption: String?,
    val post_location: String?,
    val is_delete: Boolean
)
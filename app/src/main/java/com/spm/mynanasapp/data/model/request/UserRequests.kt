package com.spm.mynanasapp.data.model.request

data class GetUsersRequest(
    val specific_user: Boolean,
    val entID: Long? = null
)
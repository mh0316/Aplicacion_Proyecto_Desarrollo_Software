package com.example.apptransito

data class UserResponse(
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    val email: String? = null
)
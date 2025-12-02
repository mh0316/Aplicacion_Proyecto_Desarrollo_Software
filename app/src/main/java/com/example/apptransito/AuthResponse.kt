package com.example.apptransito

data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val email: String? = null,
    val nombre: String? = null,
    val message: String? = null
)
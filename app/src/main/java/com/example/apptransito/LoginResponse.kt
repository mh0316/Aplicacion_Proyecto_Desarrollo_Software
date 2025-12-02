package com.example.apptransito

data class LoginResponse(
    val success: Boolean,
    val token: String,
    val email: String,
    val nombre: String? = null,
    val apellido: String? = null,
    val message: String? = null
)
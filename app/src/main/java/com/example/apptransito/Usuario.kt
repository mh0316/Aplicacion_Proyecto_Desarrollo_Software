package com.example.apptransito

data class Usuario(
    val id: String? = null,
    val rut: String,
    val password: String,
    val email: String,
    val nombre: String,
    val apellido: String
)
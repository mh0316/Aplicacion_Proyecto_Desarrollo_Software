package com.example.apptransito

data class DenunciaResponse(
    val success: Boolean,
    val message: String,
    val idDenuncia: String? = null
)
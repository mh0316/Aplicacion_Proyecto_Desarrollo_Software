package com.example.apptransito

data class DenunciaResponse(
    val success: Boolean,
    val message: String,
    val denuncia: DenunciaData? = null
)

data class DenunciaData(
    val id: Long,
    val estado: String?,
    val descripcion: String?
)
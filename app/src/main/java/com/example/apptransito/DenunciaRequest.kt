package com.example.apptransito

import com.google.gson.annotations.SerializedName

data class DenunciaRequest(
    val email: String,
    val categoriaId: Int,
    val descripcion: String,
    val latitud: Double,
    val longitud: Double,
    val patente: String?,
    val direccion: String,
    val sector: String = "Centro",
    val comuna: String = "Temuco"
)
package com.example.apptransito

import com.google.gson.annotations.SerializedName

data class DenunciaRequest(
    val email: String,
    val categoriaId: Int,
    val descripcion: String,
    val evidenciaUri: String?,
    val latitud: Double,
    val longitud: Double,
    val patente: String?,
    val tieneEvidencia: Boolean,
    val tipoEvidencia: String?,
    val direccion: String,  // Cambiado de "ubicacion" a "direccion"
    val sector: String = "Centro",  // Nuevo campo
    val comuna: String = "Temuco"   // Nuevo campo
)
package com.example.apptransito

data class Denuncia(
    val categoria: String,
    val descripcion: String,
    val patente: String,
    val usuarioEmail: String,
    val ubicacion: String,
    val latitud: Double,
    val longitud: Double,
    val tieneEvidencia: Boolean,
    val tipoEvidencia: String? = null,
    val evidenciaUri: String? = null,
    val fecha: String? = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
)
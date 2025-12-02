package com.example.apptransito

import com.google.gson.annotations.SerializedName

data class RecuperacionContraseniaRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("nuevaContrasenia")
    val nuevaContrasenia: String
)
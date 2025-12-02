package com.example.apptransito

/**
 * Clase sellada para manejar resultados de operaciones asíncronas (como el Login).
 * T es el tipo de dato que esperamos recibir si todo sale bien.
 */
sealed class Result<out T : Any> {
    // Estado de éxito: contiene los datos (data)
    data class Success<out T : Any>(val data: T) : Result<T>()

    // Estado de error: contiene la excepción (exception)
    data class Error(val exception: Exception) : Result<Nothing>()
}
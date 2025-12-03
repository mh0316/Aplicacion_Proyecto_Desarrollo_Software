package com.example.apptransito

class DenunciaRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun enviarDenuncia(denuncia: Denuncia): Result<DenunciaResponse> {
        return try {
            // CORREGIDO: Convertir Denuncia a DenunciaRequest con la nueva estructura
            val denunciaRequest = DenunciaRequest(
                email = denuncia.usuarioEmail,
                categoriaId = obtenerCategoriaId(denuncia.categoria), // Convertir nombre a ID
                descripcion = denuncia.descripcion,
                // evidenciaUri = denuncia.evidenciaUri,            // No se envía en la petición, se envía en otra petición, ya que es Multipart
                latitud = denuncia.latitud,
                longitud = denuncia.longitud,
                patente = denuncia.patente,
                // tieneEvidencia = denuncia.tieneEvidencia,        // No se envía en la petición, se envía en otra petición, ya que es Multipart
                // tipoEvidencia = denuncia.tipoEvidencia,        // No se envía en la petición, se envía en otra petición, ya que es Multipart 
                direccion = denuncia.ubicacion, // Mapear ubicacion a direccion
                sector = "Centro", // Valor por defecto o calcularlo
                comuna = "Temuco"  // Valor por defecto o calcularlo
            )

            val response = apiService.enviarDenuncia(denunciaRequest)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: response.message()
                Result.Error(Exception("Error al enviar denuncia: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Función auxiliar para convertir nombre de categoría a ID
    private fun obtenerCategoriaId(nombreCategoria: String): Int {
        val categoriasMap = mapOf(
            "Exceso de velocidad" to 1,
            "Botar basura a la calle" to 2,
            "Estacionamiento en lugar prohibido" to 3,
            "Vehículo mal estacionado" to 4,
            "No respetar señal de pare" to 5,
            "No respetar luz roja" to 6,
            "Conducir en contravía" to 7,
            "Obstrucción de vía pública" to 8,
            "Vehículo abandonado" to 9,
            "Otro" to 10
        )
        return categoriasMap[nombreCategoria] ?: 10 // Default a "Otro" si no se encuentra
    }
}
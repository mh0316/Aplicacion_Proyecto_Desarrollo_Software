package com.example.apptransito

// Crea un nuevo archivo CategoriasDenuncia.kt
object CategoriasDenuncia {
    val categorias = listOf(
        Categoria(1, "Exceso de velocidad"),
        Categoria(2, "Botar basura a la calle"),
        Categoria(3, "Estacionamiento en lugar prohibido"),
        Categoria(4, "Vehículo mal estacionado"),
        Categoria(5, "No respetar señal de pare"),
        Categoria(6, "No respetar luz roja"),
        Categoria(7, "Conducir en contravía"),
        Categoria(8, "Obstrucción de vía pública"),
        Categoria(9, "Vehículo abandonado"),
        Categoria(10, "Otro")
    )

    fun obtenerIdPorNombre(nombre: String): Int {
        return categorias.find { it.nombre == nombre }?.id ?: 10
    }

    fun obtenerNombrePorId(id: Int): String {
        return categorias.find { it.id == id }?.nombre ?: "Otro"
    }
}

data class Categoria(val id: Int, val nombre: String)
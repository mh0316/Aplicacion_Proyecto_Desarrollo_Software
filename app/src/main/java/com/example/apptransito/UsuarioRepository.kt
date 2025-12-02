package com.example.apptransito

class UsuarioRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun registerUser(user: Usuario): Result<UserResponse> {
        return try {
            val response = apiService.registrarUsuario(user)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Error en el registro: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Credenciales incorrectas"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun updatePassword(email: String, newPassword: String): Result<UserResponse> {
        return try {
            val response = apiService.updatePassword(RecuperacionContraseniaRequest(email, newPassword))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Error al actualizar contraseña"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun checkEmailExists(email: String): Result<AuthResponse> {
        return try {
            val response = apiService.checkEmail(mapOf("email" to email))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(Exception("Email no encontrado"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
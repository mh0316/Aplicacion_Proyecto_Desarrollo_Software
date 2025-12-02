import com.example.apptransito.AuthResponse
import com.example.apptransito.DenunciaRequest
import com.example.apptransito.DenunciaResponse
import com.example.apptransito.LoginRequest
import com.example.apptransito.LoginResponse
import com.example.apptransito.RecuperacionContraseniaRequest
import com.example.apptransito.UserResponse
import com.example.apptransito.Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {

    // Registro
    @POST("usuarios/registro")
    suspend fun registrarUsuario(@Body user: Usuario): Response<UserResponse>

    // NUEVO: Endpoint para login con email
    @POST("usuarios/login") // Ajusta el endpoint según tu backend
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>
    @POST("auth/check-email") // Ajusta el endpoint según tu backend
    suspend fun checkEmail(@Body emailMap: Map<String, String>): Response<AuthResponse>

    //Sección de Denuncias

    @POST("usuarios/actualizar-contrasenia")
    suspend fun updatePassword(@Body passwordUpdate: RecuperacionContraseniaRequest): Response<UserResponse>

    @POST("usuarios/check-rut")
    suspend fun checkRut(@Body rutRequest: Map<String, String>): Response<UserResponse>

    @POST("denuncias")
    suspend fun enviarDenuncia(@Body denuncia: DenunciaRequest): Response<DenunciaResponse>
}
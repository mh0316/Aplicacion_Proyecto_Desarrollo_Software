import android.annotation.SuppressLint
import com.example.apptransito.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {
    // CORREGIDO: Usa BuildConfig y formato correcto
    private val BASE_URL = BuildConfig.BASE_URL

    @SuppressLint("StaticFieldLeak")
    private lateinit var tokenManager: TokenManager

    fun initialize(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Agregar token si está disponible
        tokenManager.getToken()?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // Agregar headers comunes
        requestBuilder.header("Content-Type", "application/json")
        requestBuilder.header("Accept", "application/json")

        val newRequest = requestBuilder.build()
        chain.proceed(newRequest)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        // DEBUG: Verificar la URL antes de construir Retrofit
        println("DEBUG: Construyendo Retrofit con BASE_URL: $BASE_URL")

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
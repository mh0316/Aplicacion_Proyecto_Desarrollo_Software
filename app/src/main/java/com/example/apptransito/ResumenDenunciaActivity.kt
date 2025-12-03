package com.example.apptransito

import TokenManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ResumenDenunciaActivity : ComponentActivity() {

    // Variables para datos del usuario
    private lateinit var tokenManager: TokenManager
    private lateinit var userEmail: String
    private lateinit var userName: String

    // Variables para datos de la denuncia - MODIFICADAS
    private var categoriaId: Int = 0
    private lateinit var categoriaNombre: String
    private lateinit var descripcion: String
    private lateinit var patente: String
    private lateinit var ubicacion: String
    private var latitud: Double = 0.0
    private var longitud: Double = 0.0
    private var tieneEvidencia: Boolean = false
    private lateinit var tipoEvidencia: String
    private var evidenciaUri: String? = null

    // Mapeo de IDs de categoría a nombres (para mostrar en UI)
    private val categoriasMap = mapOf(
        1 to "Exceso de velocidad",
        2 to "Botar basura a la calle",
        3 to "Estacionamiento en lugar prohibido",
        4 to "Vehículo mal estacionado",
        5 to "No respetar señal de pare",
        6 to "No respetar luz roja",
        7 to "Conducir en contravía",
        8 to "Obstrucción de vía pública",
        9 to "Vehículo abandonado",
        10 to "Otro"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Verificar autenticación
            tokenManager = AppTransitoApplication.tokenManager
            if (!tokenManager.isLoggedIn()) {
                redirigirALogin()
                return
            }

            // Obtener datos del usuario
            userEmail = tokenManager.getUserEmail() ?: ""
            userName = tokenManager.getUserName() ?: "Usuario"

            setContentView(R.layout.resumen_denuncia)
            Toast.makeText(this, "ResumenDenunciaActivity iniciada", Toast.LENGTH_SHORT).show()

            // Obtener datos del intent PRIMERO
            obtenerDatosIntent()

            inicializarVistas()
            mostrarDatosDenuncia()
            configurarEventos()

        } catch (e: Exception) {
            Toast.makeText(this, "Error en ResumenDenunciaActivity: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    // Método para redirigir al login
    private fun redirigirALogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun obtenerDatosIntent() {
        // Obtener ID de categoría en lugar del nombre
        categoriaId = intent.getIntExtra("CATEGORIA_ID", 1)
        categoriaNombre = intent.getStringExtra("CATEGORIA_NOMBRE") ?: categoriasMap[categoriaId] ?: "Otro"

        descripcion = intent.getStringExtra("DESCRIPCION") ?: "No especificada"
        patente = intent.getStringExtra("PATENTE") ?: "No especificada"
        ubicacion = intent.getStringExtra("UBICACION") ?: "No seleccionada"
        latitud = intent.getDoubleExtra("LATITUD", 0.0)
        longitud = intent.getDoubleExtra("LONGITUD", 0.0)
        tieneEvidencia = intent.getBooleanExtra("TIENE_EVIDENCIA", false)
        tipoEvidencia = intent.getStringExtra("TIPO_EVIDENCIA") ?: ""
        evidenciaUri = intent.getStringExtra("EVIDENCIA_URI")

        // Obtener datos del usuario del intent (por si vienen de actividad anterior)
        val userEmailFromIntent = intent.getStringExtra("USER_EMAIL")
        val userNameFromIntent = intent.getStringExtra("USER_NAME")

        if (!userEmailFromIntent.isNullOrEmpty()) {
            userEmail = userEmailFromIntent
        }
        if (!userNameFromIntent.isNullOrEmpty()) {
            userName = userNameFromIntent
        }
    }

    private fun inicializarVistas() {
        // Inicializar vistas aquí si es necesario
    }

    private fun mostrarDatosDenuncia() {
        // Mostrar datos en los TextViews usando categoriaNombre para la UI
        findViewById<TextView>(R.id.tv_categoria_resumen)?.text = categoriaNombre
        findViewById<TextView>(R.id.tv_descripcion_resumen)?.text = descripcion
        findViewById<TextView>(R.id.tv_patente_resumen)?.text = patente
        findViewById<TextView>(R.id.tv_ubicacion_resumen)?.text = ubicacion

        // Mostrar coordenadas
        val coordenadasText = if (latitud != 0.0 && longitud != 0.0) {
            String.format("Lat: %.6f, Lng: %.6f", latitud, longitud)
        } else {
            "No seleccionadas"
        }
        findViewById<TextView>(R.id.tv_coordenadas_resumen)?.text = coordenadasText

        val evidenciaText = if (tieneEvidencia) {
            "Sí ($tipoEvidencia)"
        } else {
            "No"
        }
        findViewById<TextView>(R.id.tv_evidencia_resumen)?.text = evidenciaText

        // Mostrar datos del usuario en logs para debug
        println("DEBUG Resumen - Usuario: $userName, Email: $userEmail")
        println("DEBUG Resumen - Categoría ID: $categoriaId, Nombre: $categoriaNombre")

        // Mostrar en Toast para depuración
        Toast.makeText(this, "Categoría ID: $categoriaId - $categoriaNombre", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Patente: $patente", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Usuario: $userName", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Coordenadas: $coordenadasText", Toast.LENGTH_LONG).show()
    }

    private fun configurarEventos() {
        // Botones de navegación
        findViewById<Button>(R.id.btn_inicio)?.setOnClickListener {
            irAInicio()
        }

        findViewById<Button>(R.id.btn_nueva_denuncia)?.setOnClickListener {
            reiniciarDenuncia()
        }

        findViewById<Button>(R.id.btn_ayuda)?.setOnClickListener {
            mostrarAyuda()
        }

        // Botón Editar
        findViewById<Button>(R.id.btn_editar)?.setOnClickListener {
            finish() // Volver a NuevaDenuncia2Activity
        }

        // Botón Enviar Denuncia
        findViewById<Button>(R.id.btn_enviar_denuncia)?.setOnClickListener {
            enviarDenunciaFinal()
        }

        // Botón de cerrar sesión
        findViewById<Button>(R.id.btn_cerrar_sesion)?.setOnClickListener {
            cerrarSesion()
        }
    }

    // Método para cerrar sesión
    private fun cerrarSesion() {
        tokenManager.clearToken()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

    private fun enviarDenunciaFinal() {
        // Verificar que tenemos todos los datos necesarios
        if (categoriaId == 0) {
            Toast.makeText(this, "Error: Categoría no válida", Toast.LENGTH_LONG).show()
            return
        }

        if (latitud == 0.0 || longitud == 0.0) {
            Toast.makeText(this, "Error: Ubicación no válida", Toast.LENGTH_LONG).show()
            return
        }

        // Enviar denuncia usando el servicio API
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Mostrar progreso
                Toast.makeText(this@ResumenDenunciaActivity, "Enviando denuncia...", Toast.LENGTH_SHORT).show()

                // Crear objeto DenunciaRequest con todos los campos
                // NOTA: Los campos de evidencia se manejan por separado
                val denunciaRequest = DenunciaRequest(
                    email = userEmail,
                    categoriaId = categoriaId,
                    descripcion = descripcion,
                    latitud = latitud,
                    longitud = longitud,
                    patente = if (patente != "No especificada") patente else null,
                    direccion = ubicacion,
                    sector = "Centro",
                    comuna = "Temuco"
                )

                println("DEBUG: Enviando denuncia con request: $denunciaRequest")

                // 1. Enviar datos de la denuncia
                val response = RetrofitClient.apiService.enviarDenuncia(denunciaRequest)

                if (response.isSuccessful && response.body()?.success == true) {
                    val denunciaId = response.body()?.denuncia?.id
                    println("DEBUG: Denuncia creada con ID: $denunciaId")

                    // 2. Subir evidencia si existe
                    if (tieneEvidencia && denunciaId != null && evidenciaUri != null) {
                        try {
                            Toast.makeText(this@ResumenDenunciaActivity, "Subiendo evidencia...", Toast.LENGTH_SHORT).show()
                            val uri = Uri.parse(evidenciaUri)
                            val file = getFileFromUri(uri)
                            
                            if (file != null) {
                                val mediaType = if (tipoEvidencia == "video") "video/*".toMediaTypeOrNull() else "image/*".toMediaTypeOrNull()
                                val requestFile = file.asRequestBody(mediaType)
                                val body = MultipartBody.Part.createFormData("archivo", file.name, requestFile)
                                
                                val uploadResponse = RetrofitClient.apiService.subirEvidencia(denunciaId, body)
                                if (!uploadResponse.isSuccessful) {
                                    println("Error subiendo evidencia: ${uploadResponse.errorBody()?.string()}")
                                    Toast.makeText(this@ResumenDenunciaActivity, "Denuncia creada pero error al subir evidencia", Toast.LENGTH_LONG).show()
                                } else {
                                    println("DEBUG: Evidencia subida exitosamente")
                                }
                            } else {
                                println("ERROR: No se pudo crear archivo temporal desde URI")
                            }
                        } catch (e: Exception) {
                            println("Error preparando evidencia: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    val mensaje = """
                    Denuncia enviada correctamente:
                    - Categoría: $categoriaNombre
                    - Patente: $patente
                    - Usuario: $userName
                    - Ubicación: $ubicacion
                    - Coordenadas: ${String.format("%.6f, %.6f", latitud, longitud)}
                    - Evidencia: ${if (tieneEvidencia) "Sí ($tipoEvidencia)" else "No"}
                """.trimIndent()

                    Toast.makeText(this@ResumenDenunciaActivity, mensaje, Toast.LENGTH_LONG).show()
                    Toast.makeText(this@ResumenDenunciaActivity, "✅ Denuncia registrada en el sistema", Toast.LENGTH_LONG).show()

                    // Volver al inicio
                    val intent = Intent(this@ResumenDenunciaActivity, InicioActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Toast.makeText(
                        this@ResumenDenunciaActivity,
                        "❌ Error al enviar denuncia: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                    println("ERROR: Error response: $errorBody")
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ResumenDenunciaActivity,
                    "❌ Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val contentResolver = applicationContext.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val extension = if (tipoEvidencia == "video") ".mp4" else ".jpg"
            val tempFile = File.createTempFile("upload", extension, cacheDir)
            tempFile.deleteOnExit()
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun irAInicio() {
        val intent = Intent(this, InicioActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun reiniciarDenuncia() {
        val intent = Intent(this, NuevaDenunciaActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun mostrarAyuda() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://ejemplo.com/ayuda-denuncias-transito")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No se puede abrir el enlace de ayuda", Toast.LENGTH_SHORT).show()
        }
    }
}
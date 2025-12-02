package com.example.apptransito

import TokenManager
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class NuevaDenuncia2Activity : ComponentActivity() {

    private var evidenciaUri: Uri? = null
    private var tipoEvidencia: String = ""

    // Variables para ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private var ubicacionSeleccionada: GeoPoint? = null
    private var direccionCompleta: String = ""

    // Views
    private lateinit var tvCoordenadas: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvUbicacionNoSeleccionada: TextView
    private lateinit var layoutInfoUbicacion: LinearLayout

    // Variables para datos de la primera pantalla - MODIFICADAS
    private var categoriaId: Int = 0 // Cambiado de String a Int
    private lateinit var categoriaNombre: String // Para mostrar en UI
    private lateinit var descripcion: String
    private lateinit var patente: String

    // CORREGIDO: TokenManager y datos del usuario - INICIALIZADOS CON VALORES POR DEFECTO
    private lateinit var tokenManager: TokenManager
    private var userEmail: String = ""
    private var userName: String = "Usuario" // ✅ INICIALIZADO CON VALOR POR DEFECTO

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

    // Permisos de ubicación
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                mostrarDialogMapaGrande()
            }
            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                mostrarDialogMapaGrande()
            }
            else -> {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // ✅ INICIALIZAR PRIMERO: TokenManager y datos del usuario
            tokenManager = AppTransitoApplication.tokenManager
            if (!tokenManager.isLoggedIn()) {
                redirigirALogin()
                return
            }

            // ✅ INICIALIZAR: Obtener información del usuario logueado
            userEmail = tokenManager.getUserEmail() ?: ""
            userName = tokenManager.getUserName() ?: "Usuario"

            // Configuración de OSMDroid
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

            setContentView(R.layout.nueva_denuncia2)

            // ✅ AHORA podemos usar userName porque está inicializado
            findViewById<TextView>(R.id.tv_user_info)?.text = "Usuario: $userName"

            // Inicializar FusedLocationProviderClient
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Inicializar vistas de ubicación
            inicializarVistasUbicacion()

            // Configurar mapa pequeño
            configurarMapaPequeño()

            // Recibir datos de la primera pantalla - MODIFICADO
            categoriaId = intent.getIntExtra("CATEGORIA_ID", 1)
            categoriaNombre = categoriasMap[categoriaId] ?: "Otro"
            descripcion = intent.getStringExtra("DESCRIPCION") ?: ""
            patente = intent.getStringExtra("PATENTE") ?: ""

            // Recibir USER_EMAIL si viene de NuevaDenunciaActivity
            val userEmailFromIntent = intent.getStringExtra("USER_EMAIL")
            if (!userEmailFromIntent.isNullOrEmpty()) {
                userEmail = userEmailFromIntent
            }

            // Recibir CATEGORIA_NOMBRE si viene (opcional)
            val categoriaNombreFromIntent = intent.getStringExtra("CATEGORIA_NOMBRE")
            if (!categoriaNombreFromIntent.isNullOrEmpty()) {
                categoriaNombre = categoriaNombreFromIntent
            }

            inicializarVistas(categoriaNombre, descripcion)
            configurarEventos()

        } catch (e: Exception) {
            println("❌ ERROR en onCreate de NuevaDenuncia2Activity: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error al inicializar la actividad", Toast.LENGTH_LONG).show()
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

    private fun inicializarVistasUbicacion() {
        tvCoordenadas = findViewById(R.id.tv_coordenadas)
        tvDireccion = findViewById(R.id.tv_direccion)
        tvUbicacionNoSeleccionada = findViewById(R.id.tv_ubicacion_no_seleccionada)
        layoutInfoUbicacion = findViewById(R.id.layout_info_ubicacion)
        mapView = findViewById(R.id.mapView)
    }

    private fun inicializarVistas(categoriaNombre: String, descripcion: String) {
        // Mostrar información de depuración
        Toast.makeText(this, "Categoría ID: $categoriaId - $categoriaNombre", Toast.LENGTH_SHORT).show()
        Toast.makeText(this, "Patente: $patente", Toast.LENGTH_SHORT).show()

        // ✅ AHORA userName está inicializado y se puede usar
        println("DEBUG: Usuario logueado - Email: $userEmail, Nombre: $userName")
        println("DEBUG: Categoría ID: $categoriaId, Nombre: $categoriaNombre")
    }

    private fun configurarEventos() {
        // Botones de navegación
        findViewById<Button>(R.id.btn_volver).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btn_inicio).setOnClickListener {
            irAInicio()
        }

        findViewById<Button>(R.id.btn_nueva_denuncia).setOnClickListener {
            reiniciarDenuncia()
        }

        findViewById<Button>(R.id.btn_ayuda).setOnClickListener {
            mostrarAyuda()
        }

        // Botones de evidencia
        findViewById<Button>(R.id.btn_foto).setOnClickListener {
            seleccionarDeGaleria("image/*", "Selecciona una foto")
        }

        findViewById<Button>(R.id.btn_video).setOnClickListener {
            seleccionarDeGaleria("video/*", "Selecciona un video")
        }

        findViewById<Button>(R.id.btn_ver_seleccion).setOnClickListener {
            verEvidenciaSeleccionada()
        }

        // Botones de acción
        findViewById<Button>(R.id.btn_cancelar).setOnClickListener {
            cancelarDenuncia()
        }

        findViewById<Button>(R.id.btn_siguiente_revisar).setOnClickListener {
            irAResumenDenuncia()
        }

        // Botón de cerrar sesión
        findViewById<Button>(R.id.btn_cerrar_sesion).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        tokenManager.clearToken()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

    private fun configurarMapaPequeño() {
        try {
            // Configuración básica del mapa
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            mapView.setMultiTouchControls(true)

            // Centrar en Temuco, Chile
            val temuco = GeoPoint(-38.7359, -72.5907)
            mapView.controller.setCenter(temuco)
            mapView.controller.setZoom(12.0)

            // Overlay para manejar eventos de clic en el mapa
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    solicitarPermisosUbicacion()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    return false
                }
            }
            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
            mapView.overlays.add(mapEventsOverlay)

            // Agregar un marcador inicial con texto instructivo
            val marker = Marker(mapView)
            marker.position = temuco
            marker.title = "Toca el mapa para seleccionar ubicación"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al configurar el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun solicitarPermisosUbicacion() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                mostrarDialogMapaGrande()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarDialogMapaGrande() {
        try {
            println("🗺️ DEBUG: Mostrando diálogo de mapa")

            val dialogView = layoutInflater.inflate(R.layout.mapa, null)

            // Configurar MapView
            val mapViewDialog = MapView(this)
            mapViewDialog.setTileSource(TileSourceFactory.MAPNIK)
            mapViewDialog.setMultiTouchControls(true)

            val container = dialogView.findViewById<LinearLayout>(R.id.map_container)
            container?.addView(mapViewDialog, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

            // Crear diálogo
            val dialog = android.app.AlertDialog.Builder(this).create()
            dialog.setView(dialogView)
            dialog.setCancelable(true)

            // Obtener referencias de las vistas del diálogo
            val tvCoordenadasDialog = dialogView.findViewById<TextView>(R.id.tv_coordenadas_dialog)
            val tvDireccionDialog = dialogView.findViewById<TextView>(R.id.tv_direccion_dialog)
            val btnConfirmar = dialogView.findViewById<Button>(R.id.btn_confirmar_ubicacion)

            println("🗺️ DEBUG: Botón confirmar encontrado: ${btnConfirmar != null}")

            // Configurar el botón de confirmar - VERSIÓN MEJORADA
            btnConfirmar?.setOnClickListener {
                println("🗺️ DEBUG: Botón confirmar presionado")
                println("🗺️ DEBUG: ubicacionSeleccionada = $ubicacionSeleccionada")

                if (ubicacionSeleccionada != null) {
                    println("🗺️ DEBUG: Confirmando ubicación: $ubicacionSeleccionada")
                    actualizarMapaPequeño(ubicacionSeleccionada!!, direccionCompleta)
                    dialog.dismiss()
                    println("🗺️ DEBUG: Diálogo cerrado")
                } else {
                    println("❌ DEBUG: No hay ubicación seleccionada")
                    Toast.makeText(this, "Por favor selecciona una ubicación en el mapa primero", Toast.LENGTH_LONG).show()
                }
            }

            // Inicializar el botón como deshabilitado
            btnConfirmar?.isEnabled = false
            btnConfirmar?.alpha = 0.5f

            // Configurar el mapa pasando las referencias
            configurarMapaDialog(mapViewDialog, dialogView, tvCoordenadasDialog, tvDireccionDialog, btnConfirmar)

            dialog.show()

            // Ajustar tamaño del diálogo
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.95).toInt()
            val height = (displayMetrics.heightPixels * 0.80).toInt()
            dialog.window?.setLayout(width, height)

            println("🗺️ DEBUG: Diálogo mostrado correctamente")

        } catch (e: Exception) {
            println("❌ DEBUG: Error al mostrar mapa: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error al mostrar el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun configurarMapaDialog(
        mapViewDialog: MapView,
        dialogView: android.view.View,
        tvCoordenadasDialog: TextView?,
        tvDireccionDialog: TextView?,
        btnConfirmar: Button?
    ) {
        try {
            println("🗺️ DEBUG: Configurando mapa del diálogo")

            // Capa de ubicación actual
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapViewDialog)
            locationOverlay.enableMyLocation()
            mapViewDialog.overlays.add(locationOverlay)

            // Centrar en Temuco por defecto
            val temuco = GeoPoint(-38.7359, -72.5907)
            mapViewDialog.controller.setCenter(temuco)
            mapViewDialog.controller.setZoom(15.0)

            // Overlay para clics en el mapa
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    println("🗺️ DEBUG: Mapa clickeado en: $p")
                    seleccionarUbicacionEnDialog(p, mapViewDialog, tvCoordenadasDialog, tvDireccionDialog, btnConfirmar)
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            }

            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
            mapViewDialog.overlays.add(mapEventsOverlay)

            println("🗺️ DEBUG: Mapa configurado correctamente")

        } catch (e: Exception) {
            println("❌ DEBUG: Error configurando mapa: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun seleccionarUbicacionEnDialog(
        p: GeoPoint,
        mapViewDialog: MapView,
        tvCoordenadasDialog: TextView?,
        tvDireccionDialog: TextView?,
        btnConfirmar: Button?
    ) {
        try {
            println("🗺️ DEBUG: Seleccionando ubicación en diálogo: $p")

            // Limpiar marcadores anteriores
            mapViewDialog.overlays.removeIf { it is Marker }

            // Agregar nuevo marcador
            val marker = Marker(mapViewDialog)
            marker.position = p
            marker.title = "Ubicación seleccionada"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapViewDialog.overlays.add(marker)
            mapViewDialog.invalidate()

            // Obtener dirección MEJORADA
            obtenerDireccionMejorada(p) { direccion ->
                runOnUiThread {
                    println("🗺️ DEBUG: Dirección obtenida: $direccion")

                    // Actualizar textos
                    tvCoordenadasDialog?.text = "Coordenadas: ${"%.6f".format(p.latitude)}, ${"%.6f".format(p.longitude)}"
                    tvDireccionDialog?.text = "Dirección: $direccion"

                    // Guardar ubicación seleccionada
                    ubicacionSeleccionada = p
                    direccionCompleta = direccion

                    // Habilitar botón de confirmación
                    btnConfirmar?.isEnabled = true
                    btnConfirmar?.alpha = 1.0f

                    println("🗺️ DEBUG: Botón habilitado y lista actualizada")
                    Toast.makeText(this@NuevaDenuncia2Activity, "Ubicación seleccionada. Presiona Confirmar.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            println("❌ DEBUG: Error seleccionando ubicación: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun obtenerDireccionMejorada(geoPoint: GeoPoint, callback: (String) -> Unit) {
        // Primero intentar con la API más precisa
        obtenerDireccionPrecisa(geoPoint) { direccionApi ->
            if (direccionApi.contains("Ubicación seleccionada") ||
                direccionApi.contains("Error") ||
                direccionApi.length < 10) {
                // Si la API falla, usar el Geocoder de Android
                obtenerDireccionDesdeCoordenadas(geoPoint, callback)
            } else {
                callback(direccionApi)
            }
        }
    }

    private fun obtenerDireccionPrecisa(geoPoint: GeoPoint, callback: (String) -> Unit) {
        // Ejecutar en un hilo de fondo
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Usar Nominatim (OpenStreetMap) - más preciso y gratuito
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=${geoPoint.latitude}&lon=${geoPoint.longitude}&zoom=18&addressdetails=1"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "AppTransito/1.0")

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    val address = jsonObject.getJSONObject("address")
                    val direccion = StringBuilder()

                    // Construir dirección de manera específica
                    if (address.has("road")) {
                        direccion.append(address.getString("road")) // Calle

                        if (address.has("house_number")) {
                            direccion.append(" #").append(address.getString("house_number")) // Número
                        }

                        if (address.has("suburb")) {
                            direccion.append(", ").append(address.getString("suburb")) // Barrio
                        }

                        if (address.has("city") || address.has("town")) {
                            val ciudad = if (address.has("city")) address.getString("city") else address.getString("town")
                            direccion.append(", ").append(ciudad) // Ciudad
                        }
                    } else {
                        // Si no hay calle específica, usar display_name
                        direccion.append(jsonObject.getString("display_name"))
                    }

                    withContext(Dispatchers.Main) {
                        callback(direccion.toString())
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback("Error al obtener dirección de la API")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback("Error de conexión API")
                }
            }
        }
    }

    private fun obtenerDireccionDesdeCoordenadas(geoPoint: GeoPoint, callback: (String) -> Unit) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())

            // Intentar con diferentes niveles de precisión
            val addresses = geocoder.getFromLocation(
                geoPoint.latitude,
                geoPoint.longitude,
                3  // Pedir hasta 3 resultados
            )

            if (addresses != null && addresses.isNotEmpty()) {
                // Priorizar el resultado con la dirección más específica
                val bestAddress = addresses.firstOrNull { address ->
                    address.thoroughfare != null && // Calle principal
                            address.subThoroughfare != null // Número
                } ?: addresses.first() // Si no hay uno específico, usar el primero

                val direccion = StringBuilder()

                // Construir dirección de manera más específica
                if (bestAddress.thoroughfare != null) {
                    direccion.append(bestAddress.thoroughfare) // Calle principal

                    if (bestAddress.subThoroughfare != null) {
                        direccion.append(" #").append(bestAddress.subThoroughfare) // Número
                    }

                    // Agregar información adicional si está disponible
                    if (bestAddress.subLocality != null) {
                        direccion.append(", ").append(bestAddress.subLocality) // Barrio/Localidad
                    }

                    if (bestAddress.locality != null) {
                        direccion.append(", ").append(bestAddress.locality) // Ciudad
                    }
                } else {
                    // Si no hay calle específica, usar el formato completo
                    for (i in 0 until minOf(bestAddress.maxAddressLineIndex, 2)) {
                        if (i > 0) direccion.append(", ")
                        direccion.append(bestAddress.getAddressLine(i))
                    }
                }

                val direccionFinal = if (direccion.isNotEmpty()) {
                    direccion.toString()
                } else {
                    "Ubicación seleccionada (${"%.6f".format(geoPoint.latitude)}, ${"%.6f".format(geoPoint.longitude)})"
                }

                callback(direccionFinal)
            } else {
                callback("Ubicación seleccionada (${"%.6f".format(geoPoint.latitude)}, ${"%.6f".format(geoPoint.longitude)})")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // En caso de error, al menos mostrar las coordenadas
            callback("Ubicación seleccionada (${"%.6f".format(geoPoint.latitude)}, ${"%.6f".format(geoPoint.longitude)})")
        }
    }

    private fun actualizarMapaPequeño(geoPoint: GeoPoint, direccion: String) {
        try {
            // Actualizar mapa pequeño con marcador
            mapView.overlays.clear()
            val marker = Marker(mapView)
            marker.position = geoPoint
            marker.title = "Ubicación seleccionada"
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(marker)

            mapView.controller.setCenter(geoPoint)
            mapView.controller.setZoom(15.0)
            mapView.invalidate()

            // Mostrar información de ubicación
            tvCoordenadas.text = "Coordenadas: ${"%.6f".format(geoPoint.latitude)}, ${"%.6f".format(geoPoint.longitude)}"
            tvDireccion.text = "Dirección: $direccion"

            // Cambiar visibilidad
            layoutInfoUbicacion.visibility = LinearLayout.VISIBLE
            tvUbicacionNoSeleccionada.visibility = TextView.GONE

            Toast.makeText(this, "Ubicación seleccionada correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al actualizar el mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Métodos para evidencia (mantener igual)
    private fun seleccionarDeGaleria(mimeType: String, titulo: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val selectorIntent = Intent.createChooser(intent, titulo)

        try {
            if (mimeType.startsWith("image/")) {
                seleccionarFotoLauncher.launch(selectorIntent)
            } else {
                seleccionarVideoLauncher.launch(selectorIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se puede abrir la galería", Toast.LENGTH_SHORT).show()
        }
    }

    private val seleccionarFotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                evidenciaUri = uri
                tipoEvidencia = "foto"
                Toast.makeText(this, "Foto seleccionada de la galería", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.btn_ver_seleccion).text = "Ver foto seleccionada"
            }
        }
    }

    private val seleccionarVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                evidenciaUri = uri
                tipoEvidencia = "video"
                Toast.makeText(this, "Video seleccionado de la galería", Toast.LENGTH_SHORT).show()
                findViewById<Button>(R.id.btn_ver_seleccion).text = "Ver video seleccionado"
            }
        }
    }

    private fun verEvidenciaSeleccionada() {
        try {
            if (evidenciaUri != null && tipoEvidencia.isNotEmpty()) {
                val intent = Intent(this, RevisarEvidenciaActivity::class.java).apply {
                    putExtra("EVIDENCIA_URI", evidenciaUri.toString())
                    putExtra("TIPO_EVIDENCIA", tipoEvidencia)
                }

                // Verificar que la actividad existe
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "No se puede abrir el visor de evidencia: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "No hay evidencia seleccionada. Por favor, selecciona una foto o video primero.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun irAResumenDenuncia() {
        if (ubicacionSeleccionada == null) {
            Toast.makeText(this, "Por favor selecciona una ubicación en el mapa", Toast.LENGTH_LONG).show()
            return
        }

        // ✅ AHORA userName está inicializado y se puede pasar
        val intent = Intent(this, ResumenDenunciaActivity::class.java).apply {
            putExtra("CATEGORIA_ID", categoriaId) // ENVIAR ID EN LUGAR DE NOMBRE
            putExtra("CATEGORIA_NOMBRE", categoriaNombre) // Opcional: para mostrar en UI
            putExtra("DESCRIPCION", descripcion)
            putExtra("PATENTE", patente)
            putExtra("UBICACION", direccionCompleta)
            putExtra("LATITUD", ubicacionSeleccionada!!.latitude)
            putExtra("LONGITUD", ubicacionSeleccionada!!.longitude)
            putExtra("TIENE_EVIDENCIA", evidenciaUri != null)
            putExtra("USER_EMAIL", userEmail)
            putExtra("USER_NAME", userName)   // ✅ AHORA SÍ ESTÁ INICIALIZADO
            if (evidenciaUri != null) {
                putExtra("TIPO_EVIDENCIA", tipoEvidencia)
                putExtra("EVIDENCIA_URI", evidenciaUri.toString())
            }
        }
        startActivity(intent)
    }

    private fun cancelarDenuncia() {
        Toast.makeText(this, "Denuncia cancelada", Toast.LENGTH_SHORT).show()
        finish()
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

    // Métodos del ciclo de vida para OSMDroid
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
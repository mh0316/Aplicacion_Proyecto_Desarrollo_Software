package com.example.apptransito

import TokenManager
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.gridlayout.widget.GridLayout

class NuevaDenunciaActivity : ComponentActivity() {

    private var categoriaSeleccionadaId: Int = 0 // Cambiado de String a Int
    private var categoriaSeleccionadaNombre: String = "" // Mantener nombre para UI
    private val botonesCategoria = mutableListOf<Button>()
    private lateinit var etPatente: EditText
    private lateinit var tvErrorPatente: TextView
    private lateinit var tokenManager: TokenManager

    // Mapeo de categorías a IDs (debe coincidir exactamente con el backend)
    private val categoriasMap = mapOf(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.nueva_denuncia)

        // Verificar autenticación
        tokenManager = AppTransitoApplication.tokenManager
        if (!tokenManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        inicializarVistas()
        configurarEventos()
        configurarComportamientoTeclado()
    }

    private fun inicializarVistas() {
        etPatente = findViewById(R.id.et_patente)
        tvErrorPatente = findViewById(R.id.tv_error_patente)
    }

    private fun configurarEventos() {
        // Configurar botones de navegación
        findViewById<Button>(R.id.btn_inicio).setOnClickListener {
            val intent = Intent(this, InicioActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_nueva_denuncia).setOnClickListener {
            reiniciarFormulario()
        }

        findViewById<Button>(R.id.btn_ayuda).setOnClickListener {
            val intent = Intent(this, AyudaActivity::class.java)
            startActivity(intent)
        }

        // Botón Siguiente - Navegar a la segunda parte del formulario
        findViewById<Button>(R.id.btn_siguiente).setOnClickListener {
            if (validarFormularioPaso1()) {
                navegarASiguientePantalla()
            }
        }

        // Configurar eventos para los botones de categoría
        configurarCategorias()

        // Configurar validación en tiempo real para la patente
        configurarValidacionPatente()

        // Botón de cerrar sesión
        findViewById<Button>(R.id.btn_cerrar_sesion).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun configurarValidacionPatente() {
        etPatente.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Ocultar error mientras el usuario escribe
                if (tvErrorPatente.visibility == View.VISIBLE) {
                    tvErrorPatente.visibility = View.GONE
                }

                // Convertir a mayúsculas automáticamente
                val texto = s.toString()
                if (texto != texto.uppercase()) {
                    etPatente.removeTextChangedListener(this)
                    etPatente.setText(texto.uppercase())
                    etPatente.setSelection(texto.length)
                    etPatente.addTextChangedListener(this)
                }
            }
        })
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

    @SuppressLint("ClickableViewAccessibility")
    private fun configurarComportamientoTeclado() {
        val nestedScrollView = findViewById<NestedScrollView>(R.id.nestedScrollView)
        val mainLayout = findViewById<LinearLayout>(R.id.mainLinearLayout)
        val descripcionEditText = findViewById<EditText>(R.id.et_descripcion)

        // Configurar el enfoque del EditText para desplazar la vista
        descripcionEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                nestedScrollView.postDelayed({
                    nestedScrollView.smoothScrollTo(0, descripcionEditText.bottom)
                }, 200)
            }
        }

        // Configurar también para el campo de patente
        etPatente.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                nestedScrollView.postDelayed({
                    nestedScrollView.smoothScrollTo(0, etPatente.bottom)
                }, 200)
            }
        }

        // Ocultar teclado al tocar fuera del EditText
        nestedScrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                ocultarTeclado()
                descripcionEditText.clearFocus()
                etPatente.clearFocus()
            }
            false
        }

        // También configurar el clic en el layout principal
        mainLayout?.setOnClickListener {
            ocultarTeclado()
            descripcionEditText.clearFocus()
            etPatente.clearFocus()
        }
    }

    private fun ocultarTeclado() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
    }

    private fun configurarCategorias() {
        val gridLayout = findViewById<GridLayout>(R.id.grid_categorias)

        for (i in 0 until gridLayout.childCount) {
            val view = gridLayout.getChildAt(i)
            if (view is Button) {
                botonesCategoria.add(view)

                view.setOnClickListener {
                    // Resetear todos los botones al color original
                    botonesCategoria.forEach { boton ->
                        boton.setBackgroundColor(ContextCompat.getColor(this, R.color.categoria_default))
                        boton.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
                    }

                    // Establecer la categoría seleccionada y cambiar su color
                    categoriaSeleccionadaNombre = view.text.toString()
                    categoriaSeleccionadaId = categoriasMap[categoriaSeleccionadaNombre] ?: 10 // Default a "Otro"

                    view.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_color))
                    view.setTextColor(ContextCompat.getColor(this, R.color.white))

                    Toast.makeText(this, "Categoría seleccionada: $categoriaSeleccionadaNombre", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun validarFormularioPaso1(): Boolean {
        val descripcion = findViewById<EditText>(R.id.et_descripcion).text.toString()
        val patente = etPatente.text.toString().trim()

        // Validar categoría
        if (categoriaSeleccionadaId == 0) {
            Toast.makeText(this, "Por favor selecciona una categoría", Toast.LENGTH_LONG).show()
            return false
        }

        // Validar descripción
        if (descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor describe la denuncia", Toast.LENGTH_LONG).show()
            findViewById<EditText>(R.id.et_descripcion).requestFocus()
            return false
        }

        if (descripcion.length < 10) {
            Toast.makeText(this, "La descripción debe tener al menos 10 caracteres", Toast.LENGTH_LONG).show()
            findViewById<EditText>(R.id.et_descripcion).requestFocus()
            return false
        }

        // Validar patente
        if (!validarPatente(patente)) {
            return false
        }

        return true
    }

    private fun validarPatente(patente: String): Boolean {
        // Validar longitud (5-6 caracteres)
        if (patente.length < 5 || patente.length > 6) {
            tvErrorPatente.text = "La patente debe tener entre 5 y 6 caracteres"
            tvErrorPatente.visibility = View.VISIBLE
            etPatente.requestFocus()
            return false
        }

        // Validar formato (solo letras y números)
        if (!patente.matches(Regex("[A-Z0-9]+"))) {
            tvErrorPatente.text = "La patente solo puede contener letras y números"
            tvErrorPatente.visibility = View.VISIBLE
            etPatente.requestFocus()
            return false
        }

        // Si pasa todas las validaciones
        tvErrorPatente.visibility = View.GONE
        return true
    }

    private fun navegarASiguientePantalla() {
        try {
            println("DEBUG: 🚀 Iniciando navegación a NuevaDenuncia2Activity")

            val descripcion = findViewById<EditText>(R.id.et_descripcion).text.toString()
            val patente = etPatente.text.toString().trim()
            val emailUsuario = tokenManager.getUserEmail() ?: ""

            // Logs de depuración
            println("DEBUG: 📋 Datos a pasar:")
            println("DEBUG:   - Categoría ID: '$categoriaSeleccionadaId'")
            println("DEBUG:   - Categoría Nombre: '$categoriaSeleccionadaNombre'")
            println("DEBUG:   - Patente: '$patente'")
            println("DEBUG:   - Email: '$emailUsuario'")
            println("DEBUG:   - Descripción: ${descripcion.length} caracteres")

            val intent = Intent(this, NuevaDenuncia2Activity::class.java).apply {
                putExtra("CATEGORIA_ID", categoriaSeleccionadaId) // Enviar ID en lugar de nombre
                putExtra("CATEGORIA_NOMBRE", categoriaSeleccionadaNombre) // Opcional: para mostrar en UI
                putExtra("DESCRIPCION", descripcion)
                putExtra("PATENTE", patente)
                putExtra("EMAIL_USUARIO", emailUsuario)
            }

            println("DEBUG: ✅ Intent creado, iniciando actividad...")
            startActivity(intent)
            println("DEBUG: 🎉 Actividad iniciada exitosamente")

            // NO llamar finish() aquí - queremos que el usuario pueda volver atrás

        } catch (e: Exception) {
            println("ERROR: 💥 No se pudo iniciar NuevaDenuncia2Activity: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error al abrir el formulario: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

    private fun reiniciarFormulario() {
        categoriaSeleccionadaId = 0
        categoriaSeleccionadaNombre = ""
        findViewById<EditText>(R.id.et_descripcion).setText("")
        etPatente.setText("")
        tvErrorPatente.visibility = View.GONE

        // Resetear los botones de categoría a su color original
        botonesCategoria.forEach { boton ->
            boton.setBackgroundColor(ContextCompat.getColor(this, R.color.categoria_default))
            boton.setTextColor(ContextCompat.getColor(this, R.color.text_dark))
        }

        Toast.makeText(this, "Formulario reiniciado", Toast.LENGTH_SHORT).show()
    }

    // Manejar el botón de retroceso del dispositivo para ocultar el teclado primero
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        val descripcionEditText = findViewById<EditText>(R.id.et_descripcion)

        when {
            descripcionEditText.hasFocus() -> {
                ocultarTeclado()
                descripcionEditText.clearFocus()
            }
            etPatente.hasFocus() -> {
                ocultarTeclado()
                etPatente.clearFocus()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}
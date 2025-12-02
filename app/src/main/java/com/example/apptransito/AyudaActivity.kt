package com.example.apptransito

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class AyudaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.ayuda)

        configurarEventos()
    }

    private fun configurarEventos() {
        // Configurar botones de navegación
        findViewById<Button>(R.id.btn_inicio).setOnClickListener {
            // Navegar a la pantalla de inicio
            val intent = Intent(this, InicioActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_nueva_denuncia).setOnClickListener {
            // Navegar a la pantalla de nueva denuncia
            val intent = Intent(this, NuevaDenunciaActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_ayuda).setOnClickListener {
            // Ya estamos en ayuda, solo mostrar mensaje
            Toast.makeText(this, "Estás en la sección de ayuda", Toast.LENGTH_SHORT).show()
        }

        // Botón de cerrar sesión - ACTUALIZADO
        findViewById<Button>(R.id.btn_cerrar_sesion).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun cerrarSesion() {
        // Crear intent para LoginActivity con flags para limpiar el stack
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

    // Opcional: Método para manejar el botón de retroceso del dispositivo
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        // Puedes personalizar el comportamiento al presionar retroceso
        val intent = Intent(this, InicioActivity::class.java)
        startActivity(intent)
        finish()
    }
}
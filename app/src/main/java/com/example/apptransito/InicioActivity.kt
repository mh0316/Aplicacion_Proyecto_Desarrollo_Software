package com.example.apptransito


import TokenManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.apptransito.AppTransitoApplication
import com.example.apptransito.LoginActivity
import com.example.apptransito.R

class InicioActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inicio)

        tokenManager = AppTransitoApplication.tokenManager

        // Verificar si está logueado
        if (!tokenManager.isLoggedIn()) {
            irALogin()
            return
        }

        // Mostrar información del usuario
        val userEmail = tokenManager.getUserEmail()

        // Configurar botón de cerrar sesión
        findViewById<Button>(R.id.btn_cerrar_sesion).setOnClickListener {
            cerrarSesion()
        }

        findViewById<Button>(R.id.btn_nueva_denuncia).setOnClickListener {
            val intent = Intent(this, NuevaDenunciaActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_ayuda).setOnClickListener {
            val intent = Intent(this, AyudaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cerrarSesion() {
        tokenManager.clearToken()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
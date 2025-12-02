package com.example.apptransito

import TokenManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var userRepository: UsuarioRepository
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya está logueado
        tokenManager = AppTransitoApplication.tokenManager
        if (tokenManager.isLoggedIn()) {
            irAInicio()
            return
        }

        setContentView(R.layout.login)

        userRepository = UsuarioRepository()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            } else {
                // Mostrar progreso
                showLoading(true)

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = userRepository.loginUser(email, password)

                        when (result) {
                            is Result.Success -> {
                                showLoading(false)
                                if (result.data.success) {
                                    // Guardar token y información del usuario
                                    tokenManager.saveToken(result.data.token)
                                    tokenManager.saveUserInfo(result.data.email, email)

                                    Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                    irAInicio()
                                } else {
                                    Toast.makeText(this@LoginActivity, result.data.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            is Result.Error -> {
                                showLoading(false)
                                Toast.makeText(this@LoginActivity, "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                showLoading(false)
                                Toast.makeText(this@LoginActivity, "Error desconocido en el login", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, RecuperacionContraseniaActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun irAInicio() {
        val intent = Intent(this, InicioActivity::class.java)
        startActivity(intent)
        finish()
    }
}
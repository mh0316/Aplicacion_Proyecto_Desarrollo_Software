package com.example.apptransito

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class RegistroActivity : ComponentActivity() {

    private lateinit var userRepository: UsuarioRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro)

        userRepository = UsuarioRepository()

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etRut = findViewById<EditText>(R.id.etRut)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val rut = etRut.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (nombre.isEmpty() || apellido.isEmpty() || rut.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            } else if (!isValidEmail(email)) {
                Toast.makeText(this, "Por favor, ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            } else if (password.length > 10) {
                Toast.makeText(this, "La contraseña no puede tener más de 10 caracteres", Toast.LENGTH_SHORT).show()
            } else {
                // Mostrar progreso
                showLoading(true)

                // Registrar usuario en el backend
                CoroutineScope(Dispatchers.Main).launch {
                    val user = Usuario(
                        rut = rut,
                        password = password,
                        email = email,
                        nombre = nombre,
                        apellido = apellido
                    )

                    when (val result = userRepository.registerUser(user)) {
                        is Result.Success -> {
                            showLoading(false)
                            if (result.data.success) {
                                showSuccessDialog()
                            } else {
                                Toast.makeText(this@RegistroActivity, result.data.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        is Result.Error -> {
                            showLoading(false)
                            Toast.makeText(this@RegistroActivity, "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Esta rama hace que el when sea exhaustivo
                            showLoading(false)
                            Toast.makeText(this@RegistroActivity, "Error desconocido en el registro", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        btnBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            Pattern.CASE_INSENSITIVE
        )
        return emailPattern.matcher(email).matches()
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            Toast.makeText(this, "Registrando usuario...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registro Exitoso")
        builder.setMessage("Su registro ha sido completado exitosamente.")
        builder.setPositiveButton("Ok") { dialog, which ->
            dialog.dismiss()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }
}
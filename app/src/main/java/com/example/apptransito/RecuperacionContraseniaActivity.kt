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
import kotlinx.coroutines.withContext
import retrofit2.Response

class RecuperacionContraseniaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recuperacion_contrasenia)

        val etEmail = findViewById<EditText>(R.id.etEmail) // Cambiado de etRut a etEmail
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnConfirmNewPassword = findViewById<Button>(R.id.btnConfirmNewPassword)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        btnConfirmNewPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

            if (email.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            } else if (!isValidEmail(email)) {
                Toast.makeText(this, "Por favor, ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show()
            } else if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            } else if (newPassword.length > 10) {
                Toast.makeText(this, "La contraseña no puede tener más de 10 caracteres", Toast.LENGTH_SHORT).show()
            } else {
                // Usar la API para recuperar contraseña
                recuperarContrasenia(email, newPassword)
            }
        }

        btnBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun recuperarContrasenia(email: String, nuevaContrasenia: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = RecuperacionContraseniaRequest(
                    email = email,
                    nuevaContrasenia = nuevaContrasenia
                )

                val response: Response<UserResponse> = RetrofitClient.apiService.updatePassword(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        showSuccessDialog()
                    } else {
                        // Manejar error específico de la API
                        val errorMessage = when (response.code()) {
                            404 -> "El correo electrónico no está registrado"
                            400 -> "Datos de solicitud inválidos"
                            else -> "Error al actualizar la contraseña. Intente nuevamente."
                        }
                        Toast.makeText(this@RecuperacionContraseniaActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@RecuperacionContraseniaActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Contraseña Actualizada")
        builder.setMessage("Su contraseña ha sido actualizada exitosamente.")
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
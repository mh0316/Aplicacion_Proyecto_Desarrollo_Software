package com.example.apptransito

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.*

class RevisarEvidenciaActivity : ComponentActivity() {  // Cambiado a ComponentActivity

    private lateinit var evidenceImage: ImageView
    private lateinit var evidenceVideo: VideoView
    private lateinit var fileTypeIndicator: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnVolver: Button
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var tvFileDate: TextView

    private var evidenciaUri: Uri? = null
    private var tipoEvidencia: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ver_evidencia)

        inicializarVistas()
        configurarEventos()
        obtenerDatosIntent()
    }

    private fun inicializarVistas() {
        evidenceImage = findViewById(R.id.evidence_image)
        evidenceVideo = findViewById(R.id.evidence_video)
        fileTypeIndicator = findViewById(R.id.file_type_indicator)
        btnPlay = findViewById(R.id.btn_play)
        btnVolver = findViewById(R.id.btn_volver)
        tvFileName = findViewById(R.id.tv_file_name)
        tvFileSize = findViewById(R.id.tv_file_size)
        tvFileDate = findViewById(R.id.tv_file_date)
    }

    private fun configurarEventos() {
        // Botones de navegación
        findViewById<Button>(R.id.btn_inicio).setOnClickListener {
            val intent = android.content.Intent(this, InicioActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_nueva_denuncia).setOnClickListener {
            val intent = android.content.Intent(this, NuevaDenunciaActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btn_ayuda).setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://ejemplo.com/ayuda-denuncias-transito")
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se puede abrir el enlace de ayuda", Toast.LENGTH_SHORT).show()
            }
        }

        // Solo botón VOLVER en el centro
        btnVolver.setOnClickListener {
            finish() // Simplemente volver a la actividad anterior (NuevaDenuncia2Activity)
        }

        btnPlay.setOnClickListener {
            if (evidenceVideo.isPlaying) {
                evidenceVideo.pause()
                btnPlay.text = "▶"
            } else {
                evidenceVideo.start()
                btnPlay.text = "⏸"
            }
        }
    }

    private fun obtenerDatosIntent() {
        val uriString = intent.getStringExtra("EVIDENCIA_URI")
        tipoEvidencia = intent.getStringExtra("TIPO_EVIDENCIA") ?: ""

        if (!uriString.isNullOrEmpty()) {
            evidenciaUri = Uri.parse(uriString)
            mostrarEvidencia()
        } else {
            Toast.makeText(this, "No hay evidencia para mostrar", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun mostrarEvidencia() {
        when (tipoEvidencia) {
            "foto" -> {
                evidenceImage.setImageURI(evidenciaUri)
                evidenceImage.visibility = ImageView.VISIBLE
                evidenceVideo.visibility = VideoView.GONE
                btnPlay.visibility = Button.GONE
                fileTypeIndicator.text = "FOTO"

                tvFileName.text = "foto_denuncia.jpg"
                tvFileSize.text = "Tamaño estimado: 2-5 MB"
            }
            "video" -> {
                evidenceVideo.setVideoURI(evidenciaUri)
                evidenceVideo.visibility = VideoView.VISIBLE
                evidenceImage.visibility = ImageView.GONE
                btnPlay.visibility = Button.VISIBLE
                fileTypeIndicator.text = "VIDEO"

                tvFileName.text = "video_denuncia.mp4"
                tvFileSize.text = "Tamaño estimado: 10-50 MB"
            }
        }

        tvFileDate.text = "Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())}"
    }
}
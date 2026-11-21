package com.example.trabajofinal

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class GrupoActivity : AppCompatActivity() {

    private var ejercicioElegido: String? = null
    private var listener: ListenerRegistration? = null

    private val ejerciciosPorGrupo = mapOf(
        "Pecho" to listOf(
            "Press de banca", "Press inclinado con mancuernas", "Aperturas con mancuernas",
            "Fondos en paralelas", "Press en máquina", "Cruces en polea"
        ),
        "Espalda" to listOf(
            "Dominadas", "Jalón al pecho", "Remo con barra",
            "Remo con mancuerna", "Peso muerto", "Remo en polea baja"
        ),
        "Pierna" to listOf(
            "Sentadilla", "Prensa de pierna", "Peso muerto rumano",
            "Zancadas", "Extensión de cuádriceps", "Curl femoral"
        ),
        "Brazo" to listOf(
            "Curl con barra", "Curl martillo", "Press francés",
            "Extensión de tríceps en polea", "Fondos en banco", "Curl concentrado"
        )
    )

    private val coloresPorGrupo = mapOf(
        "Pecho" to "#F87171",
        "Espalda" to "#4ADE80",
        "Pierna" to "#60A5FA",
        "Brazo" to "#FACC15"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_grupo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val grupo = intent.getStringExtra("grupo") ?: "Pecho"
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        val registros = FirebaseFirestore.getInstance()
            .collection("usuarios").document(uid).collection("registros")

        val tvTitulo = findViewById<TextView>(R.id.tvGrupoTitulo)
        val tvEjercicio = findViewById<TextView>(R.id.tvEjercicio)
        val etPeso = findViewById<EditText>(R.id.etPesoSerie)
        val etReps = findViewById<EditText>(R.id.etReps)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarSerie)
        val llHistorial = findViewById<LinearLayout>(R.id.llHistorial)
        val tvVacio = findViewById<TextView>(R.id.tvVacio)

        tvTitulo.text = grupo
        tvTitulo.setTextColor(Color.parseColor(coloresPorGrupo[grupo] ?: "#FFFFFF"))
        findViewById<TextView>(R.id.tvVolver).setOnClickListener { finish() }

        // Elegir ejercicio del grupo
        tvEjercicio.setOnClickListener {
            val lista = (ejerciciosPorGrupo[grupo] ?: emptyList()).toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Ejercicio de $grupo")
                .setItems(lista) { _, i ->
                    ejercicioElegido = lista[i]
                    tvEjercicio.text = lista[i]
                    tvEjercicio.setTextColor(Color.WHITE)
                }
                .show()
        }

        // Guardar serie en Firestore: usuarios/{uid}/registros
        btnGuardar.setOnClickListener {
            val ejercicio = ejercicioElegido
            val peso = etPeso.text.toString().replace(",", ".").toDoubleOrNull()
            val reps = etReps.text.toString().toIntOrNull()

            if (ejercicio == null) {
                Toast.makeText(this, "Elige un ejercicio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (peso == null || peso <= 0) {
                etPeso.error = "Escribe el peso"
                return@setOnClickListener
            }
            if (reps == null || reps <= 0) {
                etReps.error = "Escribe las repeticiones"
                return@setOnClickListener
            }

            val serie = hashMapOf(
                "grupo" to grupo,
                "ejercicio" to ejercicio,
                "pesoKg" to peso,
                "reps" to reps,
                "fecha" to FieldValue.serverTimestamp()
            )
            registros.add(serie)
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }

            etReps.text.clear()
            Toast.makeText(this, "Serie guardada", Toast.LENGTH_SHORT).show()
        }

        // Historial del grupo en tiempo real
        val formato = SimpleDateFormat("d MMM · HH:mm", Locale("es", "CO"))
        listener = registros.whereEqualTo("grupo", grupo)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                llHistorial.removeAllViews()
                val docs = snap.documents.sortedByDescending {
                    it.getTimestamp("fecha")?.seconds ?: Long.MAX_VALUE
                }
                tvVacio.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE

                for (d in docs) {
                    val ejercicio = d.getString("ejercicio") ?: continue
                    val peso = d.getDouble("pesoKg") ?: 0.0
                    val reps = d.getLong("reps") ?: 0
                    val pesoTxt = if (peso % 1.0 == 0.0) peso.toInt().toString() else peso.toString()
                    val fecha = d.getTimestamp("fecha")?.toDate()?.let { formato.format(it) } ?: "ahora"
                    llHistorial.addView(crearFila(ejercicio, "$pesoTxt kg × $reps rep · $fecha"))
                }
            }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun crearFila(titulo: String, detalle: String): View {
        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        fila.addView(TextView(this).apply {
            text = titulo
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        fila.addView(TextView(this).apply {
            text = detalle
            setTextColor(Color.parseColor("#9A9A9A"))
            textSize = 11f
            setPadding(0, dp(4), 0, 0)
        })
        return fila
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}

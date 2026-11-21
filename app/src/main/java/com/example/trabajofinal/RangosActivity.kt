package com.example.trabajofinal

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RangosActivity : AppCompatActivity() {

    data class NivelRango(
        val nombre: String,
        val colorHex: String,
        val umbralRatio: Double
    )

    data class ResultadoRango(
        val nombre: String,
        val colorHex: String,
        val progresoSiguiente: Int,
        val indiceNivel: Int
    )

    // 9 rangos estilo Symmetry: 8 con divisiones I, II, III + Simétrico (top)
    private val niveles = listOf(
        NivelRango("Hierro I", "#9CA3AF", 0.00),
        NivelRango("Hierro II", "#9CA3AF", 0.25),
        NivelRango("Hierro III", "#9CA3AF", 0.35),
        NivelRango("Bronce I", "#CD7F32", 0.45),
        NivelRango("Bronce II", "#CD7F32", 0.55),
        NivelRango("Bronce III", "#CD7F32", 0.65),
        NivelRango("Plata I", "#E5E7EB", 0.75),
        NivelRango("Plata II", "#E5E7EB", 0.85),
        NivelRango("Plata III", "#E5E7EB", 0.95),
        NivelRango("Oro I", "#FACC15", 1.05),
        NivelRango("Oro II", "#FACC15", 1.15),
        NivelRango("Oro III", "#FACC15", 1.25),
        NivelRango("Rubí I", "#F87171", 1.35),
        NivelRango("Rubí II", "#F87171", 1.45),
        NivelRango("Rubí III", "#F87171", 1.55),
        NivelRango("Esmeralda I", "#34D399", 1.65),
        NivelRango("Esmeralda II", "#34D399", 1.75),
        NivelRango("Esmeralda III", "#34D399", 1.85),
        NivelRango("Diamante I", "#60A5FA", 1.95),
        NivelRango("Diamante II", "#60A5FA", 2.05),
        NivelRango("Diamante III", "#60A5FA", 2.15),
        NivelRango("Campeón I", "#C084FC", 2.25),
        NivelRango("Campeón II", "#C084FC", 2.35),
        NivelRango("Campeón III", "#C084FC", 2.45),
        NivelRango("Simétrico", "#22D3EE", 2.60)
    )

    // Multiplicadores por grupo para equilibrar cargas (ej. Sentadilla mueve más peso que Curl)
    private val factorGrupo = mapOf(
        "Pecho" to 1.00,
        "Espalda" to 1.05,
        "Pierna" to 0.75,
        "Brazo" to 1.65
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rangos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        findViewById<TextView>(R.id.tvVolver).setOnClickListener { finish() }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("usuarios").document(uid)

        userRef.get()
            .addOnSuccessListener { userDoc ->
                val pesoCorporal = userDoc.getDouble("pesoKg") ?: 70.0
                val pesoTxt = if (pesoCorporal % 1.0 == 0.0) pesoCorporal.toInt().toString() else pesoCorporal.toString()
                findViewById<TextView>(R.id.tvPesoReferencia).text =
                    "Fuerza relativa calculada para $pesoTxt kg corporales"

                userRef.collection("registros").get()
                    .addOnSuccessListener { snap ->
                        val mejorPorGrupo = mutableMapOf<String, Pair<Double, String>>()

                        for (doc in snap) {
                            val grupo = doc.getString("grupo") ?: continue
                            val ejercicio = doc.getString("ejercicio") ?: continue
                            val peso = doc.getDouble("pesoKg") ?: 0.0
                            val reps = doc.getLong("reps") ?: 0L
                            if (peso <= 0 || reps <= 0) continue

                            // Estimación de 1RM (fórmula de Epley)
                            val oneRm = peso * (1.0 + (reps.coerceAtMost(20) / 30.0))
                            val actual = mejorPorGrupo[grupo]?.first ?: 0.0
                            if (oneRm > actual) {
                                val pStr = if (peso % 1.0 == 0.0) peso.toInt().toString() else peso.toString()
                                mejorPorGrupo[grupo] = Pair(oneRm, "Mejor marca: $ejercicio ($pStr kg × $reps)")
                            }
                        }

                        val resPecho = actualizarTarjetaGrupo(
                            "Pecho", mejorPorGrupo["Pecho"], pesoCorporal,
                            findViewById(R.id.tvRangoPecho),
                            findViewById(R.id.tvMejorPecho),
                            findViewById(R.id.pbPecho)
                        )
                        val resEspalda = actualizarTarjetaGrupo(
                            "Espalda", mejorPorGrupo["Espalda"], pesoCorporal,
                            findViewById(R.id.tvRangoEspalda),
                            findViewById(R.id.tvMejorEspalda),
                            findViewById(R.id.pbEspalda)
                        )
                        val resPierna = actualizarTarjetaGrupo(
                            "Pierna", mejorPorGrupo["Pierna"], pesoCorporal,
                            findViewById(R.id.tvRangoPierna),
                            findViewById(R.id.tvMejorPierna),
                            findViewById(R.id.pbPierna)
                        )
                        val resBrazo = actualizarTarjetaGrupo(
                            "Brazo", mejorPorGrupo["Brazo"], pesoCorporal,
                            findViewById(R.id.tvRangoBrazo),
                            findViewById(R.id.tvMejorBrazo),
                            findViewById(R.id.pbBrazo)
                        )

                        actualizarRangoGlobal(listOfNotNull(resPecho, resEspalda, resPierna, resBrazo))
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al cargar registros: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar perfil: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarTarjetaGrupo(
        grupo: String,
        mejorMarca: Pair<Double, String>?,
        pesoCorporal: Double,
        tvRango: TextView,
        tvDetalle: TextView,
        pb: ProgressBar
    ): ResultadoRango? {
        if (mejorMarca == null) {
            tvRango.text = "Sin rango"
            tvRango.setTextColor(Color.parseColor("#8E8E93"))
            tvDetalle.text = "Registra una serie en $grupo para desbloquear"
            pb.progress = 0
            return null
        }

        val factor = factorGrupo[grupo] ?: 1.0
        val ratioAjustado = (mejorMarca.first / pesoCorporal) * factor
        val resultado = calcularRango(ratioAjustado)
        val color = Color.parseColor(resultado.colorHex)

        tvRango.text = resultado.nombre
        tvRango.setTextColor(color)
        tvDetalle.text = mejorMarca.second
        pb.progress = resultado.progresoSiguiente
        pb.progressTintList = ColorStateList.valueOf(color)

        return resultado
    }

    private fun actualizarRangoGlobal(resultados: List<ResultadoRango>) {
        val tvGlobal = findViewById<TextView>(R.id.tvRangoGlobal)
        val tvDetalleGlobal = findViewById<TextView>(R.id.tvDetalleGlobal)
        val pbGlobal = findViewById<ProgressBar>(R.id.pbGlobal)

        if (resultados.isEmpty()) {
            tvGlobal.text = "Sin clasificar"
            tvGlobal.setTextColor(Color.parseColor("#8E8E93"))
            tvDetalleGlobal.text = "Completa al menos 1 serie para obtener tu rango global"
            pbGlobal.progress = 0
            return
        }

        val promedioIndice = resultados.map { it.indiceNivel }.average().toInt().coerceIn(0, niveles.lastIndex)
        val promedioProgreso = resultados.map { it.progresoSiguiente }.average().toInt().coerceIn(0, 100)
        val nivelGlobal = niveles[promedioIndice]
        val color = Color.parseColor(nivelGlobal.colorHex)

        tvGlobal.text = nivelGlobal.nombre
        tvGlobal.setTextColor(color)
        tvDetalleGlobal.text = "${resultados.size} de 4 grupos musculares clasificados"
        pbGlobal.progress = promedioProgreso
        pbGlobal.progressTintList = ColorStateList.valueOf(color)
    }

    private fun calcularRango(ratio: Double): ResultadoRango {
        var indiceActual = 0
        for (i in niveles.indices) {
            if (ratio >= niveles[i].umbralRatio) {
                indiceActual = i
            } else {
                break
            }
        }

        val actual = niveles[indiceActual]
        val progreso = if (indiceActual == niveles.lastIndex) {
            100
        } else {
            val siguiente = niveles[indiceActual + 1]
            val tramo = (siguiente.umbralRatio - actual.umbralRatio).coerceAtLeast(0.01)
            (((ratio - actual.umbralRatio) / tramo) * 100).toInt().coerceIn(5, 99)
        }

        return ResultadoRango(
            nombre = actual.nombre,
            colorHex = actual.colorHex,
            progresoSiguiente = progreso,
            indiceNivel = indiceActual
        )
    }
}
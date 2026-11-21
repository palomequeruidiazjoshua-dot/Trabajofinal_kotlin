package com.example.trabajofinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

data class RegistroEjercicio(
    val ejercicio: String = "",
    val pesoTexto: String = "",
    val reps: Long = 0L,
    val fechaFormateada: String = ""
)

class HistorialActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvVacio: TextView
    private val listaRegistros = mutableListOf<RegistroEjercicio>()
    private lateinit var adapter: HistorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        rvHistorial = findViewById(R.id.rvHistorial)
        tvVacio = findViewById(R.id.tvVacio)

        adapter = HistorialAdapter(listaRegistros)
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.adapter = adapter

        cargarHistorial()

        // Barra inferior
        findViewById<TextView>(R.id.tabInicio).setOnClickListener {
            finish()
        }
        findViewById<TextView>(R.id.tabRangos).setOnClickListener {
            startActivity(Intent(this, RangosActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.tabPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }
    }

    private fun cargarHistorial() {
        val uid = auth.currentUser?.uid ?: return
        val formatoFecha = SimpleDateFormat("d MMM · HH:mm", Locale("es", "CO"))

        db.collection("usuarios")
            .document(uid)
            .collection("registros")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                listaRegistros.clear()
                for (doc in result) {
                    val ejercicio = doc.getString("ejercicio") ?: continue
                    val peso = doc.getDouble("pesoKg") ?: 0.0
                    val reps = doc.getLong("reps") ?: 0L
                    val pesoTexto = if (peso % 1.0 == 0.0) peso.toInt().toString() else peso.toString()
                    val fechaTexto = doc.getTimestamp("fecha")?.toDate()?.let {
                        formatoFecha.format(it).replace(".", "")
                    } ?: "ahora"

                    listaRegistros.add(
                        RegistroEjercicio(
                            ejercicio = ejercicio,
                            pesoTexto = pesoTexto,
                            reps = reps,
                            fechaFormateada = fechaTexto
                        )
                    )
                }

                adapter.notifyDataSetChanged()
                tvVacio.visibility = if (listaRegistros.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar historial: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}

class HistorialAdapter(private val items: List<RegistroEjercicio>) :
    RecyclerView.Adapter<HistorialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreEjercicio)
        val tvDetalle: TextView = view.findViewById(R.id.tvDetalleSerie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_historial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNombre.text = item.ejercicio
        val repTexto = if (item.reps == 1L) "1 rep" else "${item.reps} reps"
        holder.tvDetalle.text = "${item.pesoTexto} kg × $repTexto · ${item.fechaFormateada}"
    }

    override fun getItemCount(): Int = items.size
}
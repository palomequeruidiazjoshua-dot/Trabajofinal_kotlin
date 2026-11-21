package com.example.trabajofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            irAlLogin()
            return
        }

        val ref = FirebaseFirestore.getInstance().collection("usuarios").document(uid)

        val tvNombre = findViewById<TextView>(R.id.tvPerfilNombre)
        val tvCorreo = findViewById<TextView>(R.id.tvPerfilCorreo)
        val etEdad = findViewById<EditText>(R.id.etPerfilEdad)
        val etAltura = findViewById<EditText>(R.id.etPerfilAltura)
        val etPeso = findViewById<EditText>(R.id.etPerfilPeso)
        val etMeses = findViewById<EditText>(R.id.etPerfilMeses)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarSesion)

        findViewById<TextView>(R.id.tvVolverPerfil).setOnClickListener { finish() }

        // Carga los datos guardados
        tvCorreo.text = auth.currentUser?.email.orEmpty()
        ref.get().addOnSuccessListener { doc ->
            tvNombre.text = doc.getString("nombre") ?: "Sin nombre"
            doc.getString("correo")?.let { tvCorreo.text = it }
            doc.getLong("edad")?.let { etEdad.setText(it.toString()) }
            doc.getLong("alturaCm")?.let { etAltura.setText(it.toString()) }
            doc.getDouble("pesoKg")?.let {
                etPeso.setText(if (it % 1.0 == 0.0) it.toInt().toString() else it.toString())
            }
            doc.getLong("mesesEntrenando")?.let { etMeses.setText(it.toString()) }
        }

        // Guarda los cambios
        btnGuardar.setOnClickListener {
            val edad = etEdad.text.toString().toIntOrNull()
            val altura = etAltura.text.toString().toIntOrNull()
            val peso = etPeso.text.toString().replace(",", ".").toDoubleOrNull()
            val meses = etMeses.text.toString().toIntOrNull()

            if (edad == null || edad !in 10..100) {
                etEdad.error = "Edad entre 10 y 100"
                return@setOnClickListener
            }
            if (altura == null || altura !in 100..250) {
                etAltura.error = "Altura entre 100 y 250 cm"
                return@setOnClickListener
            }
            if (peso == null || peso < 20 || peso > 300) {
                etPeso.error = "Peso entre 20 y 300 kg"
                return@setOnClickListener
            }
            if (meses == null || meses !in 0..600) {
                etMeses.error = "Escribe los meses (0 si empiezas)"
                return@setOnClickListener
            }

            val datos = hashMapOf<String, Any>(
                "edad" to edad,
                "alturaCm" to altura,
                "pesoKg" to peso,
                "mesesEntrenando" to meses,
                "perfilCompleto" to true
            )
            ref.set(datos, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        // Cerrar sesión
        btnCerrar.setOnClickListener {
            auth.signOut()
            irAlLogin()
        }
    }

    private fun irAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}

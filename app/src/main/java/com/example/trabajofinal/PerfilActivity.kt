package com.example.trabajofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        val tvNombre = findViewById<TextView>(R.id.tvPerfilNombre)
        val tvCorreo = findViewById<TextView>(R.id.tvPerfilCorreo)
        val tvEdad = findViewById<TextView>(R.id.tvPerfilEdad)
        val tvAltura = findViewById<TextView>(R.id.tvPerfilAltura)
        val tvPeso = findViewById<TextView>(R.id.tvPerfilPeso)
        val tvMeses = findViewById<TextView>(R.id.tvPerfilMeses)

        // Solo lectura: carga los datos guardados
        tvCorreo.text = auth.currentUser?.email.orEmpty()
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                tvNombre.text = doc.getString("nombre") ?: "Sin nombre"
                doc.getString("correo")?.let { tvCorreo.text = it }
                doc.getLong("edad")?.let { tvEdad.text = it.toString() }
                doc.getLong("alturaCm")?.let { tvAltura.text = it.toString() }
                doc.getDouble("pesoKg")?.let {
                    tvPeso.text = if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
                }
                doc.getLong("mesesEntrenando")?.let { tvMeses.text = it.toString() }
            }

        // Cerrar sesión
        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            auth.signOut()
            irAlLogin()
        }

        // Barra inferior
        findViewById<TextView>(R.id.tabInicio).setOnClickListener {
            finish()
        }
        findViewById<TextView>(R.id.tabHistorial).setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.tabRangos).setOnClickListener {
            startActivity(Intent(this, RangosActivity::class.java))
            finish()
        }
    }

    private fun irAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
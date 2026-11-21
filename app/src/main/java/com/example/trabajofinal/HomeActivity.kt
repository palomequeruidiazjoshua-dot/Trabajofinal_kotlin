package com.example.trabajofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
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

        // Saludo con el primer nombre guardado en Firestore
        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        FirebaseFirestore.getInstance().collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val primerNombre = doc.getString("nombre")
                    ?.trim()?.split(" ")?.firstOrNull().orEmpty()
                tvSaludo.text = if (primerNombre.isNotEmpty()) "A por más, $primerNombre." else "A por más."
            }

        // Cada grupo muscular abre su pantalla de registro de ejercicios
        mapOf(
            R.id.cardPecho to "Pecho",
            R.id.cardEspalda to "Espalda",
            R.id.cardPierna to "Pierna",
            R.id.cardBrazo to "Brazo"
        ).forEach { (id, nombre) ->
            findViewById<View>(id).setOnClickListener {
                val intent = Intent(this, GrupoActivity::class.java)
                intent.putExtra("grupo", nombre)
                startActivity(intent)
            }
        }

        // Barra inferior
        findViewById<TextView>(R.id.tabEntrenar).setOnClickListener {
            Toast.makeText(this, "Elige un grupo muscular para registrar", Toast.LENGTH_SHORT).show()
        }
        // Perfil = cerrar sesión por ahora (útil para probar)
        findViewById<TextView>(R.id.tabPerfil).setOnClickListener {
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

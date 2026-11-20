package com.example.trabajofinal

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class DatosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_datos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val etEdad = findViewById<EditText>(R.id.etEdad)
        val etAltura = findViewById<EditText>(R.id.etAltura)
        val etPeso = findViewById<EditText>(R.id.etPeso)
        val etMeses = findViewById<EditText>(R.id.etMeses)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

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

            btnGuardar.isEnabled = false

            val datos = hashMapOf<String, Any>(
                "edad" to edad,
                "alturaCm" to altura,
                "pesoKg" to peso,
                "mesesEntrenando" to meses,
                "perfilCompleto" to true
            )

            // merge = agrega estos campos sin borrar nombre y correo
            db.collection("usuarios").document(uid).set(datos, SetOptions.merge())
                .addOnSuccessListener {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }
}

package com.example.trabajofinal

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val etConfirm = findViewById<EditText>(R.id.etConfirmPassword)
        val btnCrear = findViewById<Button>(R.id.btnCrearCuenta)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        btnCrear.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm = etConfirm.text.toString()

            // Validaciones
            if (nombre.isEmpty()) {
                etNombre.error = "Escribe tu nombre"
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Correo no válido"
                return@setOnClickListener
            }
            if (password.length < 6) {
                etPassword.error = "Mínimo 6 caracteres"
                return@setOnClickListener
            }
            if (password != confirm) {
                etConfirm.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            btnCrear.isEnabled = false

            // 1) Crea el usuario en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener

                    // 2) Guarda los datos del perfil en Firestore (SIN la contraseña)
                    val datos = hashMapOf(
                        "uid" to uid,
                        "nombre" to nombre,
                        "correo" to email,
                        "fechaRegistro" to FieldValue.serverTimestamp()
                    )

                    db.collection("usuarios").document(uid).set(datos)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DatosActivity::class.java))
                            finishAffinity()
                        }
                        .addOnFailureListener { e ->
                            btnCrear.isEnabled = true
                            Toast.makeText(this, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnCrear.isEnabled = true
                    Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }
}

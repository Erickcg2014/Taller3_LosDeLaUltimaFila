package com.example.taller3

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityIngresarBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Ingresar : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityIngresarBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var userRef: DatabaseReference
    private var isLoggingOut = false // Nueva bandera para controlar el estado de cierre de sesión

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        // Setup ViewBinding
        binding = ActivityIngresarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ProgressDialog
        setupProgressDialog()

        // Setup click listener for login button
        binding.button.setOnClickListener {
            val email = binding.correoIngresar.text.toString().trim()
            val password = binding.contraseniaIngresar.text.toString().trim()
            signInUser(email, password)
        }

        // Setup ActionBar
        supportActionBar?.apply {
            title = "Iniciar Sesión"
        }
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Iniciando sesión...")
            setCancelable(false)
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            // Establecer referencia en Firebase para el usuario actual
            userRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}")

            // Cambiar el estado de disponibilidad a true al iniciar sesión
            Log.d(TAG, "Actualizando estado de disponible a true para el usuario ${currentUser.uid}")
            userRef.child("disponible").setValue(true)
                .addOnSuccessListener {
                    Log.d(TAG, "Estado de disponible actualizado a true exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar estado de disponible a true: ${e.message}")
                }

            val intent = Intent(this, Mapa::class.java).apply {
                putExtra("user", currentUser.email)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } else {
            // Clear form fields
            binding.correoIngresar.setText("")
            binding.contraseniaIngresar.setText("")
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        val email = binding.correoIngresar.text.toString().trim()
        if (TextUtils.isEmpty(email)) {
            binding.correoIngresar.error = "Email es requerido"
            valid = false
        } else if (!isEmailValid(email)) {
            binding.correoIngresar.error = "Email inválido"
            valid = false
        } else {
            binding.correoIngresar.error = null
        }

        val password = binding.contraseniaIngresar.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.contraseniaIngresar.error = "Contraseña es requerida"
            valid = false
        } else if (password.length < 6) {
            binding.contraseniaIngresar.error = "Mínimo 6 caracteres"
            valid = false
        } else {
            binding.contraseniaIngresar.error = null
        }

        return valid
    }

    private fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun signInUser(email: String, password: String) {
        if (!validateForm()) return

        progressDialog.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    updateUI(auth.currentUser)
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    fun logoutUser() {
        isLoggingOut = true // Actualiza la bandera para indicar que el usuario está cerrando sesión
        auth.signOut()
        FirebaseDatabase.getInstance().getReference("users/${auth.currentUser?.uid}/disponible").setValue(false)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }

        /*// Solo cambia el estado de disponibilidad a false si el usuario está cerrando sesión
        if (!isLoggingOut && auth.currentUser != null) {
            Log.d(TAG, "Actualizando estado de disponible a false para el usuario ${auth.currentUser?.uid}")
            FirebaseDatabase.getInstance().getReference("users/${auth.currentUser?.uid}/disponible").setValue(false)
                .addOnSuccessListener {
                    Log.d(TAG, "Estado de disponible actualizado a false exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar estado de disponible a false: ${e.message}")
                }
        }*/
    }

    companion object {
        private const val TAG = "Ingresar"
    }
}

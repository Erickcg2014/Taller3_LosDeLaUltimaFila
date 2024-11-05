package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import android.app.ProgressDialog
import android.content.Context
import android.view.MenuItem

class Registrar : AppCompatActivity() {
    data class Usuario(
        var nombre: String = "",
        var apellido: String = "",
        var identificacion: Int = 0,
        var latitud: Double = 0.0,
        var longitud: Double = 0.0,
        var email: String = "",
        var profileImage: String = "",
        var disponible: Boolean = false // Nuevo campo para disponibilidad

    )

    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference

    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private val CAMERA_PERMISSION_CODE = 102

    private lateinit var fotoPerfil: ImageView
    private lateinit var progressDialog: ProgressDialog
    private var imageBitmap: Bitmap? = null

    // UI Elements
    private lateinit var nombre: EditText
    private lateinit var apellidos: EditText
    private lateinit var correo: EditText
    private lateinit var contrasenia: EditText
    private lateinit var numIdentificacion: EditText
    private lateinit var latitud: EditText
    private lateinit var longitud: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        myRef = database.getReference(PATH_USERS)

        // Initialize UI elements
        initializeUIElements()
        setupActionBar()
        setupProgressDialog()

        // Setup button listeners
        setupButtonListeners()
    }

    private fun initializeUIElements() {
        fotoPerfil = findViewById(R.id.fotoPerfil)
        nombre = findViewById(R.id.nombre)
        apellidos = findViewById(R.id.apellidos)
        correo = findViewById(R.id.correo)
        contrasenia = findViewById(R.id.constrasenia)
        numIdentificacion = findViewById(R.id.numIdentificacion)
        latitud = findViewById(R.id.latitud)
        longitud = findViewById(R.id.longitud)

        val btnCamera: ImageButton = findViewById(R.id.imageButton2)
        val btnGallery: ImageButton = findViewById(R.id.imageButton)
        val registrarBoton: Button = findViewById(R.id.botonRegistrarPantalla2)

        btnCamera.setOnClickListener { checkCameraPermission() }
        btnGallery.setOnClickListener { openGallery() }
        registrarBoton.setOnClickListener { registrarUsuario() }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Registro de Usuario"
        }
    }

    private fun setupProgressDialog() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Procesando...")
            setCancelable(false)
        }
    }

    private fun setupButtonListeners() {
        findViewById<Button>(R.id.botonRegistrarPantalla2).setOnClickListener {
            if (validarCampos() && isInternetAvailable()) {
                registrarUsuario()
            }
        }
    }

    private fun validarCampos(): Boolean {
        var isValid = true

        if (nombre.text.toString().isEmpty()) {
            nombre.error = "Campo requerido"
            isValid = false
        }
        if (apellidos.text.toString().isEmpty()) {
            apellidos.error = "Campo requerido"
            isValid = false
        }
        if (!isValidEmail(correo.text.toString())) {
            correo.error = "Email inválido"
            isValid = false
        }
        if (contrasenia.text.toString().length < 6) {
            contrasenia.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }
        if (numIdentificacion.text.toString().isEmpty()) {
            numIdentificacion.error = "Campo requerido"
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun registrarUsuario() {
        progressDialog.show()

        val email = correo.text.toString()
        val password = contrasenia.text.toString()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    guardarDatosUsuario(auth.currentUser?.uid)
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this,
                        "Error en el registro: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun guardarDatosUsuario(userId: String?) {
        if (userId == null) {
            progressDialog.dismiss()
            return
        }

        val usuario = Usuario(
            nombre = nombre.text.toString(),
            apellido = apellidos.text.toString(),
            identificacion = numIdentificacion.text.toString().toIntOrNull() ?: 0,
            latitud = latitud.text.toString().toDoubleOrNull() ?: 0.0,
            longitud = longitud.text.toString().toDoubleOrNull() ?: 0.0,
            email = correo.text.toString(),
            profileImage = imageBitmap?.let { encodeToBase64(it) } ?: "", // Convertir imagen a Base64
            disponible = false
        )

        database.getReference("$PATH_USERS/$userId")
            .setValue(usuario)
            .addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this,
                        "Error al guardar datos: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun openGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        fotoPerfil.setImageBitmap(it)
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let {
                        val inputStream = contentResolver.openInputStream(it)
                        imageBitmap = BitmapFactory.decodeStream(inputStream)
                        fotoPerfil.setImageBitmap(imageBitmap)
                    }
                }
            }
        }
    }

    private fun encodeToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    companion object {
        const val PATH_USERS = "users/"
    }
}

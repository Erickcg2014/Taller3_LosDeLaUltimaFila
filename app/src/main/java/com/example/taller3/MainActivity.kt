package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val botonIngresar = findViewById<Button>(R.id.botonIngresar)
        val botonRegistrar = findViewById<Button>(R.id.botonRegistrar)


        val intentIngresar = Intent(this, Ingresar::class.java)
        val intentRegistrar = Intent(this, Registrar::class.java)

        botonIngresar.setOnClickListener(){
            startActivity(intentIngresar)
        }
        botonRegistrar.setOnClickListener(){
            startActivity(intentRegistrar)
        }
    }
}
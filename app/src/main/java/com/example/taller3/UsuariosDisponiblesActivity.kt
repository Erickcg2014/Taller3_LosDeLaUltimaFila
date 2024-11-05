package com.example.taller3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taller3.databinding.ActivityUsuariosDisponiblesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UsuariosDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosDisponiblesBinding
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")
    private val usuarios = mutableListOf<Usuario>()
    private lateinit var adapter: UsuariosAdapter
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Obtener el ID del usuario actual para excluirlo de la lista de usuarios disponibles
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Configurar RecyclerView
        binding.recyclerViewUsuarios.layoutManager = LinearLayoutManager(this)
        adapter = UsuariosAdapter(usuarios) { usuario ->
            val intent = Intent(this, MapaUsuarioActivity::class.java)
            intent.putExtra("latitud", usuario.latitud)
            intent.putExtra("longitud", usuario.longitud)
            intent.putExtra("nombre", usuario.nombre)
            intent.putExtra("observedUserId", usuario.id)
            startActivity(intent)
        }
        binding.recyclerViewUsuarios.adapter = adapter

        cargarUsuariosIniciales()
        setupAvailabilityListener()
    }

    private fun cargarUsuariosIniciales() {
        dbRef.orderByChild("disponible").equalTo(true).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usuarios.clear()
                for (userSnapshot in snapshot.children) {
                    val id = userSnapshot.key ?: ""
                    if (id == currentUserId) continue // Excluir el usuario actual

                    val nombre = userSnapshot.child("nombre").getValue(String::class.java) ?: "Sin nombre"
                    val imagenBase64 = userSnapshot.child("profileImage").getValue(String::class.java) ?: ""
                    val latitud = userSnapshot.child("latitud").getValue(Double::class.java) ?: 0.0
                    val longitud = userSnapshot.child("longitud").getValue(Double::class.java) ?: 0.0

                    val usuario = Usuario(id, nombre, imagenBase64, latitud, longitud)
                    usuarios.add(usuario)
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UsuariosDisponibles", "Error al cargar usuarios", error.toException())
                Toast.makeText(this@UsuariosDisponiblesActivity, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupAvailabilityListener() {
        dbRef.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                if (id == currentUserId) return // Excluir el usuario actual

                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario desconocido"
                val disponible = snapshot.child("disponible").getValue(Boolean::class.java) ?: false
                val latitud = snapshot.child("latitud").getValue(Double::class.java) ?: 0.0
                val longitud = snapshot.child("longitud").getValue(Double::class.java) ?: 0.0
                val imagenBase64 = snapshot.child("profileImage").getValue(String::class.java) ?: ""

                if (disponible) {
                    // Añadir usuario a la lista si está disponible y no duplicado
                    if (usuarios.none { it.id == id }) {
                        usuarios.add(Usuario(id, nombre, imagenBase64, latitud, longitud))
                        Toast.makeText(this@UsuariosDisponiblesActivity, "$nombre se ha conectado", Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    // Eliminar usuario de la lista si no está disponible
                    val index = usuarios.indexOfFirst { it.id == id }
                    if (index != -1) {
                        usuarios.removeAt(index)
                        Toast.makeText(this@UsuariosDisponiblesActivity, "$nombre se ha desconectado", Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                if (id == currentUserId) return // Excluir el usuario actual

                val disponible = snapshot.child("disponible").getValue(Boolean::class.java) ?: false
                if (disponible) {
                    val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario desconocido"
                    val latitud = snapshot.child("latitud").getValue(Double::class.java) ?: 0.0
                    val longitud = snapshot.child("longitud").getValue(Double::class.java) ?: 0.0
                    val imagenBase64 = snapshot.child("profileImage").getValue(String::class.java) ?: ""

                    if (usuarios.none { it.id == id }) {
                        usuarios.add(Usuario(id, nombre, imagenBase64, latitud, longitud))
                        Toast.makeText(this@UsuariosDisponiblesActivity, "$nombre se ha conectado", Toast.LENGTH_SHORT).show()
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("UsuariosDisponibles", "Error al escuchar cambios en disponibilidad", error.toException())
            }
        })
    }
}

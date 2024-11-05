package com.example.taller3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taller3.databinding.ItemUsuarioBinding
import android.graphics.BitmapFactory
import android.util.Base64


data class Usuario(
    val id: String,
    val nombre: String,
    val imagenBase64: String,
    val latitud: Double,
    val longitud: Double
)

class UsuariosAdapter(
    private val usuarios: List<Usuario>,
    private val onVerUbicacionClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuariosAdapter.UsuarioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val binding = ItemUsuarioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UsuarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = usuarios[position]
        holder.bind(usuario)
    }

    override fun getItemCount(): Int = usuarios.size

    inner class UsuarioViewHolder(private val binding: ItemUsuarioBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(usuario: Usuario) {
            binding.textViewNombre.text = usuario.nombre

            // Decodificar imagen Base64 y establecerla en ImageView
            val imageBytes = Base64.decode(usuario.imagenBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imageViewUsuario.setImageBitmap(bitmap)

            binding.buttonVerUbicacion.setOnClickListener {
                onVerUbicacionClick(usuario)
            }
        }
    }
}
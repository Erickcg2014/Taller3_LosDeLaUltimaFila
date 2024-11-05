package com.example.taller3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller3.databinding.ActivityMapaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.json.JSONObject
import java.io.InputStream

class Mapa : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMapaBinding
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var userLocationRef: DatabaseReference
    private lateinit var availabilityStatus: TextView

    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding setup
        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Toolbar
        setSupportActionBar(binding.toolbar)

        // Initialize FirebaseAuth and database reference for user's location
        auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid
        userLocationRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId")

        // Set availability to true upon entering the map
        updateUserAvailability(true)
        availabilityStatus = findViewById(R.id.availabilityStatus)

        // Display initial availability status
        updateAvailabilityStatus(true)

        // Set up listener for changes in other users' availability
        setupAvailabilityListener()

        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        // Setup the MapView
        map = binding.map
        map.setMultiTouchControls(true)

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupMap()
            addPointsOfInterest()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun updateUserAvailability(isAvailable: Boolean) {
        auth.currentUser?.uid?.let { userId ->
            userLocationRef.child("disponible").setValue(isAvailable)
                .addOnSuccessListener {
                    Log.d("Mapa", "Estado de disponible actualizado a $isAvailable exitosamente para el usuario $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("Mapa", "Error al actualizar estado de disponible: ${e.message}")
                }
        }
    }

    private fun updateAvailabilityStatus(isAvailable: Boolean) {
        userLocationRef.child("disponible").setValue(isAvailable)
            .addOnSuccessListener {
                availabilityStatus.text = if (isAvailable) "Activo" else "No disponible"
                Log.d("Mapa", "Estado de disponibilidad actualizado a ${if (isAvailable) "true" else "false"} exitosamente")
            }
            .addOnFailureListener {
                Log.e("Mapa", "Error al actualizar estado de disponibilidad", it)
            }
    }

    private fun setupAvailabilityListener() {
        dbRef.addChildEventListener(object : ChildEventListener {
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val nombre = snapshot.child("nombre").getValue(String::class.java) ?: "Usuario desconocido"
                val disponible = snapshot.child("disponible").getValue(Boolean::class.java) ?: false

                // Show Toast if the user has changed their availability status
                if (disponible) {
                    Toast.makeText(this@Mapa, "$nombre se ha conectado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@Mapa, "$nombre se ha desconectado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("Mapa", "Error al escuchar cambios en disponibilidad", error.toException())
            }
        })
    }

    private fun setupMap() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        map.overlays.add(locationOverlay)
        map.controller.setZoom(15.0)

        locationOverlay.runOnFirstFix {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                updateUserLocationInDatabase(myLocation)
                map.controller.setCenter(myLocation)
            }
        }

        locationOverlay.myLocationProvider.startLocationProvider { location, _ ->
            location?.let {
                val currentLocation = GeoPoint(it.latitude, it.longitude)
                updateUserLocationInDatabase(currentLocation)
            }
        }
    }

    private fun updateUserLocationInDatabase(geoPoint: GeoPoint) {
        userLocationRef.child("latitud").setValue(geoPoint.latitude)
        userLocationRef.child("longitud").setValue(geoPoint.longitude)
    }

    private fun addPointsOfInterest() {
        Log.d("MapaActivity", "Attempting to load locations from JSON")
        try {
            val inputStream: InputStream = assets.open("locations.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val locationsArray = jsonObject.getJSONArray("locationsArray")

            for (i in 0 until locationsArray.length()) {
                val location = locationsArray.getJSONObject(i)
                val geoPoint = GeoPoint(location.getDouble("latitude"), location.getDouble("longitude"))
                val marker = Marker(map)
                marker.position = geoPoint
                marker.title = location.getString("name")
                map.overlays.add(marker)
            }
        } catch (e: Exception) {
            Log.e("MapaActivity", "Error loading locations", e)
            Toast.makeText(this, "Error loading locations", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuCerrarSesion -> {
                updateUserAvailability(false) // Set available to false upon logout
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
                true
            }

            R.id.menuConectarse -> {
                updateAvailabilityStatus(true)
                true
            }
            R.id.menuDesconectarse -> {
                updateAvailabilityStatus(false)
                true
            }
            R.id.usuarioDisponibles -> {
                val intent = Intent(this, UsuariosDisponiblesActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap()
                addPointsOfInterest()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        updateAvailabilityStatus(false)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

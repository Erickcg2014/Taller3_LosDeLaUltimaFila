package com.example.taller3

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.databinding.ActivityMapaUsuarioBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapaUsuarioActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapaUsuarioBinding
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var userLocationRef: DatabaseReference
    private lateinit var observedUserRef: DatabaseReference
    private var observedUserMarker: Marker? = null
    private lateinit var currentUserMarker: Marker
    private lateinit var lineBetweenMarkers: Polyline
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapaUsuarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        map = binding.map
        map.setMultiTouchControls(true)

        val currentUserId = auth.currentUser?.uid ?: return
        userLocationRef = FirebaseDatabase.getInstance().getReference("users/$currentUserId")

        val observedLatitude = intent.getDoubleExtra("latitud", 0.0)
        val observedLongitude = intent.getDoubleExtra("longitud", 0.0)

        setupMap {
            setupObservedUserMarker(GeoPoint(observedLatitude, observedLongitude))
        }
    }

    private fun setupMap(onMapInitialized: () -> Unit) {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)
        map.controller.setZoom(15.0)

        currentUserMarker = Marker(map).apply {
            title = "Tu ubicación"
            map.overlays.add(this)
        }

        lineBetweenMarkers = Polyline().apply {
            map.overlays.add(this)
        }

        locationOverlay.runOnFirstFix {
            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                updateUserLocationInDatabase(myLocation)
                updateCurrentUserMarker(myLocation)
            }
            runOnUiThread { onMapInitialized() }
        }

        locationOverlay.myLocationProvider.startLocationProvider { location, _ ->
            location?.let {
                val myLocation = GeoPoint(it.latitude, it.longitude)
                updateUserLocationInDatabase(myLocation)
                updateCurrentUserMarker(myLocation)
            }
        }
    }

    private fun updateUserLocationInDatabase(geoPoint: GeoPoint) {
        userLocationRef.child("latitud").setValue(geoPoint.latitude)
        userLocationRef.child("longitud").setValue(geoPoint.longitude)
    }

    private fun setupObservedUserMarker(observedUserLocation: GeoPoint) {
        observedUserMarker = Marker(map).apply {
            position = observedUserLocation
            title = "Usuario observado"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(this)
        }

        updateLineBetweenMarkers(locationOverlay.myLocation, observedUserLocation)

        observedUserRef = FirebaseDatabase.getInstance().getReference("users").child(intent.getStringExtra("observedUserId") ?: "")
        observedUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latitud = snapshot.child("latitud").getValue(Double::class.java) ?: return
                val longitud = snapshot.child("longitud").getValue(Double::class.java) ?: return
                val updatedObservedUserLocation = GeoPoint(latitud, longitud)

                runOnUiThread {
                    observedUserMarker?.position = updatedObservedUserLocation
                    updateLineBetweenMarkers(locationOverlay.myLocation, updatedObservedUserLocation)
                    map.invalidate()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MapaUsuarioActivity, "Error al obtener ubicación del usuario", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateCurrentUserMarker(myLocation: GeoPoint) {
        runOnUiThread {
            currentUserMarker.position = myLocation
            map.controller.setCenter(myLocation)
            observedUserMarker?.let { observedMarker ->
                updateLineBetweenMarkers(myLocation, observedMarker.position)
            }
        }
    }

    private fun updateLineBetweenMarkers(myLocation: GeoPoint?, observedUserLocation: GeoPoint) {
        myLocation?.let {
            lineBetweenMarkers.setPoints(listOf(it, observedUserLocation))
            val distance = it.distanceToAsDouble(observedUserLocation)
            Toast.makeText(this, "Distancia: ${"%.2f".format(distance)} m", Toast.LENGTH_SHORT).show()
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
}

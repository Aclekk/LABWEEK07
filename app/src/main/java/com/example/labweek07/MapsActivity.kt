package com.example.lab_week_7

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.lab_week_7.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Started")

        try {
            binding = ActivityMapsBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "onCreate: Binding inflated")

            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment

            if (mapFragment == null) {
                Log.e(TAG, "onCreate: MapFragment is NULL!")
                return
            }

            Log.d(TAG, "onCreate: MapFragment found, requesting map")
            mapFragment.getMapAsync(this)

            requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                Log.d(TAG, "Permission result: $isGranted")
                if (isGranted) {
                    getLastLocation()
                } else {
                    showPermissionRationale {
                        requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                    }
                }
            }

            Log.d(TAG, "onCreate: Finished successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Exception!", e)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: Map is ready!")
        mMap = googleMap

        Log.d(TAG, "onMapReady: Has permission = ${hasLocationPermission()}")

        when {
            hasLocationPermission() -> {
                Log.d(TAG, "onMapReady: Permission already granted")
                getLastLocation()
            }
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                Log.d(TAG, "onMapReady: Showing rationale")
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
            else -> {
                Log.d(TAG, "onMapReady: Requesting permission")
                requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        Log.d(TAG, "showPermissionRationale: Showing dialog")
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app will not work without knowing your current location")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun getLastLocation() {
        Log.d(TAG, "getLastLocation: Called")
        if (hasLocationPermission()) {
            try {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        Log.d(TAG, "getLastLocation: Success - location = $location")
                        location?.let {
                            val userLocation = LatLng(it.latitude, it.longitude)
                            updateMapLocation(userLocation)
                            addMarkerAtLocation(userLocation, "You")
                        } ?: Log.w(TAG, "getLastLocation: Location is null")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "getLastLocation: Failed", e)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "getLastLocation: SecurityException", e)
            }
        } else {
            Log.w(TAG, "getLastLocation: No permission, requesting...")
            requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    private fun updateMapLocation(location: LatLng) {
        Log.d(TAG, "updateMapLocation: $location")
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    private fun addMarkerAtLocation(location: LatLng, title: String) {
        Log.d(TAG, "addMarkerAtLocation: $title at $location")
        mMap.addMarker(MarkerOptions().title(title).position(location))
    }

    companion object {
        private const val TAG = "MapsActivity"
    }
}
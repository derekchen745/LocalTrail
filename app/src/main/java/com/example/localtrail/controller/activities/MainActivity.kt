package com.example.localtrail.controller.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.databinding.ActivityMainBinding
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import androidx.core.app.ActivityCompat
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

class MainActivity : BaseAuthenticatedActivity() {

private lateinit var locationCallback: LocationCallback
private lateinit var binding: ActivityMainBinding
private lateinit var fusedLocationClient: FusedLocationProviderClient

private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (hasLocationPermission()) {
            getLastKnownLocation()
        } else {
            requestLocationPermission()
        }

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.navigation_profile) {
                binding.toolbar.visibility = View.GONE
            } else {
                binding.toolbar.visibility = View.VISIBLE
            }
        }

        navView.setOnItemSelectedListener { item ->
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            when (item.itemId) {
                R.id.navigation_profile -> {
                    navController.popBackStack(R.id.navigation_profile, false)
                    navController.navigate(R.id.navigation_profile)
                    true
                }
                R.id.navigation_home -> {
                    navController.popBackStack(R.id.navigation_home, false)
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_dashboard -> {
                    navController.popBackStack(R.id.navigation_dashboard, false)
                    navController.navigate(R.id.navigation_dashboard)
                    true
                }
                else -> false
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("Location", "Lat: ${location.latitude}, Lng: ${location.longitude}")
                } else {
                    Log.d("Location", "Last location is null, requesting single update")
                    requestSingleLocationUpdate()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Location", "Failed to get last location", e)
            }
    }

    private fun requestSingleLocationUpdate() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMaxUpdates(1)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    Log.d("Location", "Update: Lat: ${location.latitude}, Lng: ${location.longitude}")
                } else {
                    Log.d("Location", "Location update is null")
                }
                // Remove updates after receiving if needed
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        if (hasLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getLastKnownLocation()
            } else {
                Log.d("Location", "Permission denied by user")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            getLastKnownLocation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup if continuous updates were used
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
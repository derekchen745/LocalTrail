package com.example.localtrail.controller.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.example.localtrail.utils.NetworkManager
import com.example.localtrail.utils.TrailRecordingStateListener

class MainActivity : BaseAuthenticatedActivity(), TrailRecordingStateListener {

private lateinit var locationCallback: LocationCallback
private lateinit var binding: ActivityMainBinding
private lateinit var fusedLocationClient: FusedLocationProviderClient
private lateinit var networkManager: NetworkManager
private var isOffline = false
private var isRecordingTrail = false

private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        networkManager = NetworkManager.getInstance(this)

        // Monitor network connectivity
        lifecycleScope.launch {
            networkManager.isOnline.collect { isConnected ->
                isOffline = !isConnected
                updateTabVisibility(isConnected)
                Log.d("MainActivity", "Network status changed: ${if (isConnected) "Online" else "Offline"}")
            }
        }

        if (hasLocationPermission()) {
            getLastKnownLocation()
        } else {
            requestLocationPermission()
        }

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)

        navView.setOnItemSelectedListener { item ->
            // Block navigation to other tabs when offline or recording
            if ((isOffline || isRecordingTrail) && item.itemId != R.id.navigation_home) {
                if (isOffline) {
                    showOfflineDialog()
                } else if (isRecordingTrail) {
                    showRecordingDialog()
                }
                return@setOnItemSelectedListener false
            }
            
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
                R.id.navigation_friends -> {
                    navController.popBackStack(R.id.navigation_friends, false)
                    navController.navigate(R.id.navigation_friends)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateTabVisibility(isConnected: Boolean) {
        val navView = binding.navView
        val menu = navView.menu
        val offlineIndicator = binding.offlineIndicator
        val recordingIndicator = binding.recordingIndicator
        
        // Find all tabs except home
        val socialTab = menu.findItem(R.id.navigation_dashboard)
        val profileTab = menu.findItem(R.id.navigation_profile)
        val friendsTab = menu.findItem(R.id.navigation_friends)
        
        // Show tabs only if connected AND not recording
        val shouldShowTabs = isConnected && !isRecordingTrail
        
        if (shouldShowTabs) {
            // Show all tabs when online and not recording, hide indicators
            socialTab?.isVisible = true
            profileTab?.isVisible = true
            friendsTab?.isVisible = true
            offlineIndicator.visibility = View.GONE
            recordingIndicator.visibility = View.GONE
        } else {
            // Hide other tabs when offline or recording
            socialTab?.isVisible = false
            profileTab?.isVisible = false
            friendsTab?.isVisible = false
            
            // Show appropriate indicator
            if (isRecordingTrail) {
                recordingIndicator.visibility = View.VISIBLE
                offlineIndicator.visibility = View.GONE
            } else if (isOffline) {
                offlineIndicator.visibility = View.VISIBLE
                recordingIndicator.visibility = View.GONE
            } else {
                offlineIndicator.visibility = View.GONE
                recordingIndicator.visibility = View.GONE
            }
            
            // Navigate to home if currently on another tab
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            if (navController.currentDestination?.id != R.id.navigation_home) {
                navController.navigate(R.id.navigation_home)
            }
        }
    }
    
    private fun showOfflineDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("You're currently offline. Only the Home tab is available for recording trails. Please connect to the internet to access other features.")
            .setPositiveButton("Go to Home") { _, _ ->
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                navController.navigate(R.id.navigation_home)
                binding.navView.selectedItemId = R.id.navigation_home
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showRecordingDialog() {
        AlertDialog.Builder(this)
            .setTitle("Trail Recording in Progress")
            .setMessage("You're currently recording a trail. Please finish recording before accessing other features.")
            .setPositiveButton("Continue Recording", null)
            .show()
    }
    
    override fun onTrailRecordingStateChanged(isRecording: Boolean) {
        isRecordingTrail = isRecording
        // Update tab visibility when recording state changes
        updateTabVisibility(networkManager.isOnline.value)
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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
        // Cleanup network callback
        networkManager.unregisterCallback()
    }
}
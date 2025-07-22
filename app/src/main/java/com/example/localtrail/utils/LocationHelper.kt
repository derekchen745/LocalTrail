// ContinuousLocationHelper.kt
package com.example.localtrail.utils

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.*

class ContinuousLocationHelper(
    private val host: Fragment,
    private val onLocation: (Location) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val intervalMillis: Long = 1_000L,      // ideal update interval
    private val minIntervalMillis: Long = 5_00L    // fastest update rate
) {
    private val fusedClient = LocationServices
        .getFusedLocationProviderClient(host.requireContext())

    private var locationCallback: LocationCallback? = null

    // build a continuous‐update request
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        intervalMillis
    )
        .setMinUpdateIntervalMillis(minIntervalMillis)
        .build()

    // registerPermission must happen before the Fragment is STARTED
    private val permissionLauncher = host.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) {
            startLocationUpdates()
        } else {
            onError(SecurityException("Location permission denied"))
        }
    }

    /** Call this to begin: it’ll check/request perms, then start updates. */
    fun ensureLocationUpdates() {
        val ctx = host.requireContext()
        val fine  = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine == PackageManager.PERMISSION_GRANTED ||
            coarse == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        // don’t double‐register
        if (locationCallback != null) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // deliver every new fix
                result.locations.forEach(onLocation)
            }
        }

        fusedClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    /** Call when you want to stop updates (e.g. onPause/onDestroy). */
    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }
}

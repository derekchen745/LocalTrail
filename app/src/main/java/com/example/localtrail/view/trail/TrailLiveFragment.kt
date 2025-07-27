package com.example.localtrail.view.trail

import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.databinding.FragmentTrailRecordingBinding
import com.example.localtrail.model.Trail
import com.example.localtrail.model.TrailLocation
import com.example.localtrail.model.db.AppDatabase
import com.example.localtrail.model.enums.TrailPrivacy
import com.example.localtrail.utils.ContinuousLocationHelper
import com.example.localtrail.utils.SyncManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*


class TrailRecordingFragment : Fragment() {

    private var _binding: FragmentTrailRecordingBinding? = null
    private val binding get() = _binding!!

    // for drawing the live polyline
    private lateinit var lineManager: PolylineAnnotationManager
    private val pathPoints = mutableListOf<Point>()
    private val locationTimestamps = mutableListOf<Long>()
    private var startTime: Long = 0
    private var currentLocation: Location? = null
    private var hasInitializedLocation = false

    // continuous helper (from previous step)
    private lateinit var locHelper: ContinuousLocationHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrailRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Record start time
        startTime = System.currentTimeMillis()

        locHelper = ContinuousLocationHelper(
            host      = this,
            onLocation = ::onNewLocation,
            onError    = { err ->
                Log.e("TrailRecording", "location error", err)
                Toast.makeText(requireContext(), "Location error: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            val annotationPlugin = binding.mapView.annotations
            lineManager = annotationPlugin.createPolylineAnnotationManager()
            
            locHelper.ensureLocationUpdates()
        }

        binding.btnEndTrail.setOnClickListener {
            locHelper.stopLocationUpdates()
            showSaveTrailDialog()
        }

        binding.btnCenterLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
    }

    private fun onNewLocation(loc: Location) {
        currentLocation = loc
        val pt = Point.fromLngLat(loc.longitude, loc.latitude)
        pathPoints.add(pt)
        locationTimestamps.add(System.currentTimeMillis())

        // Center map on first location update to avoid the zoom-out issue
        if (!hasInitializedLocation) {
            binding.mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(pt)
                    .zoom(16.0) // Start with a good zoom level
                    .build()
            )
            hasInitializedLocation = true
            Log.d("TrailRecording", "Initial map center at ${loc.latitude}, ${loc.longitude}")
        }

        // Just redraw the line, don't auto-center the camera after initial setup
        refreshPolyline()
    }

    private fun centerMapOnCurrentLocation() {
        currentLocation?.let { loc ->
            val pt = Point.fromLngLat(loc.longitude, loc.latitude)
            binding.mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(pt)
                    .zoom(16.0) // Slightly closer zoom for centering
                    .build()
            )
        }
    }

    private fun refreshPolyline() {
        if (!::lineManager.isInitialized) {
            Log.w("TrailRecording", "lineManager not yet initialized, skipping polyline refresh")
            return
        }
        
        // clear old
        lineManager.deleteAll()

        if (pathPoints.size < 2) return

        // draw new one
        val options = PolylineAnnotationOptions()
            .withPoints(pathPoints)
            .withLineColor("#6200EE")
            .withLineWidth(4.0)
        lineManager.create(options)
    }

    private fun showSaveTrailDialog() {
        if (pathPoints.size < 2) {
            Toast.makeText(requireContext(), "Trail too short to save", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_trail, null)
        
        val nameEdit = dialogView.findViewById<EditText>(R.id.editTrailName)
        val descriptionEdit = dialogView.findViewById<EditText>(R.id.editTrailDescription)
        val privacySpinner = dialogView.findViewById<Spinner>(R.id.spinnerPrivacy)
        val tagsContainer = dialogView.findViewById<LinearLayout>(R.id.tagsContainer)
        
        // Setup privacy spinner
        val privacyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf("Friends Only", "Public", "Private")
        )
        privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        privacySpinner.adapter = privacyAdapter

        // Setup tags checkboxes
        val availableTags = arrayOf(
            "Challenging", "Easy", "Moderate", "Scenic", "Waterfall", "Mountain",
            "Forest", "Lake", "River", "Historical", "Family-Friendly", "Dog-Friendly",
            "Kid-friendly", "Wheelchair Accessible", "Hiking", "Biking", "Running",
            "Shaded", "Sunny", "Wildlife"
        )
        
        val selectedTags = mutableSetOf<String>()
        
        for (tag in availableTags) {
            val checkBox = CheckBox(requireContext())
            checkBox.text = tag
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedTags.add(tag) else selectedTags.remove(tag)
            }
            tagsContainer.addView(checkBox)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Save Trail")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEdit.text.toString().trim()
                val description = descriptionEdit.text.toString().trim()
                
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a trail name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val privacy = when (privacySpinner.selectedItemPosition) {
                    0 -> TrailPrivacy.FRIENDS_ONLY
                    1 -> TrailPrivacy.PUBLIC
                    2 -> TrailPrivacy.PRIVATE
                    else -> TrailPrivacy.FRIENDS_ONLY
                }

                saveTrailToLocalDatabase(name, description, privacy, selectedTags.toList())
            }
            .setNegativeButton("Cancel") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveTrailToLocalDatabase(name: String, description: String, privacy: TrailPrivacy, tags: List<String>) {
        val user = AccountController.getCurrentUser()
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        lifecycleScope.launch {
            try {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                val distance = calculateDistance()
                val avgSpeed = if (duration > 0) (distance * 3600000.0) / duration else 0.0 // km/hr
                
                // Generate a unique ID for the trail
                val trailId = UUID.randomUUID().toString()
                
                val trail = Trail(
                    id = trailId,
                    userID = user.uid,
                    name = name,
                    location = "Recorded Location", // You might want to reverse geocode this
                    description = description,
                    privacy = privacy,
                    username = user.username,
                    distance = distance,
                    duration = formatDuration(duration),
                    avgSpeed = avgSpeed,
                    tags = tags,
                    createdAt = Date(),
                    isSynced = false // Will be managed by SyncManager
                )

                // Create trail locations
                val trailLocations = pathPoints.mapIndexed { index, point ->
                    TrailLocation(
                        trailId = trailId,
                        latitude = point.latitude(),
                        longitude = point.longitude(),
                        timestamp = if (index < locationTimestamps.size) locationTimestamps[index] else startTime + (index * 1000)
                    )
                }
                
                // Use SyncManager for offline-first saving
                val syncManager = SyncManager.getInstance(requireContext())
                val success = syncManager.saveTrailOfflineFirst(trail, trailLocations)
                
                if (success) {
                    Toast.makeText(requireContext(), "Trail saved! Will sync when online.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error saving trail", Toast.LENGTH_SHORT).show()
                }
                
                parentFragmentManager.popBackStack()
                
            } catch (e: Exception) {
                Log.e("TrailRecording", "Error saving trail", e)
                Toast.makeText(requireContext(), "Error saving trail: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateDistance(): Double {
        if (pathPoints.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 1 until pathPoints.size) {
            val prev = pathPoints[i - 1]
            val curr = pathPoints[i]
            totalDistance += distanceBetween(prev.latitude(), prev.longitude(), curr.latitude(), curr.longitude())
        }
        return totalDistance / 1000.0 // Convert to kilometers
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth's radius in meters
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onStart()
    }

    override fun onPause() {
        locHelper.stopLocationUpdates()
        binding.mapView.onStop()
        super.onPause()
    }

    override fun onStop() {
        // stop your location updates here too
        locHelper.stopLocationUpdates()
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        locHelper.stopLocationUpdates()
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}

package com.example.localtrail.view.home

import android.app.AlertDialog
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.localtrail.databinding.FragmentHomeBinding
import com.example.localtrail.model.Trail
import com.example.localtrail.model.enums.TrailPrivacy
import com.example.localtrail.controller.AccountController
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.localtrail.controller.TrailsController
import com.mapbox.maps.Style
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.geojson.Point
import com.example.localtrail.R
import com.example.localtrail.utils.ContinuousLocationHelper
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationHelper: ContinuousLocationHelper
    private var circleAnnotationManager: com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager? = null
    private var hasInitializedLocation = false
    private var currentLocation: Location? = null
    
    // Trail recording variables
    private var isRecording = false
    private var lineManager: PolylineAnnotationManager? = null
    private val pathPoints = mutableListOf<Point>()
    private val locationTimestamps = mutableListOf<Long>()
    private var startTime: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.buttonInitiateTrail.setOnClickListener {
            startTrailRecording()
        }

        binding.btnEndTrail.setOnClickListener {
            stopTrailRecording()
        }

        binding.btnCenterLocation.setOnClickListener {
            centerMapOnCurrentLocation()
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationHelper = ContinuousLocationHelper(
            host      = this,
            onLocation = { loc ->
                // update your map:
                updateLocationMarker(loc)
            },
            onError    = { err ->
                Toast.makeText(requireContext(), "Location error: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        locationHelper.ensureLocationUpdates()
    }

    private fun updateLocationMarker(loc: Location) {
        // Check if binding is still valid (view not destroyed)
        val currentBinding = _binding ?: return
        
        currentLocation = loc
        val pt = Point.fromLngLat(loc.longitude, loc.latitude)

        // Initialize annotation manager if not already done
        if (circleAnnotationManager == null) {
            circleAnnotationManager = currentBinding.mapView.annotations.createCircleAnnotationManager()
        }

        // Clear previous marker
        circleAnnotationManager?.deleteAll()

        // Add new marker
        circleAnnotationManager?.create(
            CircleAnnotationOptions()
                .withPoint(pt)
                .withCircleRadius(8.0)
                .withCircleColor("#6200EE")
                .withCircleStrokeColor("#FFFFFF")
                .withCircleStrokeWidth(2.0)
        )

        // Only center the map on the first location update
        if (!hasInitializedLocation) {
            currentBinding.mapView.getMapboxMap().setCamera(
                com.mapbox.maps.CameraOptions.Builder()
                    .center(pt)
                    .zoom(14.0)
                    .build()
            )
            hasInitializedLocation = true
            Log.d("HomeFragment", "Initial map center at ${loc.latitude}, ${loc.longitude}")
        } else {
            Log.d("HomeFragment", "Updated location marker at ${loc.latitude}, ${loc.longitude}")
        }

        // If recording, add point to trail
        if (isRecording) {
            pathPoints.add(pt)
            locationTimestamps.add(System.currentTimeMillis())
            refreshTrailLine()
        }
    }

    private fun centerMapOnCurrentLocation() {
        currentLocation?.let { loc ->
            val currentBinding = _binding ?: return
            val pt = Point.fromLngLat(loc.longitude, loc.latitude)
            currentBinding.mapView.getMapboxMap().setCamera(
                com.mapbox.maps.CameraOptions.Builder()
                    .center(pt)
                    .zoom(16.0) // Slightly closer zoom for manual centering
                    .build()
            )
        }
    }

    override fun onDestroyView() {
        locationHelper.stopLocationUpdates()
        _binding?.let { binding ->
            binding.mapView.onStop()
            binding.mapView.onDestroy()
        }
        circleAnnotationManager = null
        lineManager = null
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        locationHelper.ensureLocationUpdates()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    private fun showCreateTrailDialog() {
        val dialogView = layoutInflater.inflate(
            com.example.localtrail.R.layout.dialog_create_trail, null
        )
        val nameEdit = dialogView.findViewById<EditText>(com.example.localtrail.R.id.editTrailName)
        val locationEdit = dialogView.findViewById<EditText>(com.example.localtrail.R.id.editTrailLocation)
        val descriptionEdit = dialogView.findViewById<EditText>(com.example.localtrail.R.id.editTrailDescription)

        AlertDialog.Builder(requireContext())
            .setTitle("Create Trail")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = nameEdit.text.toString().trim()
                val location = locationEdit.text.toString().trim()
                val description = descriptionEdit.text.toString().trim()

                lifecycleScope.launch {
                    val user = AccountController.getUserDetails()
                    user?.let {
                        Log.d("CreateTrail", "Submit clicked - name: '$name', location: '$location', description: '$description', user: $user")

                        if (name.isNotEmpty() && location.isNotEmpty() && description.isNotEmpty()) {
                            try {
                                val trail = Trail(
                                    id = "",
                                    userID = user.uid,
                                    name = name,
                                    location = location,
                                    description = description,
                                    privacy = TrailPrivacy.FRIENDS_ONLY,
                                    username = user.username
                                )
                                Log.d("CreateTrail", "Trail object created: $trail")
                                TrailsController.saveTrail(trail) { success, exception ->
                                    if (success) {
                                        Toast.makeText(requireContext(), "Trail saved!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to save trail: ${exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CreateTrail", "Error creating Trail object", e)
                                Toast.makeText(requireContext(), "Error creating trail object", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.w("CreateTrail", "Missing fields")
                            Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.w("CreateTrail", "User is null")
                        Toast.makeText(requireContext(), "User details could not be fetched", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTrailRecording() {
        isRecording = true
        startTime = System.currentTimeMillis()
        pathPoints.clear()
        locationTimestamps.clear()
        
        // Log current location info for debugging
        currentLocation?.let { loc ->
            Log.d("HomeFragment", "Starting trail recording at location: ${loc.latitude}, ${loc.longitude}")
        }
        
        // Initialize line manager for trail drawing
        if (lineManager == null) {
            lineManager = binding.mapView.annotations.createPolylineAnnotationManager()
        }
        
        // Switch UI to recording mode
        binding.startTrailContainer.visibility = View.GONE
        binding.endTrailContainer.visibility = View.VISIBLE
        
        Log.d("HomeFragment", "Trail recording started")
    }

    private fun stopTrailRecording() {
        isRecording = false
        
        if (pathPoints.size < 2) {
            Toast.makeText(requireContext(), "Trail too short to save", Toast.LENGTH_SHORT).show()
            resetToNormalMode()
            return
        }
        
        showSaveTrailDialog()
    }

    private fun resetToNormalMode() {
        isRecording = false
        pathPoints.clear()
        locationTimestamps.clear()
        lineManager?.deleteAll()
        
        // Switch UI back to normal mode
        binding.startTrailContainer.visibility = View.VISIBLE
        binding.endTrailContainer.visibility = View.GONE
    }

    private fun refreshTrailLine() {
        if (pathPoints.size < 2) return
        
        lineManager?.deleteAll()
        val options = PolylineAnnotationOptions()
            .withPoints(pathPoints)
            .withLineColor("#6200EE")
            .withLineWidth(4.0)
        lineManager?.create(options)
    }

    private fun showSaveTrailDialog() {
        if (pathPoints.size < 2) {
            Toast.makeText(requireContext(), "Trail too short to save", Toast.LENGTH_SHORT).show()
            resetToNormalMode()
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

                saveTrailToDatabase(name, description, privacy, selectedTags.toList())
            }
            .setNegativeButton("Cancel") { _, _ ->
                resetToNormalMode()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveTrailToDatabase(name: String, description: String, privacy: TrailPrivacy, tags: List<String>) {
        val user = AccountController.getCurrentUser()
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            resetToNormalMode()
            return
        }

        Log.d("HomeFragment", "Saving trail with user: uid=${user.uid}, username='${user.username}'")

        lifecycleScope.launch {
            try {
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                val distance = calculateDistance()
                val avgSpeed = if (duration > 0) (distance * 3600000.0) / duration else 0.0 // km/hr
                
                // Generate a unique ID for the trail
                val trailId = UUID.randomUUID().toString()
                
                // Get location name from coordinates
                val locationName = getLocationName()
                Log.d("HomeFragment", "Generated location name: '$locationName'")
                
                val trail = Trail(
                    id = trailId,
                    userID = user.uid,
                    name = name,
                    location = locationName,
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

                Log.d("HomeFragment", "Created trail object: id=$trailId, name='$name', username='${trail.username}', location='${trail.location}'")

                // Create trail locations
                val trailLocations = pathPoints.mapIndexed { index, point ->
                    com.example.localtrail.model.TrailLocation(
                        trailId = trailId,
                        latitude = point.latitude(),
                        longitude = point.longitude(),
                        timestamp = if (index < locationTimestamps.size) locationTimestamps[index] else startTime + (index * 1000)
                    )
                }
                
                // Use SyncManager for offline-first saving
                val syncManager = com.example.localtrail.utils.SyncManager.getInstance(requireContext())
                val success = syncManager.saveTrailOfflineFirst(trail, trailLocations)
                
                if (success) {
                    Toast.makeText(requireContext(), "Trail saved! Will sync when online.", Toast.LENGTH_SHORT).show()
                    Log.d("HomeFragment", "Trail saved successfully to SyncManager")
                } else {
                    Toast.makeText(requireContext(), "Error saving trail", Toast.LENGTH_SHORT).show()
                    Log.e("HomeFragment", "Failed to save trail to SyncManager")
                }
                
                resetToNormalMode()
                
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error saving trail", e)
                Toast.makeText(requireContext(), "Error saving trail: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getLocationName(): String = withContext(Dispatchers.IO) {
        try {
            if (pathPoints.isEmpty()) return@withContext "Unknown Location"
            
            // Use the first point of the trail for location lookup
            val firstPoint = pathPoints.first()
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(firstPoint.latitude(), firstPoint.longitude(), 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Try to get city, or locality, or sub-admin area, or admin area
                return@withContext address.locality 
                    ?: address.subAdminArea 
                    ?: address.adminArea 
                    ?: address.countryName 
                    ?: "Unknown Location"
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error getting location name", e)
        }
        return@withContext "Unknown Location"
    }

    private fun calculateDistance(): Double {
        if (pathPoints.size < 2) return 0.0
        
        var distance = 0.0
        for (i in 1 until pathPoints.size) {
            val startPoint = pathPoints[i - 1]
            val endPoint = pathPoints[i]
            distance += calculateDistanceBetweenPoints(startPoint, endPoint)
        }
        return distance / 1000.0 // Convert to kilometers
    }
    
    private fun calculateDistanceBetweenPoints(point1: Point, point2: Point): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        
        val lat1Rad = Math.toRadians(point1.latitude())
        val lat2Rad = Math.toRadians(point2.latitude())
        val deltaLatRad = Math.toRadians(point2.latitude() - point1.latitude())
        val deltaLonRad = Math.toRadians(point2.longitude() - point1.longitude())
        
        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c // Distance in meters
    }

    private fun formatDuration(durationMillis: Long): String {
        val seconds = (durationMillis / 1000) % 60
        val minutes = (durationMillis / (1000 * 60)) % 60
        val hours = (durationMillis / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
package com.example.localtrail.view.home

import android.app.AlertDialog
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.localtrail.controller.TrailsController
import com.mapbox.maps.Style
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.geojson.Point
import com.example.localtrail.R
import com.example.localtrail.utils.ContinuousLocationHelper
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationHelper: ContinuousLocationHelper

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
            findNavController()
                .navigate(R.id.action_navigation_home_to_trailRecordingFragment)
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationHelper = ContinuousLocationHelper(
            host      = this,
            onLocation = { loc ->
                // update your map:
                centerMapOnLocation(loc)
            },
            onError    = { err ->
                Toast.makeText(requireContext(), "Location error: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        )
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS)
        locationHelper.ensureLocationUpdates()
    }

    private fun centerMapOnLocation(loc: Location) {
        val pt = Point.fromLngLat(loc.longitude, loc.latitude)

        // move camera
        binding.mapView.getMapboxMap().setCamera(
            com.mapbox.maps.CameraOptions.Builder()
                .center(pt)
                .zoom(14.0)
                .build()
        )

        // draw a circle annotation
        val mgr = binding.mapView.annotations.createCircleAnnotationManager()
        mgr.create(
            CircleAnnotationOptions()
                .withPoint(pt)
                .withCircleRadius(8.0)
                .withCircleColor("#3FB1CE")
                .withCircleStrokeColor("#FFFFFF")
                .withCircleStrokeWidth(2.0)
        )

        Log.d("HomeFragment", "Centered map at ${loc.latitude}, ${loc.longitude}")
    }

    override fun onDestroyView() {
        locationHelper.stopLocationUpdates()
        binding.mapView.onStop()
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        locationHelper.ensureLocationUpdates()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
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
                                    privacy = TrailPrivacy.PUBLIC,
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
}
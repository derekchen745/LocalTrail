package com.example.localtrail.view.trail

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.localtrail.databinding.FragmentTrailRecordingBinding
import com.example.localtrail.utils.ContinuousLocationHelper
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager


class TrailRecordingFragment : Fragment() {

    private var _binding: FragmentTrailRecordingBinding? = null
    private val binding get() = _binding!!

    // for drawing the live polyline
    private lateinit var lineManager: PolylineAnnotationManager
    private val pathPoints = mutableListOf<Point>()

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
            // TODO: save `pathPoints` to ViewModel or DB here
            parentFragmentManager.popBackStack()
        }
    }

    private fun onNewLocation(loc: Location) {
        val pt = Point.fromLngLat(loc.longitude, loc.latitude)
        pathPoints.add(pt)

        // update camera to follow
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(pt)
                .build()
        )

        // redraw the line
        refreshPolyline()
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
            .withLineColor("#3FB1CE")
            .withLineWidth(4.0)
        lineManager.create(options)
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

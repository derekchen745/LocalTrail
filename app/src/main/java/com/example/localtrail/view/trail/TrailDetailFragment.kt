package com.example.localtrail.view.trail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.localtrail.R
import com.example.localtrail.model.Trail
import com.example.localtrail.databinding.FragmentTrailDetailBinding

class TrailDetailFragment : Fragment() {
    private var _binding: FragmentTrailDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrailDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get Trail from arguments or intent
        val trail = arguments?.getParcelable<Trail>("trail")
            ?: requireActivity().intent.getParcelableExtra("trail")
        if (trail != null) {
            bindTrail(trail)
        }

        // Back button
        view.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            requireActivity().finish()
        }
    }

    private fun bindTrail(trail: Trail) {
        // Set title
        view?.findViewById<TextView>(R.id.trailNameTextView)?.text = trail.name ?: getString(R.string.trail_detail_title)
        // Placeholder for map is in the layout
        // Set author and date (placeholder for now)
        view?.findViewById<TextView>(R.id.usernameTextView)?.text = trail.username ?: "Unknown"
        view?.findViewById<TextView>(R.id.dateTextView)?.text = "June 15, 2026" // TODO: Use real date if available
        // Set stats (placeholders if missing)
        view?.findViewById<TextView>(R.id.distanceTextView)?.text = trail.distance?.let { "${it}km" } ?: "-"
        view?.findViewById<TextView>(R.id.durationTextView)?.text = trail.duration ?: "-"
        view?.findViewById<TextView>(R.id.elevationTextView)?.text = trail.elevation?.let { "${it}m" } ?: "-"
        view?.findViewById<TextView>(R.id.speedTextView)?.text = trail.avgSpeed?.let { "${it}km/hr" } ?: "-"
        view?.findViewById<TextView>(R.id.effortTextView)?.text = trail.effort ?: "-"
        view?.findViewById<TextView>(R.id.weatherTextView)?.text = trail.weather ?: "-"
        // Tags (if you want to add chips, you can do so here)
        // Notes
        view?.findViewById<TextView>(R.id.notesTextView)?.text = trail.notes ?: ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

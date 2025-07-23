package com.example.localtrail.view.trail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.localtrail.R
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.model.Trail
import com.example.localtrail.model.enums.TrailPrivacy
import com.example.localtrail.databinding.FragmentTrailDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth

class TrailDetailFragment : Fragment() {
    private var _binding: FragmentTrailDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var trail: Trail
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrailDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get trail from arguments or intent
        trail = arguments?.getParcelable("trail")
            ?: requireActivity().intent.getParcelableExtra("trail")
            ?: return

        // Initialize views
        bindTrail(trail)
        
        // Only show and enable tags editing for the trail owner
        val currentUser = auth.currentUser
        val isOwner = currentUser != null && trail.userID == currentUser.uid
        
        if (isOwner) {
            // Show menu button for owner
            binding.menuButton.visibility = View.VISIBLE
            binding.menuButton.setOnClickListener {
                showPrivacyMenu()
            }
            
            // Set click listener for tags button only if user owns the trail
            binding.tagsTextView.setOnClickListener {
                showTagsDialog()
            }
            binding.tagsTextView.isEnabled = true
            binding.tagsTextView.alpha = 1.0f
        } else {
            // Hide menu button for non-owners
            binding.menuButton.visibility = View.GONE
            
            // Disable tags button for non-owners
            binding.tagsTextView.isEnabled = false
            binding.tagsTextView.alpha = 0.5f // Make it look disabled
        }

        // Set click listener for back button
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Initialize tags display
        updateTagsDisplay()
    }

    private fun bindTrail(trail: Trail) {
        // Set title
        view?.findViewById<TextView>(R.id.trailNameTextView)?.text = trail.name ?: getString(R.string.trail_detail_title)
        // Placeholder for map is in the layout
        // Set author and date
        view?.findViewById<TextView>(R.id.usernameTextView)?.text = trail.username ?: "Unknown"
        
        // Format and display the creation date
        val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault())
        view?.findViewById<TextView>(R.id.dateTextView)?.text = dateFormat.format(trail.createdAt)
        
        // Set location
        view?.findViewById<TextView>(R.id.locationTextView)?.text = trail.location ?: "Unknown location"
        
        // Set description with visibility handling
        val descriptionSection = view?.findViewById<LinearLayout>(R.id.descriptionSection)
        val descriptionTextView = view?.findViewById<TextView>(R.id.descriptionTextView)
        if (trail.description.isNullOrBlank()) {
            descriptionSection?.visibility = View.GONE
        } else {
            descriptionSection?.visibility = View.VISIBLE
            descriptionTextView?.text = trail.description
        }
        
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

    private fun updateTagsDisplay() {
        binding.tagsContainer.removeAllViews()
        trail.tags?.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isClickable = false
                setChipBackgroundColorResource(R.color.chip_background)
                setTextColor(ContextCompat.getColor(context, R.color.chip_text))
            }
            binding.tagsContainer.addView(chip)
        }
    }

    private fun showTagsDialog() {
        val tags = arrayOf(
            "Challenging", "Easy", "Moderate",
            "Scenic", "Waterfall", "Mountain",
            "Forest", "Lake", "River",
            "Dog-friendly", "Kid-friendly", "Wheelchair Accessible",
            "Hiking", "Biking", "Running",
            "Shaded", "Sunny", "Wildlife"
        )
        
        val selectedTags = trail.tags?.toMutableList() ?: mutableListOf()
        val checkedItems = tags.map { selectedTags.contains(it) }.toBooleanArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Trail Tags")
            .setMultiChoiceItems(tags, checkedItems) { _, which, isChecked ->
                if (isChecked) {
                    selectedTags.add(tags[which])
                } else {
                    selectedTags.remove(tags[which])
                }
            }
            .setPositiveButton("Save") { _, _ ->
                updateTrailTags(selectedTags)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPrivacyMenu() {
        val popup = PopupMenu(requireContext(), binding.menuButton)
        
        // Add privacy options
        popup.menu.add("Public")
        popup.menu.add("Friends Only")
        popup.menu.add("Private")
        
        // Add current privacy indicator
        val currentPrivacyText = when (trail.privacy) {
            TrailPrivacy.PUBLIC -> "✓ Public"
            TrailPrivacy.FRIENDS_ONLY -> "✓ Friends Only"
            TrailPrivacy.PRIVATE -> "✓ Private"
        }
        
        // Replace current option with checkmark
        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            when (item.title) {
                "Public" -> if (trail.privacy == TrailPrivacy.PUBLIC) item.title = "✓ Public"
                "Friends Only" -> if (trail.privacy == TrailPrivacy.FRIENDS_ONLY) item.title = "✓ Friends Only"
                "Private" -> if (trail.privacy == TrailPrivacy.PRIVATE) item.title = "✓ Private"
            }
        }
        
        popup.setOnMenuItemClickListener { menuItem ->
            val newPrivacy = when (menuItem.title.toString().replace("✓ ", "")) {
                "Public" -> TrailPrivacy.PUBLIC
                "Friends Only" -> TrailPrivacy.FRIENDS_ONLY
                "Private" -> TrailPrivacy.PRIVATE
                else -> return@setOnMenuItemClickListener false
            }
            
            if (newPrivacy != trail.privacy) {
                updateTrailPrivacy(newPrivacy)
            }
            true
        }
        
        popup.show()
    }

    private fun updateTrailPrivacy(newPrivacy: TrailPrivacy) {
        TrailsController.updateTrailPrivacy(trail.id, newPrivacy) { success, exception ->
            if (success) {
                trail.privacy = newPrivacy
                val privacyText = when (newPrivacy) {
                    TrailPrivacy.PUBLIC -> "Public"
                    TrailPrivacy.FRIENDS_ONLY -> "Friends Only"
                    TrailPrivacy.PRIVATE -> "Private"
                }
                Toast.makeText(requireContext(), "Privacy set to $privacyText", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to update privacy: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTrailTags(selectedTags: List<String>) {
        TrailsController.updateTrailTags(trail.id, selectedTags) { success, exception ->
            if (success) {
                // Update the trail object
                trail.tags = selectedTags
                // Update the UI immediately
                updateTagsDisplay()
                // Send result back to calling activity/fragment
                setResult(selectedTags)
                Toast.makeText(requireContext(), "Tags updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to update tags: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setResult(tags: List<String>) {
        // Send result back to parent activity
        requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
            putStringArrayListExtra("updated_tags", ArrayList(tags))
            putExtra("trail_id", trail.id)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

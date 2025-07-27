package com.example.localtrail.view.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.databinding.IncludeSavedTrailsBinding
import com.example.localtrail.model.Trail
import com.example.localtrail.view.trail.TrailDetailActivity
import com.google.android.material.button.MaterialButton

class SavedTrailsTabFragment : Fragment() {
    private var _binding: IncludeSavedTrailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var trailsAdapter: TrailsAdapter
    private val trails = mutableListOf<Trail>()
    private val filteredTrails = mutableListOf<Trail>()
    private val selectedTags = mutableSetOf<String>()
    private val availableTags = arrayOf(
        "Challenging", "Easy", "Moderate",
        "Scenic", "Waterfall", "Mountain",
        "Forest", "Lake", "River",
        "Historical", "Family-Friendly", "Dog-Friendly"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = IncludeSavedTrailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (AccountController.getCurrentUser() == null) {
            showPlaceholder(true)
            return
        }

        setupRecyclerView()
        setupTagFilters()
        loadSavedTrails()

        // Register for activity result
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedTags = result.data?.getStringArrayListExtra("updated_tags")
                val trailId = result.data?.getStringExtra("trail_id")
                val trailUnsaved = result.data?.getBooleanExtra("trail_unsaved", false) ?: false
                
                if (trailUnsaved && trailId != null) {
                    // Remove the trail from our local list and refresh UI
                    removeTrailFromList(trailId)
                } else if (updatedTags != null && trailId != null) {
                    // Update the trail in our list
                    updateTrailTags(trailId, updatedTags)
                }
            }
        }.also { launcher ->
            // Store the launcher for use when starting the detail activity
            this.activityLauncher = launcher
        }
    }

    private fun setupRecyclerView() {
        trailsAdapter = TrailsAdapter { trail ->
            val intent = Intent(requireContext(), TrailDetailActivity::class.java)
            intent.putExtra("trail", trail)
            activityLauncher.launch(intent)
        }
        binding.recyclerViewTrails.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trailsAdapter
        }
    }

    private fun setupTagFilters() {
        binding.buttonTagFilter.setOnClickListener {
            showTagFilterDialog()
        }
        updateFilterButtonText()
    }

    private fun showTagFilterDialog() {
        val checkedItems = availableTags.map { selectedTags.contains(it) }.toBooleanArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Tags")
            .setMultiChoiceItems(availableTags, checkedItems) { _, which, isChecked ->
                val tag = availableTags[which]
                if (isChecked) {
                    selectedTags.add(tag)
                } else {
                    selectedTags.remove(tag)
                }
            }
            .setPositiveButton("Apply") { _, _ ->
                filterTrails()
                updateFilterButtonText()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateFilterButtonText() {
        val count = selectedTags.size
        binding.buttonTagFilter.text = when {
            count == 0 -> "Filters"
            count == 1 -> "1 Filter"
            else -> "$count Filters"
        }
    }

    private fun filterTrails() {
        if (selectedTags.isEmpty()) {
            // If no tags selected, show all trails sorted by newest first
            filteredTrails.clear()
            filteredTrails.addAll(trails.sortedByDescending { it.createdAt })
        } else {
            // Filter trails that have ANY of the selected tags, sorted by newest first
            filteredTrails.clear()
            filteredTrails.addAll(trails.filter { trail ->
                trail.tags?.any { it in selectedTags } == true
            }.sortedByDescending { it.createdAt })
        }
        trailsAdapter.updateTrails(filteredTrails)
    }

    private fun loadSavedTrails() {
        TrailsController.getSavedTrails { loadedTrails, error ->
            if (error != null || loadedTrails.isEmpty()) {
                showPlaceholder(true)
                return@getSavedTrails
            }
            showPlaceholder(false)
            trails.clear()
            trails.addAll(loadedTrails)
            filterTrails() // This will update the adapter with all trails initially
        }
    }

    private fun showPlaceholder(show: Boolean) {
        binding.textSavedTrailsPlaceholder.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewTrails.visibility = if (show) View.GONE else View.VISIBLE
        binding.sortLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateTrailTags(trailId: String, tags: List<String>) {
        val trailIndex = trails.indexOfFirst { it.id == trailId }
        if (trailIndex != -1) {
            trails[trailIndex].tags = tags
            filterTrails() // Reapply filters after updating tags
        }
    }

    private fun removeTrailFromList(trailId: String) {
        val removed = trails.removeAll { it.id == trailId }
        if (removed) {
            filterTrails() // Refresh the filtered list and adapter
            
            // Show placeholder if no trails left
            if (trails.isEmpty()) {
                showPlaceholder(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh trails when returning to this fragment
        // This helps catch any changes that might have been missed
        if (AccountController.getCurrentUser() != null) {
            loadSavedTrails()
        }
    }

    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

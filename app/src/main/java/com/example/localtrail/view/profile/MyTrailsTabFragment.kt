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
import com.example.localtrail.databinding.IncludeMyTrailsBinding
import com.example.localtrail.model.Trail
import com.example.localtrail.view.trail.TrailDetailActivity
import com.google.android.material.button.MaterialButton

class MyTrailsTabFragment : Fragment() {
    private var _binding: IncludeMyTrailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var trailsAdapter: TrailsAdapter
    private val trails = mutableListOf<Trail>()
    private val filteredTrails = mutableListOf<Trail>()
    private val selectedTags = mutableSetOf<String>()

    private val availableTags = arrayOf(
        "Challenging", "Easy", "Moderate",
        "Scenic", "Waterfall", "Mountain",
        "Forest", "Lake", "River",
        "Dog-friendly", "Kid-friendly", "Wheelchair Accessible",
        "Hiking", "Biking", "Running",
        "Shaded", "Sunny", "Wildlife"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = IncludeMyTrailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTagFilters()
        loadTrails()

        // Register for activity result
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedTags = result.data?.getStringArrayListExtra("updated_tags")
                val trailId = result.data?.getStringExtra("trail_id")
                if (updatedTags != null && trailId != null) {
                    // Update the trail in our list
                    updateTrailTags(trailId, updatedTags)
                }
            }
        }.also { launcher ->
            // Store the launcher for use when starting the detail activity
            this.activityLauncher = launcher
        }
    }

    private fun setupTagFilters() {
        binding.filterButton.setOnClickListener {
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
        binding.filterButton.text = when {
            count == 0 -> "Filters"
            count == 1 -> "1 Filter"
            else -> "$count Filters"
        }
    }

    private fun filterTrails() {
        if (selectedTags.isEmpty()) {
            // If no tags selected, show all trails
            filteredTrails.clear()
            filteredTrails.addAll(trails)
        } else {
            // Filter trails that have ANY of the selected tags
            filteredTrails.clear()
            filteredTrails.addAll(trails.filter { trail ->
                trail.tags?.any { it in selectedTags } == true
            })
        }
        trailsAdapter.updateTrails(filteredTrails)
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
            adapter = trailsAdapter // Set adapter here
        }
    }

    private fun loadTrails() {
        val user = AccountController.getCurrentUser() ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.textNoTrails.visibility = View.GONE
        TrailsController.fetchUserTrails(user.uid) { fetchedTrails ->
            requireActivity().runOnUiThread {
                binding.progressBar.visibility = View.GONE
                trails.clear()
                trails.addAll(fetchedTrails)
                
                if (trails.isEmpty()) {
                    binding.textNoTrails.visibility = View.VISIBLE
                } else {
                    binding.textNoTrails.visibility = View.GONE
                }
                
                filterTrails() // Apply any existing filters
            }
        }
    }

    private fun updateTrailTags(trailId: String, tags: List<String>) {
        val trailIndex = trails.indexOfFirst { it.id == trailId }
        if (trailIndex != -1) {
            trails[trailIndex].tags = tags
            filterTrails() // Reapply filters after updating tags
        }
    }

    private lateinit var activityLauncher: ActivityResultLauncher<Intent>

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

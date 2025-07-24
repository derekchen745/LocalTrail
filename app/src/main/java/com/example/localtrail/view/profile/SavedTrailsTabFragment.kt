package com.example.localtrail.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.TrailsController
import com.example.localtrail.databinding.IncludeSavedTrailsBinding
import com.example.localtrail.model.Trail
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
    }

    private fun setupRecyclerView() {
        trailsAdapter = TrailsAdapter { trail ->
            val intent = Intent(requireContext(), com.example.localtrail.view.trail.TrailDetailActivity::class.java)
            intent.putExtra("trail", trail)
            startActivity(intent)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

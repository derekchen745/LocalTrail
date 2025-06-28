package com.example.localtrail.view.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.controller.AccountController
import com.example.localtrail.databinding.IncludeSavedTrailsBinding

class SavedTrailsTabFragment : Fragment() {
    private var _binding: IncludeSavedTrailsBinding? = null
    private val binding get() = _binding!!

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

        AccountController.getSavedTrails { trails, error ->
            if (error != null || trails.isEmpty()) {
                showPlaceholder(true)
                return@getSavedTrails
            }
            showPlaceholder(false)
            val adapter = TrailsAdapter(trails) { trail ->
                val intent = Intent(requireContext(), com.example.localtrail.view.trail.TrailDetailActivity::class.java)
                intent.putExtra("trail", trail)
                startActivity(intent)
            }
            binding.recyclerViewTrails.adapter = adapter
            binding.recyclerViewTrails.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showPlaceholder(show: Boolean) {
        binding.textSavedTrailsPlaceholder.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewTrails.visibility = if (show) View.GONE else View.VISIBLE
        binding.chipGroupFilters.visibility = if (show) View.GONE else View.VISIBLE
        binding.sortLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

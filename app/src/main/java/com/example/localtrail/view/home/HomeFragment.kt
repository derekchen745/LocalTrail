package com.example.localtrail.view.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.localtrail.databinding.FragmentHomeBinding
import com.example.localtrail.model.Trail
import com.example.localtrail.model.enums.TrailPrivacy
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.TrailsController
import com.mapbox.maps.Style
import com.mapbox.maps.MapView
import com.example.localtrail.R

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        binding.buttonInitiateTrail.setOnClickListener {
            showCreateTrailDialog()
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.getMapboxMap().loadStyleUri(com.mapbox.maps.Style.MAPBOX_STREETS)
    }

    override fun onDestroyView() {
        binding.mapView.onStop()
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
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
                val user = AccountController.getCurrentUser()

                Log.d("CreateTrail", "Submit clicked - name: '$name', location: '$location', description: '$description', user: $user")

                if (user != null && name.isNotEmpty() && location.isNotEmpty() && description.isNotEmpty()) {
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
                    Log.w("CreateTrail", "Missing fields or user is null")
                    Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
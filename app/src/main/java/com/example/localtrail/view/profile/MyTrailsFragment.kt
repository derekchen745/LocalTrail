package com.example.localtrail.view.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.databinding.FragmentMyTrailsBinding
import com.example.localtrail.model.Trail
import com.example.localtrail.view.profile.TrailsAdapter

class MyTrailsFragment : Fragment() {
    private var _binding: FragmentMyTrailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTrailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d("MyTrailsFragment", "Back button pressed in MyTrailsFragment")
                    findNavController().navigate(
                        R.id.navigation_profile,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.navigation_profile, false)
                            .build()
                    )
                }
            }
        )
        // Example data, replace with real data source
        val trails = listOf(
            Trail(1, "Trail 1", "Location 1", "Description 1"),
            Trail(2, "Trail 2", "Location 2", "Description 2")
        )
        val adapter = TrailsAdapter(trails)
        binding.recyclerViewTrails.adapter = adapter
        binding.recyclerViewTrails.layoutManager = LinearLayoutManager(requireContext())

        binding.buttonBackToProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

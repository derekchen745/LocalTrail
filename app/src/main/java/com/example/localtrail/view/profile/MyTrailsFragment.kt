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
import com.example.localtrail.controller.TrailsController

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

        binding.buttonBackToProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }

        val user = com.example.localtrail.controller.AccountController.getCurrentUser()
        if (user != null) {
            Log.d("MyTrailsFragment", "Current userId: ${user.uid}")
            TrailsController.fetchUserTrails(user.uid) { trails ->
                Log.d("MyTrailsFragment", "Fetched trails: $trails")
                val adapter = TrailsAdapter(trails)
                binding.recyclerViewTrails.adapter = adapter
                binding.recyclerViewTrails.layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

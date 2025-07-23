package com.example.localtrail.view.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.databinding.FragmentFriendsBinding

class FriendsFragment : Fragment() {
    
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadFriends()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewFriends.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClickListeners() {
        binding.buttonFriendRequests.setOnClickListener {
            findNavController().navigate(R.id.action_friendsFragment_to_friendRequestsFragment)
        }

        binding.buttonAddFriends.setOnClickListener {
            findNavController().navigate(R.id.action_friendsFragment_to_addFriendsFragment)
        }
    }

    private fun loadFriends() {
        FriendsController.getFriends { friends, exception ->
            if (exception != null) {
                // Handle error
                return@getFriends
            }

            val mutableFriends = (friends ?: emptyList()).toMutableList()
            val adapter = FriendsAdapter(mutableFriends)
            binding.recyclerViewFriends.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

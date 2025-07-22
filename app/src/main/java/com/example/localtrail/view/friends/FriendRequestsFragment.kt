package com.example.localtrail.view.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.databinding.FragmentFriendRequestsBinding

class FriendRequestsFragment : Fragment() {

    private var _binding: FragmentFriendRequestsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFriendRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        loadFriendRequests()
    }

    private fun setupUI() {
        binding.title.text = "Friend Requests"

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadFriendRequests() {
        // One-time migration to clear old format friend requests
        val sharedPrefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val migrationDone = sharedPrefs.getBoolean("friend_requests_migration_done", false)
        
        if (!migrationDone) {
            FriendsController.clearAllFriendRequests { success, _ ->
                if (success) {
                    sharedPrefs.edit().putBoolean("friend_requests_migration_done", true).apply()
                }
                // Continue loading friend requests regardless of migration result
                loadFriendRequestsActual()
            }
        } else {
            loadFriendRequestsActual()
        }
    }
    
    private fun loadFriendRequestsActual() {
        FriendsController.getFriendRequests { requests, exception ->
            if (exception != null) {
                Toast.makeText(requireContext(), "Failed to load friend requests", Toast.LENGTH_SHORT).show()
                adapter = FriendRequestsAdapter(mutableListOf()) { _, _ -> }
                binding.recyclerView.adapter = adapter
                return@getFriendRequests
            }

            val validRequests = requests?.toMutableList() ?: mutableListOf()

            adapter = FriendRequestsAdapter(validRequests) { friendRequest, action ->
                when (action) {
                    FriendRequestsAdapter.Action.ACCEPT -> {
                        FriendsController.acceptFriendRequest(friendRequest.userId) { success, _ ->
                            if (success) {
                                adapter.removeRequest(friendRequest)
                                Toast.makeText(requireContext(), "Friend request accepted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to accept friend request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    FriendRequestsAdapter.Action.DENY -> {
                        FriendsController.denyFriendRequest(friendRequest.userId) { success, _ ->
                            if (success) {
                                adapter.removeRequest(friendRequest)
                                Toast.makeText(requireContext(), "Friend request denied", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to deny friend request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.localtrail.view.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.databinding.FragmentAddFriendsBinding

class AddFriendsFragment : Fragment() {

    private var _binding: FragmentAddFriendsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupUserInfo()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        val currentUser = AccountController.getCurrentUser()
        val userId = currentUser?.uid ?: "Unknown User"
        
        binding.buttonCopyUserId.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("User ID", userId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSendFriendRequest.setOnClickListener {
            val friendId = binding.editTextFriendId.text.toString()
            if (friendId.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a User ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FriendsController.sendFriendRequest(friendId) { success, exception ->
                if (success) {
                    Toast.makeText(requireContext(), "Friend request sent successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to send friend request: ${exception?.message ?: "User not found"}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupUserInfo() {
        val currentUser = AccountController.getCurrentUser()
        val userId = currentUser?.uid ?: "Unknown User"
        binding.textViewUserId.text = userId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

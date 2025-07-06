package com.example.localtrail.controller.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.localtrail.R
import com.example.localtrail.controller.AccountController
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.databinding.ActivityAddFriendsBinding

class AddFriendsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddFriendsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFriendsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val currentUser = AccountController.getCurrentUser()
        val userId = currentUser?.uid ?: "Unknown User"
        binding.buttonCopyUserId.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("User ID", userId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "User ID copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSendFriendRequest.setOnClickListener {
            val friendId = binding.editTextFriendId.text.toString()
            if (friendId.isBlank()) {
                Toast.makeText(this, "Please enter a User ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FriendsController.sendFriendRequest(friendId) { success, exception ->
                if (success) {
                    Toast.makeText(this, "Friend request sent successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send friend request: ${exception?.message ?: "User not found"}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.textViewUserId.text = userId
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

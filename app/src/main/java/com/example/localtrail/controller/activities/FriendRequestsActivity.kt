package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localtrail.R
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.databinding.ActivityFriendRequestsBinding
import com.example.localtrail.view.friends.FriendRequestsAdapter

class FriendRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFriendRequestsBinding
    private lateinit var adapter: FriendRequestsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.title.text = "Friend Requests"

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        FriendsController.getFriendRequests { requests, exception ->
            if (exception != null) {
                Toast.makeText(this, "Failed to load friend requests", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to accept friend request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    FriendRequestsAdapter.Action.DENY -> {
                        FriendsController.denyFriendRequest(friendRequest.userId) { success, _ ->
                            if (success) {
                                adapter.removeRequest(friendRequest)
                                Toast.makeText(this, "Friend request denied", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to deny friend request", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = adapter
        }
    }
}

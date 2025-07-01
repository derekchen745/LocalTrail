package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.controller.FriendsController
import com.example.localtrail.view.friends.FriendsAdapter
import com.google.android.material.button.MaterialButton

class FriendsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewFriends)
        recyclerView.layoutManager = LinearLayoutManager(this)

        FriendsController.getFriends { friends, exception ->
            if (exception != null) {
                // Handle error
                return@getFriends
            }

            val adapter = FriendsAdapter(friends ?: emptyList())
            recyclerView.adapter = adapter
        }

        val materialFriendRequestsButton = findViewById<MaterialButton>(R.id.buttonFriendRequests)
        materialFriendRequestsButton.setOnClickListener {
            val intent = Intent(this, FriendRequestsActivity::class.java)
            startActivity(intent)
        }
    }
}

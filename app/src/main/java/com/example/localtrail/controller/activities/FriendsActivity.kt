package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.localtrail.R
import com.google.android.material.button.MaterialButton

class FriendsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val listView = findViewById<ListView>(R.id.listViewFriends)
        val placeholderFriends = listOf(
            "Alice Johnson",
            "Bob Smith",
            "Charlie Lee",
            "Diana Patel",
            "Evan Kim"
        )
        listView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            placeholderFriends
        )

        val materialFriendRequestsButton = findViewById<MaterialButton>(R.id.buttonFriendRequests)
        materialFriendRequestsButton.setOnClickListener {
            val intent = Intent(this, FriendRequestsActivity::class.java)
            startActivity(intent)
        }
    }
}

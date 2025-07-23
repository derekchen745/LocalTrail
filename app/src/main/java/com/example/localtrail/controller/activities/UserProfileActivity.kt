package com.example.localtrail.controller.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.localtrail.R
import com.example.localtrail.model.Trail
import com.example.localtrail.view.profile.TrailsAdapter
import com.example.localtrail.view.trail.TrailDetailActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot

class UserProfileActivity : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var avatarImageView: ImageView
    private lateinit var friendsTextView: TextView
    private lateinit var trailsRecyclerView: RecyclerView

    private val trailList = mutableListOf<Trail>()
    private lateinit var adapter: TrailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Initialize views
        usernameTextView = findViewById(R.id.textViewProfileUsername)
        bioTextView = findViewById(R.id.textViewProfileBio)
        avatarImageView = findViewById(R.id.imageProfileAvatar)
        friendsTextView = findViewById(R.id.textViewProfileFriends)
        trailsRecyclerView = findViewById(R.id.recyclerViewUserTrails)

        val userId = intent.getStringExtra("USER_ID")
        setupRecyclerView()

        if (userId != null) {
            loadUserProfile(userId)
            loadUserTrails(userId)
        } else {
            usernameTextView.text = "User not found"
        }
    }

    private fun setupRecyclerView() {
        adapter = TrailsAdapter(trailList) { selectedTrail ->
            val intent = Intent(this, TrailDetailActivity::class.java)
            intent.putExtra("trail", selectedTrail) // Trail must be Parcelable
            startActivity(intent)
        }
        trailsRecyclerView.layoutManager = LinearLayoutManager(this)
        trailsRecyclerView.adapter = adapter
    }

    private fun loadUserProfile(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document: DocumentSnapshot ->
                if (document.exists()) {
                    usernameTextView.text = document.getString("username") ?: "No username"
                    bioTextView.text = document.getString("description") ?: "No description"
                    val friends = document.get("friends") as? List<*> ?: emptyList<Any>()
                    friendsTextView.text = "${friends.size} Friends"
                } else {
                    usernameTextView.text = "User not found"
                }
            }
            .addOnFailureListener {
                usernameTextView.text = "Error loading profile"
            }
    }

    private fun loadUserTrails(userId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("trails")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val trails = querySnapshot.documents.mapNotNull {
                    it.toObject(com.example.localtrail.model.Trail::class.java)
                }
                trailList.clear()
                trailList.addAll(trails)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Optional: show error
            }
    }
}

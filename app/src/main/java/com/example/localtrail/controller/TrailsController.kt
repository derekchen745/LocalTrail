package com.example.localtrail.controller

import com.example.localtrail.model.Trail
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object TrailsController {
    init {
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    fun fetchUserTrails(userId: String, onResult: (List<Trail>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("trails")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val trails = querySnapshot.map { doc ->
                    val trail = doc.toObject(Trail::class.java)
                    trail.id = doc.id
                    trail
                }
                onResult(trails)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun fetchOtherUsersTrails(currentUserId: String, onResult: (List<Trail>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("trails")
            .whereNotEqualTo("userID", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val trails = querySnapshot.map { doc ->
                    val trail = doc.toObject(Trail::class.java)
                    trail.id = doc.id
                    trail
                }
                onResult(trails)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun saveTrail(trail: Trail, onResult: (Boolean, Exception?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val trailsCollection = db.collection("trails")
        val data = hashMapOf(
            "name" to trail.name,
            "location" to trail.location,
            "description" to trail.description,
            "userID" to trail.userID, 
            "privacy" to trail.privacy.name,
            "username" to trail.username 
        )
        if (trail.id.isNotEmpty()) {
            trailsCollection.document(trail.id).set(data)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e) }
        } else {
            trailsCollection.add(data)
                .addOnSuccessListener { onResult(true, null) }
                .addOnFailureListener { e -> onResult(false, e) }
        }
    }
}

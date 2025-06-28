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

    /**
     * Fetches trails by their IDs
     * @param trailIds List of trail IDs to fetch
     * @param onResult Callback with the list of fetched trails
     */
    fun fetchTrailsByIds(trailIds: List<String>, onResult: (List<Trail>) -> Unit) {
        if (trailIds.isEmpty()) {
            onResult(emptyList())
            return
        }
        
        val db = FirebaseFirestore.getInstance()
        val trailsCollection = db.collection("trails")
        
        val batches = trailIds.chunked(10)
        val allTrails = mutableListOf<Trail>()
        var completedBatches = 0
        
        for (batch in batches) {
            trailsCollection.whereIn("__name__", batch)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val trails = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val trail = doc.toObject(Trail::class.java)
                            trail?.id = doc.id
                            trail
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    synchronized(allTrails) {
                        allTrails.addAll(trails)
                        completedBatches++
                        
                        if (completedBatches == batches.size) {
                            onResult(allTrails)
                        }
                    }
                }
                .addOnFailureListener {
                    synchronized(allTrails) {
                        completedBatches++
                        if (completedBatches == batches.size) {
                            onResult(allTrails)
                        }
                    }
                }
        }
    }
}

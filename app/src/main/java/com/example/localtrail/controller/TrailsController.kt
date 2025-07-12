package com.example.localtrail.controller

import android.util.Log
import com.example.localtrail.App
import com.example.localtrail.model.Trail
import com.example.localtrail.model.SavedTrail
import com.example.localtrail.model.db.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val localDb = AppDatabase.getInstance(App.context)
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
                .addOnSuccessListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        localDb.trailDao().insert(trail)
                    }
                    onResult(true, null)
                }
                .addOnFailureListener { e -> onResult(false, e) }
        } else {
            trailsCollection.add(data)
                .addOnSuccessListener { docRef ->
                    val updatedTrail = trail.copy(id = docRef.id)
                    CoroutineScope(Dispatchers.IO).launch {
                        localDb.trailDao().insert(updatedTrail)
                    }
                    onResult(true, null)
                }
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

    fun isTrailSavedByUser(trailId: String, onResult: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false)
            return
        }
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val savedTrails = document.get("savedTrails") as? List<Map<String, Any>> ?: listOf()
                        val isSaved = savedTrails.any { (it["id"] as? String) == trailId }
                        onResult(isSaved)
                    } catch (e: Exception) {
                        onResult(false)
                    }
                } else {
                    onResult(false)
                }
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun saveTrailToUser(trail: Trail, onResult: (Boolean, Exception?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false, Exception("User not logged in"))
            return
        }
        val userRef = db.collection("users").document(currentUser.uid)
        val savedTrail = SavedTrail(
            id = trail.id,
            name = trail.name,
            location = trail.location,
            userID = trail.userID,
            username = trail.username,
            savedAt = java.util.Date()
        )
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val savedTrails = try {
                snapshot.get("savedTrails") as? List<Map<String, Any>> ?: listOf()
            } catch (e: Exception) {
                listOf<Map<String, Any>>()
            }
            if (savedTrails.none { (it["id"] as? String) == trail.id }) {
                val savedTrailMap = mapOf(
                    "id" to savedTrail.id,
                    "name" to savedTrail.name,
                    "location" to savedTrail.location,
                    "userID" to savedTrail.userID,
                    "username" to savedTrail.username,
                    "savedAt" to savedTrail.savedAt
                )
                transaction.update(userRef, "savedTrails", savedTrails + savedTrailMap)
            }
        }.addOnSuccessListener {
            onResult(true, null)
        }.addOnFailureListener { e ->
            onResult(false, e)
        }
    }

    fun removeTrailFromUser(trailId: String, onResult: (Boolean, Exception?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false, Exception("User not logged in"))
            return
        }
        val userRef = db.collection("users").document(currentUser.uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            try {
                val savedTrails = snapshot.get("savedTrails") as? List<Map<String, Any>> ?: listOf()
                val updatedTrails = savedTrails.filterNot { (it["id"] as? String) == trailId }
                if (updatedTrails.size < savedTrails.size) {
                    transaction.update(userRef, "savedTrails", updatedTrails)
                }
            } catch (e: Exception) {
                throw Exception("Error processing saved trails: "+e.message)
            }
        }.addOnSuccessListener {
            onResult(true, null)
        }.addOnFailureListener { e ->
            onResult(false, e)
        }
    }

    fun getSavedTrails(onResult: (List<Trail>, Exception?) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(emptyList(), Exception("User not logged in"))
            return
        }
        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val savedTrailsData = document.get("savedTrails") as? List<Map<String, Any>> ?: listOf()
                    val trails = savedTrailsData.mapNotNull { data ->
                        try {
                            val trail = Trail(
                                id = data["id"] as? String ?: "",
                                userID = data["userID"] as? String ?: "",
                                name = data["name"] as? String,
                                location = data["location"] as? String,
                                username = data["username"] as? String ?: ""
                            )
                            val savedAt = data["savedAt"] as? Timestamp
                            Pair(trail, savedAt)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val sortedTrails = trails.sortedByDescending { it.second }
                                            .map { it.first }
                    onResult(sortedTrails, null)
                } else {
                    onResult(emptyList(), null)
                }
            }
            .addOnFailureListener { exception ->
                onResult(emptyList(), exception)
            }
    }
}

package com.example.localtrail.controller

import android.util.Log
import com.example.localtrail.App
import com.example.localtrail.model.Trail
import com.example.localtrail.model.SavedTrail
import com.example.localtrail.model.db.AppDatabase
import com.example.localtrail.model.enums.TrailPrivacy
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

    fun fetchUserTrails(userId: String, selectedTags: List<String>? = null, onResult: (List<Trail>) -> Unit) {
        // Log current auth state and user details
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d("TrailsController", "Current Firebase Auth User: ${currentUser?.uid}")
        Log.d("TrailsController", "Current Firebase Auth Email: ${currentUser?.email}")
        Log.d("TrailsController", "Requested userId: $userId")
        Log.d("TrailsController", "Selected tags: $selectedTags")
        
        if (userId.isEmpty()) {
            Log.e("TrailsController", "UserId is empty!")
            onResult(emptyList())
            return
        }

        val db = FirebaseFirestore.getInstance()
        
        // First, let's check if there are any trails in the collection
        db.collection("trails")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("TrailsController", "Trails collection exists: ${snapshot.metadata.isFromCache}")
                Log.d("TrailsController", "Total trails in collection: ${snapshot.size()}")
            }

        // Now query for user's trails
        db.collection("trails")
            .whereEqualTo("userID", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("TrailsController", "Query snapshot size: ${querySnapshot.size()}")
                Log.d("TrailsController", "Query snapshot empty: ${querySnapshot.isEmpty}")
                Log.d("TrailsController", "Query source: ${if (querySnapshot.metadata.isFromCache) "cache" else "server"}")
                
                if (querySnapshot.isEmpty) {
                    Log.d("TrailsController", "No trails found for user $userId")
                    // Let's check what trails exist in the collection
                    db.collection("trails")
                        .get()
                        .addOnSuccessListener { allTrails ->
                            Log.d("TrailsController", "Total trails in collection: ${allTrails.size()}")
                            allTrails.documents.forEach { doc ->
                                Log.d("TrailsController", "Trail document: id=${doc.id}, userID=${doc.getString("userID")}")
                            }
                        }
                    onResult(emptyList())
                    return@addOnSuccessListener
                }
                
                val trails = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        Log.d("TrailsController", "Processing document ${doc.id}")
                        Log.d("TrailsController", "Document data: $data")
                        Log.d("TrailsController", "Document userID: ${data?.get("userID")}")
                        
                        if (data != null) {
                            val tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }
                            Log.d("TrailsController", "Document ${doc.id} tags: $tags")
                            
                            Trail(
                                id = doc.id,
                                name = data["name"] as? String,
                                location = data["location"] as? String,
                                description = data["description"] as? String,
                                userID = data["userID"] as? String ?: "",
                                privacy = try {
                                    TrailPrivacy.valueOf(data["privacy"] as? String ?: "PUBLIC")
                                } catch (e: Exception) {
                                    TrailPrivacy.PUBLIC
                                },
                                username = data["username"] as? String ?: "",
                                distance = (data["distance"] as? Number)?.toDouble(),
                                duration = data["duration"] as? String,
                                elevation = (data["elevation"] as? Number)?.toInt(),
                                avgSpeed = (data["avgSpeed"] as? Number)?.toDouble(),
                                effort = data["effort"] as? String,
                                weather = data["weather"] as? String,
                                tags = tags,
                                notes = data["notes"] as? String
                            ).also { trail ->
                                Log.d("TrailsController", "Created Trail object: id=${trail.id}, name=${trail.name}, userID=${trail.userID}")
                            }
                        } else {
                            Log.w("TrailsController", "Document ${doc.id} has null data")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("TrailsController", "Error converting document ${doc.id}", e)
                        Log.e("TrailsController", "Exception details: ${e.message}", e)
                        null
                    }
                }
                
                Log.d("TrailsController", "Total trails found: ${trails.size}")
                
                // Filter trails by tags if selectedTags is provided and not empty
                val filteredTrails = if (selectedTags.isNullOrEmpty()) {
                    trails.also { Log.d("TrailsController", "No tag filtering applied") }
                } else {
                    trails.filter { trail ->
                        val hasMatchingTag = trail.tags?.any { tag -> selectedTags.contains(tag) } == true
                        Log.d("TrailsController", "Trail ${trail.id} (${trail.name}) has tags: ${trail.tags}, matches filter: $hasMatchingTag")
                        hasMatchingTag
                    }
                }
                
                Log.d("TrailsController", "Final filtered trails count: ${filteredTrails.size}")
                onResult(filteredTrails)
            }
            .addOnFailureListener { e ->
                Log.e("TrailsController", "Error fetching user trails", e)
                Log.e("TrailsController", "Exception details: ${e.message}", e)
                onResult(emptyList())
            }
    }

    fun fetchOtherUsersTrails(currentUserId: String, onResult: (List<Trail>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("trails")
            .whereNotEqualTo("userID", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val trails = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            Trail(
                                id = doc.id,
                                name = data["name"] as? String,
                                location = data["location"] as? String,
                                description = data["description"] as? String,
                                userID = data["userID"] as? String ?: "",
                                privacy = try {
                                    TrailPrivacy.valueOf(data["privacy"] as? String ?: "PUBLIC")
                                } catch (e: Exception) {
                                    TrailPrivacy.PUBLIC
                                },
                                username = data["username"] as? String ?: "",
                                distance = (data["distance"] as? Number)?.toDouble(),
                                duration = data["duration"] as? String,
                                elevation = (data["elevation"] as? Number)?.toInt(),
                                avgSpeed = (data["avgSpeed"] as? Number)?.toDouble(),
                                effort = data["effort"] as? String,
                                weather = data["weather"] as? String,
                                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String },
                                notes = data["notes"] as? String
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e("TrailsController", "Error converting document ${doc.id}", e)
                        null
                    }
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

        // Log the trail data being saved
        Log.d("TrailsController", "Saving trail: id=${trail.id}, name=${trail.name}")
        Log.d("TrailsController", "Trail userID=${trail.userID}")
        
        val data = hashMapOf(
            "name" to trail.name,
            "location" to trail.location,
            "description" to trail.description,
            "userID" to trail.userID,
            "privacy" to trail.privacy.name,
            "username" to trail.username,
            "distance" to trail.distance,
            "duration" to trail.duration,
            "elevation" to trail.elevation,
            "avgSpeed" to trail.avgSpeed,
            "effort" to trail.effort,
            "weather" to trail.weather,
            "tags" to trail.tags,
            "notes" to trail.notes
        )

        Log.d("TrailsController", "Saving trail data: $data")

        if (trail.id.isNotEmpty()) {
            trailsCollection.document(trail.id).set(data)
                .addOnSuccessListener {
                    Log.d("TrailsController", "Successfully saved trail with id: ${trail.id}")
                    CoroutineScope(Dispatchers.IO).launch {
                        localDb.trailDao().insert(trail)
                    }
                    onResult(true, null)
                }
                .addOnFailureListener { e -> 
                    Log.e("TrailsController", "Failed to save trail", e)
                    onResult(false, e) 
                }
        } else {
            trailsCollection.add(data)
                .addOnSuccessListener { docRef ->
                    Log.d("TrailsController", "Successfully added new trail with id: ${docRef.id}")
                    val updatedTrail = trail.copy(id = docRef.id)
                    CoroutineScope(Dispatchers.IO).launch {
                        localDb.trailDao().insert(updatedTrail)
                    }
                    onResult(true, null)
                }
                .addOnFailureListener { e -> 
                    Log.e("TrailsController", "Failed to add new trail", e)
                    onResult(false, e) 
                }
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

    fun updateTrailTags(trailId: String, tags: List<String>, onResult: (Boolean, Exception?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("trails")
            .document(trailId)
            .update("tags", tags)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e)
            }
    }
}

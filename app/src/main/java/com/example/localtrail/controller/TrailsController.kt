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
import java.util.Date
import com.example.localtrail.utils.SyncManager
import com.example.localtrail.utils.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object TrailsController {
    init {
        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
    }

    /**
     * Fetches trails using offline-first approach
     * Returns local trails immediately, then syncs with remote data
     */
    fun fetchUserTrailsOfflineFirst(userId: String, selectedTags: List<String>? = null, onResult: (List<Trail>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val networkManager = NetworkManager.getInstance(App.context)
                val syncManager = SyncManager.getInstance(App.context)
                
                // First, get local trails immediately
                val localTrails = syncManager.getLocalTrails(userId)
                
                // Filter by tags if specified
                val filteredLocalTrails = if (selectedTags.isNullOrEmpty()) {
                    localTrails
                } else {
                    localTrails.filter { trail ->
                        trail.tags?.any { tag -> selectedTags.contains(tag) } == true
                    }
                }
                
                // Return local data immediately
                onResult(filteredLocalTrails)
                
                // If online, also fetch remote data to get any trails not yet downloaded
                if (networkManager.isOnline.first()) {
                    fetchUserTrails(userId, selectedTags) { remoteTrails ->
                        // Combine local and remote, prioritizing local for unsynced trails
                        val combinedTrails = mutableListOf<Trail>()
                        val localTrailIds = localTrails.map { it.id }.toSet()
                        
                        // Add all local trails
                        combinedTrails.addAll(filteredLocalTrails)
                        
                        // Add remote trails that aren't already local
                        remoteTrails.forEach { remoteTrail ->
                            if (!localTrailIds.contains(remoteTrail.id)) {
                                combinedTrails.add(remoteTrail)
                            }
                        }
                        
                        // Return combined results
                        onResult(combinedTrails)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("TrailsController", "Error in offline-first fetch", e)
                // Fallback to regular remote fetch
                fetchUserTrails(userId, selectedTags, onResult)
            }
        }
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
                                    TrailPrivacy.valueOf(data["privacy"] as? String ?: "FRIENDS_ONLY")
                                } catch (e: Exception) {
                                    TrailPrivacy.FRIENDS_ONLY
                                },
                                username = data["username"] as? String ?: "",
                                distance = (data["distance"] as? Number)?.toDouble(),
                                duration = data["duration"] as? String,
                                avgSpeed = (data["avgSpeed"] as? Number)?.toDouble(),
                                effort = data["effort"] as? String,
                                weather = data["weather"] as? String,
                                tags = tags,
                                notes = data["notes"] as? String,
                                createdAt = try {
                                    val firestoreDate = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                                    if (firestoreDate != null) {
                                        Log.d("TrailsController", "Trail ${data["name"]} has createdAt: $firestoreDate")
                                        firestoreDate
                                    } else {
                                        Log.w("TrailsController", "Trail ${data["name"]} missing createdAt field, using old fallback date")
                                        // Use a date from 2020 to indicate missing timestamp, rather than today's date
                                        java.util.Date(1577836800000L) // January 1, 2020
                                    }
                                } catch (e: Exception) {
                                    Log.e("TrailsController", "Error parsing createdAt for trail ${data["name"]}", e)
                                    java.util.Date(1577836800000L) // January 1, 2020
                                }
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
                
                // Populate missing usernames for user's own trails too
                populateMissingUsernames(filteredTrails) { trailsWithUsernames ->
                    onResult(trailsWithUsernames)
                }
            }
            .addOnFailureListener { e ->
                Log.e("TrailsController", "Error fetching user trails", e)
                Log.e("TrailsController", "Exception details: ${e.message}", e)
                onResult(emptyList())
            }
    }

    fun fetchOtherUsersTrails(currentUserId: String, onResult: (List<Trail>) -> Unit) {
        // First, get the current user's friends list
        com.example.localtrail.controller.FriendsController.getFriends { friendsList, exception ->
            if (exception != null) {
                Log.e("TrailsController", "Error fetching friends", exception)
                // Continue with just public trails if friends fetch fails
                fetchPublicTrails(currentUserId, emptyList(), onResult)
                return@getFriends
            }
            
            val friendsIds = friendsList?.map { it.userId } ?: emptyList()
            fetchPublicTrails(currentUserId, friendsIds, onResult)
        }
    }
    
    fun fetchPublicTrails(currentUserId: String, friendsIds: List<String>, onResult: (List<Trail>) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection("trails")
                .whereNotEqualTo("userID", currentUserId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val trails = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            if (data != null) {
                                val trail = Trail(
                                    id = doc.id,
                                    name = data["name"] as? String,
                                    location = data["location"] as? String ?: "Unknown Location",
                                    description = data["description"] as? String,
                                    userID = data["userID"] as? String ?: "",
                                    privacy = try {
                                        TrailPrivacy.valueOf(data["privacy"] as? String ?: "FRIENDS_ONLY")
                                    } catch (e: Exception) {
                                        TrailPrivacy.FRIENDS_ONLY
                                    },
                                    username = data["username"] as? String ?: "",
                                    distance = (data["distance"] as? Number)?.toDouble(),
                                    duration = data["duration"] as? String,
                                    avgSpeed = (data["avgSpeed"] as? Number)?.toDouble(),
                                    effort = data["effort"] as? String,
                                    weather = data["weather"] as? String,
                                    tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String },
                                    notes = data["notes"] as? String,
                                    createdAt = try {
                                        val firestoreDate = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                                        if (firestoreDate != null) {
                                            Log.d("TrailsController", "Social trail ${data["name"]} has createdAt: $firestoreDate")
                                            firestoreDate
                                        } else {
                                            Log.w("TrailsController", "Social trail ${data["name"]} missing createdAt field, using old fallback date")
                                            // Use a date from 2020 to indicate missing timestamp
                                            java.util.Date(1577836800000L) // January 1, 2020
                                        }
                                    } catch (e: Exception) {
                                        Log.e("TrailsController", "Error parsing createdAt for social trail ${data["name"]}", e)
                                        java.util.Date(1577836800000L) // January 1, 2020
                                    }
                                )
                                
                                // Log for debugging dates and sorting
                                Log.d("TrailsController", "Fetched trail: ${trail.name}, username: '${trail.username}', location: '${trail.location}', createdAt: ${trail.createdAt}, userID: ${trail.userID}")
                                
                                // Filter based on privacy settings
                                when (trail.privacy) {
                                    TrailPrivacy.PUBLIC -> trail // Always visible
                                    TrailPrivacy.FRIENDS_ONLY -> {
                                        // Only show if user is friends with trail owner
                                        if (friendsIds.contains(trail.userID)) trail else null
                                    }
                                    TrailPrivacy.PRIVATE -> null // Never visible to others
                                }
                            } else null
                        } catch (e: Exception) {
                            Log.e("TrailsController", "Error converting document ${doc.id}", e)
                            null
                        }
                    }.filterNotNull()
                    
                    // Populate missing usernames
                    populateMissingUsernames(trails) { trailsWithUsernames ->
                        onResult(trailsWithUsernames)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TrailsController", "Error fetching trails", e)
                    onResult(emptyList())
                }
    }

    private fun populateMissingUsernames(trails: List<Trail>, onResult: (List<Trail>) -> Unit) {
        if (trails.isEmpty()) {
            onResult(trails)
            return
        }
        
        val trailsWithMissingUsernames = trails.filter { it.username.isBlank() }
        if (trailsWithMissingUsernames.isEmpty()) {
            // All trails already have usernames, just sort and return
            val sortedTrails = trails.sortedByDescending { it.createdAt }
            Log.d("TrailsController", "Sorted ${sortedTrails.size} trails by creation date (newest first)")
            onResult(sortedTrails)
            return
        }
        
        Log.d("TrailsController", "Found ${trailsWithMissingUsernames.size} trails with missing usernames")
        
        val userIds = trailsWithMissingUsernames.map { it.userID }.distinct()
        val usernames = mutableMapOf<String, String>()
        var pendingRequests = userIds.size
        
        if (pendingRequests == 0) {
            onResult(trails)
            return
        }
        
        // Fetch usernames for all missing userIds
        userIds.forEach { userId ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username") ?: "Unknown User"
                    usernames[userId] = username
                    Log.d("TrailsController", "Fetched username for $userId: $username")
                    
                    pendingRequests--
                    if (pendingRequests == 0) {
                        // All usernames fetched, update trails and sort
                        val updatedTrails = trails.map { trail ->
                            if (trail.username.isBlank() && usernames.containsKey(trail.userID)) {
                                trail.copy(username = usernames[trail.userID]!!)
                            } else {
                                trail
                            }
                        }.sortedByDescending { it.createdAt }
                        
                        Log.d("TrailsController", "Updated and sorted ${updatedTrails.size} trails by creation date (newest first)")
                        onResult(updatedTrails)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TrailsController", "Error fetching username for user $userId", e)
                    usernames[userId] = "Unknown User"
                    
                    pendingRequests--
                    if (pendingRequests == 0) {
                        // All requests completed, update trails and sort
                        val updatedTrails = trails.map { trail ->
                            if (trail.username.isBlank() && usernames.containsKey(trail.userID)) {
                                trail.copy(username = usernames[trail.userID]!!)
                            } else {
                                trail
                            }
                        }.sortedByDescending { it.createdAt }
                        
                        Log.d("TrailsController", "Updated and sorted ${updatedTrails.size} trails by creation date (newest first, with errors)")
                        onResult(updatedTrails)
                    }
                }
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
                    
                    if (savedTrailsData.isEmpty()) {
                        onResult(emptyList(), null)
                        return@addOnSuccessListener
                    }
                    
                    // Extract trail IDs and their saved timestamps
                    val trailInfo = savedTrailsData.mapNotNull { data ->
                        val trailId = data["id"] as? String
                        val savedAt = data["savedAt"] as? Timestamp
                        if (trailId != null) Pair(trailId, savedAt) else null
                    }
                    
                    // Fetch full trail data from trails collection using document IDs
                    val trailIds = trailInfo.map { it.first }
                    val allTrails = mutableListOf<Trail>()
                    var completedRequests = 0
                    
                    trailIds.forEach { trailId ->
                        db.collection("trails").document(trailId)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    try {
                                        val data = doc.data!!
                                        val trail = Trail(
                                            id = doc.id, // Use document ID as trail ID
                                            userID = data["userID"] as? String ?: "",
                                            name = data["name"] as? String,
                                            location = data["location"] as? String,
                                            description = data["description"] as? String,
                                            username = data["username"] as? String ?: "",
                                            createdAt = (data["createdAt"] as? Timestamp)?.toDate() ?: Date(),
                                            tags = data["tags"] as? List<String>,
                                            distance = data["distance"] as? Double,
                                            duration = data["duration"] as? String,
                                            avgSpeed = data["avgSpeed"] as? Double,
                                            effort = data["effort"] as? String,
                                            weather = data["weather"] as? String,
                                            notes = data["notes"] as? String
                                        )
                                        allTrails.add(trail)
                                    } catch (e: Exception) {
                                        Log.e("TrailsController", "Error parsing saved trail", e)
                                    }
                                }
                                
                                completedRequests++
                                if (completedRequests == trailIds.size) {
                                    // Sort by saved timestamp (most recently saved first)
                                    val sortedTrails = allTrails.sortedByDescending { trail ->
                                        trailInfo.find { it.first == trail.id }?.second
                                    }
                                    onResult(sortedTrails, null)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("TrailsController", "Error fetching saved trail: $trailId", e)
                                completedRequests++
                                if (completedRequests == trailIds.size) {
                                    val sortedTrails = allTrails.sortedByDescending { trail ->
                                        trailInfo.find { it.first == trail.id }?.second
                                    }
                                    onResult(sortedTrails, null)
                                }
                            }
                    }
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

    fun updateTrailPrivacy(trailId: String, privacy: TrailPrivacy, onResult: (Boolean, Exception?) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("trails")
            .document(trailId)
            .update("privacy", privacy.name)
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e)
            }
    }

    fun updateAllTrailsPrivacy(privacy: TrailPrivacy, onResult: (Int, Exception?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("trails")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                var count = 0
                
                querySnapshot.documents.forEach { document ->
                    batch.update(document.reference, "privacy", privacy.name)
                    count++
                }
                
                if (count > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d("TrailsController", "Updated $count trails to ${privacy.name}")
                            onResult(count, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e("TrailsController", "Failed to update trails privacy", e)
                            onResult(0, e)
                        }
                } else {
                    onResult(0, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("TrailsController", "Failed to fetch trails for privacy update", e)
                onResult(0, e)
            }
    }

    /**
     * Deletes a trail from both local database and Firestore
     */
    fun deleteTrail(trailId: String, onResult: (Boolean, Exception?) -> Unit) {
        Log.d("TrailsController", "Attempting to delete trail: $trailId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val syncManager = SyncManager.getInstance(App.context)
                val networkManager = NetworkManager.getInstance(App.context)
                
                // First delete from local database
                val localDeleted = syncManager.deleteTrailLocally(trailId)
                
                if (!localDeleted) {
                    Log.e("TrailsController", "Failed to delete trail locally")
                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(false, Exception("Failed to delete trail from local database"))
                    }
                    return@launch
                }
                
                // If online, also delete from Firestore
                if (networkManager.isOnline.value) {
                    val firestore = FirebaseFirestore.getInstance()
                    
                    // Delete trail document and its locations
                    firestore.collection("trails").document(trailId)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("TrailsController", "Trail deleted successfully from Firestore")
                            onResult(true, null)
                        }
                        .addOnFailureListener { e ->
                            Log.e("TrailsController", "Failed to delete trail from Firestore", e)
                            // Even if Firestore deletion fails, local deletion succeeded
                            onResult(true, null) // Consider it successful since local deletion worked
                        }
                } else {
                    Log.d("TrailsController", "Offline - trail deleted locally only")
                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(true, null)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("TrailsController", "Error deleting trail", e)
                CoroutineScope(Dispatchers.Main).launch {
                    onResult(false, e)
                }
            }
        }
    }
}

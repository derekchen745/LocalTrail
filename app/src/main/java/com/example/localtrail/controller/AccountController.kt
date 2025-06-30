package com.example.localtrail.controller

import android.app.Activity
import android.content.Intent
import com.example.localtrail.controller.activities.LoginActivity
import com.example.localtrail.model.SavedTrail
import com.example.localtrail.model.Trail
import com.example.localtrail.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

object AccountController {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Logs in a user with email and password
     * @param email The user's email address
     * @param password The user's password
     * @param onResult Callback with success status and optional exception
     */
    fun login(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception)
            }
    }
        
    /**
     * Creates a new user account in Firebase and saves basic user profile information
     * @param email The email address for the new account
     * @param password The password for the new account
     * @param onResult Callback with success status and optional exception  
     */
    fun createAccount(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    if (uid != null) {
                        val userProfile = hashMapOf(
                            "uid" to uid,
                            "email" to email,
                            "username" to "",
                            "description" to "",
                            "savedTrails" to emptyList<Map<String, Any>>()
                        )

                        db.collection("users").document(uid)
                            .set(userProfile)
                            .addOnSuccessListener { onResult(true, null) }
                            .addOnFailureListener { e -> onResult(false, e) }
                    } else {
                        onResult(false, Exception("User UID is null"))
                    }
                } else {
                    onResult(false, task.exception)
                }
            }
    }

        
    /**
     * Checks if a user is valid in Firebase, no user details are fetched
     */
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
        } else {
            null
        }
    }

    fun signOut() = auth.signOut()

    /**
     * Saves a trail to the user's saved trails collection in denormalized form
     * @param trail The trail to save
     * @param onResult Callback with success status and optional exception
     */
    fun saveTrailToUser(trail: Trail, onResult: (Boolean, Exception?) -> Unit) {
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
            savedAt = Date()
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
    
    /**
     * Checks if a trail is saved in the current user's saved trails
     * @param trailId The ID of the trail to check
     * @param onResult Callback with result (true if saved, false if not or error)
     */
    fun isTrailSavedByUser(trailId: String, onResult: (Boolean) -> Unit) {
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
    
    /**
     * Removes a trail from the user's saved trails
     * @param trailId The ID of the trail to remove
     * @param onResult Callback with success status and optional exception
     */
    fun removeTrailFromUser(trailId: String, onResult: (Boolean, Exception?) -> Unit) {
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
                throw Exception("Error processing saved trails: ${e.message}")
            }
        }.addOnSuccessListener {
            onResult(true, null)
        }.addOnFailureListener { e ->
            onResult(false, e)
        }
    }
    
    /**
     * Fetches detailed user information using coroutines
     * @return User object or null if not logged in
     */
    suspend fun getUserDetails(): User? = withContext(Dispatchers.IO) {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            return@withContext null
        }
        
        try {
            val document = db.collection("users").document(firebaseUser.uid).get().await()
            
            if (document.exists()) {
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: document.getString("email") ?: "",
                    username = document.getString("username") ?: "",
                    description = document.getString("description") ?: "",
                    savedTrails = emptyList()
                )
            } else {
                User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
            }
        } catch (e: Exception) {
            User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
        }
    }
    
    /**
     * Gets saved trails for the current user
     */
    fun getSavedTrails(onResult: (List<Trail>, Exception?) -> Unit) {
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
    
    /**
     * Updates the user's description in Firestore
     */
    suspend fun updateUserDescription(newDescription: String) = withContext(Dispatchers.IO) {
        val firebaseUser = auth.currentUser ?: return@withContext
        try {
            db.collection("users").document(firebaseUser.uid)
                .update("description", newDescription)
                .await()
        } catch (e: Exception) {
            // Optionally log or handle error
        }
    }
}

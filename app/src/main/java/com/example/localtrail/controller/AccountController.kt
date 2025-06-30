package com.example.localtrail.controller

import android.app.Activity
import android.content.Intent
import com.example.localtrail.controller.activities.LoginActivity
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

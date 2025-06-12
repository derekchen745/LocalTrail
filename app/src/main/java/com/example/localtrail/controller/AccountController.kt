package com.example.localtrail.controller

import com.google.firebase.auth.FirebaseAuth

object AccountController {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception)
            }
    }

    fun createAccount(email: String, password: String, onResult: (Boolean, Exception?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful, task.exception)
            }
    }

    fun getCurrentUser() = auth.currentUser
    fun signOut() = auth.signOut()
}

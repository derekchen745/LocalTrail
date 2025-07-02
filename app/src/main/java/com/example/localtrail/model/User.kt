package com.example.localtrail.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val savedTrails: List<SavedTrail> = emptyList(),
    val description: String = ""
) {
    // Converts SavedTrail objects to Trail objects
    fun getSavedTrailsAsFullTrails(): List<Trail> {
        return savedTrails.map { it.toTrail() }
    }
}

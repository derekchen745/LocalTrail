package com.example.localtrail.model

import java.util.Date

/**
 * A lightweight version of Trail that is stored in the user document
 * to avoid having to make multiple Firestore queries
 */
data class SavedTrail(
    val id: String = "",
    val name: String? = null,
    val location: String? = null,
    val userID: String = "",
    val username: String = "",
    val savedAt: Date = Date()
) {
    constructor() : this(
        id = "",
        name = null,
        location = null,
        userID = "",
        username = ""
    )
    
    // Convert to full Trail object (for backward compatibility)
    fun toTrail(): Trail {
        return Trail(
            id = id,
            userID = userID,
            name = name,
            location = location,
            username = username
        )
    }
}

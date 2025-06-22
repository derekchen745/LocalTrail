package com.example.localtrail.model

import com.example.localtrail.model.enums.TrailPrivacy

data class Trail(
    val id: Int? = null,
    val userID: String,
    val name: String? = null,
    val location: String? = null,
    val description: String? = null,
    val privacy: TrailPrivacy = TrailPrivacy.PUBLIC,
)

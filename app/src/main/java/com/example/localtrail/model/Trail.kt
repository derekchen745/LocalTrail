package com.example.localtrail.model

data class Trail(
    val id: Int,
    val name: String,
    val location: String,
    val description: String,
    val privacy: TrailPrivacy = TrailPrivacy.PUBLIC,
)

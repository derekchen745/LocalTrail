package com.example.localtrail.model

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "FAKE USERNAME",
    val savedTrails: List<String> = emptyList()
)

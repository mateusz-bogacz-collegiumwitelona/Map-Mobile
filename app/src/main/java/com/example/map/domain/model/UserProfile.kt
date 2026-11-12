package com.example.map.domain.model

data class UserProfile(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isPublic: Boolean = false
)

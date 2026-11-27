package com.example.map.domain.model.user

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val isPublic: Boolean = false
)

package com.example.map.data.remote.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
    val bio: String? = null,
    val isPublic: Boolean = false
)

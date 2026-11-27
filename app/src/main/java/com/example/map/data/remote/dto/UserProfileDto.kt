package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val firstName: String,
    val lastName: String
)
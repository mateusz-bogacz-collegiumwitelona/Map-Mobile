package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDataDto(
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val isPublic: Boolean? = null
)

@Serializable
data class EditUserNamesRequestDto(
    val firstName: String,
    val lastName: String
)

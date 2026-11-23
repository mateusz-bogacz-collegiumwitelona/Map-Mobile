package com.example.map.data.remote.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDataResponse(
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val isPublic: Boolean? = null
)

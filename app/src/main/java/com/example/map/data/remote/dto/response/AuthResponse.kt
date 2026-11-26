package com.example.map.data.remote.dto.response

import kotlinx.serialization.Serializable


@Serializable
data class AuthResponse(
    val userId: String,
    val roles: List<String>,
    val accessToken: String,
    val accessTokenExpiration: String,
    val refreshToken: String
)
package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable


@Serializable
data class AuthResponseDto(
    val userId: String,
    val roles: List<String>,
    val accessToken: String,
    val accessTokenExpiration: String,
    val refreshToken: String
)
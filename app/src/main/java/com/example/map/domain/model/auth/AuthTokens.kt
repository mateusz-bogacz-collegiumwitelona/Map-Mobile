package com.example.map.domain.model.auth

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String
)
package com.example.map.domain.model.auth

data class AuthenticatedAccount(
    val tokens: AuthTokens,
    val roles: List<String>
)
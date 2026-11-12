package com.example.map.domain.model

data class AuthenticatedAccount(
    val tokens: AuthTokens,
    val roles: List<String>
)
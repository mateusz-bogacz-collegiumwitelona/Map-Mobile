package com.example.map.domain.repository

import com.example.map.domain.model.AuthenticatedAccount

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthenticatedAccount

    suspend fun refresh(refreshToken: String): AuthenticatedAccount

    suspend fun logout(accessToken: String, refreshToken: String)
}


package com.example.map.domain.repository

import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.RegistrationResult

interface AuthRepository {
    suspend fun login(email: String, password: String): AuthenticatedAccount

    suspend fun refresh(refreshToken: String): AuthenticatedAccount

    suspend fun logout(accessToken: String, refreshToken: String)

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        bio: String?,
        isPublic: Boolean
    ): RegistrationResult
}


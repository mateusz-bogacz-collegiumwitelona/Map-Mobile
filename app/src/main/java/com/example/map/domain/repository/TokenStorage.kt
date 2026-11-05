package com.example.map.domain.repository

import com.example.map.domain.model.AuthTokens

interface TokenStorage {
    fun saveTokens(tokens: AuthTokens)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
}
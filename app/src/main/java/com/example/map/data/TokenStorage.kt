package com.example.map.data

interface TokenStorage {
    fun saveTokens(tokens: AuthTokens)
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun clearTokens()
}
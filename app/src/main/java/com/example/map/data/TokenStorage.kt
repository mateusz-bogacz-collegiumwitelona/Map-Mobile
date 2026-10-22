package com.example.map.data

interface TokenStorage {
    fun saveTokens(tokens: AuthTokens)
    fun getTokens(): AuthTokens?
    fun clearTokens()
}
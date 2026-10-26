package com.example.map.domain

import com.example.map.data.AuthTokens
import com.example.map.data.TokenStorage

class AuthSession(
    private val tokenStorage: TokenStorage
) {
    fun saveAfterLogin(tokens: AuthTokens, rememberMe: Boolean) {
        if (rememberMe) {
            tokenStorage.saveTokens(tokens)
        } else {
            tokenStorage.clearTokens()
        }
    }

    fun hasSavedSession(): Boolean =
        !tokenStorage.getAccessToken().isNullOrBlank()

    fun logout() {
        tokenStorage.clearTokens()
    }
}
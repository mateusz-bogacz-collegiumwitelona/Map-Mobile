package com.example.map.domain.usecase

import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.model.AuthTokens

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
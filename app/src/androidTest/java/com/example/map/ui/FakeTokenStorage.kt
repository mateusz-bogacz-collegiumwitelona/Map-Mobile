package com.example.map.ui

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.repository.TokenStorage

internal class FakeTokenStorage(
    initialTokens: AuthTokens? = null
) : TokenStorage {

    private var tokens: AuthTokens? = initialTokens

    override fun saveTokens(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override fun getAccessToken(): String? = tokens?.accessToken

    override fun getRefreshToken(): String? = tokens?.refreshToken

    override fun clearTokens() {
        tokens = null
    }
}
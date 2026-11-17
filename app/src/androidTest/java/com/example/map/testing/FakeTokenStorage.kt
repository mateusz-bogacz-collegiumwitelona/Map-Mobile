package com.example.map.testing

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.repository.TokenStorage

class FakeTokenStorage(initialTokens: AuthTokens? = null) : TokenStorage {

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

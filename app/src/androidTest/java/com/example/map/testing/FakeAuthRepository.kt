package com.example.map.testing

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.repository.AuthRepository

class FakeAuthRepository : AuthRepository {

    var loginResult: AuthenticatedAccount = AuthenticatedAccount(
        tokens = AuthTokens(
            accessToken = "access-test",
            refreshToken = "refresh-test"
        ),
        roles = listOf("User")
    )
    var loginError: Exception? = null
    var refreshResult: AuthenticatedAccount = AuthenticatedAccount(
        tokens = AuthTokens(
            accessToken = "access-refreshed",
            refreshToken = "refresh-refreshed"
        ),
        roles = listOf("User")
    )
    var refreshError: Exception? = null
    var logoutError: Exception? = null

    var loginCalls = 0
    var refreshCalls = 0
    var logoutCalls = 0

    var lastLoginEmail: String? = null
    var lastLoggedOutAccessToken: String? = null
    var lastLoggedOutRefreshToken: String? = null

    override suspend fun login(
        email: String,
        password: String
    ): AuthenticatedAccount {
        loginCalls++
        lastLoginEmail = email
        loginError?.let { throw it }
        return loginResult
    }

    override suspend fun refresh(refreshToken: String): AuthenticatedAccount {
        refreshCalls++
        refreshError?.let { throw it }
        return refreshResult
    }

    override suspend fun logout(accessToken: String, refreshToken: String) {
        logoutCalls++
        lastLoggedOutAccessToken = accessToken
        lastLoggedOutRefreshToken = refreshToken
        logoutError?.let { throw it }
    }
}
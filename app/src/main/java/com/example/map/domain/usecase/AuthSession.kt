package com.example.map.domain.usecase

import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.TokenStorage
import java.util.concurrent.CancellationException

class AuthSession(
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository
) {
    suspend fun login(email: String, password: String) {
        val account = authRepository.login(email, password)

        val hasOnlyUserRole =
            account.roles.size == 1 &&
                    account.roles.single().equals("USER", ignoreCase = true)

        if (!hasOnlyUserRole) {
            try {
                authRepository.logout(
                    account.tokens.accessToken,
                    account.tokens.refreshToken
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            }

            throw AuthException(AuthFailure.ROLE_NOT_ALLOWED)
        }

        tokenStorage.saveTokens(account.tokens)
    }

    fun hasSavedSession(): Boolean =
        !tokenStorage.getAccessToken().isNullOrBlank() &&
                !tokenStorage.getRefreshToken().isNullOrBlank()

    suspend fun logout() {
        val refreshToken = tokenStorage.getRefreshToken()

        try {
            if (refreshToken.isNullOrBlank()) {
                throw AuthException(AuthFailure.INVALID_SESSION)
            }

            val refreshedAccount = authRepository.refresh(refreshToken)
            tokenStorage.saveTokens(refreshedAccount.tokens)

            authRepository.logout(
                accessToken = refreshedAccount.tokens.accessToken,
                refreshToken = refreshedAccount.tokens.refreshToken
            )
        } finally {
            tokenStorage.clearTokens()
        }
    }
}
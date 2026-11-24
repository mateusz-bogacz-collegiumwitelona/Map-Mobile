package com.example.map.domain.session

import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.EmailVerificationError
import com.example.map.domain.model.EmailVerificationResult
import com.example.map.domain.model.RegistrationResult
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CancellationException

class AuthSession(
    private val tokenStorage: TokenStorage,
    private val authRepository: AuthRepository,
) {
    private val _pendingVerificationEmail = MutableStateFlow<String?>(null)
    val pendingVerificationEmail: StateFlow<String?> = _pendingVerificationEmail.asStateFlow()

    suspend fun login(email: String, password: String) {
        val account = authRepository.login(email, password)

        val hasOnlyUserRole =
            account.roles.size == 1 &&
                    account.roles.single().equals("USER", ignoreCase = true)

        if (!hasOnlyUserRole) {
            try {
                authRepository.logout(
                    accessToken = account.tokens.accessToken,
                    refreshToken = account.tokens.refreshToken
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Nie przyjmuj konta z niedozwoloną rolą, nawet gdy revoke się nie uda.
            }

            throw AuthException(AuthFailure.ROLE_NOT_ALLOWED)
        }

        tokenStorage.saveTokens(account.tokens)
    }

    fun hasSavedSession(): Boolean =
        !tokenStorage.getAccessToken().isNullOrBlank() &&
                !tokenStorage.getRefreshToken().isNullOrBlank()

    suspend fun logout() {
        val accessToken = tokenStorage.getAccessToken()
        val refreshToken = tokenStorage.getRefreshToken()

        try {
            if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                throw AuthException(AuthFailure.INVALID_SESSION)
            }

            try {
                authRepository.logout(accessToken, refreshToken)
            } catch (error: AuthException) {
                if (error.failure != AuthFailure.INVALID_SESSION) throw error

                val refreshedAccount = authRepository.refresh(refreshToken)
                tokenStorage.saveTokens(refreshedAccount.tokens)

                authRepository.logout(
                    accessToken = refreshedAccount.tokens.accessToken,
                    refreshToken = refreshedAccount.tokens.refreshToken
                )
            }
        } finally {
            tokenStorage.clearTokens()
        }
    }

    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        bio: String?,
        isPublic: Boolean
    ): RegistrationResult {
        val normalizedEmail = email.trim()
        val result = authRepository.register(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = normalizedEmail,
            password = password,
            confirmPassword = confirmPassword,
            bio = bio?.trim(),
            isPublic = isPublic
        )
        if (result is RegistrationResult.Success) {
            _pendingVerificationEmail.value = normalizedEmail
        }
        return result
    }

    suspend fun confirmEmail(code: String): EmailVerificationResult {
        val targetEmail = _pendingVerificationEmail.value
            ?: return EmailVerificationResult.Failure(
                error = EmailVerificationError.VALIDATION
            )

        val result = authRepository.confirmEmail(
            email = targetEmail,
            code = code.trim()
        )
        if (result is EmailVerificationResult.Success) {
            _pendingVerificationEmail.value = null
        }
        return result
    }

    suspend fun resendVerificationCode(): EmailVerificationResult {
        val targetEmail = _pendingVerificationEmail.value
            ?: return EmailVerificationResult.Failure(
                error = EmailVerificationError.VALIDATION,
            )

        return authRepository.resendVerificationCode(email = targetEmail)
    }

    fun clearLocalSession() {
        tokenStorage.clearTokens()
    }

    fun setPendingVerificationEmail(email: String) {
        val normalized = email.trim()
        if (normalized.isNotEmpty()) {
            _pendingVerificationEmail.value = normalized
        }
    }

    fun clearPendingVerificationEmail() {
        _pendingVerificationEmail.value = null
    }
}
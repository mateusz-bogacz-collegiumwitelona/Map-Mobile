package com.example.map.testing

import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.EmailVerificationResult
import com.example.map.domain.model.RegistrationResult
import com.example.map.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred

class FakeAuthRepository : AuthRepository {

    var registerResult: RegistrationResult = RegistrationResult.Success
    var registerGate: CompletableDeferred<Unit>? = null
    var registerCalls = 0
    var lastRegisteredFirstName: String? = null
    var lastRegisteredLastName: String? = null
    var lastRegisteredEmail: String? = null
    var lastRegisteredPassword: String? = null
    var lastRegisteredConfirmPassword: String? = null
    var lastRegisteredBio: String? = null
    var lastRegisteredIsPublic: Boolean? = null

    var confirmEmailResult: EmailVerificationResult = EmailVerificationResult.Success
    var confirmEmailGate: CompletableDeferred<Unit>? = null
    var confirmEmailCalls = 0
    var lastConfirmedEmail: String? = null
    var lastConfirmedCode: String? = null

    var resendVerificationResult: EmailVerificationResult =
        EmailVerificationResult.Success
    var resendVerificationGate: CompletableDeferred<Unit>? = null
    var resendVerificationCalls = 0
    var lastResendVerificationEmail: String? = null

    var failFirstLogoutWithInvalidSession = false
    var lastLogoutTokens: Pair<String, String>? = null

    var loginAccount = AuthenticatedAccount(
        tokens = AuthTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token"
        ),
        roles = listOf("USER")
    )

    var refreshedAccount = AuthenticatedAccount(
        tokens = AuthTokens(
            accessToken = "refreshed-access",
            refreshToken = "refreshed-refresh"
        ),
        roles = listOf("USER")
    )

    var loginResult: AuthenticatedAccount
        get() = loginAccount
        set(value) {
            loginAccount = value
        }

    var refreshResult: AuthenticatedAccount
        get() = refreshedAccount
        set(value) {
            refreshedAccount = value
        }

    var loginError: Exception? = null
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
        return loginAccount
    }

    override suspend fun refresh(refreshToken: String): AuthenticatedAccount {
        refreshCalls++
        refreshError?.let { throw it }
        return refreshedAccount
    }

    override suspend fun logout(accessToken: String, refreshToken: String) {
        logoutCalls++
        lastLogoutTokens = accessToken to refreshToken
        lastLoggedOutAccessToken = accessToken
        lastLoggedOutRefreshToken = refreshToken

        if (failFirstLogoutWithInvalidSession && logoutCalls == 1) {
            throw AuthException(AuthFailure.INVALID_SESSION)
        }

        logoutError?.let { throw it }
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        bio: String?,
        isPublic: Boolean
    ): RegistrationResult {
        registerCalls++
        lastRegisteredFirstName = firstName
        lastRegisteredLastName = lastName
        lastRegisteredEmail = email
        lastRegisteredPassword = password
        lastRegisteredConfirmPassword = confirmPassword
        lastRegisteredBio = bio
        lastRegisteredIsPublic = isPublic
        registerGate?.await()
        return registerResult
    }

    override suspend fun confirmEmail(
        email: String,
        code: String
    ): EmailVerificationResult {
        confirmEmailCalls++
        lastConfirmedEmail = email
        lastConfirmedCode = code
        confirmEmailGate?.await()
        return confirmEmailResult
    }

    override suspend fun resendVerificationCode(
        email: String
    ): EmailVerificationResult {
        resendVerificationCalls++
        lastResendVerificationEmail = email
        resendVerificationGate?.await()
        return resendVerificationResult
    }
}
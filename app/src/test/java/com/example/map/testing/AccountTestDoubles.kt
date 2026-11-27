package com.example.map.testing

import com.example.map.domain.model.account.AccountDeletionResult
import com.example.map.domain.model.account.EmailChangeResult
import com.example.map.domain.model.account.EmailVerificationResult
import com.example.map.domain.model.auth.AuthException
import com.example.map.domain.model.auth.AuthFailure
import com.example.map.domain.model.auth.AuthTokens
import com.example.map.domain.model.auth.AuthenticatedAccount
import com.example.map.domain.model.auth.RegistrationResult
import com.example.map.domain.model.user.UserProfile
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.repository.TokenStorage
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

    var resendVerificationResult: EmailVerificationResult = EmailVerificationResult.Success
    var resendVerificationGate: CompletableDeferred<Unit>? = null
    var resendVerificationCalls = 0
    var lastResendVerificationEmail: String? = null

    var failFirstLogoutWithInvalidSession = false
    var lastLogoutTokens: Pair<String, String>? = null

    var loginAccount = AuthenticatedAccount(
        tokens = AuthTokens("access-token", "refresh-token"),
        roles = listOf("USER")
    )
    var refreshedAccount = AuthenticatedAccount(
        tokens = AuthTokens("refreshed-access", "refreshed-refresh"),
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
        return refreshResult
    }

    override suspend fun logout(accessToken: String, refreshToken: String) {
        logoutCalls++
        lastLogoutTokens = accessToken to refreshToken
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

class FakeAccountRepository : AccountRepository {

    var requestResult: EmailChangeResult = EmailChangeResult.Success
    var confirmResult: EmailChangeResult = EmailChangeResult.Success
    var deletionResult: AccountDeletionResult = AccountDeletionResult.Success

    var requestGate: CompletableDeferred<Unit>? = null
    var confirmGate: CompletableDeferred<Unit>? = null
    var deletionGate: CompletableDeferred<Unit>? = null

    var requestCalls = 0
    var confirmCalls = 0
    var deletionCalls = 0

    var lastRequestedEmail: String? = null
    var lastConfirmedCode: String? = null

    override suspend fun requestEmailChange(newEmail: String): EmailChangeResult {
        requestCalls++
        lastRequestedEmail = newEmail
        requestGate?.await()
        return requestResult
    }

    override suspend fun confirmEmailChange(code: String): EmailChangeResult {
        confirmCalls++
        lastConfirmedCode = code
        confirmGate?.await()
        return confirmResult
    }

    override suspend fun deleteAccount(): AccountDeletionResult {
        deletionCalls++
        deletionGate?.await()
        return deletionResult
    }
}

class FakeProfileStorage(
    initialProfile: UserProfile? = null,
    initialEmail: String? = null
) : ProfileStorage {

    constructor(initialEmail: String?) : this(
        initialProfile = null,
        initialEmail = initialEmail
    )

    private var email: String? = initialEmail
    private var profile: UserProfile? = initialProfile

    override fun getEmail(): String? = email

    override fun saveEmail(email: String) {
        this.email = email
    }

    override fun getProfile(): UserProfile? = profile

    override fun saveProfile(profile: UserProfile) {
        this.profile = profile
    }

    override fun clear() {
        email = null
        profile = null
    }
}

class FakeTokenStorage(initialTokens: AuthTokens? = null) : TokenStorage {
    private var access: String? = initialTokens?.accessToken
    private var refresh: String? = initialTokens?.refreshToken

    override fun saveTokens(tokens: AuthTokens) {
        access = tokens.accessToken
        refresh = tokens.refreshToken
    }

    override fun getAccessToken(): String? = access

    override fun getRefreshToken(): String? = refresh

    override fun clearTokens() {
        access = null
        refresh = null
    }
}

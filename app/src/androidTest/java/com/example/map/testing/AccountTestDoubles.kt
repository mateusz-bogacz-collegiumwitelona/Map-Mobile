package com.example.map.testing

import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.repository.TokenStorage
import kotlinx.coroutines.CompletableDeferred

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

class FakeProfileStorage(initialEmail: String? = null) : ProfileStorage {

    private var email: String? = initialEmail

    override fun getEmail(): String? = email

    override fun saveEmail(email: String) {
        this.email = email
    }

    override fun clear() {
        email = null
    }
}

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

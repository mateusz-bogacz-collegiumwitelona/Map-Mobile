package com.example.map.testing

import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.repository.AccountRepository
import kotlinx.coroutines.CompletableDeferred
import com.example.map.domain.model.UserProfile

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

    override suspend fun getCurrentUserProfile(): UserProfile {
        currentUserProfileError?.let { throw it }
        return currentUserProfile
    }

    var currentUserProfile = UserProfile(
        firstName = "Jan",
        lastName = "Kowalski"
    )
    var currentUserProfileError: Exception? = null
}
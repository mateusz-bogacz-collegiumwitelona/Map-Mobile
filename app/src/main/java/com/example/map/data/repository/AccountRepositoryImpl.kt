package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.ChangeEmailRequest
import com.example.map.data.remote.dto.request.ConfirmEmailChangeRequest
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.repository.AccountRepository
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val json: Json
) : AccountRepository {

    override suspend fun requestEmailChange(newEmail: String): EmailChangeResult =
        executeEmailChange {
            userApiService.requestEmailChange(ChangeEmailRequest(newEmail))
        }

    override suspend fun confirmEmailChange(code: String): EmailChangeResult =
        executeEmailChange {
            userApiService.confirmEmailChange(ConfirmEmailChangeRequest(code))
        }

    override suspend fun deleteAccount(): AccountDeletionResult =
        executeUnitApiCall(
            json = json,
            request = { userApiService.deleteAccount() },
            onSuccess = { AccountDeletionResult.Success },
            onApiError = { status, code, msg ->
                AccountDeletionResult.Failure(mapDeletionError(status, code), msg)
            },
            onNetworkError = { AccountDeletionResult.Failure(AccountDeletionError.NETWORK) },
            onUnknownError = { msg -> AccountDeletionResult.Failure(AccountDeletionError.UNKNOWN, msg) }
        )

    private suspend fun executeEmailChange(
        request: suspend () -> Response<ApiEnvelope<Unit>>
    ): EmailChangeResult =
        executeUnitApiCall(
            json = json,
            request = request,
            onSuccess = { EmailChangeResult.Success },
            onApiError = { status, code, msg ->
                EmailChangeResult.Failure(mapError(status, code), msg)
            },
            onNetworkError = { EmailChangeResult.Failure(EmailChangeError.NETWORK) },
            onUnknownError = { msg -> EmailChangeResult.Failure(EmailChangeError.UNKNOWN, msg) }
        )

    private fun mapDeletionError(
        statusCode: Int,
        errorCode: String?
    ): AccountDeletionError =
        when (errorCode) {
            "COMMON_002" -> AccountDeletionError.NOT_FOUND
            "COMMON_005" -> AccountDeletionError.RATE_LIMITED
            else -> when (statusCode) {
                401, 403 -> AccountDeletionError.UNAUTHORIZED
                404 -> AccountDeletionError.NOT_FOUND
                429 -> AccountDeletionError.RATE_LIMITED
                else -> AccountDeletionError.UNKNOWN
            }
        }

    private fun mapError(statusCode: Int, errorCode: String?): EmailChangeError =
        when (errorCode) {
            "USR_004" -> EmailChangeError.EMAIL_INVALID
            "USR_005" -> EmailChangeError.EMAIL_TAKEN
            "USR_006" -> EmailChangeError.EMAIL_UNCHANGED
            "VER_001" -> EmailChangeError.INVALID_CODE
            "VER_002" -> EmailChangeError.CODE_EXPIRED
            "VER_003" -> EmailChangeError.ATTEMPTS_EXCEEDED
            "COMMON_005" -> EmailChangeError.RATE_LIMITED
            else -> when (statusCode) {
                401, 403 -> EmailChangeError.UNAUTHORIZED
                409 -> EmailChangeError.EMAIL_TAKEN
                429 -> EmailChangeError.RATE_LIMITED
                else -> EmailChangeError.UNKNOWN
            }
        }
}
package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.ChangeEmailRequest
import com.example.map.data.remote.dto.request.ConfirmEmailChangeRequest
import com.example.map.data.remote.dto.request.EditUserNamesRequest
import com.example.map.data.remote.dto.response.UserProfileDataResponse
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteAccountRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val service = FakeUserApiService()
    private val repository = AccountRepositoryImpl(service, json)

    @Test
    fun mapsEmailTakenErrorCodeToEmailTaken() = runTest {
        service.requestResponse = errorResponse(
            status = 409,
            errorCode = "USR_005",
            message = "Email taken"
        )

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.EMAIL_TAKEN, "Email taken"),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsConflictWithoutErrorCodeToEmailTaken() = runTest {
        service.requestResponse = errorResponse(status = 409)

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.EMAIL_TAKEN),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsInvalidVerificationCodeError() = runTest {
        service.confirmResponse = errorResponse(
            status = 400,
            errorCode = "VER_001",
            message = "Invalid code"
        )

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.INVALID_CODE, "Invalid code"),
            repository.confirmEmailChange("123456")
        )
    }

    @Test
    fun mapsExpiredVerificationCodeError() = runTest {
        service.confirmResponse = errorResponse(
            status = 400,
            errorCode = "VER_002",
            message = "Code expired"
        )

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.CODE_EXPIRED, "Code expired"),
            repository.confirmEmailChange("123456")
        )
    }

    @Test
    fun mapsInvalidEmailErrorCode() = runTest {
        service.requestResponse = errorResponse(
            status = 400,
            errorCode = "USR_004",
            message = "Invalid email"
        )

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.EMAIL_INVALID, "Invalid email"),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsUnchangedEmailErrorCode() = runTest {
        service.requestResponse = errorResponse(
            status = 400,
            errorCode = "USR_006",
            message = "Email unchanged"
        )

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.EMAIL_UNCHANGED, "Email unchanged"),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsAttemptsExceededErrorCode() = runTest {
        service.confirmResponse = errorResponse(
            status = 400,
            errorCode = "VER_003",
            message = "Attempts exceeded"
        )

        assertEquals(
            EmailChangeResult.Failure(
                EmailChangeError.ATTEMPTS_EXCEEDED,
                "Attempts exceeded"
            ),
            repository.confirmEmailChange("123456")
        )
    }

    @Test
    fun mapsRateLimitErrorCode() = runTest {
        service.requestResponse = errorResponse(
            status = 429,
            errorCode = "COMMON_005",
            message = "Too many requests"
        )

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.RATE_LIMITED, "Too many requests"),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsRateLimitStatusWithoutErrorCode() = runTest {
        service.requestResponse = errorResponse(status = 429)

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.RATE_LIMITED),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsUnauthorizedStatusForEmailChange() = runTest {
        service.requestResponse = errorResponse(status = 403)

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.UNAUTHORIZED),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsNetworkFailureForEmailChange() = runTest {
        service.requestException = IOException("offline")

        assertEquals(
            EmailChangeResult.Failure(EmailChangeError.NETWORK),
            repository.requestEmailChange("new@example.com")
        )
    }

    @Test
    fun mapsDeletionNotFoundError() = runTest {
        service.deleteResponse = errorResponse(
            status = 404,
            errorCode = "COMMON_002",
            message = "Account not found"
        )

        assertEquals(
            AccountDeletionResult.Failure(
                AccountDeletionError.NOT_FOUND,
                "Account not found"
            ),
            repository.deleteAccount()
        )
    }

    @Test
    fun mapsDeletionRateLimitError() = runTest {
        service.deleteResponse = errorResponse(
            status = 429,
            errorCode = "COMMON_005",
            message = "Too many requests"
        )

        assertEquals(
            AccountDeletionResult.Failure(
                AccountDeletionError.RATE_LIMITED,
                "Too many requests"
            ),
            repository.deleteAccount()
        )
    }

    @Test
    fun mapsDeletionUnauthorizedStatus() = runTest {
        service.deleteResponse = errorResponse(status = 401)

        assertEquals(
            AccountDeletionResult.Failure(AccountDeletionError.UNAUTHORIZED),
            repository.deleteAccount()
        )
    }

    @Test
    fun mapsDeletionNetworkFailure() = runTest {
        service.deleteException = IOException("offline")

        assertEquals(
            AccountDeletionResult.Failure(AccountDeletionError.NETWORK),
            repository.deleteAccount()
        )
    }

    @Test
    fun mapsDeletionUnknownStatus() = runTest {
        service.deleteResponse = errorResponse(
            status = 500,
            errorCode = "SERVER_500",
            message = "Server error"
        )

        assertEquals(
            AccountDeletionResult.Failure(AccountDeletionError.UNKNOWN, "Server error"),
            repository.deleteAccount()
        )
    }

    @Test
    fun returnsSuccessWhenDeletionSucceeds() = runTest {
        service.deleteResponse = Response.success(ApiEnvelope<Unit>(isSuccess = true))

        assertEquals(AccountDeletionResult.Success, repository.deleteAccount())
    }

    private fun errorResponse(
        status: Int,
        errorCode: String? = null,
        message: String? = null
    ): Response<ApiEnvelope<Unit>> {
        val body = json.encodeToString(
            ApiEnvelope<Unit>(
                isSuccess = false,
                message = message,
                errorCode = errorCode
            )
        )
        return Response.error(
            status,
            body.toResponseBody("application/json".toMediaType())
        )
    }
}

private class FakeUserApiService : UserApiService {

    var requestResponse: Response<ApiEnvelope<Unit>> =
        Response.success(ApiEnvelope<Unit>(isSuccess = true))
    var confirmResponse: Response<ApiEnvelope<Unit>> =
        Response.success(ApiEnvelope<Unit>(isSuccess = true))
    var deleteResponse: Response<ApiEnvelope<Unit>> =
        Response.success(ApiEnvelope<Unit>(isSuccess = true))

    var requestException: Exception? = null
    var confirmException: Exception? = null
    var deleteException: Exception? = null

    override suspend fun getProfile(): Response<ApiEnvelope<UserProfileDataResponse>> {
        error("Nie używane w testach konta.")
    }

    override suspend fun updateNames(
        request: EditUserNamesRequest
    ): Response<ApiEnvelope<Unit>> {
        error("Nie używane w testach konta.")
    }

    override suspend fun setVisibility(isPublic: Boolean): Response<ApiEnvelope<Unit>> {
        error("Nie używane w testach konta.")
    }

    override suspend fun requestEmailChange(
        request: ChangeEmailRequest
    ): Response<ApiEnvelope<Unit>> {
        requestException?.let { throw it }
        return requestResponse
    }

    override suspend fun confirmEmailChange(
        request: ConfirmEmailChangeRequest
    ): Response<ApiEnvelope<Unit>> {
        confirmException?.let { throw it }
        return confirmResponse
    }

    override suspend fun deleteAccount(): Response<ApiEnvelope<Unit>> {
        deleteException?.let { throw it }
        return deleteResponse
    }
}

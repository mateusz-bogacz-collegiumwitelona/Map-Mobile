package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.ChangeEmailRequestDto
import com.example.map.data.remote.dto.ConfirmEmailChangeRequestDto
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteAccountRepositoryTest {

    private val gson = Gson()
    private val service = FakeUserApiService()
    private val repository = RemoteAccountRepository(service, gson)

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
    fun returnsSuccessWhenDeletionSucceeds() = runTest {
        service.deleteResponse = Response.success(ApiResultDto(isSuccess = true))

        assertEquals(AccountDeletionResult.Success, repository.deleteAccount())
    }

    private fun errorResponse(
        status: Int,
        errorCode: String? = null,
        message: String? = null
    ): Response<ApiResultDto> {
        val body = gson.toJson(
            ApiResultDto(
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

    var requestResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))
    var confirmResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))
    var deleteResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))

    var requestException: Exception? = null
    var confirmException: Exception? = null
    var deleteException: Exception? = null

    override suspend fun requestEmailChange(
        request: ChangeEmailRequestDto
    ): Response<ApiResultDto> {
        requestException?.let { throw it }
        return requestResponse
    }

    override suspend fun confirmEmailChange(
        request: ConfirmEmailChangeRequestDto
    ): Response<ApiResultDto> {
        confirmException?.let { throw it }
        return confirmResponse
    }

    override suspend fun deleteAccount(): Response<ApiResultDto> {
        deleteException?.let { throw it }
        return deleteResponse
    }
}

package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.ChangeEmailRequestDto
import com.example.map.data.remote.dto.ConfirmEmailChangeRequestDto
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.repository.AccountRepository
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class RemoteAccountRepository @Inject constructor(
    private val userApiService: UserApiService,
    private val gson: Gson
) : AccountRepository {

    override suspend fun requestEmailChange(newEmail: String): EmailChangeResult =
        execute {
            userApiService.requestEmailChange(ChangeEmailRequestDto(newEmail))
        }

    override suspend fun confirmEmailChange(code: String): EmailChangeResult =
        execute {
            userApiService.confirmEmailChange(ConfirmEmailChangeRequestDto(code))
        }

    private suspend fun execute(
        request: suspend () -> Response<ApiResultDto>
    ): EmailChangeResult {
        return try {
            val response = request()
            val body = response.body()

            if (response.isSuccessful && body?.isSuccess == true) {
                EmailChangeResult.Success
            } else {
                val errorBody = body ?: parseErrorBody(response)
                EmailChangeResult.Failure(
                    error = mapError(response.code(), errorBody?.errorCode),
                    message = errorBody?.message
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: IOException) {
            EmailChangeResult.Failure(EmailChangeError.NETWORK)
        } catch (exception: Exception) {
            EmailChangeResult.Failure(
                error = EmailChangeError.UNKNOWN,
                message = exception.message
            )
        }
    }

    private fun parseErrorBody(response: Response<ApiResultDto>): ApiResultDto? {
        val rawBody = response.errorBody()?.string() ?: return null
        return runCatching {
            gson.fromJson(rawBody, ApiResultDto::class.java)
        }.getOrNull()
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

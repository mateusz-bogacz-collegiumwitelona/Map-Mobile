package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiErrorDto
import com.example.map.data.remote.dto.AuthResponseDto
import com.example.map.data.remote.dto.LogoutRequestDto
import com.example.map.data.remote.dto.LoginRequestDto
import com.example.map.data.remote.service.AuthApi
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.repository.AuthRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.example.map.data.remote.dto.RefreshTokenRequestDto
import com.example.map.data.remote.dto.ApiEnvelopeDto
import kotlinx.serialization.SerializationException
import retrofit2.Response

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val json: Json
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthenticatedAccount =
        executeAuthCall {
            authApi.login(LoginRequestDto(email = email, password = password))
        }.toDomain()

    override suspend fun refresh(
        refreshToken: String
    ): AuthenticatedAccount =
        executeAuthCall {
            authApi.refresh(RefreshTokenRequestDto(refreshToken))
        }.toDomain()
    private suspend fun <T : Any> executeAuthCall(
        apiCall: suspend () -> Response<ApiEnvelopeDto<T>>
    ): T {
        val response = try {
            apiCall()
        } catch (error: IOException) {
            throw AuthException(AuthFailure.NETWORK_ERROR, error)
        } catch (error: SerializationException) {
            throw AuthException(AuthFailure.INVALID_RESPONSE, error)
        }

        if (!response.isSuccessful) {
            throw failure(
                errorCode = readErrorCode(response.errorBody()?.string()),
                statusCode = response.code()
            )
        }

        val envelope = response.body()
            ?: throw AuthException(AuthFailure.INVALID_RESPONSE)

        if (!envelope.isSuccess) {
            throw failure(
                errorCode = envelope.errorCode,
                statusCode = envelope.statusCode ?: response.code()
            )
        }

        return envelope.data
            ?: throw AuthException(AuthFailure.INVALID_RESPONSE)
    }

    override suspend fun logout(
        accessToken: String,
        refreshToken: String
    ) {
        val response = try {
            authApi.logout(
                authorization = "Bearer $accessToken",
                request = LogoutRequestDto(refreshToken)
            )
        } catch (error: IOException) {
            throw AuthException(AuthFailure.NETWORK_ERROR, error)
        }

        if (!response.isSuccessful) {
            val errorCode = readErrorCode(response.errorBody()?.string())

            if (response.code() == 401 || errorCode == "AUTH_003") {
                throw AuthException(AuthFailure.INVALID_SESSION)
            }

            throw failure(
                errorCode = errorCode,
                statusCode = response.code()
            )
        }

        response.body()?.close()
    }

    private fun AuthResponseDto.toDomain() = AuthenticatedAccount(
        tokens = AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        ),
        roles = roles
    )

    private fun readErrorCode(body: String?): String? {
        if (body.isNullOrBlank()) return null

        return try {
            json.decodeFromString<ApiErrorDto>(body).errorCode
        } catch (_: Exception) {
            null
        }
    }

    private fun failure(errorCode: String?, statusCode: Int): AuthException {
        val reason = when (errorCode) {
            "AUTH_001" -> AuthFailure.INVALID_CREDENTIALS
            "AUTH_002" -> AuthFailure.EMAIL_NOT_CONFIRMED
            "AUTH_003" -> AuthFailure.INVALID_SESSION
            "AUTH_004" -> AuthFailure.ACCOUNT_LOCKED
            "AUTH_005" -> AuthFailure.ACCOUNT_BANNED
            "COMMON_005" -> AuthFailure.TOO_MANY_ATTEMPTS
            else -> when (statusCode) {
                401 -> AuthFailure.INVALID_CREDENTIALS
                423 -> AuthFailure.ACCOUNT_LOCKED
                403 -> AuthFailure.ACCOUNT_BANNED
                429 -> AuthFailure.TOO_MANY_ATTEMPTS
                else -> AuthFailure.SERVER_ERROR
            }
        }

        return AuthException(reason)
    }
}
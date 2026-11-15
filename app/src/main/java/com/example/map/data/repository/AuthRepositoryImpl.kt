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

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val json: Json
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthenticatedAccount {
        val response = try {
            authApi.login(LoginRequestDto(email = email, password = password))
        } catch (error: IOException) {
            throw AuthException(AuthFailure.NETWORK_ERROR, error)
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

        val data = envelope.data
            ?: throw AuthException(AuthFailure.INVALID_RESPONSE)

        return data.toDomain()
    }

    override suspend fun refresh(
        refreshToken: String
    ): AuthenticatedAccount {
        val response = try {
            authApi.refresh(RefreshTokenRequestDto(refreshToken))
        } catch (error: IOException) {
            throw AuthException(AuthFailure.NETWORK_ERROR, error)
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

        val data = envelope.data
            ?: throw AuthException(AuthFailure.INVALID_RESPONSE)

        return data.toDomain()
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
            throw failure(
                errorCode = readErrorCode(response.errorBody()?.string()),
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
            else -> when (statusCode) {
                401 -> AuthFailure.INVALID_CREDENTIALS
                423 -> AuthFailure.ACCOUNT_LOCKED
                403 -> AuthFailure.ACCOUNT_BANNED
                else -> AuthFailure.SERVER_ERROR
            }
        }

        return AuthException(reason)
    }
}
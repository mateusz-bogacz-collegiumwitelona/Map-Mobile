package com.example.map.data.repository

import com.example.map.data.remote.dto.response.AuthResponse
import com.example.map.data.remote.dto.request.LogoutRequest
import com.example.map.data.remote.dto.request.LoginRequest
import com.example.map.data.remote.service.AuthApiService
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.repository.AuthRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.serialization.json.Json
import com.example.map.data.remote.dto.request.RefreshTokenRequest
import com.example.map.data.remote.dto.common.ApiEnvelope
import kotlinx.serialization.SerializationException
import retrofit2.Response

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApiService,
    private val json: Json
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): AuthenticatedAccount =
        executeAuthCall {
            authApi.login(LoginRequest(email = email, password = password))
        }.toDomain()

    override suspend fun refresh(
        refreshToken: String
    ): AuthenticatedAccount =
        executeAuthCall {
            authApi.refresh(RefreshTokenRequest(refreshToken))
        }.toDomain()
    private suspend fun <T : Any> executeAuthCall(
        apiCall: suspend () -> Response<ApiEnvelope<T>>
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
                errorCode = response.parseErrorEnvelope(json)?.errorCode,
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
                request = LogoutRequest(refreshToken)
            )
        } catch (error: IOException) {
            throw AuthException(AuthFailure.NETWORK_ERROR, error)
        }

        if (!response.isSuccessful) {
            val errorCode = response.parseErrorEnvelope(json)?.errorCode

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

    private fun AuthResponse.toDomain() = AuthenticatedAccount(
        tokens = AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        ),
        roles = roles
    )

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
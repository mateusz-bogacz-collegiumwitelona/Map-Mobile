package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.ConfirmEmailRequest
import com.example.map.data.remote.dto.request.LoginRequest
import com.example.map.data.remote.dto.request.LogoutRequest
import com.example.map.data.remote.dto.request.RefreshTokenRequest
import com.example.map.data.remote.dto.request.RegisterRequest
import com.example.map.data.remote.dto.request.ResendVerificationCodeRequest
import com.example.map.data.remote.dto.response.AuthResponse
import com.example.map.data.remote.service.AuthApiService
import com.example.map.domain.model.account.EmailVerificationError
import com.example.map.domain.model.account.EmailVerificationResult
import com.example.map.domain.model.auth.AuthException
import com.example.map.domain.model.auth.AuthFailure
import com.example.map.domain.model.auth.AuthTokens
import com.example.map.domain.model.auth.AuthenticatedAccount
import com.example.map.domain.model.auth.RegistrationError
import com.example.map.domain.model.auth.RegistrationResult
import com.example.map.domain.repository.AuthRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

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

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        bio: String?,
        isPublic: Boolean
    ): RegistrationResult =
        executeUnitApiCall(
            json = json,
            request = {
                authApi.register(
                    RegisterRequest(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword,
                        bio = bio?.takeIf { it.isNotBlank() },
                        isPublic = isPublic
                    )
                )
            },
            onSuccess = { RegistrationResult.Success },
            onApiError = { status, code, msg ->
                RegistrationResult.Failure(mapRegistrationError(status, code), msg)
            },
            onNetworkError = { RegistrationResult.Failure(RegistrationError.NETWORK) },
            onUnknownError = { msg -> RegistrationResult.Failure(RegistrationError.UNKNOWN, msg) }
        )

    override suspend fun confirmEmail(
        email: String,
        code: String
    ): EmailVerificationResult =
        executeUnitApiCall(
            json = json,
            request = {
                authApi.confirmEmail(
                    ConfirmEmailRequest(
                        email = email.trim(),
                        code = code.trim()
                    )
                )
            },
            onSuccess = { EmailVerificationResult.Success },
            onApiError = { status, codeValue, msg ->
                EmailVerificationResult.Failure(
                    error = mapEmailVerificationError(status, codeValue),
                    message = msg
                )
            },
            onNetworkError = {
                EmailVerificationResult.Failure(error = EmailVerificationError.NETWORK)
            },
            onUnknownError = { msg ->
                EmailVerificationResult.Failure(
                    error = EmailVerificationError.UNKNOWN,
                    message = msg
                )
            }
        )

    override suspend fun resendVerificationCode(
        email: String
    ): EmailVerificationResult =
        executeUnitApiCall(
            json = json,
            request = {
                authApi.resendVerificationCode(
                    ResendVerificationCodeRequest(
                        email = email.trim()
                    )
                )
            },
            onSuccess = { EmailVerificationResult.Success },
            onApiError = { status, codeValue, msg ->
                EmailVerificationResult.Failure(
                    error = mapEmailVerificationError(status, codeValue),
                    message = msg
                )
            },
            onNetworkError = {
                EmailVerificationResult.Failure(error = EmailVerificationError.NETWORK)
            },
            onUnknownError = { msg ->
                EmailVerificationResult.Failure(
                    error = EmailVerificationError.UNKNOWN,
                    message = msg
                )
            }
        )


    private fun mapEmailVerificationError(
        statusCode: Int,
        errorCode: String?
    ): EmailVerificationError =
        when (errorCode) {
            "COMMON_005" -> EmailVerificationError.RATE_LIMITED
            else -> when (statusCode) {
                429 -> EmailVerificationError.RATE_LIMITED
                400 -> EmailVerificationError.INVALID_CODE
                else -> EmailVerificationError.UNKNOWN
            }
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

    private fun mapRegistrationError(
        statusCode: Int,
        errorCode: String?
    ): RegistrationError =
        when (errorCode) {
            "USR_001" -> RegistrationError.FIRST_NAME_INVALID
            "USR_002" -> RegistrationError.LAST_NAME_INVALID
            "USR_003" -> RegistrationError.BIO_INVALID
            "USR_004" -> RegistrationError.EMAIL_INVALID
            "VAL_001" -> RegistrationError.VALIDATION_ERROR
            "COMMON_005" -> RegistrationError.RATE_LIMITED
            else -> when (statusCode) {
                429 -> RegistrationError.RATE_LIMITED
                else -> RegistrationError.UNKNOWN
            }
        }

}
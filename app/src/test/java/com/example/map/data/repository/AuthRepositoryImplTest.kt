package com.example.map.data.repository

import com.example.map.data.remote.service.AuthApiService
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.EmailVerificationError
import com.example.map.domain.model.EmailVerificationResult
import com.example.map.domain.model.RegistrationError
import com.example.map.domain.model.RegistrationResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthRepositoryImplTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: AuthRepositoryImpl

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(false)
                    .build()
            )
            .addConverterFactory(
                json.asConverterFactory(
                    "application/json; charset=utf-8".toMediaType()
                )
            )
            .build()

        repository = AuthRepositoryImpl(
            authApi = retrofit.create(AuthApiService::class.java),
            json = json
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun loginPostsCredentialsAndMapsSuccessfulResponse() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "message": "Authentication successful.",
                      "errorCode": null,
                      "statusCode": 200,
                      "errors": null,
                      "data": {
                        "userId": "00000000-0000-0000-0000-000000000001",
                        "roles": ["User"],
                        "accessToken": "access-test",
                        "accessTokenExpiration": "2030-01-01T00:00:00Z",
                        "refreshToken": "refresh-test"
                      }
                    }
                    """.trimIndent()
                )
        )

        val account = repository.login(
            email = "user@example.com",
            password = "Password123!"
        )

        assertEquals("access-test", account.tokens.accessToken)
        assertEquals("refresh-test", account.tokens.refreshToken)
        assertEquals(listOf("User"), account.roles)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/login", request.path)
        assertEquals(
            """{"email":"user@example.com","password":"Password123!"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun loginMapsInvalidCredentialsError() = runBlocking {
        assertLoginFailure(
            httpStatus = 401,
            errorCode = "AUTH_001",
            expectedFailure = AuthFailure.INVALID_CREDENTIALS
        )
    }

    @Test
    fun loginMapsLockedAccountError() = runBlocking {
        assertLoginFailure(
            httpStatus = 423,
            errorCode = "AUTH_004",
            expectedFailure = AuthFailure.ACCOUNT_LOCKED
        )
    }

    @Test
    fun loginMapsNetworkFailure() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val error = captureAuthException {
            repository.login("user@example.com", "Password123!")
        }

        assertEquals(AuthFailure.NETWORK_ERROR, error.failure)
    }

    @Test
    fun loginMapsMalformedSuccessfulResponseToInvalidResponse() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("<html>proxy error</html>")
        )

        val error = captureAuthException {
            repository.login("user@example.com", "Password123!")
        }

        assertEquals(AuthFailure.INVALID_RESPONSE, error.failure)
    }

    @Test
    fun logoutSendsBearerTokenAndRefreshToken() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "message": "Logged out successfully.",
                      "errorCode": null,
                      "statusCode": 200,
                      "errors": null
                    }
                    """.trimIndent()
                )
        )

        repository.logout(
            accessToken = "access-test",
            refreshToken = "refresh-test"
        )

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/logout", request.path)
        assertEquals("Bearer access-test", request.getHeader("Authorization"))
        assertEquals(
            """{"refreshToken":"refresh-test"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun loginMapsEmailNotConfirmedError() = runBlocking {
        assertLoginFailure(
            httpStatus = 403,
            errorCode = "AUTH_002",
            expectedFailure = AuthFailure.EMAIL_NOT_CONFIRMED
        )
    }

    @Test
    fun loginMaps429StatusEvenWhenErrorCodeIsUnknown() = runBlocking {
        assertLoginFailure(
            httpStatus = 429,
            errorCode = "UNKNOWN_CODE",
            expectedFailure = AuthFailure.TOO_MANY_ATTEMPTS
        )
    }

    @Test
    fun refreshPostsRefreshTokenAndMapsRotatedTokens() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                {
                  "isSuccess": true,
                  "message": "Token refreshed.",
                  "errorCode": null,
                  "statusCode": 200,
                  "errors": null,
                  "data": {
                    "userId": "00000000-0000-0000-0000-000000000001",
                    "roles": ["User"],
                    "accessToken": "access-new",
                    "accessTokenExpiration": "2030-01-01T00:00:00Z",
                    "refreshToken": "refresh-new"
                  }
                }
                """.trimIndent()
                )
        )

        val account = repository.refresh("refresh-old")

        assertEquals("access-new", account.tokens.accessToken)
        assertEquals("refresh-new", account.tokens.refreshToken)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/refresh", request.path)
        assertEquals(
            """{"refreshToken":"refresh-old"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun loginMapsRateLimitError() = runBlocking {
        assertLoginFailure(
            httpStatus = 429,
            errorCode = "COMMON_005",
            expectedFailure = AuthFailure.TOO_MANY_ATTEMPTS
        )
    }

    @Test
    fun registerPostsPayloadAndNormalizesBlankBioToNull() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "message": "Registered.",
                      "errorCode": null,
                      "statusCode": 200,
                      "errors": null
                    }
                    """.trimIndent()
                )
        )

        val result = repository.register(
            firstName = "Jan",
            lastName = "Kowalski",
            email = "jan@example.com",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = "   ",
            isPublic = true
        )

        assertEquals(RegistrationResult.Success, result)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/register", request.path)

        val body = json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("Jan", body["firstName"]?.jsonPrimitive?.content)
        assertEquals("Kowalski", body["lastName"]?.jsonPrimitive?.content)
        assertEquals("jan@example.com", body["email"]?.jsonPrimitive?.content)
        assertEquals("Password1!", body["password"]?.jsonPrimitive?.content)
        assertEquals("Password1!", body["confirmPassword"]?.jsonPrimitive?.content)
        assertEquals("true", body["isPublic"]?.jsonPrimitive?.content)
        assertEquals(true, body["bio"] == null || body["bio"]?.jsonPrimitive?.isString == false)
    }

    @Test
    fun registerMapsApiErrorCodes() = runBlocking {
        val cases = listOf(
            Triple(400, "USR_001", RegistrationError.FIRST_NAME_INVALID),
            Triple(400, "USR_002", RegistrationError.LAST_NAME_INVALID),
            Triple(400, "USR_003", RegistrationError.BIO_INVALID),
            Triple(400, "USR_004", RegistrationError.EMAIL_INVALID),
            Triple(400, "VAL_001", RegistrationError.VALIDATION_ERROR),
            Triple(429, "COMMON_005", RegistrationError.RATE_LIMITED),
            Triple(429, "UNKNOWN_CODE", RegistrationError.RATE_LIMITED),
            Triple(500, "SERVER_ERR", RegistrationError.UNKNOWN)
        )

        for ((status, errorCode, expectedError) in cases) {
            assertRegisterFailure(status, errorCode, expectedError)
        }
    }

    @Test
    fun registerMapsNetworkFailure() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val result = repository.register(
            firstName = "Jan",
            lastName = "Kowalski",
            email = "jan@example.com",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = null,
            isPublic = false
        )

        assertEquals(RegistrationResult.Failure(RegistrationError.NETWORK), result)
    }

    @Test
    fun confirmEmailTrimsInputAndReturnsSuccess() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "message": "Email confirmed.",
                      "errorCode": null,
                      "statusCode": 200,
                      "errors": null
                    }
                    """.trimIndent()
                )
        )

        val result = repository.confirmEmail(
            email = "  jan@example.com  ",
            code = "  123456  "
        )

        assertEquals(EmailVerificationResult.Success, result)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/confirm-email", request.path)
        assertEquals(
            """{"email":"jan@example.com","code":"123456"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun confirmEmailMapsApiErrors() = runBlocking {
        assertConfirmEmailFailure(400, "VER_001", EmailVerificationError.INVALID_CODE)
        assertConfirmEmailFailure(429, "COMMON_005", EmailVerificationError.RATE_LIMITED)
        assertConfirmEmailFailure(429, "OTHER", EmailVerificationError.RATE_LIMITED)
        assertConfirmEmailFailure(500, "SERVER_ERR", EmailVerificationError.UNKNOWN)
    }

    @Test
    fun resendVerificationCodeTrimsEmailAndReturnsSuccess() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                    {
                      "isSuccess": true,
                      "message": "Code resent.",
                      "errorCode": null,
                      "statusCode": 200,
                      "errors": null
                    }
                    """.trimIndent()
                )
        )

        val result = repository.resendVerificationCode("  jan@example.com  ")

        assertEquals(EmailVerificationResult.Success, result)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/confirm-email/resend", request.path)
        assertEquals(
            """{"email":"jan@example.com"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun resendVerificationCodeMapsApiErrors() = runBlocking {
        assertResendFailure(400, "VER_001", EmailVerificationError.INVALID_CODE)
        assertResendFailure(429, "COMMON_005", EmailVerificationError.RATE_LIMITED)
        assertResendFailure(500, "SERVER_ERR", EmailVerificationError.UNKNOWN)
    }

    @Test
    fun resendVerificationCodeMapsNetworkFailure() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val result = repository.resendVerificationCode("jan@example.com")

        assertEquals(EmailVerificationResult.Failure(EmailVerificationError.NETWORK), result)
    }

    @Test
    fun confirmEmailMapsNetworkFailure() = runBlocking {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val result = repository.confirmEmail("jan@example.com", "123456")

        assertEquals(EmailVerificationResult.Failure(EmailVerificationError.NETWORK), result)
    }

    private suspend fun assertRegisterFailure(
        httpStatus: Int,
        errorCode: String,
        expectedError: RegistrationError
    ) {
        enqueueErrorResponse(httpStatus, errorCode)

        val result = repository.register(
            firstName = "Jan",
            lastName = "Kowalski",
            email = "jan@example.com",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = null,
            isPublic = false
        )

        assertEquals(expectedError, (result as RegistrationResult.Failure).error)
    }

    private suspend fun assertConfirmEmailFailure(
        httpStatus: Int,
        errorCode: String,
        expectedError: EmailVerificationError
    ) {
        enqueueErrorResponse(httpStatus, errorCode)

        val result = repository.confirmEmail("jan@example.com", "123456")

        assertEquals(expectedError, (result as EmailVerificationResult.Failure).error)
    }

    private suspend fun assertResendFailure(
        httpStatus: Int,
        errorCode: String,
        expectedError: EmailVerificationError
    ) {
        enqueueErrorResponse(httpStatus, errorCode)

        val result = repository.resendVerificationCode("jan@example.com")

        assertEquals(expectedError, (result as EmailVerificationResult.Failure).error)
    }

    private fun enqueueErrorResponse(httpStatus: Int, errorCode: String) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(httpStatus)
                .setBody(
                    """
                    {
                      "isSuccess": false,
                      "message": "Request failed.",
                      "errorCode": "$errorCode",
                      "statusCode": $httpStatus,
                      "errors": [],
                      "data": null
                    }
                    """.trimIndent()
                )
        )
    }

    private suspend fun assertLoginFailure(
        httpStatus: Int,
        errorCode: String,
        expectedFailure: AuthFailure
    ) {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(httpStatus)
                .setBody(
                    """
                    {
                      "isSuccess": false,
                      "message": "Request failed.",
                      "errorCode": "$errorCode",
                      "statusCode": $httpStatus,
                      "errors": [],
                      "data": null
                    }
                    """.trimIndent()
                )
        )

        val error = captureAuthException {
            repository.login("user@example.com", "wrong-password")
        }

        assertEquals(expectedFailure, error.failure)
    }

    private suspend fun <T> captureAuthException(
        action: suspend () -> T
    ): AuthException {
        var caught: AuthException? = null

        try {
            action()
        } catch (error: AuthException) {
            caught = error
        }

        assertNotNull(caught)
        return caught!!
    }
}
package com.example.map.data.repository

import com.example.map.data.remote.service.AuthApi
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
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
            authApi = retrofit.create(AuthApi::class.java),
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
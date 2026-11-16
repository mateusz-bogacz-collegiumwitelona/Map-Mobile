package com.example.map.di

import com.example.map.BuildConfig
import com.example.map.data.remote.AuthInterceptor
import com.example.map.domain.model.AuthTokens
import com.example.map.testing.FakeTokenStorage
import kotlinx.serialization.Serializable
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

class NetworkModuleTest {

    private lateinit var mockWebServer: MockWebServer

    private fun clientWithNoToken(): okhttp3.OkHttpClient =
        NetworkModule.provideOkHttpClient(
            AuthInterceptor(FakeTokenStorage())
        )

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun retrofitUsesTheBaseUrlFromBuildConfiguration() {
        val json = NetworkModule.provideJson()
        val client = clientWithNoToken()
        val retrofit = NetworkModule.provideRetrofit(json, client)

        assertEquals(
            BuildConfig.API_BASE_URL,
            retrofit.baseUrl().toString()
        )
        assertTrue(BuildConfig.API_BASE_URL.endsWith("/"))
    }

    @Test
    fun jsonEncodesAndIgnoresUnknownResponseFields() {
        val json = NetworkModule.provideJson()

        assertEquals(
            """{"name":"Map"}""",
            json.encodeToString(JsonSample(name = "Map"))
        )

        val decoded = json.decodeFromString<JsonSample>(
            """{"name":"Map","newBackendField":true}"""
        )

        assertEquals("Map", decoded.name)
    }

    @Test
    fun clientHasTimeoutsAndAddsLoggingOnlyInDebug() {
        val client = clientWithNoToken()

        assertEquals(15_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
        assertEquals(45_000, client.callTimeoutMillis)
        assertTrue(client.retryOnConnectionFailure)

        val loggingInterceptors =
            client.interceptors.filterIsInstance<HttpLoggingInterceptor>()

        if (BuildConfig.DEBUG) {
            assertEquals(1, loggingInterceptors.size)
            assertEquals(
                HttpLoggingInterceptor.Level.BASIC,
                loggingInterceptors.single().level
            )
        } else {
            assertTrue(loggingInterceptors.isEmpty())
        }
    }

    @Test
    fun retrofitClientCanMakeARequest() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"name":"server"}""")
        )

        val retrofit = NetworkModule.provideRetrofit(
            NetworkModule.provideJson(),
            clientWithNoToken()
        )
            .newBuilder()
            .baseUrl(mockWebServer.url("/"))
            .build()

        val api = retrofit.create(ProbeApi::class.java)
        val response = api.probe(JsonSample(name = "client")).execute()

        assertEquals(200, response.code())
        assertEquals("server", response.body()?.name)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/probe", request.path)
        assertEquals("""{"name":"client"}""", request.body.readUtf8())
    }

    @Test
    fun clientAttachesBearerTokenOnlyToAuthenticatedEndpoints() {
        val tokenStorage = FakeTokenStorage(
            AuthTokens(
                accessToken = "access-test",
                refreshToken = "refresh-test"
            )
        )
        val client = NetworkModule.provideOkHttpClient(AuthInterceptor(tokenStorage))

        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        client.newCall(
            Request.Builder().url(mockWebServer.url("/api/user")).build()
        ).execute().close()

        assertEquals(
            "Bearer access-test",
            mockWebServer.takeRequest().getHeader("Authorization")
        )

        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        client.newCall(
            Request.Builder().url(mockWebServer.url("/api/auth/login")).build()
        ).execute().close()

        assertNull(mockWebServer.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun providesUserApiServiceFromTheSharedRetrofit() {
        val retrofit = NetworkModule.provideRetrofit(
            NetworkModule.provideJson(),
            clientWithNoToken()
        )

        assertNotNull(NetworkModule.provideUserApiService(retrofit))
    }

    @Serializable
    private data class JsonSample(
        val name: String
    )
    private interface ProbeApi {
        @POST("probe")
        fun probe(@Body sample: JsonSample): Call<JsonSample>
    }
}

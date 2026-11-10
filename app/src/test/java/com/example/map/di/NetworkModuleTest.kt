package com.example.map.di

import com.example.map.BuildConfig
import kotlinx.serialization.Serializable
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import okhttp3.ResponseBody

class NetworkModuleTest {

    private lateinit var mockWebServer: MockWebServer

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
        val client = NetworkModule.provideOkHttpClient()
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
        val client = NetworkModule.provideOkHttpClient()

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
                .setBody("pong")
        )

        val client = NetworkModule.provideOkHttpClient()
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .build()

        val api = retrofit.create(ProbeApi::class.java)

        val response = api.probe().execute()
        assertEquals(200, response.code())
        assertEquals("pong", response.body()?.string())

        assertEquals("/probe", mockWebServer.takeRequest().path)
    }

    @Serializable
    private data class JsonSample(
        val name: String
    )
    private interface ProbeApi {
        @GET("probe")
        fun probe(): Call<ResponseBody>
    }
}
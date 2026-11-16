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
import retrofit2.http.Body
import retrofit2.http.POST

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
            """{"name":"Map","isPublic":true}""",
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
    fun retrofitClientSerializesRequestAndDeserializesResponse() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"name":"Server map","isPublic":false,"newField":"ignored"}"""
                )
        )

        val json = NetworkModule.provideJson()
        val client = NetworkModule.provideOkHttpClient()
        val retrofit = NetworkModule.provideRetrofit(json, client)
            .newBuilder()
            .baseUrl(mockWebServer.url("/"))
            .build()

        val api = retrofit.create(ProbeApi::class.java)
        val response = api.probe(JsonSample(name = "Client map")).execute()

        assertTrue(response.isSuccessful)
        assertEquals(JsonSample(name = "Server map", isPublic = false), response.body())

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/probe", request.path)
        assertEquals(
            """{"name":"Client map","isPublic":true}""",
            request.body.readUtf8()
        )
    }

    @Serializable
    private data class JsonSample(
        val name: String,
        val isPublic: Boolean = true
    )

    private interface ProbeApi {
        @POST("probe")
        fun probe(@Body request: JsonSample): Call<JsonSample>
    }
}
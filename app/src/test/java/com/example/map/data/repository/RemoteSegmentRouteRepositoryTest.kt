package com.example.map.data.repository

import com.example.map.data.remote.service.TripApiService
import com.example.map.di.NetworkModule
import com.example.map.domain.model.GeoPoint
import com.example.map.domain.model.RouteGeometry
import com.example.map.domain.model.SegmentRouteSaveError
import com.example.map.domain.model.SegmentRouteSaveResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteSegmentRouteRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteSegmentRouteRepository

    private val json = NetworkModule.provideJson()
    private val segmentId = "0d7c5a4e-3b2f-4f7e-9c1a-2b6d8e9f0a11"
    private val route = RouteGeometry(
        points = listOf(GeoPoint(52.2297, 21.0122), GeoPoint(51.4, 20.4), GeoPoint(50.0647, 19.945)),
        distanceKm = 293.41254,
        isFallbackRoute = false
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TripApiService::class.java)

        repository = RemoteSegmentRouteRepository(service, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsRouteToTheSegmentRouteEndpoint() = runTest {
        server.enqueue(MockResponse().setBody("""{"isSuccess":true,"statusCode":200}"""))

        val result = repository.saveRoute(segmentId, route)

        val request = server.takeRequest()
        assertEquals(SegmentRouteSaveResult.Success, result)
        assertEquals("PUT", request.method)
        assertEquals("/api/trip/segments/$segmentId/route", request.path)

        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("293.41", body.getValue("distanceKm").jsonPrimitive.content)
        assertEquals("false", body.getValue("isFallbackRoute").jsonPrimitive.content)
        assertEquals(
            listOf(52.2297 to 21.0122, 51.4 to 20.4, 50.0647 to 19.945),
            body.getValue("coordinates").jsonArray.map { point ->
                point.jsonObject.getValue("latitude").jsonPrimitive.double to
                    point.jsonObject.getValue("longitude").jsonPrimitive.double
            }
        )
    }

    @Test
    fun sendsFallbackFlag() = runTest {
        server.enqueue(MockResponse().setBody("""{"isSuccess":true}"""))

        repository.saveRoute(segmentId, route.copy(isFallbackRoute = true))

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("true", body.getValue("isFallbackRoute").jsonPrimitive.content)
    }

    @Test
    fun mapsRouteValidationErrorsToInvalidRoute() = runTest {
        listOf("TRIP_008", "TRIP_009", "TRIP_010").forEach { errorCode ->
            server.enqueue(errorResponse(400, errorCode, "Route rejected"))

            assertEquals(
                SegmentRouteSaveResult.Failure(SegmentRouteSaveError.INVALID_ROUTE, "Route rejected"),
                repository.saveRoute(segmentId, route)
            )
        }
    }

    @Test
    fun mapsMissingSegmentToNotFound() = runTest {
        server.enqueue(errorResponse(404, "COMMON_002", "Trip segment was not found."))

        assertEquals(
            SegmentRouteSaveResult.Failure(SegmentRouteSaveError.NOT_FOUND, "Trip segment was not found."),
            repository.saveRoute(segmentId, route)
        )
    }

    @Test
    fun mapsUnauthorizedStatus() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        assertEquals(
            SegmentRouteSaveResult.Failure(SegmentRouteSaveError.UNAUTHORIZED),
            repository.saveRoute(segmentId, route)
        )
    }

    @Test
    fun mapsRateLimitStatus() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        assertEquals(
            SegmentRouteSaveResult.Failure(SegmentRouteSaveError.RATE_LIMITED),
            repository.saveRoute(segmentId, route)
        )
    }

    @Test
    fun mapsConnectionFailureToNetworkError() = runTest {
        server.shutdown()

        assertEquals(
            SegmentRouteSaveResult.Failure(SegmentRouteSaveError.NETWORK),
            repository.saveRoute(segmentId, route)
        )
    }

    @Test
    fun doesNotSendRouteWithoutTwoDistinctPoints() = runTest {
        val point = GeoPoint(50.0647, 19.945)

        val result = repository.saveRoute(
            segmentId,
            RouteGeometry(points = listOf(point, point), distanceKm = 0.0, isFallbackRoute = false)
        )

        assertEquals(SegmentRouteSaveResult.Failure(SegmentRouteSaveError.INVALID_ROUTE), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun doesNotSendRouteShorterThanTheApiMinimum() = runTest {
        val result = repository.saveRoute(
            segmentId,
            RouteGeometry(
                points = listOf(GeoPoint(50.06470, 19.94500), GeoPoint(50.06471, 19.94501)),
                distanceKm = 0.001,
                isFallbackRoute = true
            )
        )

        assertEquals(SegmentRouteSaveResult.Failure(SegmentRouteSaveError.INVALID_ROUTE), result)
        assertEquals(0, server.requestCount)
    }

    private fun errorResponse(status: Int, errorCode: String, message: String): MockResponse =
        MockResponse()
            .setResponseCode(status)
            .setBody("""{"isSuccess":false,"errorCode":"$errorCode","message":"$message","statusCode":$status}""")
}

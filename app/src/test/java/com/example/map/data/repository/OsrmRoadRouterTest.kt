package com.example.map.data.repository

import com.example.map.data.remote.service.OsrmApiService
import com.example.map.di.NetworkModule
import com.example.map.di.RoutingModule
import com.example.map.domain.model.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class OsrmRoadRouterTest {

    private lateinit var server: MockWebServer
    private lateinit var router: OsrmRoadRouter

    private val warsaw = GeoPoint(52.2297, 21.0122)
    private val krakow = GeoPoint(50.0647, 19.9450)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        router = OsrmRoadRouter(serviceFor(RoutingModule.provideRoutingOkHttpClient()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun returnsRoadPointsAndDistanceInKilometers() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[20.4,51.4],[19.945,50.0647]]", distance = 293_412.5))

        val route = router.route(warsaw, krakow)

        assertNotNull(route)
        assertEquals(
            listOf(GeoPoint(52.2297, 21.0122), GeoPoint(51.4, 20.4), GeoPoint(50.0647, 19.945)),
            route!!.points
        )
        assertEquals(293.4125, route.distanceKm, 1e-9)
    }

    @Test
    fun requestsDrivingRouteWithLongitudeFirstAndFullGeoJsonGeometry() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        router.route(warsaw, krakow)

        assertEquals(
            "/route/v1/driving/21.012200,52.229700;19.945000,50.064700" +
                "?overview=full&geometries=geojson&alternatives=false&steps=false",
            server.takeRequest().path
        )
    }

    @Test
    fun writesSmallCoordinatesWithoutExponentNotation() = runTest {
        server.enqueue(okRoute("[[0.0001,0.0002],[0.01,0.02]]"))

        router.route(GeoPoint(0.0002, 0.0001), GeoPoint(0.02, 0.01))

        assertEquals(
            "/route/v1/driving/0.000100,0.000200;0.010000,0.020000",
            server.takeRequest().path!!.substringBefore("?")
        )
    }

    @Test
    fun doesNotSendAuthorizationHeaderToTheRoutingService() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        router.route(warsaw, krakow)

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun identifiesTheAppWithUserAgent() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        router.route(warsaw, krakow)

        val userAgent = server.takeRequest().getHeader("User-Agent")
        assertEquals(RoutingModule.USER_AGENT, userAgent)
        assertTrue(userAgent!!.startsWith("MAP-Mobile/"))
    }

    @Test
    fun waitsAtLeastOneSecondBetweenRequests() = runTest {
        val timedRouter = OsrmRoadRouter(serviceFor(RoutingModule.provideRoutingOkHttpClient())) {
            testScheduler.currentTime
        }
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))
        server.enqueue(okRoute("[[19.945,50.0647],[21.0122,52.2297]]"))

        timedRouter.route(warsaw, krakow)
        val afterFirst = testScheduler.currentTime
        timedRouter.route(krakow, warsaw)

        assertEquals(0L, afterFirst)
        assertEquals(OsrmRoadRouter.MIN_REQUEST_INTERVAL_MS, testScheduler.currentTime)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun doesNotWaitWhenTheLastRequestWasLongEnoughAgo() = runTest {
        val timedRouter = OsrmRoadRouter(serviceFor(RoutingModule.provideRoutingOkHttpClient())) {
            testScheduler.currentTime
        }
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))
        server.enqueue(okRoute("[[19.945,50.0647],[21.0122,52.2297]]"))

        timedRouter.route(warsaw, krakow)
        advanceTimeBy(1_500)
        timedRouter.route(krakow, warsaw)

        assertEquals(1_500L, testScheduler.currentTime)
    }

    @Test
    fun doesNotWaitForCachedRoutes() = runTest {
        val timedRouter = OsrmRoadRouter(serviceFor(RoutingModule.provideRoutingOkHttpClient())) {
            testScheduler.currentTime
        }
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        timedRouter.route(warsaw, krakow)
        timedRouter.route(warsaw, krakow)

        assertEquals(0L, testScheduler.currentTime)
    }

    @Test
    fun requestsTheSameEndpointsOnceWhenCalledConcurrently() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        val routes = List(3) { async { router.route(warsaw, krakow) } }.awaitAll()

        assertTrue(routes.all { it != null && it == routes.first() })
        assertEquals(1, server.requestCount)
    }

    @Test
    fun returnsNullWhenThereIsNoDrivingRoute() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"code":"NoRoute","message":"Impossible route between points"}""")
        )

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullWhenResponseCodeIsNotOk() = runTest {
        server.enqueue(MockResponse().setBody("""{"code":"NoSegment","routes":[]}"""))

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullOnServerError() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullOnMalformedBody() = runTest {
        server.enqueue(MockResponse().setBody("not json"))

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullWhenTheServiceTimesOut() = runTest {
        val shortTimeoutClient = OkHttpClient.Builder()
            .readTimeout(200, TimeUnit.MILLISECONDS)
            .build()
        val timingOutRouter = OsrmRoadRouter(serviceFor(shortTimeoutClient))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertNull(timingOutRouter.route(warsaw, krakow))
    }

    @Test
    fun returnsNullWhenAnEndpointIsSnappedTooFarFromTheRoad() = runTest {
        server.enqueue(
            okRoute(
                "[[21.0122,52.2297],[19.945,50.0647]]",
                waypointDistances = listOf(12.0, 10_000.1)
            )
        )

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun acceptsEndpointsSnappedWithinTheTolerance() = runTest {
        server.enqueue(
            okRoute(
                "[[21.0122,52.2297],[19.945,50.0647]]",
                waypointDistances = listOf(12.0, 10_000.0)
            )
        )

        assertNotNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullWhenGeometryHasInvalidCoordinates() = runTest {
        server.enqueue(okRoute("[[21.0122,95.0],[19.945,50.0647]]"))

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullWhenGeometryHasFewerThanTwoPoints() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297]]"))

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun returnsNullWhenDistanceIsMissing() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"code":"Ok","routes":[{"geometry":{"coordinates":[[21.0122,52.2297],[19.945,50.0647]]}}]}"""
            )
        )

        assertNull(router.route(warsaw, krakow))
    }

    @Test
    fun reusesTheRouteForTheSameEndpoints() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        val first = router.route(warsaw, krakow)
        val second = router.route(warsaw, krakow)

        assertEquals(first, second)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun requestsAgainWhenEndpointsChange() = runTest {
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))
        server.enqueue(okRoute("[[19.945,50.0647],[21.0122,52.2297]]"))

        router.route(warsaw, krakow)
        router.route(krakow, warsaw)

        assertEquals(2, server.requestCount)
    }

    @Test
    fun doesNotRememberFailedRequests() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(okRoute("[[21.0122,52.2297],[19.945,50.0647]]"))

        assertNull(router.route(warsaw, krakow))
        assertNotNull(router.route(warsaw, krakow))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun limitsLongRoutesToTheMaximumNumberOfPointsKeepingBothEnds() = runTest {
        val pointCount = 25_001
        val coordinates = (0 until pointCount).joinToString(prefix = "[", postfix = "]") { index ->
            "[${index * 0.0001},50.0]"
        }
        server.enqueue(okRoute(coordinates))

        val route = router.route(GeoPoint(50.0, 0.0), GeoPoint(50.0, 2.5))

        assertEquals(OsrmRoadRouter.MAX_ROUTE_POINTS, route!!.points.size)
        assertEquals(GeoPoint(50.0, 0.0), route.points.first())
        assertEquals(GeoPoint(50.0, (pointCount - 1) * 0.0001), route.points.last())
    }

    private fun serviceFor(client: OkHttpClient): OsrmApiService =
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(
                NetworkModule.provideJson().asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(OsrmApiService::class.java)

    private fun okRoute(
        coordinates: String,
        distance: Double = 293_000.0,
        waypointDistances: List<Double> = listOf(3.2, 4.8)
    ): MockResponse {
        val waypoints = waypointDistances.joinToString { """{"distance":$it}""" }
        return MockResponse().setBody(
            """
            {
              "code": "Ok",
              "routes": [
                {
                  "distance": $distance,
                  "duration": 12000.0,
                  "geometry": { "type": "LineString", "coordinates": $coordinates }
                }
              ],
              "waypoints": [$waypoints]
            }
            """.trimIndent()
        )
    }
}

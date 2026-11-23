package com.example.map.domain

import com.example.map.domain.geo.GeoDistance
import com.example.map.domain.model.GeoPoint
import com.example.map.domain.model.RoadRoute
import com.example.map.domain.model.TransportType
import com.example.map.domain.repository.RoadRouter
import com.example.map.domain.usecase.RouteGeometryCalculator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeometryCalculatorTest {

    private val roadRouter = FakeRoadRouter()
    private val calculator = RouteGeometryCalculator(roadRouter)
    private val warsaw = GeoPoint(52.2297, 21.0122)
    private val newYork = GeoPoint(40.6413, -73.7781)
    private val krakow = GeoPoint(50.0647, 19.9450)

    @Test
    fun planeUsesGreatCircleArcWithHaversineDistance() = runTest {
        val route = calculator.calculate(TransportType.PLANE, warsaw, newYork)

        assertTrue(route.points.size > 2)
        assertEquals(warsaw, route.points.first())
        assertEquals(newYork, route.points.last())
        assertEquals(GeoDistance.haversineKm(warsaw, newYork), route.distanceKm, 1e-9)
        assertFalse(route.isFallbackRoute)
        assertEquals(0, roadRouter.calls)
    }

    @Test
    fun trainAndOtherUseStraightLine() = runTest {
        listOf(TransportType.TRAIN, TransportType.OTHER).forEach { type ->
            val route = calculator.calculate(type, warsaw, newYork)

            assertEquals(listOf(warsaw, newYork), route.points)
            assertEquals(GeoDistance.haversineKm(warsaw, newYork), route.distanceKm, 1e-9)
            assertFalse(route.isFallbackRoute)
        }
        assertEquals(0, roadRouter.calls)
    }

    @Test
    fun carUsesRoadRouteWhenAvailable() = runTest {
        val roadPoints = listOf(warsaw, GeoPoint(51.4, 20.4), krakow)
        roadRouter.result = RoadRoute(points = roadPoints, distanceKm = 293.4)

        val route = calculator.calculate(TransportType.CAR, warsaw, krakow)

        assertEquals(roadPoints, route.points)
        assertEquals(293.4, route.distanceKm, 0.0)
        assertFalse(route.isFallbackRoute)
        assertEquals(warsaw to krakow, roadRouter.lastRequest)
    }

    @Test
    fun carWithoutRoadRouteFallsBackToFlaggedStraightLine() = runTest {
        roadRouter.result = null

        val route = calculator.calculate(TransportType.CAR, warsaw, newYork)

        assertEquals(listOf(warsaw, newYork), route.points)
        assertEquals(GeoDistance.haversineKm(warsaw, newYork), route.distanceKm, 1e-9)
        assertTrue(route.isFallbackRoute)
        assertEquals(1, roadRouter.calls)
    }

    private class FakeRoadRouter : RoadRouter {
        var result: RoadRoute? = null
        var calls = 0
        var lastRequest: Pair<GeoPoint, GeoPoint>? = null

        override suspend fun route(origin: GeoPoint, destination: GeoPoint): RoadRoute? {
            calls++
            lastRequest = origin to destination
            return result
        }
    }
}

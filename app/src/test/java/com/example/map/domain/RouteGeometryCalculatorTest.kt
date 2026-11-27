package com.example.map.domain

import com.example.map.domain.geo.GeoDistance
import com.example.map.domain.helper.RouteGeometryCalculator
import com.example.map.domain.model.GeoPoint
import com.example.map.domain.model.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteGeometryCalculatorTest {

    private val calculator = RouteGeometryCalculator()
    private val warsaw = GeoPoint(52.2297, 21.0122)
    private val newYork = GeoPoint(40.6413, -73.7781)

    @Test
    fun planeUsesGreatCircleArcWithHaversineDistance() {
        val route = calculator.calculate(TransportType.PLANE, warsaw, newYork)

        assertTrue(route.points.size > 2)
        assertEquals(warsaw, route.points.first())
        assertEquals(newYork, route.points.last())
        assertEquals(GeoDistance.haversineKm(warsaw, newYork), route.distanceKm, 1e-9)
        assertFalse(route.isFallbackRoute)
    }

    @Test
    fun trainAndOtherUseStraightLine() {
        listOf(TransportType.TRAIN, TransportType.OTHER).forEach { type ->
            val route = calculator.calculate(type, warsaw, newYork)

            assertEquals(listOf(warsaw, newYork), route.points)
            assertEquals(GeoDistance.haversineKm(warsaw, newYork), route.distanceKm, 1e-9)
            assertFalse(route.isFallbackRoute)
        }
    }

    @Test
    fun carWithoutRoadRouteFallsBackToFlaggedStraightLine() {
        val route = calculator.calculate(TransportType.CAR, warsaw, newYork)

        assertEquals(listOf(warsaw, newYork), route.points)
        assertEquals(GeoDistance.haversineKm(warsaw, newYork), route.distanceKm, 1e-9)
        assertTrue(route.isFallbackRoute)
    }
}

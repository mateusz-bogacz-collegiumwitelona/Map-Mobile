package com.example.map.domain.geo

import com.example.map.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class GreatCircleTest {

    private val warsaw = GeoPoint(52.2297, 21.0122)
    private val losAngeles = GeoPoint(34.0522, -118.2437)
    private val tokyo = GeoPoint(35.5494, 139.7798)
    private val sanFrancisco = GeoPoint(37.6213, -122.3790)

    @Test
    fun startsAtOriginAndEndsAtDestination() {
        val arc = GreatCircle.arc(warsaw, losAngeles)

        assertEquals(warsaw, arc.first())
        assertEquals(losAngeles, arc.last())
    }

    @Test
    fun keepsSegmentsWithinMaximumLength() {
        val arc = GreatCircle.arc(warsaw, losAngeles)

        arc.zipWithNext().forEach { (from, to) ->
            assertTrue(GeoDistance.haversineKm(from, to) <= GreatCircle.MAX_SEGMENT_LENGTH_KM + 1e-6)
        }
    }

    @Test
    fun usesSingleSegmentForShortDistances() {
        val arc = GreatCircle.arc(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.5))

        assertEquals(2, arc.size)
    }

    @Test
    fun followsTheShortestPath() {
        val arc = GreatCircle.arc(warsaw, losAngeles)
        val pathLength = arc.zipWithNext().sumOf { (from, to) -> GeoDistance.haversineKm(from, to) }

        assertEquals(GeoDistance.haversineKm(warsaw, losAngeles), pathLength, 1e-6)
    }

    @Test
    fun passesThroughTheMidpointOfTheEquator() {
        val arc = GreatCircle.arc(GeoPoint(0.0, 0.0), GeoPoint(0.0, 90.0), maxSegmentLengthKm = 5003.8)

        assertEquals(3, arc.size)
        assertEquals(0.0, arc[1].latitude, 1e-9)
        assertEquals(45.0, arc[1].longitude, 1e-9)
    }

    @Test
    fun curvesNorthOnLongEastWestFlights() {
        val arc = GreatCircle.arc(warsaw, losAngeles)

        assertEquals(70.623, arc.maxOf { it.latitude }, 0.01)
    }

    @Test
    fun crossesTheAntimeridianOverThePacific() {
        val arc = GreatCircle.arc(tokyo, sanFrancisco)

        assertTrue(arc.all { it.longitude >= tokyo.longitude || it.longitude <= sanFrancisco.longitude })
        assertTrue(arc.all { it.longitude in -180.0..180.0 })
    }

    @Test
    fun routesAntipodalPointsAlongHalfOfAGreatCircle() {
        assertHalfCircleRoute(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0))
    }

    @Test
    fun routesAntipodalPointsWithDetourBeyondTheAntimeridian() {
        assertHalfCircleRoute(GeoPoint(30.0, 120.0), GeoPoint(-30.0, -60.0))
    }

    @Test
    fun routesFromSouthPoleToNorthPole() {
        assertHalfCircleRoute(GeoPoint(-90.0, 0.0), GeoPoint(90.0, 0.0))
    }

    @Test
    fun routesFromNorthPoleToSouthPole() {
        assertHalfCircleRoute(GeoPoint(90.0, 0.0), GeoPoint(-90.0, 0.0))
    }

    @Test
    fun routesAntipodalPointsNearThePole() {
        assertHalfCircleRoute(GeoPoint(-89.99999999, 10.0), GeoPoint(89.99999999, -170.0))
    }

    @Test
    fun routesAntipodalPointsWhenCentralAngleIsRoundedBelowPi() {
        assertHalfCircleRoute(GeoPoint(-65.0832, -27.4619), GeoPoint(65.0832, 152.5381))
    }

    @Test
    fun routesAllAntipodalPointsOnAGridAlongHalfOfAGreatCircle() {
        for (latitudeStep in 0..24) {
            for (longitudeStep in 0..24) {
                val latitude = -87.3 + latitudeStep * 7.27
                val longitude = -178.9 + longitudeStep * 14.53
                val antipodeLongitude = if (longitude > 0.0) longitude - 180.0 else longitude + 180.0

                assertHalfCircleRoute(GeoPoint(latitude, longitude), GeoPoint(-latitude, antipodeLongitude))
            }
        }
    }

    private fun assertHalfCircleRoute(from: GeoPoint, to: GeoPoint) {
        val arc = GreatCircle.arc(from, to)
        val pathLength = arc.zipWithNext().sumOf { (a, b) -> GeoDistance.haversineKm(a, b) }

        assertEquals(from, arc.first())
        assertEquals(to, arc.last())
        assertEquals(PI * GeoDistance.EARTH_RADIUS_KM, pathLength, 1e-6)
        arc.zipWithNext().forEach { (a, b) ->
            assertTrue(GeoDistance.haversineKm(a, b) <= GreatCircle.MAX_SEGMENT_LENGTH_KM + 1e-6)
        }
    }

    @Test
    fun returnsBothPointsWhenOriginEqualsDestination() {
        val arc = GreatCircle.arc(warsaw, warsaw)

        assertEquals(listOf(warsaw, warsaw), arc)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveSegmentLength() {
        GreatCircle.arc(warsaw, losAngeles, maxSegmentLengthKm = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSegmentLengthBelowTheMinimum() {
        GreatCircle.arc(warsaw, losAngeles, maxSegmentLengthKm = 1e-9)
    }

    @Test
    fun acceptsTheMinimumSegmentLength() {
        val arc = GreatCircle.arc(
            GeoPoint(0.0, 0.0),
            GeoPoint(0.0, 1.0),
            maxSegmentLengthKm = GreatCircle.MIN_SEGMENT_LENGTH_KM
        )

        assertEquals(113, arc.size)
        arc.zipWithNext().forEach { (from, to) ->
            assertTrue(GeoDistance.haversineKm(from, to) <= GreatCircle.MIN_SEGMENT_LENGTH_KM + 1e-6)
        }
    }
}

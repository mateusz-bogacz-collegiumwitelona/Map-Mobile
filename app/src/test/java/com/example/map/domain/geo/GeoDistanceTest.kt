package com.example.map.domain.geo

import com.example.map.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class GeoDistanceTest {

    private val nashville = GeoPoint(36.12, -86.67)
    private val losAngeles = GeoPoint(33.94, -118.40)

    @Test
    fun matchesRosettaCodeReferenceForNashvilleToLosAngeles() {
        val distance = GeoDistance.haversineKm(nashville, losAngeles, radiusKm = 6372.8)

        assertEquals(2887.2599506071106, distance, 1e-6)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveRadius() {
        GeoDistance.haversineKm(nashville, losAngeles, radiusKm = 0.0)
    }

    @Test
    fun usesMeanEarthRadiusByDefault() {
        val distance = GeoDistance.haversineKm(nashville, losAngeles)

        assertEquals(2886.444442837984, distance, 1e-6)
    }

    @Test
    fun returnsZeroForTheSamePoint() {
        val point = GeoPoint(52.2297, 21.0122)

        assertEquals(0.0, GeoDistance.haversineKm(point, point), 0.0)
    }

    @Test
    fun isSymmetric() {
        assertEquals(
            GeoDistance.haversineKm(nashville, losAngeles),
            GeoDistance.haversineKm(losAngeles, nashville),
            1e-9
        )
    }

    @Test
    fun quarterOfTheEquatorIsQuarterOfTheCircumference() {
        val distance = GeoDistance.haversineKm(GeoPoint(0.0, 0.0), GeoPoint(0.0, 90.0))

        assertEquals(PI / 2 * GeoDistance.EARTH_RADIUS_KM, distance, 1e-6)
    }

    @Test
    fun antipodalPointsAreHalfOfTheCircumferenceApart() {
        val alongEquator = GeoDistance.haversineKm(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0))
        val poleToPole = GeoDistance.haversineKm(GeoPoint(90.0, 0.0), GeoPoint(-90.0, 0.0))

        assertEquals(PI * GeoDistance.EARTH_RADIUS_KM, alongEquator, 1e-6)
        assertEquals(PI * GeoDistance.EARTH_RADIUS_KM, poleToPole, 1e-6)
    }

    @Test
    fun takesTheShortWayAcrossTheAntimeridian() {
        val acrossAntimeridian = GeoDistance.haversineKm(GeoPoint(0.0, 179.0), GeoPoint(0.0, -179.0))
        val acrossPrimeMeridian = GeoDistance.haversineKm(GeoPoint(0.0, -1.0), GeoPoint(0.0, 1.0))

        assertEquals(222.38985328911653, acrossAntimeridian, 1e-6)
        assertEquals(acrossPrimeMeridian, acrossAntimeridian, 1e-6)
    }
}

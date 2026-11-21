package com.example.map.domain

import com.example.map.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointTest {

    @Test
    fun acceptsBoundaryCoordinates() {
        val point = GeoPoint(-90.0, 180.0)

        assertEquals(-90.0, point.latitude, 0.0)
        assertEquals(180.0, point.longitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLatitudeOutOfRange() {
        GeoPoint(90.1, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLongitudeOutOfRange() {
        GeoPoint(0.0, -180.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNaN() {
        GeoPoint(Double.NaN, 0.0)
    }
}

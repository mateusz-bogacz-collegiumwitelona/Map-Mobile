package com.example.map.domain.geo

import com.example.map.domain.model.geo.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AntimeridianTest {

    @Test
    fun keepsLineThatDoesNotCrossInOnePart() {
        val points = listOf(GeoPoint(52.2, 21.0), GeoPoint(50.0, 19.9), GeoPoint(48.8, 2.3))

        assertEquals(listOf(points), Antimeridian.split(points))
    }

    @Test
    fun splitsEastboundCrossingAtTheEdge() {
        val parts = Antimeridian.split(listOf(GeoPoint(10.0, 170.0), GeoPoint(20.0, -170.0)))

        assertEquals(
            listOf(
                listOf(GeoPoint(10.0, 170.0), GeoPoint(15.0, 180.0)),
                listOf(GeoPoint(15.0, -180.0), GeoPoint(20.0, -170.0))
            ),
            parts
        )
    }

    @Test
    fun splitsWestboundCrossingAtTheEdge() {
        val parts = Antimeridian.split(listOf(GeoPoint(20.0, -170.0), GeoPoint(10.0, 170.0)))

        assertEquals(
            listOf(
                listOf(GeoPoint(20.0, -170.0), GeoPoint(15.0, -180.0)),
                listOf(GeoPoint(15.0, 180.0), GeoPoint(10.0, 170.0))
            ),
            parts
        )
    }

    @Test
    fun startsLineFromTheEdgeOnTheSideOfTheNextPoint() {
        val parts = Antimeridian.split(listOf(GeoPoint(5.0, 180.0), GeoPoint(6.0, -179.0)))

        assertEquals(listOf(listOf(GeoPoint(5.0, -180.0), GeoPoint(6.0, -179.0))), parts)
    }

    @Test
    fun keepsPointOnTheEdgeOnTheSideOfThePreviousPoint() {
        val parts = Antimeridian.split(listOf(GeoPoint(5.0, 179.0), GeoPoint(6.0, -180.0)))

        assertEquals(listOf(listOf(GeoPoint(5.0, 179.0), GeoPoint(6.0, 180.0))), parts)
    }

    @Test
    fun keepsLineAlongTheEdgeInOnePart() {
        val points = listOf(
            GeoPoint(10.0, 180.0),
            GeoPoint(20.0, -180.0),
            GeoPoint(30.0, 180.0),
            GeoPoint(40.0, -180.0)
        )

        val parts = Antimeridian.split(points)

        assertEquals(1, parts.size)
        assertEquals(points.map { it.latitude }, parts[0].map { it.latitude })
        assertTrue(parts[0].all { it.longitude == 180.0 })
    }

    @Test
    fun returnsOnlyPartsWithAtLeastTwoPoints() {
        val parts = Antimeridian.split(
            listOf(GeoPoint(5.0, -180.0), GeoPoint(6.0, 179.0), GeoPoint(7.0, -179.0))
        )

        assertTrue(parts.isNotEmpty())
        assertTrue(parts.all { it.size >= 2 })
    }

    @Test
    fun splitsPacificFlightIntoPartsWithoutJumps() {
        val arc = GreatCircle.arc(GeoPoint(35.5494, 139.7798), GeoPoint(37.6213, -122.3790))

        val parts = Antimeridian.split(arc)

        assertEquals(2, parts.size)
        assertEquals(180.0, parts[0].last().longitude, 0.0)
        assertEquals(-180.0, parts[1].first().longitude, 0.0)
        assertEquals(parts[0].last().latitude, parts[1].first().latitude, 0.0)
        parts.forEach { part ->
            part.zipWithNext().forEach { (from, to) ->
                assertTrue(abs(to.longitude - from.longitude) <= 180.0)
            }
        }
    }

    @Test
    fun returnsNoPartsForEmptyLine() {
        assertEquals(emptyList<List<GeoPoint>>(), Antimeridian.split(emptyList()))
    }

    @Test
    fun returnsNoPartsForSinglePoint() {
        assertEquals(emptyList<List<GeoPoint>>(), Antimeridian.split(listOf(GeoPoint(1.0, 2.0))))
    }
}

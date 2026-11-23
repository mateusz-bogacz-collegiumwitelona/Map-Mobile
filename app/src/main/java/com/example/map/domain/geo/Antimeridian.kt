package com.example.map.domain.geo

import com.example.map.domain.model.GeoPoint
import kotlin.math.abs

object Antimeridian {

    private const val EDGE_LONGITUDE = 180.0
    private const val EDGE_EPSILON = 1e-9

    fun split(points: List<GeoPoint>): List<List<GeoPoint>> {
        if (points.isEmpty()) {
            return emptyList()
        }

        val parts = mutableListOf<List<GeoPoint>>()
        var current = mutableListOf(points.first())

        for (index in 1 until points.size) {
            val next = points[index]
            val previous = current.last()

            when {
                isOnEdge(next) -> current.addIfNotLast(next.onEdgeSideOf(previous))

                isOnEdge(previous) && isEast(previous) != isEast(next) -> {
                    parts.add(current)
                    current = mutableListOf(previous.onEdgeSideOf(next), next)
                }

                crosses(previous, next) -> {
                    val edge = if (previous.longitude > 0.0) EDGE_LONGITUDE else -EDGE_LONGITUDE
                    val latitude = crossingLatitude(previous, next, edge)

                    current.addIfNotLast(GeoPoint(latitude, edge))
                    parts.add(current)
                    current = mutableListOf(GeoPoint(latitude, -edge), next)
                }

                else -> current.addIfNotLast(next)
            }
        }

        parts.add(current)
        return parts.filter { it.size >= 2 }
    }

    private fun isOnEdge(point: GeoPoint): Boolean =
        abs(abs(point.longitude) - EDGE_LONGITUDE) < EDGE_EPSILON

    private fun isEast(point: GeoPoint): Boolean = point.longitude > 0.0

    private fun GeoPoint.onEdgeSideOf(other: GeoPoint): GeoPoint =
        GeoPoint(latitude, if (isEast(other)) EDGE_LONGITUDE else -EDGE_LONGITUDE)

    private fun crosses(previous: GeoPoint, next: GeoPoint): Boolean =
        abs(next.longitude - previous.longitude) > EDGE_LONGITUDE

    private fun crossingLatitude(previous: GeoPoint, next: GeoPoint, edge: Double): Double {
        val unwrappedNextLongitude =
            if (edge > 0.0) next.longitude + 360.0 else next.longitude - 360.0
        val longitudeSpan = unwrappedNextLongitude - previous.longitude

        if (longitudeSpan == 0.0) {
            return previous.latitude
        }

        val fraction = (edge - previous.longitude) / longitudeSpan
        return previous.latitude + fraction * (next.latitude - previous.latitude)
    }

    private fun MutableList<GeoPoint>.addIfNotLast(point: GeoPoint) {
        if (lastOrNull() != point) {
            add(point)
        }
    }
}

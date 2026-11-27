package com.example.map.domain.geo

import com.example.map.domain.model.geo.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GreatCircle {

    const val MAX_SEGMENT_LENGTH_KM = 100.0
    const val MIN_SEGMENT_LENGTH_KM = 1.0

    private const val ANGLE_EPSILON = 1e-6

    fun arc(
        from: GeoPoint,
        to: GeoPoint,
        maxSegmentLengthKm: Double = MAX_SEGMENT_LENGTH_KM
    ): List<GeoPoint> {
        require(maxSegmentLengthKm >= MIN_SEGMENT_LENGTH_KM) {
            "Maximum segment length must be at least $MIN_SEGMENT_LENGTH_KM km, was $maxSegmentLengthKm."
        }

        val angle = GeoDistance.centralAngle(from, to)

        if (angle < ANGLE_EPSILON) {
            return listOf(from, to)
        }

        if (PI - angle < ANGLE_EPSILON) {
            val detour = GeoPoint(0.0, wrapLongitude(from.longitude + 90.0))
            return arc(from, detour, maxSegmentLengthKm) +
                    arc(detour, to, maxSegmentLengthKm).drop(1)
        }

        val segments = ceil(angle * GeoDistance.EARTH_RADIUS_KM / maxSegmentLengthKm)
            .toInt()
            .coerceAtLeast(1)

        val start = UnitVector.of(from)
        val end = UnitVector.of(to)
        val sinAngle = sin(angle)

        return (0..segments).map { index ->
            when (index) {
                0 -> from
                segments -> to
                else -> interpolate(start, end, angle, sinAngle, index.toDouble() / segments)
            }
        }
    }

    private fun wrapLongitude(longitude: Double): Double =
        if (longitude > 180.0) longitude - 360.0 else longitude

    private fun interpolate(
        start: UnitVector,
        end: UnitVector,
        angle: Double,
        sinAngle: Double,
        fraction: Double
    ): GeoPoint {
        val startWeight = sin((1 - fraction) * angle) / sinAngle
        val endWeight = sin(fraction * angle) / sinAngle

        val x = startWeight * start.x + endWeight * end.x
        val y = startWeight * start.y + endWeight * end.y
        val z = startWeight * start.z + endWeight * end.z

        return GeoPoint(
            latitude = Math.toDegrees(atan2(z, sqrt(x * x + y * y))),
            longitude = Math.toDegrees(atan2(y, x))
        )
    }

    private class UnitVector(val x: Double, val y: Double, val z: Double) {
        companion object {
            fun of(point: GeoPoint): UnitVector {
                val latitude = Math.toRadians(point.latitude)
                val longitude = Math.toRadians(point.longitude)

                return UnitVector(
                    x = cos(latitude) * cos(longitude),
                    y = cos(latitude) * sin(longitude),
                    z = sin(latitude)
                )
            }
        }
    }
}

package com.example.map.domain.geo

import com.example.map.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GreatCircle {

    const val MAX_SEGMENT_LENGTH_KM = 100.0

    private const val ANGLE_EPSILON = 1e-6

    fun arc(
        from: GeoPoint,
        to: GeoPoint,
        maxSegmentLengthKm: Double = MAX_SEGMENT_LENGTH_KM
    ): List<GeoPoint> {
        require(maxSegmentLengthKm > 0.0) {
            "Maximum segment length must be positive, was $maxSegmentLengthKm."
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

        return (0..segments).map { index ->
            when (index) {
                0 -> from
                segments -> to
                else -> interpolate(from, to, angle, index.toDouble() / segments)
            }
        }
    }

    private fun wrapLongitude(longitude: Double): Double =
        if (longitude > 180.0) longitude - 360.0 else longitude

    private fun interpolate(
        from: GeoPoint,
        to: GeoPoint,
        angle: Double,
        fraction: Double
    ): GeoPoint {
        val fromLatitude = Math.toRadians(from.latitude)
        val fromLongitude = Math.toRadians(from.longitude)
        val toLatitude = Math.toRadians(to.latitude)
        val toLongitude = Math.toRadians(to.longitude)

        val fromWeight = sin((1 - fraction) * angle) / sin(angle)
        val toWeight = sin(fraction * angle) / sin(angle)

        val x = fromWeight * cos(fromLatitude) * cos(fromLongitude) +
            toWeight * cos(toLatitude) * cos(toLongitude)
        val y = fromWeight * cos(fromLatitude) * sin(fromLongitude) +
            toWeight * cos(toLatitude) * sin(toLongitude)
        val z = fromWeight * sin(fromLatitude) + toWeight * sin(toLatitude)

        return GeoPoint(
            latitude = Math.toDegrees(atan2(z, sqrt(x * x + y * y))),
            longitude = Math.toDegrees(atan2(y, x))
        )
    }
}

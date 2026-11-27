package com.example.map.domain.geo

import com.example.map.domain.model.geo.GeoPoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoDistance {

    const val EARTH_RADIUS_KM = 6371.0

    fun haversineKm(
        from: GeoPoint,
        to: GeoPoint,
        radiusKm: Double = EARTH_RADIUS_KM
    ): Double {
        require(radiusKm > 0.0) {
            "Radius must be positive, was $radiusKm."
        }

        return centralAngle(from, to) * radiusKm
    }

    internal fun centralAngle(from: GeoPoint, to: GeoPoint): Double {
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val latitudeDelta = toLatitude - fromLatitude
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)

        val haversine = sin(latitudeDelta / 2).pow(2) +
                cos(fromLatitude) * cos(toLatitude) * sin(longitudeDelta / 2).pow(2)

        return 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
    }
}

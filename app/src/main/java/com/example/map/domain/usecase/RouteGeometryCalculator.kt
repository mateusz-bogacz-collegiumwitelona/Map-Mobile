package com.example.map.domain.usecase

import com.example.map.domain.geo.GeoDistance
import com.example.map.domain.geo.GreatCircle
import com.example.map.domain.model.GeoPoint
import com.example.map.domain.model.RouteGeometry
import com.example.map.domain.model.TransportType
import com.example.map.domain.repository.RoadRouter

class RouteGeometryCalculator(
    private val roadRouter: RoadRouter
) {

    suspend fun calculate(
        transportType: TransportType,
        origin: GeoPoint,
        destination: GeoPoint
    ): RouteGeometry {
        val distanceKm = GeoDistance.haversineKm(origin, destination)

        return when (transportType) {
            TransportType.PLANE -> RouteGeometry(
                points = GreatCircle.arc(origin, destination),
                distanceKm = distanceKm,
                isFallbackRoute = false
            )

            TransportType.TRAIN, TransportType.OTHER -> RouteGeometry(
                points = listOf(origin, destination),
                distanceKm = distanceKm,
                isFallbackRoute = false
            )

            TransportType.CAR -> roadRouter.route(origin, destination)
                ?.let { road ->
                    RouteGeometry(
                        points = road.points,
                        distanceKm = road.distanceKm,
                        isFallbackRoute = false
                    )
                }
                ?: RouteGeometry(
                    points = listOf(origin, destination),
                    distanceKm = distanceKm,
                    isFallbackRoute = true
                )
        }
    }
}

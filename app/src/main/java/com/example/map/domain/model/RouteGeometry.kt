package com.example.map.domain.model

data class RouteGeometry(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val isFallbackRoute: Boolean
)

package com.example.map.domain.model.geo

data class RouteGeometry(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val isFallbackRoute: Boolean
)

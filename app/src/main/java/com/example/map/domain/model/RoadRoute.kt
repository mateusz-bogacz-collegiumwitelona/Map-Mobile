package com.example.map.domain.model

data class RoadRoute(
    val points: List<GeoPoint>,
    val distanceKm: Double
)

package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSegmentRouteRequestDto(
    val coordinates: List<RoutePointDto>,
    val distanceKm: Double,
    val isFallbackRoute: Boolean
)

@Serializable
data class RoutePointDto(
    val latitude: Double,
    val longitude: Double
)

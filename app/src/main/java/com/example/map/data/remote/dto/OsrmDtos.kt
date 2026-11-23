package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OsrmRouteResponseDto(
    val code: String? = null,
    val routes: List<OsrmRouteDto>? = null,
    val waypoints: List<OsrmWaypointDto>? = null
)

@Serializable
data class OsrmRouteDto(
    val distance: Double? = null,
    val geometry: OsrmGeometryDto? = null
)

@Serializable
data class OsrmGeometryDto(
    val coordinates: List<List<Double>>? = null
)

@Serializable
data class OsrmWaypointDto(
    val distance: Double? = null
)

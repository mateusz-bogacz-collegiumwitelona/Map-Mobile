package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripDetailsSegmentDto(
    val orderIndex: Int,
    val transportType: String,
    val distanceKm: Double = 0.0,
    val isFallbackRoute: Boolean = false,
    val origin: TripPointResponseDto,
    val destination: TripPointResponseDto
)
package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripDetailsSegmentDto(
    val orderIndex: Int,
    val transportType: String,
    val origin: TripPointResponseDto,
    val destination: TripPointResponseDto
)
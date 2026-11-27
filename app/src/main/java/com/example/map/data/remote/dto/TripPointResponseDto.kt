package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripPointResponseDto(
    val placeId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
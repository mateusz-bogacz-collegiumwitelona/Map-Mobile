package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripPointRequestDto(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
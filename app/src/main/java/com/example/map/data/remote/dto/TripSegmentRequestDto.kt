package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripSegmentRequestDto(
    val origin: TripPointRequestDto,
    val destination: TripPointRequestDto,
    val transportType: String
)
package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpsertTripRequestDto(
    val title: String,
    val description: String?,
    val startDate: String,
    val endDate: String?,
    val segments: List<TripSegmentRequestDto>
)
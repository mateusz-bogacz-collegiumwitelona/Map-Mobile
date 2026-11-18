package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripDetailsDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val segments: List<TripDetailsSegmentDto> = emptyList()
)
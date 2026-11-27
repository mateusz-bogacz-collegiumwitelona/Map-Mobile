package com.example.map.data.remote.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class TripSummaryResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val totalDistance: Double = 0.0,
    val transportTypes: List<String> = emptyList()
)
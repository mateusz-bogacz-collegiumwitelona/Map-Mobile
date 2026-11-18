package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripSummaryDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val startDate: String,
    val endDate: String? = null,
    val totalDistance: Double,
    val transportTypes: List<String> = emptyList()
)
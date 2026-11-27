package com.example.map.domain.model

data class TripSummary(
    val id: String,
    val title: String,
    val startDateMillis: Long,
    val endDateMillis: Long?,
    val totalDistanceKm: Double
)
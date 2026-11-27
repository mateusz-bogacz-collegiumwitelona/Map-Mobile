package com.example.map.domain.model

data class TripSummary(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: String,
    val endDate: String?,
    val totalDistance: Double,
    val transportTypes: List<TransportType>
)
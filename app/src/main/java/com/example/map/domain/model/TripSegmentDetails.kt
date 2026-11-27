package com.example.map.domain.model

data class TripSegmentDetails(
    val orderIndex: Int,
    val originName: String,
    val destinationName: String,
    val transportType: TripTransportType,
    val distanceKm: Double,
    val isFallbackRoute: Boolean
)
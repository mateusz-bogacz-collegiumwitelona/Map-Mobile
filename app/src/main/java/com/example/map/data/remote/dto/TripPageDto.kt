package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TripPageDto(
    val items: List<TripSummaryDto> = emptyList()
)
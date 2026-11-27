package com.example.map.domain.model

data class TripDraft(
    val title: String,
    val description: String?,
    val startDateMillis: Long,
    val endDateMillis: Long?,
    val segments: List<TripSegmentDraft>
)
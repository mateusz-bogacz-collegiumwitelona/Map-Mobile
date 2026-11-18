package com.example.map.domain.model

data class TripSegmentDraft(
    val id: Long,
    val origin: PlaceOption? = null,
    val destination: PlaceOption? = null,
    val transportType: TripTransportType = TripTransportType.CAR
)
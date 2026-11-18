package com.example.map.testing

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDraft
import com.example.map.domain.repository.TripRepository

class NoOpTripRepository : TripRepository {
    override suspend fun searchPlaces(query: String): List<PlaceOption> =
        emptyList()

    override suspend fun createTrip(trip: TripDraft) = Unit
}
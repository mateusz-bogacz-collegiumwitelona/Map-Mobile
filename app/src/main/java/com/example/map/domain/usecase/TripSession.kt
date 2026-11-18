package com.example.map.domain.usecase

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDraft
import com.example.map.domain.repository.TripRepository

class TripSession(
    private val tripRepository: TripRepository
) {
    suspend fun searchPlaces(query: String): List<PlaceOption> =
        tripRepository.searchPlaces(query)

    suspend fun createTrip(trip: TripDraft) {
        tripRepository.createTrip(trip)
    }
}
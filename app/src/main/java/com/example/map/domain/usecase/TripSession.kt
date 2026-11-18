package com.example.map.domain.usecase

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSummary
import com.example.map.domain.repository.TripRepository

class TripSession(
    private val tripRepository: TripRepository
) {
    suspend fun searchPlaces(query: String): List<PlaceOption> =
        tripRepository.searchPlaces(query)

    suspend fun getTrips(
        searchTerm: String? = null,
        sortBy: String = "startDate",
        sortDescending: Boolean = true
    ): List<TripSummary> =
        tripRepository.getTrips(searchTerm, sortBy, sortDescending)

    suspend fun getTripDetails(tripId: String): TripDetails =
        tripRepository.getTripDetails(tripId)

    suspend fun createTrip(trip: TripDraft) {
        tripRepository.createTrip(trip)
    }

    suspend fun updateTrip(tripId: String, trip: TripDraft) {
        tripRepository.updateTrip(tripId, trip)
    }

    suspend fun deleteTrip(tripId: String) {
        tripRepository.deleteTrip(tripId)
    }
}
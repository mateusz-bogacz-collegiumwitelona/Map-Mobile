package com.example.map.testing

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSummary
import com.example.map.domain.repository.TripRepository

class NoOpTripRepository : TripRepository {
    override suspend fun searchPlaces(query: String): List<PlaceOption> =
        emptyList()

    override suspend fun getTrips(
        searchTerm: String?,
        sortBy: String,
        sortDescending: Boolean
    ): List<TripSummary> = emptyList()

    override suspend fun getTripDetails(tripId: String): TripDetails =
        throw UnsupportedOperationException("Trip details are not configured.")

    override suspend fun createTrip(trip: TripDraft) = Unit

    override suspend fun updateTrip(tripId: String, trip: TripDraft) = Unit

    override suspend fun deleteTrip(tripId: String) = Unit
}
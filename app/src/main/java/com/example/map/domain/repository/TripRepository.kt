package com.example.map.domain.repository

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSummary

interface TripRepository {
    suspend fun searchPlaces(query: String): List<PlaceOption>
    suspend fun getTrips(
        searchTerm: String? = null,
        sortBy: String = "startDate",
        sortDescending: Boolean = true
    ): List<TripSummary>
    suspend fun getTripDetails(tripId: String): TripDetails
    suspend fun createTrip(trip: TripDraft)
    suspend fun updateTrip(tripId: String, trip: TripDraft)
    suspend fun deleteTrip(tripId: String)
}
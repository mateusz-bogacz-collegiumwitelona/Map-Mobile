package com.example.map.testing

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSummary
import com.example.map.domain.repository.TripRepository

class FakeTripRepository : TripRepository {
    val createdTrips = mutableListOf<TripDraft>()
    val updatedTrips = mutableListOf<Pair<String, TripDraft>>()
    val deletedTripIds = mutableListOf<String>()

    val tripSummaries = mutableListOf<TripSummary>()
    val tripDetails = mutableMapOf<String, TripDetails>()

    var places: List<PlaceOption> = emptyList()
    var createError: Exception? = null
    var updateError: Exception? = null
    var deleteError: Exception? = null
    var getTripsError: Exception? = null
    var lastTripsSearchTerm: String? = null
    var lastTripsSortBy: String? = null
    var lastTripsSortDescending: Boolean? = null

    override suspend fun searchPlaces(query: String): List<PlaceOption> =
        places.filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun getTrips(
        searchTerm: String?,
        sortBy: String,
        sortDescending: Boolean
    ): List<TripSummary> {
        getTripsError?.let { throw it }

        lastTripsSearchTerm = searchTerm
        lastTripsSortBy = sortBy
        lastTripsSortDescending = sortDescending

        return tripSummaries.toList()
    }

    override suspend fun getTripDetails(tripId: String): TripDetails =
        tripDetails[tripId]
            ?: throw IllegalArgumentException("Trip not found.")

    override suspend fun createTrip(trip: TripDraft) {
        createError?.let { throw it }
        createdTrips += trip
    }

    override suspend fun updateTrip(tripId: String, trip: TripDraft) {
        updateError?.let { throw it }
        updatedTrips += tripId to trip
    }

    override suspend fun deleteTrip(tripId: String) {
        deleteError?.let { throw it }
        deletedTripIds += tripId
        tripSummaries.removeAll { it.id == tripId }
    }
}
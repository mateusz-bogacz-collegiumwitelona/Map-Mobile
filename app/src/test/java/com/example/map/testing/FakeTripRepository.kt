package com.example.map.testing

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDraft
import com.example.map.domain.repository.TripRepository

class FakeTripRepository : TripRepository {
    val createdTrips = mutableListOf<TripDraft>()
    var places: List<PlaceOption> = emptyList()
    var createError: Exception? = null

    override suspend fun searchPlaces(query: String): List<PlaceOption> =
        places.filter { it.name.contains(query, ignoreCase = true) }

    override suspend fun createTrip(trip: TripDraft) {
        createError?.let { throw it }
        createdTrips += trip
    }
}
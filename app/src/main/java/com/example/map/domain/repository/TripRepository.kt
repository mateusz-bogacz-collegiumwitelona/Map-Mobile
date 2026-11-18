package com.example.map.domain.repository

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDraft

interface TripRepository {
    suspend fun searchPlaces(query: String): List<PlaceOption>
    suspend fun createTrip(trip: TripDraft)
}
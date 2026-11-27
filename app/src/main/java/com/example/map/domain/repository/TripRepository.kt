package com.example.map.domain.repository

import com.example.map.domain.model.TripSortBy
import com.example.map.domain.model.TripsResult

interface TripRepository {
    suspend fun getUserTrips(
        pageNumber: Int? = null,
        pageSize: Int? = null,
        sortBy: TripSortBy? = null,
        sortDescending: Boolean = true,
        searchTerm: String? = null
    ): TripsResult
}
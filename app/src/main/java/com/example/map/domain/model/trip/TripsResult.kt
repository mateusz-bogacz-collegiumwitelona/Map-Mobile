package com.example.map.domain.model.trip

import com.example.map.domain.model.common.PagedData

enum class TripsError {
    UNAUTHORIZED,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN
}

sealed interface TripsResult {
    data class Success(val data: PagedData<TripSummary>) : TripsResult

    data class Failure(
        val error: TripsError,
        val message: String? = null
    ) : TripsResult
}
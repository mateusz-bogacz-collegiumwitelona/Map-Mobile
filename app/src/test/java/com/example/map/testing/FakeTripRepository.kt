package com.example.map.testing

import com.example.map.domain.model.PagedData
import com.example.map.domain.model.TripSortBy
import com.example.map.domain.model.TripsResult
import com.example.map.domain.repository.TripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

data class RecordedTripsCall(
    val pageNumber: Int?,
    val pageSize: Int?,
    val sortBy: TripSortBy?,
    val sortDescending: Boolean,
    val searchTerm: String?
)

class FakeTripRepository : TripRepository {

    val calls = mutableListOf<RecordedTripsCall>()
    var cancelledCallsCount = 0

    var getTripsGate: CompletableDeferred<Unit>? = null
    var resultProvider: (pageNumber: Int) -> TripsResult = { page ->
        TripsResult.Success(
            PagedData(
                items = emptyList(),
                pageNumber = page,
                pageSize = 10,
                totalCount = 30,
                totalPages = 3,
                hasPreviousPage = page > 1,
                hasNextPage = page < 3
            )
        )
    }

    override suspend fun getUserTrips(
        pageNumber: Int?,
        pageSize: Int?,
        sortBy: TripSortBy?,
        sortDescending: Boolean,
        searchTerm: String?
    ): TripsResult {
        calls += RecordedTripsCall(
            pageNumber = pageNumber,
            pageSize = pageSize,
            sortBy = sortBy,
            sortDescending = sortDescending,
            searchTerm = searchTerm
        )

        val gate = getTripsGate
        if (gate != null) {
            try {
                gate.await()
            } catch (e: CancellationException) {
                cancelledCallsCount++
                throw e
            }
        }

        return resultProvider(pageNumber ?: 1)
    }
}
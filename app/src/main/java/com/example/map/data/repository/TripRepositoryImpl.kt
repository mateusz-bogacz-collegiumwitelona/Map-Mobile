package com.example.map.data.repository

import com.example.map.data.remote.dto.common.PagedResponse
import com.example.map.data.remote.dto.response.TripSummaryResponse
import com.example.map.data.remote.service.TripApiService
import com.example.map.domain.model.PagedData
import com.example.map.domain.model.TransportType
import com.example.map.domain.model.TripSortBy
import com.example.map.domain.model.TripSummary
import com.example.map.domain.model.TripsError
import com.example.map.domain.model.TripsResult
import com.example.map.domain.repository.TripRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripApi: TripApiService,
    private val json: Json
) : TripRepository {

    override suspend fun getUserTrips(
        pageNumber: Int?,
        pageSize: Int?,
        sortBy: TripSortBy?,
        sortDescending: Boolean,
        searchTerm: String?
    ): TripsResult =
        executeDataApiCall(
            json = json,
            request = {
                tripApi.getUserTrips(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    sortBy = sortBy?.apiValue,
                    sortDescending = sortDescending,
                    searchTerm = searchTerm?.trim()?.takeIf { it.isNotBlank() }
                )
            },
            onSuccess = { paged ->
                TripsResult.Success(paged.toDomain())
            },
            onApiError = { status, code, msg ->
                TripsResult.Failure(mapError(status, code), msg)
            },
            onNetworkError = { TripsResult.Failure(TripsError.NETWORK) },
            onUnknownError = { msg -> TripsResult.Failure(TripsError.UNKNOWN, msg) }
        )

    private fun PagedResponse<TripSummaryResponse>.toDomain(): PagedData<TripSummary> =
        PagedData(
            items = items.map { it.toDomain() },
            pageNumber = pageNumber,
            pageSize = pageSize,
            totalCount = totalCount,
            totalPages = totalPages,
            hasPreviousPage = hasPreviousPage,
            hasNextPage = hasNextPage
        )

    private fun TripSummaryResponse.toDomain(): TripSummary =
        TripSummary(
            id = id,
            title = title,
            description = description,
            startDate = startDate,
            endDate = endDate,
            totalDistance = totalDistance,
            transportTypes = transportTypes.map { it.toTransportType() }
        )

    private fun String.toTransportType(): TransportType =
        when (trim().uppercase()) {
            "PLANE" -> TransportType.PLANE
            "TRAIN" -> TransportType.TRAIN
            "CAR" -> TransportType.CAR
            else -> TransportType.OTHER
        }

    private fun mapError(statusCode: Int, errorCode: String?): TripsError =
        when (errorCode) {
            "COMMON_005" -> TripsError.RATE_LIMITED
            else -> when (statusCode) {
                401, 403 -> TripsError.UNAUTHORIZED
                429 -> TripsError.RATE_LIMITED
                else -> TripsError.UNKNOWN
            }
        }
}
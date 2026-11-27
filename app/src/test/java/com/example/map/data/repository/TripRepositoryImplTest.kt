package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.common.PagedResponse
import com.example.map.data.remote.dto.response.TripSummaryResponse
import com.example.map.data.remote.service.TripApiService
import com.example.map.domain.model.PagedData
import com.example.map.domain.model.TransportType
import com.example.map.domain.model.TripSortBy
import com.example.map.domain.model.TripSummary
import com.example.map.domain.model.TripsError
import com.example.map.domain.model.TripsResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class TripRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val service = FakeTripApiService()
    private val repository = TripRepositoryImpl(service, json)

    @Test
    fun mapsSuccessfulPagedResponseAndTransportTypes() = runTest {
        service.response = Response.success(
            ApiEnvelope(
                isSuccess = true,
                data = PagedResponse(
                    items = listOf(
                        TripSummaryResponse(
                            id = "trip-1",
                            title = "Wakacje",
                            description = "Opis",
                            startDate = "2026-07-01T10:00:00Z",
                            endDate = "2026-07-10T18:00:00Z",
                            totalDistance = 1234.5,
                            transportTypes = listOf("Plane", " car ", "TRAIN", "Ship")
                        )
                    ),
                    pageNumber = 2,
                    pageSize = 10,
                    totalCount = 25,
                    totalPages = 3,
                    hasPreviousPage = true,
                    hasNextPage = true
                )
            )
        )

        val result = repository.getUserTrips(
            pageNumber = 2,
            pageSize = 10,
            sortBy = TripSortBy.START_DATE,
            sortDescending = false,
            searchTerm = "  Wakacje  "
        )

        assertEquals(
            TripsResult.Success(
                PagedData(
                    items = listOf(
                        TripSummary(
                            id = "trip-1",
                            title = "Wakacje",
                            description = "Opis",
                            startDate = "2026-07-01T10:00:00Z",
                            endDate = "2026-07-10T18:00:00Z",
                            totalDistance = 1234.5,
                            transportTypes = listOf(
                                TransportType.PLANE,
                                TransportType.CAR,
                                TransportType.TRAIN,
                                TransportType.OTHER
                            )
                        )
                    ),
                    pageNumber = 2,
                    pageSize = 10,
                    totalCount = 25,
                    totalPages = 3,
                    hasPreviousPage = true,
                    hasNextPage = true
                )
            ),
            result
        )
        assertEquals(2, service.lastPageNumber)
        assertEquals(10, service.lastPageSize)
        assertEquals("startDate", service.lastSortBy)
        assertEquals(false, service.lastSortDescending)
        assertEquals("Wakacje", service.lastSearchTerm)
    }

    @Test
    fun normalizesBlankSearchTermToNull() = runTest {
        repository.getUserTrips(searchTerm = "    ")

        assertNull(service.lastSearchTerm)
    }

    @Test
    fun mapsRateLimitErrorCode() = runTest {
        service.response = errorResponse(
            status = 429,
            errorCode = "COMMON_005",
            message = "Too many requests"
        )

        assertEquals(
            TripsResult.Failure(TripsError.RATE_LIMITED, "Too many requests"),
            repository.getUserTrips()
        )
    }

    @Test
    fun mapsRateLimitStatusWithoutErrorCode() = runTest {
        service.response = errorResponse(status = 429)

        assertEquals(
            TripsResult.Failure(TripsError.RATE_LIMITED),
            repository.getUserTrips()
        )
    }

    @Test
    fun mapsUnauthorizedStatuses() = runTest {
        service.response = errorResponse(status = 401)
        assertEquals(
            TripsResult.Failure(TripsError.UNAUTHORIZED),
            repository.getUserTrips()
        )

        service.response = errorResponse(status = 403)
        assertEquals(
            TripsResult.Failure(TripsError.UNAUTHORIZED),
            repository.getUserTrips()
        )
    }

    @Test
    fun mapsNetworkFailure() = runTest {
        service.exception = IOException("offline")

        assertEquals(
            TripsResult.Failure(TripsError.NETWORK),
            repository.getUserTrips()
        )
    }

    @Test
    fun mapsUnknownErrorWithBackendMessage() = runTest {
        service.response = errorResponse(
            status = 500,
            errorCode = "SERVER_500",
            message = "Server error"
        )

        assertEquals(
            TripsResult.Failure(TripsError.UNKNOWN, "Server error"),
            repository.getUserTrips()
        )
    }

    @Test
    fun rethrowsCancellationException() = runTest {
        service.exception = CancellationException("cancelled")

        try {
            repository.getUserTrips()
            fail("Expected CancellationException to be rethrown")
        } catch (_: CancellationException) {
            // OK - musi przelecieć dalej, żeby Retrofit anulował request HTTP
        }
    }

    private fun errorResponse(
        status: Int,
        errorCode: String? = null,
        message: String? = null
    ): Response<ApiEnvelope<PagedResponse<TripSummaryResponse>>> {
        val body = json.encodeToString(
            ApiEnvelope<PagedResponse<TripSummaryResponse>>(
                isSuccess = false,
                message = message,
                errorCode = errorCode
            )
        )
        return Response.error(
            status,
            body.toResponseBody("application/json".toMediaType())
        )
    }
}

private class FakeTripApiService : TripApiService {

    var response: Response<ApiEnvelope<PagedResponse<TripSummaryResponse>>> =
        Response.success(
            ApiEnvelope(
                isSuccess = true,
                data = PagedResponse()
            )
        )
    var exception: Exception? = null

    var lastPageNumber: Int? = null
    var lastPageSize: Int? = null
    var lastSortBy: String? = null
    var lastSortDescending: Boolean? = null
    var lastSearchTerm: String? = null

    override suspend fun getUserTrips(
        pageNumber: Int?,
        pageSize: Int?,
        sortBy: String?,
        sortDescending: Boolean,
        searchTerm: String?
    ): Response<ApiEnvelope<PagedResponse<TripSummaryResponse>>> {
        lastPageNumber = pageNumber
        lastPageSize = pageSize
        lastSortBy = sortBy
        lastSortDescending = sortDescending
        lastSearchTerm = searchTerm
        exception?.let { throw it }
        return response
    }
}
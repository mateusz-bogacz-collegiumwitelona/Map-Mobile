package com.example.map.data.remote.service

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.common.PagedResponse
import com.example.map.data.remote.dto.response.TripSummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TripApiService {
    @GET("api/trips")
    suspend fun getUserTrips(
        @Query("pageNumber") pageNumber: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortDescending") sortDescending: Boolean = true,
        @Query("searchTerm") searchTerm: String? = null
    ): Response<ApiEnvelope<PagedResponse<TripSummaryResponse>>>
}
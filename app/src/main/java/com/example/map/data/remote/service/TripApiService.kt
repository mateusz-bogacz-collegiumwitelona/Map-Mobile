package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.UpdateSegmentRouteRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path

interface TripApiService {

    @PUT("api/trip/segments/{segmentId}/route")
    suspend fun updateSegmentRoute(
        @Path("segmentId") segmentId: String,
        @Body request: UpdateSegmentRouteRequestDto
    ): Response<ApiResultDto>
}

package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.PlaceSearchDto
import com.example.map.data.remote.dto.TripPointRequestDto
import com.example.map.data.remote.dto.TripSegmentRequestDto
import com.example.map.data.remote.dto.UpsertTripRequestDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TripApi {
    @GET("api/place/search")
    suspend fun searchPlaces(
        @Query("query") query: String
    ): Response<ApiEnvelopeDto<List<PlaceSearchDto>>>

    @POST("api/trip/create")
    suspend fun createTrip(
        @Body request: UpsertTripRequestDto
    ): Response<ApiEnvelopeDto<JsonObject>>
}
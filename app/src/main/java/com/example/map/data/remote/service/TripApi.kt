package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.PlaceSearchDto
import com.example.map.data.remote.dto.TripDetailsDto
import com.example.map.data.remote.dto.TripPageDto
import com.example.map.data.remote.dto.TripPointRequestDto
import com.example.map.data.remote.dto.TripSegmentRequestDto
import com.example.map.data.remote.dto.UpsertTripRequestDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TripApi {
    @GET("api/place/search")
    suspend fun searchPlaces(
        @Query("query") query: String
    ): Response<ApiEnvelopeDto<List<PlaceSearchDto>>>

    @GET("api/trip")
    suspend fun getTrips(
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 100,
        @Query("sortBy") sortBy: String? = "startDate",
        @Query("sortDescending") sortDescending: Boolean = true,
        @Query("searchTerm") searchTerm: String? = null
    ): Response<ApiEnvelopeDto<TripPageDto>>

    @GET("api/trip/details/{tripId}")
    suspend fun getTripDetails(
        @Path("tripId") tripId: String
    ): Response<ApiEnvelopeDto<TripDetailsDto>>

    @POST("api/trip/create")
    suspend fun createTrip(
        @Body request: UpsertTripRequestDto
    ): Response<ApiEnvelopeDto<JsonObject>>

    @PUT("api/trip/update/{tripId}")
    suspend fun updateTrip(
        @Path("tripId") tripId: String,
        @Body request: UpsertTripRequestDto
    ): Response<ApiEnvelopeDto<JsonObject>>

    @DELETE("api/trip/delete/{tripId}")
    suspend fun deleteTrip(
        @Path("tripId") tripId: String
    ): Response<ApiResultDto>
}
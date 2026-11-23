package com.example.map.data.remote.service

import com.example.map.data.remote.dto.OsrmRouteResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface OsrmApiService {

    @GET("route/v1/driving/{coordinates}?overview=full&geometries=geojson&alternatives=false&steps=false")
    suspend fun route(
        @Path("coordinates", encoded = true) coordinates: String
    ): Response<OsrmRouteResponseDto>
}

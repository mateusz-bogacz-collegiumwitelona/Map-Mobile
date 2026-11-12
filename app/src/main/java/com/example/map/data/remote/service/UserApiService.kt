package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.EditUserNamesRequestDto
import com.example.map.data.remote.dto.UserProfileResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserApiService {

    @GET("api/user")
    suspend fun getProfile(): Response<UserProfileResponseDto>

    @PATCH("api/user")
    suspend fun updateNames(
        @Body request: EditUserNamesRequestDto
    ): Response<ApiResultDto>

    @PATCH("api/user/visibility/{isPublic}")
    suspend fun setVisibility(
        @Path("isPublic") isPublic: Boolean
    ): Response<ApiResultDto>
}

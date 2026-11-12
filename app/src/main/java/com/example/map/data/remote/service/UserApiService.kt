package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.ChangeEmailRequestDto
import com.example.map.data.remote.dto.ConfirmEmailChangeRequestDto
import com.example.map.data.remote.dto.EditUserNamesRequestDto
import com.example.map.data.remote.dto.UserProfileDataDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface UserApiService {

    @GET("api/user")
    suspend fun getProfile(): Response<ApiEnvelopeDto<UserProfileDataDto>>

    @PATCH("api/user")
    suspend fun updateNames(
        @Body request: EditUserNamesRequestDto
    ): Response<ApiResultDto>

    @PATCH("api/user/visibility/{isPublic}")
    suspend fun setVisibility(
        @Path("isPublic") isPublic: Boolean
    ): Response<ApiResultDto>

    @POST("api/user/email")
    suspend fun requestEmailChange(
        @Body request: ChangeEmailRequestDto
    ): Response<ApiResultDto>

    @POST("api/user/email/confirm")
    suspend fun confirmEmailChange(
        @Body request: ConfirmEmailChangeRequestDto
    ): Response<ApiResultDto>

    @DELETE("api/user")
    suspend fun deleteAccount(): Response<ApiResultDto>
}

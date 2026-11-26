package com.example.map.data.remote.service

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.ChangeEmailRequest
import com.example.map.data.remote.dto.request.ConfirmEmailChangeRequest
import com.example.map.data.remote.dto.request.EditUserNamesRequest
import com.example.map.data.remote.dto.response.UserProfileDataResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.UserProfileDto
import retrofit2.http.GET
import retrofit2.http.Path

interface UserApiService {

    @GET("api/user")
    suspend fun getCurrentUserProfile(): Response<ApiEnvelopeDto<UserProfileDto>>

    @GET("api/user")
    suspend fun getProfile(): Response<ApiEnvelope<UserProfileDataResponse>>

    @PATCH("api/user")
    suspend fun updateNames(
        @Body request: EditUserNamesRequest
    ): Response<ApiEnvelope<Unit>>

    @PATCH("api/user/visibility/{isPublic}")
    suspend fun setVisibility(
        @Path("isPublic") isPublic: Boolean
    ): Response<ApiEnvelope<Unit>>

    @POST("api/user/email")
    suspend fun requestEmailChange(
        @Body request: ChangeEmailRequest
    ): Response<ApiEnvelope<Unit>>

    @POST("api/user/email/confirm")
    suspend fun confirmEmailChange(
        @Body request: ConfirmEmailChangeRequest
    ): Response<ApiEnvelope<Unit>>

    @DELETE("api/user")
    suspend fun deleteAccount(): Response<ApiEnvelope<Unit>>
}

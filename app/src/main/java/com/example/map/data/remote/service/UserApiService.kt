package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.ChangeEmailRequestDto
import com.example.map.data.remote.dto.ConfirmEmailChangeRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserApiService {

    @POST("api/user/email")
    suspend fun requestEmailChange(
        @Body request: ChangeEmailRequestDto
    ): Response<ApiResultDto>

    @POST("api/user/email/confirm")
    suspend fun confirmEmailChange(
        @Body request: ConfirmEmailChangeRequestDto
    ): Response<ApiResultDto>
}

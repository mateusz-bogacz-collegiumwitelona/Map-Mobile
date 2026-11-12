package com.example.map.data.remote.service

import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.AuthResponseDto
import com.example.map.data.remote.dto.LogoutRequestDto
import com.example.map.data.remote.dto.LoginRequestDto
import com.example.map.data.remote.dto.RefreshTokenRequestDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiEnvelopeDto<AuthResponseDto>>

    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestDto
    ): Response<ApiEnvelopeDto<AuthResponseDto>>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body request: LogoutRequestDto
    ): Response<ResponseBody>
}
package com.example.map.data.remote.service

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.ConfirmEmailRequest
import com.example.map.data.remote.dto.request.LoginRequest
import com.example.map.data.remote.dto.request.LogoutRequest
import com.example.map.data.remote.dto.request.RefreshTokenRequest
import com.example.map.data.remote.dto.request.RegisterRequest
import com.example.map.data.remote.dto.request.ResendVerificationCodeRequest
import com.example.map.data.remote.dto.response.AuthResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiEnvelope<AuthResponse>>

    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequest
    ): Response<ApiEnvelope<AuthResponse>>

    @POST("api/auth/logout")
    suspend fun logout(
        @Header("Authorization") authorization: String,
        @Body request: LogoutRequest
    ): Response<ResponseBody>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiEnvelope<Unit>>

    @POST("api/auth/confirm-email")
    suspend fun confirmEmail(
        @Body body: ConfirmEmailRequest
    ): Response<ApiEnvelope<Unit>>

    @POST("api/auth/confirm-email/resend")
    suspend fun resendVerificationCode(
        @Body body: ResendVerificationCodeRequest
    ): Response<ApiEnvelope<Unit>>
}
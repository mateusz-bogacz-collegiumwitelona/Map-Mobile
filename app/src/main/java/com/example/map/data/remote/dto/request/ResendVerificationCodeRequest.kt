package com.example.map.data.remote.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ResendVerificationCodeRequest(
    val email: String
)
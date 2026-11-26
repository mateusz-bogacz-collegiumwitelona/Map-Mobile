package com.example.map.data.remote.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ConfirmEmailRequest(
    val email: String,
    val code: String
)
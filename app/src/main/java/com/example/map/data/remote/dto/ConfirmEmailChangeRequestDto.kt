package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ConfirmEmailChangeRequestDto(
    val code: String
)
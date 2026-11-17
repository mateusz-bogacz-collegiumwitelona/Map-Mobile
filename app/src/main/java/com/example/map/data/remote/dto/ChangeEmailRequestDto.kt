package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangeEmailRequestDto(
    val newEmail: String
)
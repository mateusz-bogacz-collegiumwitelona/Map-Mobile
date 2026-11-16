package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val errorCode: String? = null,
    val message: String? = null
)
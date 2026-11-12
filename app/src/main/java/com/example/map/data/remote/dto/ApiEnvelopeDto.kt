package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelopeDto<T>(
    val isSuccess: Boolean = false,
    val message: String? = null,
    val errorCode: String? = null,
    val statusCode: Int? = null,
    val errors: List<String>? = null,
    val data: T? = null
)
package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResultDto(
    val isSuccess: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val statusCode: Int? = null,
    val errors: List<String>? = null
)
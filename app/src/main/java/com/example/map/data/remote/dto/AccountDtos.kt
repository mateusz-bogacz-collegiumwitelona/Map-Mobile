package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangeEmailRequestDto(
    val newEmail: String
)

@Serializable
data class ConfirmEmailChangeRequestDto(
    val code: String
)

@Serializable
data class ApiResultDto(
    val isSuccess: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val statusCode: Int? = null,
    val errors: List<String>? = null
)

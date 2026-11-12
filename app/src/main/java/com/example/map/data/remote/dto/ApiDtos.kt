package com.example.map.data.remote.dto

data class ApiResultDto(
    val isSuccess: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val statusCode: Int? = null,
    val errors: List<String>? = null
)

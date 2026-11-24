package com.example.map.data.remote.dto.request

import kotlinx.serialization.Serializable


@Serializable
data class ConfirmEmailChangeRequest(
    val code: String
)

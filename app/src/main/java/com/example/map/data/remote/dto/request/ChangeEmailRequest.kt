package com.example.map.data.remote.dto.request

import kotlinx.serialization.Serializable


@Serializable
data class ChangeEmailRequest(
    val newEmail: String
)

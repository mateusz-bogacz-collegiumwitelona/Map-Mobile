package com.example.map.data.remote.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class EditUserNamesRequest(
    val firstName: String,
    val lastName: String
)

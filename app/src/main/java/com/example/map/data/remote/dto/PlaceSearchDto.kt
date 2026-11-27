package com.example.map.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaceSearchDto(
    val id: String,
    val name: String,
    val countryName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double
)
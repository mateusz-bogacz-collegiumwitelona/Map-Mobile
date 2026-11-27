package com.example.map.domain.model

data class PlaceOption(
    val id: String,
    val name: String,
    val countryName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double
)
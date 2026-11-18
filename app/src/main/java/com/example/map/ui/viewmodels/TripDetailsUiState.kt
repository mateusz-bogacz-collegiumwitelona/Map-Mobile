package com.example.map.ui.viewmodels

import com.example.map.domain.model.TripDetails

data class TripDetailsUiState(
    val trip: TripDetails? = null,
    val totalDistanceKm: Double = 0.0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
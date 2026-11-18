package com.example.map.ui.viewmodels

import com.example.map.domain.model.TripSummary

data class TripListUiState(
    val trips: List<TripSummary> = emptyList(),
    val isLoading: Boolean = false,
    val deletingTripIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val searchTerm: String = "",
    val sortBy: String = "startDate",
    val sortDescending: Boolean = true
)
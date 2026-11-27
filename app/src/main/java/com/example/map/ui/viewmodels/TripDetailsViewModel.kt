package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.usecase.TripSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TripDetailsViewModel @Inject constructor(
    private val tripSession: TripSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadTrip(tripId: String, totalDistanceKm: Double) {
        loadJob?.cancel()

        _uiState.value = TripDetailsUiState(
            totalDistanceKm = totalDistanceKm,
            isLoading = true
        )

        loadJob = viewModelScope.launch {
            try {
                val trip = tripSession.getTripDetails(tripId)

                _uiState.value = TripDetailsUiState(
                    trip = trip,
                    totalDistanceKm = totalDistanceKm
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = TripDetailsUiState(
                    totalDistanceKm = totalDistanceKm,
                    errorMessage = exception.message
                        ?: "Nie udało się pobrać szczegółów podróży."
                )
            }
        }
    }
}
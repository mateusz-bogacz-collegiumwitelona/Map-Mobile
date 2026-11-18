package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.TripSummary
import com.example.map.domain.usecase.TripSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TripListViewModel @Inject constructor(
    private val tripSession: TripSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState = _uiState.asStateFlow()

    private var loadTripsJob: Job? = null
    private var searchJob: Job? = null

    fun loadTrips() {
        val requestState = _uiState.value

        _uiState.update {
            it.copy(isLoading = true, errorMessage = null)
        }

        loadTripsJob?.cancel()
        loadTripsJob = viewModelScope.launch {
            try {
                val trips = tripSession.getTrips(
                    searchTerm = requestState.searchTerm
                        .trim()
                        .takeIf(String::isNotEmpty),
                    sortBy = requestState.sortBy,
                    sortDescending = requestState.sortDescending
                )

                _uiState.update {
                    it.copy(
                        trips = trips,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message
                            ?: "Nie udało się pobrać podróży."
                    )
                }
            }
        }
    }

    fun onSearchTermChange(value: String) {
        _uiState.update {
            it.copy(searchTerm = value)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadTrips()
        }
    }

    fun onSortChange(sortBy: String, sortDescending: Boolean) {
        searchJob?.cancel()

        _uiState.update {
            it.copy(
                sortBy = sortBy,
                sortDescending = sortDescending
            )
        }

        loadTrips()
    }

    fun deleteTrip(tripId: String) {
        val tripToRestore = _uiState.value.trips
            .firstOrNull { it.id == tripId }
            ?: return
        val originalIndex = _uiState.value.trips.indexOf(tripToRestore)

        _uiState.update {
            it.copy(
                trips = it.trips.filterNot { trip -> trip.id == tripId },
                deletingTripIds = it.deletingTripIds + tripId,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                tripSession.deleteTrip(tripId)
                _uiState.update {
                    it.copy(deletingTripIds = it.deletingTripIds - tripId)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { state ->
                    val restoredTrips = state.trips.toMutableList()
                    if (restoredTrips.none { it.id == tripToRestore.id }) {
                        restoredTrips.add(
                            originalIndex.coerceIn(0, restoredTrips.size),
                            tripToRestore
                        )
                    }

                    state.copy(
                        trips = restoredTrips,
                        deletingTripIds = state.deletingTripIds - tripId,
                        errorMessage = exception.message
                            ?: "Nie udało się usunąć podróży."
                    )
                }
            }
        }
    }
}
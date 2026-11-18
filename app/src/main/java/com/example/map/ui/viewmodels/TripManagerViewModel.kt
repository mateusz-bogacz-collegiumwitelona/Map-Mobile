package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSegmentDraft
import com.example.map.domain.model.TripTransportType
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
class TripManagerViewModel @Inject constructor(
    private val tripSession: TripSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripManagerUiState())
    val uiState = _uiState.asStateFlow()

    private var nextSegmentId = 2L
    private var placeSearchJob: Job? = null

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun onStartDateSelected(value: Long) {
        _uiState.update {
            it.copy(startDateMillis = value, errorMessage = null)
        }
    }

    fun onEndDateEnabledChange(enabled: Boolean) {
        _uiState.update {
            it.copy(
                hasEndDate = enabled,
                endDateMillis = if (enabled) it.endDateMillis else null,
                errorMessage = null
            )
        }
    }

    fun onEndDateSelected(value: Long) {
        _uiState.update {
            it.copy(endDateMillis = value, errorMessage = null)
        }
    }

    fun addSegment() {
        _uiState.update { state ->
            val previousDestination = state.segments.lastOrNull()?.destination

            state.copy(
                segments = state.segments + TripSegmentDraft(
                    id = nextSegmentId++,
                    origin = previousDestination
                ),
                errorMessage = null
            )
        }
    }

    fun moveSegment(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            if (
                fromIndex !in state.segments.indices ||
                toIndex !in state.segments.indices ||
                fromIndex == toIndex
            ) {
                return@update state
            }

            val reorderedSegments = state.segments.toMutableList()
            val segment = reorderedSegments.removeAt(fromIndex)
            reorderedSegments.add(toIndex, segment)

            state.copy(
                segments = reorderedSegments,
                errorMessage = null
            )
        }
    }

    fun removeSegment(segmentId: Long) {
        _uiState.update { state ->
            if (state.segments.size <= 1) return@update state

            state.copy(
                segments = state.segments.filterNot { it.id == segmentId },
                activePlaceSegmentId = state.activePlaceSegmentId
                    .takeUnless { it == segmentId },
                errorMessage = null
            )
        }
    }

    fun onTransportTypeChange(
        segmentId: Long,
        transportType: TripTransportType
    ) {
        _uiState.update { state ->
            state.copy(
                segments = state.segments.map { segment ->
                    if (segment.id == segmentId) {
                        segment.copy(transportType = transportType)
                    } else {
                        segment
                    }
                },
                errorMessage = null
            )
        }
    }

    fun openPlacePicker(segmentId: Long, isOrigin: Boolean) {
        placeSearchJob?.cancel()
        _uiState.update {
            it.copy(
                activePlaceSegmentId = segmentId,
                activePlaceIsOrigin = isOrigin,
                placeQuery = "",
                placeResults = emptyList(),
                placeSearchError = null
            )
        }
    }

    fun closePlacePicker() {
        placeSearchJob?.cancel()
        _uiState.update {
            it.copy(
                activePlaceSegmentId = null,
                placeQuery = "",
                placeResults = emptyList(),
                isSearchingPlaces = false,
                placeSearchError = null
            )
        }
    }

    fun onPlaceQueryChange(value: String) {
        placeSearchJob?.cancel()

        _uiState.update {
            it.copy(
                placeQuery = value,
                placeResults = emptyList(),
                placeSearchError = null,
                isSearchingPlaces = false
            )
        }

        val query = value.trim()
        if (query.length < 2) return

        if (query.length > 100) {
            _uiState.update {
                it.copy(placeSearchError = "Wpisz maksymalnie 100 znaków.")
            }
            return
        }

        placeSearchJob = viewModelScope.launch {
            delay(300)

            _uiState.update { it.copy(isSearchingPlaces = true) }

            try {
                val results = tripSession.searchPlaces(query)
                _uiState.update {
                    it.copy(
                        isSearchingPlaces = false,
                        placeResults = results
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSearchingPlaces = false,
                        placeSearchError = "Nie udało się wyszukać miejsc."
                    )
                }
            }
        }
    }

    fun selectPlace(place: PlaceOption) {
        _uiState.update { state ->
            val segmentIndex = state.segments.indexOfFirst {
                it.id == state.activePlaceSegmentId
            }

            if (segmentIndex < 0) {
                return@update state.copy(activePlaceSegmentId = null)
            }

            val updated = state.segments.toMutableList()
            val current = updated[segmentIndex]

            if (state.activePlaceIsOrigin) {
                updated[segmentIndex] = current.copy(origin = place)
            } else {
                val oldDestination = current.destination
                updated[segmentIndex] = current.copy(destination = place)

                val nextIndex = segmentIndex + 1
                if (
                    nextIndex < updated.size &&
                    (updated[nextIndex].origin == null ||
                            updated[nextIndex].origin == oldDestination)
                ) {
                    updated[nextIndex] = updated[nextIndex].copy(origin = place)
                }
            }

            state.copy(
                segments = updated,
                activePlaceSegmentId = null,
                placeQuery = "",
                placeResults = emptyList(),
                errorMessage = null
            )
        }
    }

    fun saveTrip() {
        val state = _uiState.value
        val validationError = state.validationError

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val draft = TripDraft(
            title = state.title.trim(),
            description = state.description.trim().ifBlank { null },
            startDateMillis = state.startDateMillis!!,
            endDateMillis = if (state.hasEndDate) state.endDateMillis else null,
            segments = state.segments
        )

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                tripSession.createTrip(draft)
                _uiState.update {
                    it.copy(isSaving = false, isSaved = true)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message
                            ?: "Nie udało się zapisać podróży."
                    )
                }
            }
        }
    }
}
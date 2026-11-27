package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.common.PagedData
import com.example.map.domain.model.trip.TripSortBy
import com.example.map.domain.model.trip.TripSummary
import com.example.map.domain.model.trip.TripsError
import com.example.map.domain.model.trip.TripsResult
import com.example.map.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class TripsQuery(
    val pageNumber: Int = 1,
    val pageSize: Int = 10,
    val sortBy: TripSortBy? = null,
    val sortDescending: Boolean = true,
    val searchTerm: String? = null,
    val refreshTrigger: Int = 0
)

sealed interface TripsUiState {
    data object Loading : TripsUiState
    data class Success(val data: PagedData<TripSummary>) : TripsUiState
    data class Error(val error: TripsError, val message: String? = null) : TripsUiState
}

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {
    private val _query = MutableStateFlow(TripsQuery())
    val query: StateFlow<TripsQuery> = _query.asStateFlow()

    val uiState: StateFlow<TripsUiState> = _query
        .debounce { if (it.searchTerm.isNullOrBlank()) 0L else 300L }
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow {
                emit(TripsUiState.Loading)
                when (val result = tripRepository.getUserTrips(
                    pageNumber = q.pageNumber,
                    pageSize = q.pageSize,
                    sortBy = q.sortBy,
                    sortDescending = q.sortDescending,
                    searchTerm = q.searchTerm
                )) {
                    is TripsResult.Success -> emit(TripsUiState.Success(result.data))
                    is TripsResult.Failure -> emit(TripsUiState.Error(result.error, result.message))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripsUiState.Loading
        )

    fun onSearchChanged(term: String) {
        _query.update { it.copy(searchTerm = term, pageNumber = 1) }
    }

    fun onSortChange(sortBy: TripSortBy, descending: Boolean) {
        _query.update { it.copy(sortBy = sortBy, sortDescending = descending, pageNumber = 1) }
    }

    fun onNextPage() {
        val current = (uiState.value as? TripsUiState.Success)?.data ?: return
        if (current.hasNextPage) _query.update { it.copy(pageNumber = current.pageNumber + 1) }
    }

    fun onPreviousPage() {
        val current = (uiState.value as? TripsUiState.Success)?.data ?: return
        if (current.hasPreviousPage) _query.update { it.copy(pageNumber = current.pageNumber - 1) }
    }

    fun refresh() {
        _query.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
    }
}
package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.trip.TripSortBy
import com.example.map.domain.model.trip.TripSummary
import com.example.map.domain.model.trip.TripsError
import com.example.map.domain.model.trip.TripsResult
import com.example.map.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripsUiState(
    val items: List<TripSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasNextPage: Boolean = false,
    val currentPage: Int = 0,
    val error: TripsError? = null,
    val errorMessage: String? = null,
    val searchTerm: String = "",
    val sortBy: TripSortBy? = null,
    val sortDescending: Boolean = true
) {
    val isEmpty: Boolean
        get() = !isLoading && !isRefreshing && items.isEmpty() && error == null
}

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(TripsUiState(isLoading = true))
    val uiState: StateFlow<TripsUiState> = _uiState.asStateFlow()
    private var fetchJob: Job? = null
    private var searchDebounceJob: Job? = null


    init {
        loadFirstPage(isRefresh = false)
    }

    fun refresh() {
        searchDebounceJob?.cancel()
        loadFirstPage(isRefresh = true)
    }

    fun retry() {
        if (_uiState.value.items.isEmpty()) {
            loadFirstPage(isRefresh = false)
        } else {
            loadNextPage()
        }
    }

    fun onSearchChange(term: String) {
        _uiState.update { it.copy(searchTerm = term) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            if (term.isNotBlank()) delay(300L)
            loadFirstPage(isRefresh = false)
        }
    }

    fun onSortChange(sortBy: TripSortBy, descending: Boolean) {
        searchDebounceJob?.cancel()
        _uiState.update {
            it.copy(sortBy = sortBy, sortDescending = descending)
        }
        loadFirstPage(isRefresh = false)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.hasNextPage) {
            return
        }

        val nextPage = state.currentPage + 1
        fetchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingMore = true,
                    error = null,
                    errorMessage = null
                )
            }

            when (val result = tripRepository.getUserTrips(
                pageNumber = nextPage,
                pageSize = PAGE_SIZE,
                sortBy = state.sortBy,
                sortDescending = state.sortDescending,
                searchTerm = state.searchTerm
            )) {
                is TripsResult.Success -> {
                    val existingIds = _uiState.value.items.mapTo(HashSet()) {it.id}
                    val newItems = result.data.items.filterNot { it.id in existingIds }

                    _uiState.update {
                        it.copy(
                            items = it.items + newItems,
                            isLoadingMore = false,
                            currentPage = result.data.pageNumber,
                            hasNextPage = result.data.hasNextPage
                        )
                    }
                }

                is TripsResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = result.error,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun loadFirstPage(isRefresh: Boolean) {
        fetchJob?.cancel()
        val state = _uiState.value

        fetchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    isLoadingMore = false,
                    error = null,
                    errorMessage = null
                )
            }

            when(val result = tripRepository.getUserTrips(
                pageNumber = 1,
                pageSize = PAGE_SIZE,
                sortBy = state.sortBy,
                sortDescending = state.sortDescending,
                searchTerm = state.searchTerm
            )) {
                is TripsResult.Success -> {
                    _uiState.update {
                        it.copy(
                            items = result.data.items,
                            isLoading = false,
                            isRefreshing = false,
                            currentPage = result.data.pageNumber,
                            hasNextPage = result.data.hasNextPage,
                            error = null,
                            errorMessage = null
                        )
                    }
                }

                is TripsResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = result.error,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 10
    }
}
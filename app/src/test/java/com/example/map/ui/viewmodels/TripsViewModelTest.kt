package com.example.map.ui.viewmodels

import com.example.map.domain.model.common.PagedData
import com.example.map.domain.model.trip.TripSortBy
import com.example.map.domain.model.trip.TripsError
import com.example.map.domain.model.trip.TripsResult
import com.example.map.testing.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeTripRepository
    private lateinit var viewModel: TripsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTripRepository()
        viewModel = TripsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsInitialPageOnSubscribe() = runTest(dispatcher) {
        observeUiState()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TripsUiState.Success)
        assertEquals(1, repository.calls.size)
        assertEquals(1, repository.calls.first().pageNumber)
    }

    @Test
    fun debouncesSearchAndResetsPageToOne() = runTest(dispatcher) {
        observeUiState()
        advanceUntilIdle()

        viewModel.onNextPage()
        advanceUntilIdle()
        assertEquals(2, viewModel.query.value.pageNumber)

        viewModel.onSearchChanged("w")
        advanceTimeBy(150)
        viewModel.onSearchChanged("wa")
        advanceTimeBy(150)
        viewModel.onSearchChanged("wakacje")
        advanceTimeBy(300)
        advanceUntilIdle()

        val searchCalls = repository.calls.filter { it.searchTerm != null }
        assertEquals(1, searchCalls.size)
        assertEquals("wakacje", searchCalls.first().searchTerm)
        assertEquals(1, searchCalls.first().pageNumber)
    }

    @Test
    fun cancelsInFlightRequestWhenSortChanges() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.getTripsGate = gate

        observeUiState()
        runCurrent()

        assertEquals(1, repository.calls.size)
        assertEquals(TripsUiState.Loading, viewModel.uiState.value)

        // Zdejmujemy bramkę dla kolejnego zapytania i zmieniamy sortowanie
        repository.getTripsGate = null
        viewModel.onSortChange(TripSortBy.TITLE, descending = false)
        advanceUntilIdle()

        assertEquals(1, repository.cancelledCallsCount)
        assertEquals(2, repository.calls.size)
        assertEquals(TripSortBy.TITLE, repository.calls.last().sortBy)
        assertEquals(false, repository.calls.last().sortDescending)
        assertTrue(viewModel.uiState.value is TripsUiState.Success)
    }

    @Test
    fun paginationRespectsHasNextAndHasPreviousFlags() = runTest(dispatcher) {
        repository.resultProvider = { page ->
            TripsResult.Success(
                PagedData(
                    items = emptyList(),
                    pageNumber = page,
                    pageSize = 10,
                    totalCount = 20,
                    totalPages = 2,
                    hasPreviousPage = page > 1,
                    hasNextPage = page < 2
                )
            )
        }

        observeUiState()
        advanceUntilIdle()

        viewModel.onPreviousPage()
        advanceUntilIdle()
        assertEquals(1, viewModel.query.value.pageNumber)

        viewModel.onNextPage()
        advanceUntilIdle()
        assertEquals(2, viewModel.query.value.pageNumber)

        viewModel.onNextPage()
        advanceUntilIdle()
        assertEquals(2, viewModel.query.value.pageNumber)
    }

    @Test
    fun refreshTriggersReloadWithSameFilters() = runTest(dispatcher) {
        observeUiState()
        advanceUntilIdle()
        assertEquals(1, repository.calls.size)

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun emitsErrorStateWhenRepositoryFails() = runTest(dispatcher) {
        repository.resultProvider = {
            TripsResult.Failure(TripsError.NETWORK, "Brak internetu")
        }

        observeUiState()
        advanceUntilIdle()

        assertEquals(
            TripsUiState.Error(TripsError.NETWORK, "Brak internetu"),
            viewModel.uiState.value
        )
    }

    private fun TestScope.observeUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
    }
}
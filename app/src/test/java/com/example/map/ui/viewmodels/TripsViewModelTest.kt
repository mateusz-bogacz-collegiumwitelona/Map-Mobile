package com.example.map.ui.viewmodels

import com.example.map.domain.model.common.PagedData
import com.example.map.domain.model.trip.TripSortBy
import com.example.map.domain.model.trip.TripSummary
import com.example.map.domain.model.trip.TripsError
import com.example.map.domain.model.trip.TripsResult
import com.example.map.testing.FakeTripRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTripRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTripRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsInitialPageOnInit() = runTest(dispatcher) {
        repository.resultProvider = { page ->
            TripsResult.Success(
                pagedData(
                    items = listOf(trip("1"), trip("2")),
                    page = page,
                    totalPages = 2
                )
            )
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertEquals(1, state.currentPage)
        assertTrue(state.hasNextPage)
        assertEquals(1, repository.calls.size)
        assertEquals(1, repository.calls.first().pageNumber)
    }

    @Test
    fun loadNextPageAppendsItemsAndDeduplicates() = runTest(dispatcher) {
        repository.resultProvider = { page ->
            val items = when (page) {
                1 -> listOf(trip("1"), trip("2"))
                2 -> listOf(trip("2"), trip("3")) // "2" jest duplikatem
                else -> emptyList()
            }
            TripsResult.Success(pagedData(items = items, page = page, totalPages = 2))
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingMore)
        assertEquals(listOf("1", "2", "3"), state.items.map { it.id })
        assertEquals(2, state.currentPage)
        assertFalse(state.hasNextPage)
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun loadNextPageDoesNothingWhenNoNextPageOrAlreadyLoadingMore() = runTest(dispatcher) {
        repository.resultProvider = { page ->
            TripsResult.Success(
                pagedData(items = listOf(trip("p$page")), page = page, totalPages = 2)
            )
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        val gate = CompletableDeferred<Unit>()
        repository.getTripsGate = gate

        viewModel.loadNextPage()
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingMore)

        viewModel.loadNextPage()
        runCurrent()
        assertEquals(2, repository.calls.size)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasNextPage)

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(2, repository.calls.size)
    }

    @Test
    fun refreshResetsListToFirstPageAndSetsIsRefreshing() = runTest(dispatcher) {
        repository.resultProvider = { page ->
            TripsResult.Success(
                pagedData(items = listOf(trip("p$page")), page = page, totalPages = 2)
            )
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(listOf("p1", "p2"), viewModel.uiState.value.items.map { it.id })

        val gate = CompletableDeferred<Unit>()
        repository.getTripsGate = gate
        repository.resultProvider = { page ->
            TripsResult.Success(
                pagedData(items = listOf(trip("fresh-$page")), page = page, totalPages = 1)
            )
        }

        viewModel.refresh()
        runCurrent()
        assertTrue(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRefreshing)
        assertEquals(listOf("fresh-1"), state.items.map { it.id })
        assertEquals(1, state.currentPage)
        assertFalse(state.hasNextPage)
    }

    @Test
    fun debouncesSearchAndResetsToFirstPage() = runTest(dispatcher) {
        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.currentPage)

        viewModel.onSearchChange("w")
        advanceTimeBy(150)
        viewModel.onSearchChange("wa")
        advanceTimeBy(150)
        viewModel.onSearchChange("wakacje")
        advanceTimeBy(300)
        advanceUntilIdle()

        val searchCalls = repository.calls.filter { !it.searchTerm.isNullOrBlank() }
        assertEquals(1, searchCalls.size)
        assertEquals("wakacje", searchCalls.first().searchTerm)
        assertEquals(1, searchCalls.first().pageNumber)
    }

    @Test
    fun cancelsInFlightRequestWhenSortChanges() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.getTripsGate = gate

        val viewModel = TripsViewModel(repository)
        runCurrent()

        assertEquals(1, repository.calls.size)
        assertTrue(viewModel.uiState.value.isLoading)

        repository.getTripsGate = null
        viewModel.onSortChange(TripSortBy.TITLE, descending = false)
        advanceUntilIdle()

        assertEquals(1, repository.cancelledCallsCount)
        assertEquals(2, repository.calls.size)
        assertEquals(TripSortBy.TITLE, repository.calls.last().sortBy)
        assertEquals(false, repository.calls.last().sortDescending)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun emitsErrorStateAndRetryReloadsFirstPageWhenEmpty() = runTest(dispatcher) {
        repository.resultProvider = {
            TripsResult.Failure(TripsError.NETWORK, "Brak internetu")
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertEquals(TripsError.NETWORK, errorState.error)
        assertEquals("Brak internetu", errorState.errorMessage)
        assertFalse(errorState.isEmpty)

        repository.resultProvider = { page ->
            TripsResult.Success(pagedData(items = listOf(trip("1")), page = page, totalPages = 1))
        }

        viewModel.retry()
        advanceUntilIdle()

        val recoveredState = viewModel.uiState.value
        assertNull(recoveredState.error)
        assertEquals(listOf("1"), recoveredState.items.map { it.id })
    }

    @Test
    fun retryLoadsNextPageWhenItemsAlreadyExist() = runTest(dispatcher) {
        repository.resultProvider = { page ->
            if (page == 1) {
                TripsResult.Success(pagedData(items = listOf(trip("1")), page = 1, totalPages = 2))
            } else {
                TripsResult.Failure(TripsError.NETWORK, "Brak internetu")
            }
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(TripsError.NETWORK, viewModel.uiState.value.error)
        assertEquals(listOf("1"), viewModel.uiState.value.items.map { it.id })

        repository.resultProvider = { page ->
            TripsResult.Success(pagedData(items = listOf(trip("2")), page = page, totalPages = 2))
        }

        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertEquals(listOf("1", "2"), state.items.map { it.id })
        assertEquals(2, state.currentPage)
    }

    @Test
    fun isEmptyIsTrueWhenFirstPageReturnsNoItems() = runTest(dispatcher) {
        repository.resultProvider = {
            TripsResult.Success(pagedData(items = emptyList(), page = 1, totalPages = 0))
        }

        val viewModel = TripsViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    private fun trip(id: String) = TripSummary(
        id = id,
        title = "Trip $id",
        description = null,
        startDate = "2026-07-01T10:00:00Z",
        endDate = null,
        totalDistance = 100.0,
        transportTypes = emptyList()
    )

    private fun pagedData(
        items: List<TripSummary>,
        page: Int,
        totalPages: Int
    ) = PagedData(
        items = items,
        pageNumber = page,
        pageSize = 10,
        totalCount = totalPages * 10,
        totalPages = totalPages,
        hasPreviousPage = page > 1,
        hasNextPage = page < totalPages
    )
}
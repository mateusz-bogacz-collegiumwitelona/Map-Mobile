package com.example.map.ui.viewmodels

import com.example.map.domain.model.TripSummary
import com.example.map.domain.usecase.TripSession
import com.example.map.testing.FakeTripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTripRepository
    private lateinit var viewModel: TripListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTripRepository()
        viewModel = TripListViewModel(TripSession(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTripsDisplaysTripsFromRepository() = runTest(dispatcher) {
        repository.tripSummaries += TripSummary(
            id = "trip-1",
            title = "Weekend",
            startDateMillis = 1_000L,
            endDateMillis = 2_000L,
            totalDistanceKm = 226.0
        )

        viewModel.loadTrips()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.trips.size)
        assertEquals("Weekend", viewModel.uiState.value.trips.single().title)
    }

    @Test
    fun deleteTripRemovesItFromListAfterRequest() = runTest(dispatcher) {
        repository.tripSummaries += TripSummary(
            id = "trip-1",
            title = "Weekend",
            startDateMillis = 1_000L,
            endDateMillis = 2_000L,
            totalDistanceKm = 226.0
        )

        viewModel.loadTrips()
        advanceUntilIdle()
        viewModel.deleteTrip("trip-1")
        advanceUntilIdle()

        assertEquals(emptyList<TripSummary>(), viewModel.uiState.value.trips)
        assertEquals(listOf("trip-1"), repository.deletedTripIds)
    }

    @Test
    fun deleteTripRestoresCardWhenRequestFails() = runTest(dispatcher) {
        repository.tripSummaries += TripSummary(
            id = "trip-1",
            title = "Weekend",
            startDateMillis = 1_000L,
            endDateMillis = 2_000L,
            totalDistanceKm = 226.0
        )
        repository.deleteError = IllegalStateException("Offline")

        viewModel.loadTrips()
        advanceUntilIdle()
        viewModel.deleteTrip("trip-1")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.trips.size)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun searchTermIsPassedToRepository() = runTest(dispatcher) {
        viewModel.onSearchTermChange("  Warszawa  ")
        advanceUntilIdle()

        assertEquals("Warszawa", repository.lastTripsSearchTerm)
    }

    @Test
    fun selectedSortIsPassedToRepository() = runTest(dispatcher) {
        viewModel.onSortChange("title", false)
        advanceUntilIdle()

        assertEquals("title", repository.lastTripsSortBy)
        assertEquals(false, repository.lastTripsSortDescending)
    }
}
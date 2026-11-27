package com.example.map.ui.viewmodels

import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSegmentDetails
import com.example.map.domain.model.TripTransportType
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripDetailsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTripRepository
    private lateinit var viewModel: TripDetailsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTripRepository()
        viewModel = TripDetailsViewModel(TripSession(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadTripDisplaysDetailsAndListDistance() = runTest(dispatcher) {
        val tripId = "trip-1"
        val details = TripDetails(
            id = tripId,
            draft = TripDraft(
                title = "Silent Disco",
                description = "Wyjazd do Warszawy",
                startDateMillis = 1_000L,
                endDateMillis = 2_000L,
                segments = emptyList()
            ),
            segmentDetails = listOf(
                TripSegmentDetails(
                    orderIndex = 0,
                    originName = "Wrocław",
                    destinationName = "Warszawa",
                    transportType = TripTransportType.TRAIN,
                    distanceKm = 301.84,
                    isFallbackRoute = true
                )
            )
        )
        repository.tripDetails[tripId] = details

        viewModel.loadTrip(tripId, 603.68)
        advanceUntilIdle()

        assertEquals(details, viewModel.uiState.value.trip)
        assertEquals(603.68, viewModel.uiState.value.totalDistanceKm, 0.0)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadTripShowsErrorWhenRepositoryFails() = runTest(dispatcher) {
        viewModel.loadTrip("missing-trip", 0.0)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
package com.example.map.ui.viewmodels

import com.example.map.domain.model.PlaceOption
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSegmentDraft

@OptIn(ExperimentalCoroutinesApi::class)
class TripManagerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeTripRepository
    private lateinit var viewModel: TripManagerViewModel
    private val warsaw = PlaceOption(
        id = "1",
        name = "Warszawa",
        countryName = "Polska",
        countryCode = "PL",
        latitude = 52.2297,
        longitude = 21.0122
    )
    private val wroclaw = PlaceOption(
        id = "2",
        name = "Wrocław",
        countryName = "Polska",
        countryCode = "PL",
        latitude = 51.1079,
        longitude = 17.0385
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeTripRepository().apply {
            places = listOf(warsaw, wroclaw)
        }
        viewModel = TripManagerViewModel(TripSession(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithOneSegmentAndDoesNotAllowRemovingTheLastOne() {
        assertEquals(1, viewModel.uiState.value.segments.size)

        viewModel.removeSegment(viewModel.uiState.value.segments.single().id)

        assertEquals(1, viewModel.uiState.value.segments.size)
    }

    @Test
    fun addingSegmentStartsItAtPreviousDestination() = runTest(dispatcher) {
        viewModel.openPlacePicker(1, isOrigin = true)
        viewModel.onPlaceQueryChange("War")
        advanceUntilIdle()
        viewModel.selectPlace(warsaw)

        viewModel.openPlacePicker(1, isOrigin = false)
        viewModel.onPlaceQueryChange("Wro")
        advanceUntilIdle()
        viewModel.selectPlace(wroclaw)

        viewModel.addSegment()

        assertEquals(
            wroclaw,
            viewModel.uiState.value.segments[1].origin
        )
    }

    @Test
    fun doesNotSubmitTripWithInvalidDateRange() = runTest(dispatcher) {
        viewModel.onTitleChange("Wyjazd")
        viewModel.onStartDateSelected(2_000L)
        viewModel.onEndDateEnabledChange(true)
        viewModel.onEndDateSelected(1_000L)

        viewModel.saveTrip()
        advanceUntilIdle()

        assertTrue(repository.createdTrips.isEmpty())
        assertEquals(
            "Data zakończenia nie może być wcześniejsza niż rozpoczęcia.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun savesTripAfterSelectingBothPlaces() = runTest(dispatcher) {
        viewModel.onTitleChange("Weekend")
        viewModel.onDescriptionChange("Wyjazd do miasta")
        viewModel.onStartDateSelected(1_000L)

        viewModel.openPlacePicker(1, isOrigin = true)
        viewModel.onPlaceQueryChange("War")
        advanceUntilIdle()
        viewModel.selectPlace(warsaw)

        viewModel.openPlacePicker(1, isOrigin = false)
        viewModel.onPlaceQueryChange("Wro")
        advanceUntilIdle()
        viewModel.selectPlace(wroclaw)

        assertNull(viewModel.uiState.value.validationError)

        viewModel.saveTrip()
        advanceUntilIdle()

        assertEquals(1, repository.createdTrips.size)
        assertEquals("Weekend", repository.createdTrips.single().title)
        assertEquals(1, repository.createdTrips.single().segments.size)
        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun doesNotSaveWhenPlaceEndpointsAreMissing() {
        viewModel.onTitleChange("Weekend")
        viewModel.onStartDateSelected(1_000L)

        viewModel.saveTrip()

        assertTrue(repository.createdTrips.isEmpty())
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun moveSegmentChangesItsPositionInTheList() {
        viewModel.addSegment()
        viewModel.addSegment()

        viewModel.moveSegment(fromIndex = 2, toIndex = 1)

        assertEquals(
            listOf(1L, 3L, 2L),
            viewModel.uiState.value.segments.map { it.id }
        )
    }

    @Test
    fun loadsTripAndUpdatesExistingTrip() = runTest(dispatcher) {
        val tripId = "trip-1"
        val draft = TripDraft(
            title = "Weekend",
            description = "Wyjazd do miasta",
            startDateMillis = 1_000L,
            endDateMillis = 2_000L,
            segments = listOf(
                TripSegmentDraft(
                    id = 1L,
                    origin = warsaw,
                    destination = wroclaw
                )
            )
        )
        repository.tripDetails[tripId] = TripDetails(
            id = tripId,
            draft = draft
        )

        viewModel.loadTrip(tripId)
        advanceUntilIdle()

        assertEquals(tripId, viewModel.uiState.value.tripId)
        assertEquals("Weekend", viewModel.uiState.value.title)
        assertEquals("Wyjazd do miasta", viewModel.uiState.value.description)
        assertEquals(1, viewModel.uiState.value.segments.size)

        viewModel.onTitleChange("Weekend po edycji")
        viewModel.saveTrip()
        advanceUntilIdle()

        assertTrue(repository.createdTrips.isEmpty())
        assertEquals(tripId, repository.updatedTrips.single().first)
        assertEquals(
            "Weekend po edycji",
            repository.updatedTrips.single().second.title
        )
        assertTrue(viewModel.uiState.value.isSaved)
    }
}
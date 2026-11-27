package com.example.map.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.trip.TransportType
import com.example.map.domain.model.trip.TripSummary
import com.example.map.domain.model.trip.TripsError
import com.example.map.ui.components.trip.TripListSection
import com.example.map.ui.viewmodels.TripsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class TripListSectionUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTripCardDetailsAndTransportIcons() {
        val trip = sampleTrip(
            id = "trip-1",
            title = "Wyprawa w Alpy",
            description = "Piękne widoki i górskie trasy",
            startDate = "2026-07-01T10:00:00Z",
            endDate = "2026-07-10T18:00:00Z",
            totalDistance = 1245.5,
            transportTypes = listOf(TransportType.PLANE, TransportType.CAR, TransportType.TRAIN)
        )

        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(items = listOf(trip)),
                isReadOnly = false,
                onRefresh = {},
                onLoadMore = {},
                onRetry = {},
                onViewOnMapClick = {}
            )
        }

        val expectedDistance = String.format(Locale.getDefault(), "%.1f km", 1245.5)

        composeRule.onNodeWithText("Wyprawa w Alpy").assertIsDisplayed()
        composeRule.onNodeWithText("2026-07-01 – 2026-07-10").assertIsDisplayed()
        composeRule.onNodeWithText("Piękne widoki i górskie trasy").assertIsDisplayed()
        composeRule.onNodeWithText(expectedDistance).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Samolot").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Samochód").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pociąg").assertIsDisplayed()
    }

    @Test
    fun ownerModeInvokesMapEditAndDeleteCallbacks() {
        var mappedId: String? = null
        var editedId: String? = null
        var deletedId: String? = null

        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(items = listOf(sampleTrip(id = "trip-42"))),
                isReadOnly = false,
                onRefresh = {},
                onLoadMore = {},
                onRetry = {},
                onViewOnMapClick = { mappedId = it },
                onEditTripClick = { editedId = it },
                onDeleteTripClick = { deletedId = it }
            )
        }

        composeRule.onNodeWithTag("trip_map_trip-42").performClick()
        composeRule.onNodeWithTag("trip_edit_trip-42").performClick()
        composeRule.onNodeWithTag("trip_delete_trip-42").performClick()

        assertEquals("trip-42", mappedId)
        assertEquals("trip-42", editedId)
        assertEquals("trip-42", deletedId)
    }

    @Test
    fun readOnlyModeHidesEditAndDeleteButKeepsMapAction() {
        var mappedId: String? = null

        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(items = listOf(sampleTrip(id = "trip-99"))),
                isReadOnly = true,
                onRefresh = {},
                onLoadMore = {},
                onRetry = {},
                onViewOnMapClick = { mappedId = it }
            )
        }

        composeRule.onNodeWithTag("trip_map_trip-99").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithTag("trip_edit_trip-99").assertCountEquals(0)
        composeRule.onAllNodesWithTag("trip_delete_trip-99").assertCountEquals(0)
        assertEquals("trip-99", mappedId)
    }

    @Test
    fun showsLoadingSpinnerWhenInitialLoadIsInProgress() {
        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(isLoading = true),
                onRefresh = {},
                onLoadMore = {},
                onRetry = {},
                onViewOnMapClick = {}
            )
        }

        composeRule.onNodeWithTag("trips_loading").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsCreateButtonForOwnerAndHidesItInReadOnlyMode() {
        var createClicks = 0

        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(items = emptyList(), isLoading = false),
                isReadOnly = false,
                onRefresh = {},
                onLoadMore = {},
                onRetry = {},
                onViewOnMapClick = {},
                onCreateTripClick = { createClicks++ }
            )
        }

        composeRule.onNodeWithTag("trips_empty").assertIsDisplayed()
        composeRule.onNodeWithText("Nie masz jeszcze żadnych podróży.").assertIsDisplayed()
        composeRule.onNodeWithTag("trips_create_first").assertIsDisplayed().performClick()
        assertEquals(1, createClicks)
    }

    @Test
    fun emptyStateInReadOnlyModeHidesCreateButton() {
        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(items = emptyList(), isLoading = false),
                isReadOnly = true,
                onRefresh = {},
                onLoadMore = {},
                onRetry = {},
                onViewOnMapClick = {}
            )
        }

        composeRule.onNodeWithText("Ten użytkownik nie ma jeszcze żadnych podróży.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag("trips_create_first").assertCountEquals(0)
    }

    @Test
    fun errorBannerDisplaysMessageAndTriggersRetry() {
        var retryClicks = 0

        composeRule.setContent {
            TripListSection(
                uiState = TripsUiState(
                    items = emptyList(),
                    error = TripsError.NETWORK
                ),
                onRefresh = {},
                onLoadMore = {},
                onRetry = { retryClicks++ },
                onViewOnMapClick = {}
            )
        }

        composeRule.onNodeWithTag("trips_error_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Brak połączenia z internetem.").assertIsDisplayed()
        composeRule.onNodeWithTag("trips_retry").performClick()

        assertEquals(1, retryClicks)
    }

    private fun sampleTrip(
        id: String,
        title: String = "Wycieczka",
        description: String? = null,
        startDate: String = "2026-07-01T10:00:00Z",
        endDate: String? = null,
        totalDistance: Double = 100.0,
        transportTypes: List<TransportType> = emptyList()
    ) = TripSummary(
        id = id,
        title = title,
        description = description,
        startDate = startDate,
        endDate = endDate,
        totalDistance = totalDistance,
        transportTypes = transportTypes
    )
}
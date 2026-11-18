package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.TripSummary
import com.example.map.ui.screens.TripListScreen
import com.example.map.ui.viewmodels.TripListUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripListScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val trip = TripSummary(
        id = "trip-1",
        title = "Weekend",
        startDateMillis = 1_000L,
        endDateMillis = 2_000L,
        totalDistanceKm = 226.0
    )

    private fun showScreen(
        onEditTrip: (String) -> Unit = {},
        onDeleteTrip: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            TripListScreen(
                state = TripListUiState(trips = listOf(trip)),
                onBackClick = {},
                onSettingsClick = {},
                onLogoutClick = {},
                onRetryClick = {},
                onEditTrip = onEditTrip,
                onDeleteTrip = onDeleteTrip
            )
        }
    }

    @Test
    fun cancelingDeleteKeepsTripAndDoesNotCallDelete() {
        var deletedTripId: String? = null
        showScreen(onDeleteTrip = { deletedTripId = it })

        composeRule
            .onNodeWithTag("trip_delete_button_trip-1")
            .performClick()

        composeRule
            .onNodeWithText("Usunąć podróż?")
            .assertIsDisplayed()

        composeRule
            .onNodeWithTag("trip_delete_cancel")
            .performClick()

        composeRule
            .onNodeWithText("Usunąć podróż?")
            .assertDoesNotExist()

        composeRule
            .onNodeWithTag("trip_card_trip-1")
            .assertIsDisplayed()

        assertNull(deletedTripId)
    }

    @Test
    fun confirmingDeleteCallsCallbackWithTripId() {
        var deletedTripId: String? = null
        showScreen(onDeleteTrip = { deletedTripId = it })

        composeRule
            .onNodeWithTag("trip_delete_button_trip-1")
            .performClick()

        composeRule
            .onNodeWithTag("trip_delete_confirm")
            .performClick()

        assertEquals("trip-1", deletedTripId)
    }

    @Test
    fun editButtonCallsCallbackWithTripId() {
        var editedTripId: String? = null
        showScreen(onEditTrip = { editedTripId = it })

        composeRule
            .onNodeWithTag("trip_edit_button_trip-1")
            .performClick()

        assertEquals("trip-1", editedTripId)
    }
}
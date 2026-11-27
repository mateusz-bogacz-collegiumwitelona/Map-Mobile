package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSegmentDetails
import com.example.map.domain.model.TripTransportType
import com.example.map.ui.screens.TripDetailsScreen
import com.example.map.ui.viewmodels.TripDetailsUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDetailsScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val trip = TripDetails(
        id = "trip-1",
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

    private fun showScreen(onBackClick: () -> Unit = {}) {
        composeRule.setContent {
            TripDetailsScreen(
                state = TripDetailsUiState(
                    trip = trip,
                    totalDistanceKm = 603.68
                ),
                onBackClick = onBackClick,
                onSettingsClick = {},
                onLogoutClick = {},
                onRetryClick = {}
            )
        }
    }

    @Test
    fun displaysTripDescriptionDistanceAndSegment() {
        showScreen()

        composeRule.onNodeWithText("Silent Disco").assertIsDisplayed()
        composeRule.onNodeWithText("Wyjazd do Warszawy").assertIsDisplayed()
        composeRule.onNodeWithText("Całkowity dystans").assertIsDisplayed()
        composeRule.onNodeWithText("603,68 km").assertIsDisplayed()
        composeRule.onNodeWithText("Wrocław → Warszawa").assertIsDisplayed()
        composeRule.onNodeWithText("Transport: Pociąg").assertIsDisplayed()
        composeRule.onNodeWithText("Dystans: 301,84 km").assertIsDisplayed()
        composeRule.onNodeWithText("Dystans orientacyjny").assertIsDisplayed()
    }

    @Test
    fun backButtonCallsCallback() {
        var wentBack = false
        showScreen(onBackClick = { wentBack = true })

        composeRule
            .onNodeWithTag("trip_details_back_button")
            .performClick()

        assertTrue(wentBack)
    }
}
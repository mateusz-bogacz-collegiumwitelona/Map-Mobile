package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.viewmodels.HomeUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeDisplaysProfileAndCallsTripButtonCallbacks() {
        var tripsClicked = false
        var createTripClicked = false

        composeRule.setContent {
            HomeScreen(
                onProfileClick = {},
                onLogoutClick = {},
                uiState = HomeUiState(
                    firstName = "Tomasz",
                    lastName = "Kot",
                    isLoading = false
                ),
                onMyTripsClick = { tripsClicked = true },
                onNewTripClick = { createTripClicked = true }
            )
        }

        composeRule.onNodeWithText("TK").assertIsDisplayed()
        composeRule.onNodeWithText("Tomasz Kot").assertIsDisplayed()

        composeRule.onNodeWithTag("home_my_trips_button").performClick()
        composeRule.onNodeWithTag("home_new_trip_button").performClick()

        composeRule.runOnIdle {
            assertTrue(tripsClicked)
            assertTrue(createTripClicked)
        }
    }

    @Test
    fun profileErrorRetryButtonCallsCallback() {
        var retryClicked = false

        composeRule.setContent {
            HomeScreen(
                onProfileClick = {},
                onLogoutClick = {},
                uiState = HomeUiState(
                    isLoading = false,
                    errorMessage = "Nie udało się pobrać danych profilu."
                ),
                onRetryProfileClick = { retryClicked = true }
            )
        }

        composeRule.onNodeWithText("Spróbuj ponownie").performClick()

        composeRule.runOnIdle {
            assertTrue(retryClicked)
        }
    }
}
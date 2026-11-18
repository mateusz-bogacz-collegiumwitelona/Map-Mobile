package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.HomeViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.map.domain.usecase.TripSession
import com.example.map.testing.NoOpTripRepository
import com.example.map.ui.viewmodels.TripManagerViewModel

@RunWith(AndroidJUnit4::class)
class HomeNavigationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeButtonsOpenTripListAndTripManagerScreens() {
        val accountSession = AccountSession(
            FakeAccountRepository(),
            FakeProfileStorage()
        )
        val authViewModel = AuthViewModel(
            AuthSession(
                FakeTokenStorage(
                    AuthTokens(
                        accessToken = "access-test",
                        refreshToken = "refresh-test"
                    )
                ),
                FakeAuthRepository()
            ),
            accountSession
        )
        val homeViewModel = HomeViewModel(accountSession)

        composeRule.setContent {
            AppNavigation(
                viewModel = authViewModel,
                homeViewModelFactory = { homeViewModel },
                tripManagerViewModelFactory = {
                    TripManagerViewModel(
                        TripSession(NoOpTripRepository())
                    )
                }
            )
        }

        composeRule.onNodeWithTag("home_my_trips_button").performClick()
        composeRule.onNodeWithText("Moje podróże").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wróć").performClick()

        composeRule.onNodeWithTag("home_new_trip_button").performClick()
        composeRule.onNodeWithText("Dodawanie podróży").assertIsDisplayed()
    }
}
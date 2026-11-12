package com.example.map.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.usecase.AuthSession
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Assert.assertFalse
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class MainActivityLoginTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successfulUserLoginStoresTokensAndOpensHome() {
        val tokenStorage = FakeTokenStorage()

        val authRepository = object : AuthRepository {
            override suspend fun login(
                email: String,
                password: String
            ) = AuthenticatedAccount(
                tokens = AuthTokens(
                    accessToken = "access-user",
                    refreshToken = "refresh-user"
                ),
                roles = listOf("User")
            )

            override suspend fun refresh(
                refreshToken: String
            ): AuthenticatedAccount {
                error("Refresh nie jest używany w tym teście.")
            }

            override suspend fun logout(
                accessToken: String,
                refreshToken: String
            ) = Unit
        }

        val viewModel = AuthViewModel(
            AuthSession(tokenStorage, authRepository)
        )

        composeRule.setContent {
            AppNavigation(viewModel = viewModel)
        }

        composeRule.onNodeWithText("E-mail")
            .performTextInput("user@example.com")
        composeRule.onNodeWithText("Hasło")
            .performTextInput("Password123!")
        composeRule.onNodeWithText("Zaloguj się")
            .performClick()

        composeRule.onNodeWithText("Jan Kowalski").assertExists()

        composeRule.runOnIdle {
            assertEquals("access-user", tokenStorage.getAccessToken())
            assertEquals("refresh-user", tokenStorage.getRefreshToken())
        }
    }

    @Test
    fun invalidEmailShowsValidationErrorWithoutCallingBackend() {
        var loginCalled = false

        val repository = object : AuthRepository {
            override suspend fun login(
                email: String,
                password: String
            ): AuthenticatedAccount {
                loginCalled = true
                error("Backend nie powinien być wywołany dla błędnego e-maila.")
            }

            override suspend fun refresh(
                refreshToken: String
            ): AuthenticatedAccount {
                error("Refresh nie jest używany w tym teście.")
            }

            override suspend fun logout(
                accessToken: String,
                refreshToken: String
            ) = Unit
        }

        val viewModel = AuthViewModel(
            AuthSession(FakeTokenStorage(), repository)
        )

        composeRule.setContent {
            AppNavigation(viewModel = viewModel)
        }

        composeRule.onNodeWithText("E-mail")
            .performTextInput("to-nie-jest-email")
        composeRule.onNodeWithText("Hasło")
            .performTextInput("Password123!")
        composeRule.onNodeWithText("Zaloguj się")
            .performClick()

        composeRule.onNodeWithText("Wpisz poprawny adres e-mail.")
            .assertIsDisplayed()

        composeRule.runOnIdle {
            assertFalse(loginCalled)
        }
    }

    @Test
    fun networkFailureFromRepositoryIsShownOnLoginScreen() {
        var loginCalled = false

        val repository = object : AuthRepository {
            override suspend fun login(
                email: String,
                password: String
            ): AuthenticatedAccount {
                loginCalled = true
                throw AuthException(AuthFailure.NETWORK_ERROR)
            }

            override suspend fun refresh(
                refreshToken: String
            ): AuthenticatedAccount {
                error("Refresh nie jest używany w tym teście.")
            }

            override suspend fun logout(
                accessToken: String,
                refreshToken: String
            ) = Unit
        }

        val viewModel = AuthViewModel(
            AuthSession(FakeTokenStorage(), repository)
        )

        composeRule.setContent {
            AppNavigation(viewModel = viewModel)
        }

        composeRule.onNodeWithText("E-mail")
            .performTextInput("user@example.com")
        composeRule.onNodeWithText("Hasło")
            .performTextInput("Password123!")
        composeRule.onNodeWithText("Zaloguj się")
            .performClick()

        composeRule.onNodeWithText(
            "Brak połączenia z serwerem. Sprawdź internet i spróbuj ponownie."
        ).assertIsDisplayed()

        composeRule.runOnIdle {
            assertTrue(loginCalled)
        }
    }
}
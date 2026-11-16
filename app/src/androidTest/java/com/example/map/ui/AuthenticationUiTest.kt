package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.viewmodels.AuthUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.assertIsNotEnabled

@RunWith(AndroidJUnit4::class)
class AuthenticationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginPassesEnteredEmailAndPassword() {
        var receivedEmail: String? = null
        var receivedPassword: String? = null

        composeRule.setContent {
            LoginScreen(
                state = AuthUiState(isAuthenticated = false),
                onLoginClick = { email, password ->
                    receivedEmail = email
                    receivedPassword = password
                },
                onInputsChanged = {}
            )
        }

        composeRule.onNodeWithText("E-mail").performTextInput("user@example.com")
        composeRule.onNodeWithText("Hasło").performTextInput("Password123!")
        composeRule.onNodeWithText("Zaloguj się").performClick()

        composeRule.runOnIdle {
            assertEquals("user@example.com", receivedEmail)
            assertEquals("Password123!", receivedPassword)
        }
    }

    @Test
    fun loginDisplaysValidationErrorsFromUiState() {
        composeRule.setContent {
            LoginScreen(
                state = AuthUiState(
                    isAuthenticated = false,
                    emailError = "Wpisz poprawny adres e-mail.",
                    passwordError = "Wpisz hasło."
                ),
                onLoginClick = { _, _ -> },
                onInputsChanged = {}
            )
        }

        composeRule.onNodeWithText("Wpisz poprawny adres e-mail.").assertIsDisplayed()
        composeRule.onNodeWithText("Wpisz hasło.").assertIsDisplayed()
    }

    @Test
    fun loginDisplaysServerOrNetworkError() {
        composeRule.setContent {
            LoginScreen(
                state = AuthUiState(
                    isAuthenticated = false,
                    errorMessage = "Brak połączenia z serwerem."
                ),
                onLoginClick = { _, _ -> },
                onInputsChanged = {}
            )
        }

        composeRule.onNodeWithText("Brak połączenia z serwerem.").assertIsDisplayed()
    }

    @Test
    fun homeDisplaysLogoutButtonAndInvokesCallback() {
        var logoutCalled = false

        composeRule.setContent {
            HomeScreen(onLogoutClick = { logoutCalled = true })
        }

        composeRule.onNodeWithContentDescription("Wyloguj").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wyloguj").performClick()

        composeRule.runOnIdle {
            assertTrue(logoutCalled)
        }
    }

    @Test
    fun loadingStateDisablesLoginInputs() {
        composeRule.setContent {
            LoginScreen(
                state = AuthUiState(
                    isAuthenticated = false,
                    isLoading = true
                ),
                onLoginClick = { _, _ -> },
                onInputsChanged = {}
            )
        }

        composeRule.onNodeWithText("E-mail").assertIsNotEnabled()
        composeRule.onNodeWithText("Hasło").assertIsNotEnabled()
    }
}
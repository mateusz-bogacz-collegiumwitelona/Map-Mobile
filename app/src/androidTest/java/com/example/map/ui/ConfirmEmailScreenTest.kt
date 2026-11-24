package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.map.ui.screens.ConfirmEmailScreen
import com.example.map.ui.viewmodels.ConfirmEmailUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmEmailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysPendingEmailWhenPresent() {
        composeRule.setContent {
            ConfirmEmailScreen(
                uiState = ConfirmEmailUiState(email = "jan@example.com"),
                onCodeChange = {},
                onConfirmClick = {},
                onResendClick = {},
                onBackClick = {}
            )
        }

        composeRule.onNodeWithTag("confirm_email_address")
            .assertIsDisplayed()
        composeRule.onNodeWithText("jan@example.com")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_email_submit")
            .assertIsEnabled()
    }

    @Test
    fun displaysFallbackWhenEmailIsBlank() {
        composeRule.setContent {
            ConfirmEmailScreen(
                uiState = ConfirmEmailUiState(email = ""),
                onCodeChange = {},
                onConfirmClick = {},
                onResendClick = {},
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Brak adresu w pamięci")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_email_submit")
            .assertIsNotEnabled()
    }

    @Test
    fun enteringCodeAndClickingSubmitInvokesCallbacks() {
        var latestCode = ""
        var confirmClicks = 0

        composeRule.setContent {
            ConfirmEmailScreen(
                uiState = ConfirmEmailUiState(email = "jan@example.com"),
                onCodeChange = { latestCode = it },
                onConfirmClick = { confirmClicks++ },
                onResendClick = {},
                onBackClick = {}
            )
        }

        composeRule.onNodeWithTag("confirm_email_code")
            .performTextInput("123456")
        composeRule.onNodeWithTag("confirm_email_submit")
            .performClick()

        assertEquals("123456", latestCode)
        assertEquals(1, confirmClicks)
    }

    @Test
    fun displaysFieldErrorGeneralErrorAndInfoMessage() {
        composeRule.setContent {
            ConfirmEmailScreen(
                uiState = ConfirmEmailUiState(
                    email = "jan@example.com",
                    codeError = "Wpisz 6-cyfrowy kod weryfikacyjny.",
                    errorMessage = "Nieprawidłowy lub wygasły kod weryfikacyjny.",
                    infoMessage = "Wysłano nowy kod weryfikacyjny."
                ),
                onCodeChange = {},
                onConfirmClick = {},
                onResendClick = {},
                onBackClick = {}
            )
        }

        composeRule.onNodeWithText("Wpisz 6-cyfrowy kod weryfikacyjny.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_email_error")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Nieprawidłowy lub wygasły kod weryfikacyjny.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_email_info")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Wysłano nowy kod weryfikacyjny.")
            .assertIsDisplayed()
    }

    @Test
    fun resendAndBackButtonsInvokeCallbacks() {
        var resendClicks = 0
        var backClicks = 0

        composeRule.setContent {
            ConfirmEmailScreen(
                uiState = ConfirmEmailUiState(email = "jan@example.com"),
                onCodeChange = {},
                onConfirmClick = {},
                onResendClick = { resendClicks++ },
                onBackClick = { backClicks++ }
            )
        }

        composeRule.onNodeWithTag("confirm_email_resend")
            .performClick()
        composeRule.onNodeWithTag("confirm_email_back_button")
            .performClick()

        assertEquals(1, resendClicks)
        assertEquals(1, backClicks)
    }

    @Test
    fun disablesControlsDuringVerificationDelayAfterSuccess() {
        composeRule.setContent {
            ConfirmEmailScreen(
                uiState = ConfirmEmailUiState(
                    email = "jan@example.com",
                    code = "123456",
                    isVerified = true,
                    infoMessage = "Adres e-mail potwierdzony. Konto zostało zarejestrowane."
                ),
                onCodeChange = {},
                onConfirmClick = {},
                onResendClick = {},
                onBackClick = {}
            )
        }

        composeRule.onNodeWithTag("confirm_email_code")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("confirm_email_submit")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("confirm_email_resend")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("confirm_email_back_button")
            .assertIsNotEnabled()
    }
}
package com.example.map.ui

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.RegistrationError
import com.example.map.domain.model.RegistrationResult
import com.example.map.domain.session.AuthSession
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeTokenStorage
import com.example.map.ui.auth.RegisterScreen
import com.example.map.ui.auth.RegisterViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel
    private var backClicks = 0
    private var registeredEmail: String? = null

    @Before
    fun setUp() {
        backClicks = 0
        registeredEmail = null
        repository = FakeAuthRepository()
        viewModel = RegisterViewModel(
            AuthSession(
                tokenStorage = FakeTokenStorage(),
                authRepository = repository
            )
        )

        composeRule.setContent {
            RegisterScreen(
                onRegisterSuccess = { email -> registeredEmail = email },
                onNavigateBackToLogin = { backClicks++ },
                viewModel = viewModel
            )
        }
    }

    @Test
    fun backButtonInvokesNavigationCallback() {
        composeRule.onNodeWithTag("register_back_button").performClick()

        assertEquals(1, backClicks)
    }

    @Test
    fun visibilityToggleUpdatesSwitchState() {
        composeRule
            .onNodeWithTag("register_visibility_toggle")
            .performScrollTo()
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun passwordVisibilityToggleChangesContentDescription() {
        composeRule
            .onNodeWithTag("register_password")
            .performScrollTo()
            .performTextInput("Password1!")

        composeRule
            .onNodeWithContentDescription("Pokaż hasło")
            .performClick()

        composeRule
            .onNodeWithContentDescription("Ukryj hasło")
            .assertIsDisplayed()
    }

    @Test
    fun submittingInvalidFormShowsErrorWithoutCallingBackend() {
        composeRule
            .onNodeWithTag("register_first_name")
            .performScrollTo()
            .performTextInput("Jan")
        composeRule
            .onNodeWithTag("register_last_name")
            .performScrollTo()
            .performTextInput("Kowalski")
        composeRule
            .onNodeWithTag("register_email")
            .performScrollTo()
            .performTextInput("jan@example.com")
        composeRule
            .onNodeWithTag("register_password")
            .performScrollTo()
            .performTextInput("slabe")
        composeRule
            .onNodeWithTag("register_confirm_password")
            .performScrollTo()
            .performTextInput("inne")

        composeRule
            .onNodeWithTag("register_submit")
            .performScrollTo()
            .performClick()

        composeRule
            .onNodeWithText("Hasła nie są takie same.")
            .performScrollTo()
            .assertIsDisplayed()

        assertEquals(0, repository.registerCalls)
        assertNull(registeredEmail)
    }

    @Test
    fun inFlightRegistrationDisablesFormAndShowsProgress() {
        val gate = CompletableDeferred<Unit>()
        repository.registerGate = gate
        fillValidForm()

        try {
            composeRule
                .onNodeWithTag("register_submit")
                .performScrollTo()
                .performClick()

            composeRule.waitForIdle()

            try {
                composeRule.waitUntil(timeoutMillis = 5_000) {
                    repository.registerCalls == 1 && viewModel.uiState.value.isLoading
                }
            } catch (ex: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError(
                    "Timeout czekania na start rejestracji. registerCalls=${repository.registerCalls}, uiState=${viewModel.uiState.value}",
                    ex
                )
            }

            composeRule.onNodeWithTag("register_first_name").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_last_name").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_email").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_password").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_confirm_password").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_bio").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_visibility_toggle").assertIsNotEnabled()
            composeRule.onNodeWithTag("register_submit").assertIsNotEnabled()
            composeRule.onNode(
                hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
            ).assertIsDisplayed()

            composeRule.runOnIdle {
                gate.complete(Unit)
            }

            composeRule.waitUntil(timeoutMillis = 5_000) {
                registeredEmail == "jan@example.com"
            }
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            if (!gate.isCompleted) {
                gate.complete(Unit)
            }
        }
    }

    @Test
    fun networkFailureShowsErrorAndDoesNotNavigate() {
        repository.registerResult =
            RegistrationResult.Failure(RegistrationError.NETWORK)
        fillValidForm()

        composeRule
            .onNodeWithTag("register_submit")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorMessage != null
        }

        composeRule
            .onNodeWithText("Brak połączenia z internetem.")
            .performScrollTo()
            .assertIsDisplayed()
        assertNull(registeredEmail)
    }

    @Test
    fun validRegistrationTriggersOnRegisterSuccessWithTrimmedEmail() {
        fillValidForm(email = "  jan@example.com  ")

        composeRule
            .onNodeWithTag("register_submit")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            registeredEmail != null
        }

        assertEquals("jan@example.com", registeredEmail)
        assertEquals(1, repository.registerCalls)
    }

    private fun fillValidForm(email: String = "jan@example.com") {
        composeRule.onNodeWithTag("register_first_name").performScrollTo().performTextInput("Jan")
        composeRule.onNodeWithTag("register_last_name").performScrollTo().performTextInput("Kowalski")
        composeRule.onNodeWithTag("register_email").performScrollTo().performTextInput(email)
        composeRule.onNodeWithTag("register_password").performScrollTo().performTextInput("Password1!")
        composeRule.onNodeWithTag("register_confirm_password").performScrollTo().performTextInput("Password1!")
    }
}
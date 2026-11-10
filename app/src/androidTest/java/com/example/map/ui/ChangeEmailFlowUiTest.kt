package com.example.map.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.usecase.AccountSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.ui.screens.ChangeEmailScreen
import com.example.map.ui.viewmodels.ChangeEmailStep
import com.example.map.ui.viewmodels.ChangeEmailViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChangeEmailFlowUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeAccountRepository
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var viewModel: ChangeEmailViewModel

    @Before
    fun setUp() {
        repository = FakeAccountRepository()
        profileStorage = FakeProfileStorage("current@example.com")
        viewModel = ChangeEmailViewModel(AccountSession(repository, profileStorage))

        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            ChangeEmailScreen(
                uiState = uiState,
                onNewEmailChange = viewModel::onNewEmailChange,
                onCodeChange = viewModel::onCodeChange,
                onSubmitEmail = viewModel::submitNewEmail,
                onSubmitCode = viewModel::submitCode,
                onBackToEmail = viewModel::backToEmailEntry,
                onDone = {},
                onBackClick = {}
            )
        }
    }

    @Test
    fun invalidEmailShowsInlineErrorAndDisablesSubmit() {
        composeRule.onNode(hasSetTextAction()).performTextInput("not-an-email")

        composeRule.onNodeWithText("Podaj prawidłowy adres e-mail.").assertIsDisplayed()
        composeRule.onNodeWithTag("change_email_submit_email").assertIsNotEnabled()
    }

    @Test
    fun overlyLongEmailShowsLengthErrorAndDisablesSubmit() {
        composeRule.onNode(hasSetTextAction())
            .performTextInput("a".repeat(250) + "@x.com" + "b")

        composeRule.onNodeWithText("Adres e-mail może mieć maksymalnie 256 znaków.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("change_email_submit_email").assertIsNotEnabled()
    }

    @Test
    fun emailMatchingCurrentAddressShowsErrorAndDisablesSubmit() {
        composeRule.onNode(hasSetTextAction())
            .performTextInput(" CURRENT@example.com ")

        composeRule.onNodeWithText("Nowy adres e-mail musi różnić się od obecnego.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("change_email_submit_email").assertIsNotEnabled()
    }

    @Test
    fun validEmailMovesToVerificationAndCodeConfirmsChange() {
        composeRule.onNode(hasSetTextAction()).performTextInput("new@example.com")
        composeRule.onNodeWithTag("change_email_submit_email")
            .assertIsEnabled()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.step == ChangeEmailStep.CODE_VERIFICATION
        }
        composeRule.onNodeWithText("Potwierdź nowy adres").assertIsDisplayed()
        composeRule.onNodeWithText("new@example.com").assertIsDisplayed()

        composeRule.onNode(hasSetTextAction()).performTextInput("123456")
        composeRule.onNodeWithTag("change_email_submit_code")
            .assertIsEnabled()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.step == ChangeEmailStep.SUCCESS
        }
        composeRule.onNodeWithText("Adres e-mail został zmieniony").assertIsDisplayed()
        assertEquals("new@example.com", profileStorage.getEmail())
        assertEquals("new@example.com", viewModel.uiState.value.normalizedNewEmail)
    }

    @Test
    fun emailTakenFailureShowsInlineError() {
        repository.requestResult =
            EmailChangeResult.Failure(EmailChangeError.EMAIL_TAKEN)

        composeRule.onNode(hasSetTextAction()).performTextInput("taken@example.com")
        composeRule.onNodeWithTag("change_email_submit_email").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorMessage != null
        }
        composeRule.onNodeWithText("Ten adres e-mail jest już używany.").assertIsDisplayed()
    }

    @Test
    fun invalidCodeFailureShowsInlineErrorOnVerificationStep() {
        composeRule.onNode(hasSetTextAction()).performTextInput("new@example.com")
        composeRule.onNodeWithTag("change_email_submit_email").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.step == ChangeEmailStep.CODE_VERIFICATION
        }

        repository.confirmResult =
            EmailChangeResult.Failure(EmailChangeError.INVALID_CODE)
        composeRule.onNode(hasSetTextAction()).performTextInput("123456")
        composeRule.onNodeWithTag("change_email_submit_code").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorMessage != null
        }
        composeRule.onNodeWithText("Nieprawidłowy kod weryfikacyjny.")
            .assertIsDisplayed()
        assertEquals(ChangeEmailStep.CODE_VERIFICATION, viewModel.uiState.value.step)
    }

    @Test
    fun inFlightRequestDisablesInputAndSubmitButton() {
        repository.requestGate = CompletableDeferred()

        composeRule.onNode(hasSetTextAction()).performTextInput("new@example.com")
        composeRule.onNodeWithTag("change_email_submit_email").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isSubmitting
        }
        composeRule.onNodeWithText("new@example.com").assertIsNotEnabled()
        composeRule.onNodeWithTag("change_email_submit_email").assertIsNotEnabled()

        composeRule.runOnIdle {
            repository.requestGate?.complete(Unit)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.step == ChangeEmailStep.CODE_VERIFICATION
        }
    }
}

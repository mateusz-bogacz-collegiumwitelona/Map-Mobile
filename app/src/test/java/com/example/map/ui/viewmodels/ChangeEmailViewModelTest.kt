package com.example.map.ui.viewmodels

import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.usecase.AccountSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangeEmailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeAccountRepository
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var accountSession: AccountSession
    private lateinit var viewModel: ChangeEmailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAccountRepository()
        profileStorage = FakeProfileStorage("current@example.com")
        accountSession = AccountSession(repository, profileStorage)
        viewModel = ChangeEmailViewModel(accountSession)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankEmailIsRejectedClientSide() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("   ")

        val state = viewModel.uiState.value
        assertNull(state.emailValidationError)
        assertFalse(state.canSubmitEmail)

        viewModel.submitNewEmail()
        advanceUntilIdle()

        assertEquals(0, repository.requestCalls)
        assertEquals(ChangeEmailStep.EMAIL_ENTRY, viewModel.uiState.value.step)
    }

    @Test
    fun malformedEmailShowsValidationErrorAndCannotBeSubmitted() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("not-an-email")

        val state = viewModel.uiState.value
        assertEquals("Podaj prawidłowy adres e-mail.", state.emailValidationError)
        assertFalse(state.canSubmitEmail)
    }

    @Test
    fun overlyLongEmailIsRejectedClientSide() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("a".repeat(300) + "@example.com")

        val state = viewModel.uiState.value
        assertEquals(256, state.newEmail.length)
        assertNotNull(state.emailValidationError)
        assertFalse(state.canSubmitEmail)
    }

    @Test
    fun emailMatchingCurrentAddressIsRejectedCaseInsensitively() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange(" CURRENT@example.com ")

        val state = viewModel.uiState.value
        assertEquals(
            "Nowy adres e-mail musi różnić się od obecnego.",
            state.emailValidationError
        )
        assertFalse(state.canSubmitEmail)
    }

    @Test
    fun validNewEmailMovesToVerificationStep() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("new@example.com")
        assertTrue(viewModel.uiState.value.canSubmitEmail)

        viewModel.submitNewEmail()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChangeEmailStep.CODE_VERIFICATION, state.step)
        assertEquals("new@example.com", repository.lastRequestedEmail)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun validCodeConfirmsChangeAndUpdatesLocalSession() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("new@example.com")
        viewModel.submitNewEmail()
        advanceUntilIdle()

        viewModel.onCodeChange("12ab34567")
        assertEquals("123456", viewModel.uiState.value.code)
        assertTrue(viewModel.uiState.value.canSubmitCode)

        viewModel.submitCode()
        advanceUntilIdle()

        assertEquals(ChangeEmailStep.SUCCESS, viewModel.uiState.value.step)
        assertEquals("123456", repository.lastConfirmedCode)
        assertEquals("new@example.com", profileStorage.getEmail())
        assertEquals("new@example.com", accountSession.email.value)
    }

    @Test
    fun codeShorterThanSixDigitsCannotBeSubmitted() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("new@example.com")
        viewModel.submitNewEmail()
        advanceUntilIdle()

        viewModel.onCodeChange("12345")
        assertFalse(viewModel.uiState.value.canSubmitCode)

        viewModel.submitCode()
        advanceUntilIdle()

        assertEquals(0, repository.confirmCalls)
        assertEquals(ChangeEmailStep.CODE_VERIFICATION, viewModel.uiState.value.step)
    }

    @Test
    fun emailTakenFailureShowsInlineMessageAndStaysOnEmailStep() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.requestResult = EmailChangeResult.Failure(EmailChangeError.EMAIL_TAKEN)

        viewModel.onNewEmailChange("taken@example.com")
        viewModel.submitNewEmail()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChangeEmailStep.EMAIL_ENTRY, state.step)
        assertEquals("Ten adres e-mail jest już używany.", state.errorMessage)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun invalidCodeFailureShowsInlineMessageAndStaysOnVerificationStep() =
        runTest(dispatcher) {
            advanceUntilIdle()

            viewModel.onNewEmailChange("new@example.com")
            viewModel.submitNewEmail()
            advanceUntilIdle()

            repository.confirmResult = EmailChangeResult.Failure(EmailChangeError.INVALID_CODE)
            viewModel.onCodeChange("123456")
            viewModel.submitCode()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ChangeEmailStep.CODE_VERIFICATION, state.step)
            assertEquals("Nieprawidłowy kod weryfikacyjny.", state.errorMessage)
            assertFalse(state.isSubmitting)
        }

    @Test
    fun expiredCodeFailureShowsInlineMessage() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("new@example.com")
        viewModel.submitNewEmail()
        advanceUntilIdle()

        repository.confirmResult = EmailChangeResult.Failure(EmailChangeError.CODE_EXPIRED)
        viewModel.onCodeChange("123456")
        viewModel.submitCode()
        advanceUntilIdle()

        assertEquals(
            "Kod weryfikacyjny wygasł. Wróć i wyślij nowy kod.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun networkFailureShowsInlineMessage() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.requestResult = EmailChangeResult.Failure(EmailChangeError.NETWORK)

        viewModel.onNewEmailChange("new@example.com")
        viewModel.submitNewEmail()
        advanceUntilIdle()

        assertEquals(
            "Brak połączenia z internetem. Spróbuj ponownie.",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun inFlightCodeConfirmationDisablesSubmitAndBlocksDuplicates() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onNewEmailChange("new@example.com")
        viewModel.submitNewEmail()
        advanceUntilIdle()

        repository.confirmGate = CompletableDeferred()
        viewModel.onCodeChange("123456")
        viewModel.submitCode()
        runCurrent()

        val loadingState = viewModel.uiState.value
        assertTrue(loadingState.isSubmitting)
        assertFalse(loadingState.canSubmitCode)
        assertEquals(1, repository.confirmCalls)

        viewModel.submitCode()
        runCurrent()
        assertEquals(1, repository.confirmCalls)

        repository.confirmGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(ChangeEmailStep.SUCCESS, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun inFlightRequestDisablesSubmissionAndBlocksDuplicates() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.requestGate = CompletableDeferred()

        viewModel.onNewEmailChange("new@example.com")
        viewModel.submitNewEmail()
        runCurrent()

        val loadingState = viewModel.uiState.value
        assertTrue(loadingState.isSubmitting)
        assertFalse(loadingState.canSubmitEmail)
        assertEquals(1, repository.requestCalls)

        viewModel.submitNewEmail()
        runCurrent()
        assertEquals(1, repository.requestCalls)

        repository.requestGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals(ChangeEmailStep.CODE_VERIFICATION, viewModel.uiState.value.step)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }
}

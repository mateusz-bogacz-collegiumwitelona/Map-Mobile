package com.example.map.ui.viewmodels

import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeAccountRepository
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var accountSession: AccountSession
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAccountRepository()
        profileStorage = FakeProfileStorage("user@example.com")
        accountSession = AccountSession(repository, profileStorage)
        viewModel = SettingsViewModel(accountSession)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun settingsEmailUpdatesImmediatelyWhenSessionChanges() = runTest(dispatcher) {
        advanceUntilIdle()
        assertEquals("user@example.com", viewModel.uiState.value.email)

        accountSession.rememberEmail("new@example.com")
        advanceUntilIdle()

        assertEquals("new@example.com", viewModel.uiState.value.email)
        assertEquals("new@example.com", profileStorage.getEmail())
    }

    @Test
    fun showDeleteDialogDoesNotCallApi() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.showDeleteDialog()

        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)
        assertEquals(0, repository.deletionCalls)
    }

    @Test
    fun cancelClosesDialogWithoutCallingApiOrClearingProfile() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.showDeleteDialog()
        viewModel.dismissDeleteDialog()

        assertFalse(viewModel.uiState.value.isDeleteDialogVisible)
        assertNull(viewModel.uiState.value.deleteErrorMessage)
        assertEquals(0, repository.deletionCalls)
        assertEquals("user@example.com", profileStorage.getEmail())
    }

    @Test
    fun successfulDeletionInvokesApiAndReportsDeleted() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.isDeletingAccount)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.deletionCalls)
        assertFalse(state.isDeletingAccount)
        assertFalse(state.isDeleteDialogVisible)
        assertTrue(state.isAccountDeleted)
        assertNull(state.deleteErrorMessage)
    }

    @Test
    fun deletionFailureKeepsDialogOpenAndShowsErrorMessage() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.deletionResult =
            AccountDeletionResult.Failure(AccountDeletionError.NETWORK)

        viewModel.showDeleteDialog()
        viewModel.deleteAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.deletionCalls)
        assertTrue(state.isDeleteDialogVisible)
        assertFalse(state.isDeletingAccount)
        assertFalse(state.isAccountDeleted)
        assertEquals(
            "Brak połączenia z internetem. Spróbuj ponownie.",
            state.deleteErrorMessage
        )
    }

    @Test
    fun unauthorizedDeletionFailureShowsSessionMessage() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.deletionResult =
            AccountDeletionResult.Failure(AccountDeletionError.UNAUTHORIZED)

        viewModel.showDeleteDialog()
        viewModel.deleteAccount()
        advanceUntilIdle()

        assertEquals(
            "Sesja wygasła. Zaloguj się ponownie.",
            viewModel.uiState.value.deleteErrorMessage
        )
    }

    @Test
    fun remainingDeletionFailuresShowInlineMessages() = runTest(dispatcher) {
        advanceUntilIdle()
        val cases = listOf(
            AccountDeletionError.NOT_FOUND to
                "Konto nie istnieje lub zostało już usunięte.",
            AccountDeletionError.RATE_LIMITED to
                "Zbyt wiele żądań. Odczekaj chwilę i spróbuj ponownie.",
            AccountDeletionError.UNKNOWN to
                "Nie udało się usunąć konta. Spróbuj ponownie."
        )

        cases.forEach { (error, expectedMessage) ->
            repository.deletionResult = AccountDeletionResult.Failure(error)
            viewModel.showDeleteDialog()
            viewModel.deleteAccount()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(expectedMessage, state.deleteErrorMessage)
            assertTrue(state.isDeleteDialogVisible)
            assertFalse(state.isAccountDeleted)
        }
    }

    @Test
    fun inFlightDeletionBlocksDuplicateCallsAndDismissal() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.deletionGate = CompletableDeferred()

        viewModel.showDeleteDialog()
        viewModel.deleteAccount()
        runCurrent()

        assertTrue(viewModel.uiState.value.isDeletingAccount)
        assertEquals(1, repository.deletionCalls)

        viewModel.deleteAccount()
        viewModel.dismissDeleteDialog()
        runCurrent()

        assertEquals(1, repository.deletionCalls)
        assertTrue(viewModel.uiState.value.isDeleteDialogVisible)

        repository.deletionGate?.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAccountDeleted)
    }
}

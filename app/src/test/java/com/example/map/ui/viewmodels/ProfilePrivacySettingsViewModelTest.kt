package com.example.map.ui.viewmodels

import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.usecase.AuthSession
import com.example.map.domain.usecase.ProfileSession
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeSessionTokenStorage
import com.example.map.testing.FakeUserProfileRepository
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
class ProfilePrivacySettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeUserProfileRepository
    private lateinit var storage: FakeProfileStorage
    private lateinit var profileSession: ProfileSession
    private lateinit var viewModel: ProfilePrivacySettingsViewModel

    private val storedProfile = UserProfile(
        email = "user@example.com",
        firstName = "Jan",
        lastName = "Kowalski",
        isPublic = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        createViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun populatesFieldsFromStoredProfile() = runTest(dispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("user@example.com", state.email)
        assertEquals("Jan", state.firstName)
        assertEquals("Kowalski", state.lastName)
        assertFalse(state.isPublic)
        assertNull(state.firstNameError)
        assertNull(state.lastNameError)
        assertFalse(state.canSave)
    }

    @Test
    fun doesNotHighlightEmptyFieldsBeforeUserInteraction() = runTest(dispatcher) {
        createViewModel(UserProfile())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.firstNameError)
        assertNull(state.lastNameError)
        assertFalse(state.canSave)
    }

    @Test
    fun doesNotRefreshWithoutSavedSession() = runTest(dispatcher) {
        createViewModel(accessToken = null)
        advanceUntilIdle()

        assertEquals(0, repository.getProfileCalls)
    }

    @Test
    fun refreshesProfileWhenSessionExists() = runTest(dispatcher) {
        createViewModel()
        repository.remoteProfile = UserProfile(
            firstName = "Anna",
            lastName = "Nowak",
            isPublic = true
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Anna", state.firstName)
        assertEquals("Nowak", state.lastName)
        assertTrue(state.isPublic)
        assertEquals("user@example.com", state.email)
        assertFalse(state.canSave)
    }

    @Test
    fun blankFirstNameShowsErrorAndBlocksSave() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onFirstNameChange("   ")

        val state = viewModel.uiState.value
        assertEquals("Imię jest wymagane.", state.firstNameError)
        assertFalse(state.canSave)

        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, repository.updateNamesCalls)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun tooShortLastNameShowsError() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onLastNameChange("N")

        assertEquals(
            "Nazwisko musi mieć co najmniej 2 znaki.",
            viewModel.uiState.value.lastNameError
        )
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun tooLongFirstNameShowsError() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onFirstNameChange("a".repeat(51))

        assertEquals(
            "Imię może mieć maksymalnie 50 znaków.",
            viewModel.uiState.value.firstNameError
        )
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun validChangeEnablesSave() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onFirstNameChange("Anna")

        val state = viewModel.uiState.value
        assertNull(state.firstNameError)
        assertTrue(state.canSave)
    }

    @Test
    fun visibilityToggleMarksFormAsChanged() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onVisibilityChange(true)

        val state = viewModel.uiState.value
        assertTrue(state.isPublic)
        assertTrue(state.hasChanges)
        assertTrue(state.canSave)
    }

    @Test
    fun saveSendsOnlyChangedNames() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onFirstNameChange("Anna")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.updateNamesCalls)
        assertEquals(0, repository.setVisibilityCalls)
        assertEquals("Anna" to "Kowalski", repository.lastNames)
        assertEquals("Anna", storage.getProfile()?.firstName)
        assertEquals("Anna", profileSession.profile.value.firstName)
        assertEquals("Zapisano zmiany.", state.successMessage)
        assertNull(state.errorMessage)
        assertFalse(state.isSaving)
        assertFalse(state.canSave)
    }

    @Test
    fun saveTrimsTypedNamesBeforeSending() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onLastNameChange("  Nowak  ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Jan" to "Nowak", repository.lastNames)
        assertEquals("Nowak", viewModel.uiState.value.lastName)
        assertEquals("Nowak", storage.getProfile()?.lastName)
    }

    @Test
    fun saveSendsOnlyVisibilityWhenNamesUnchanged() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onVisibilityChange(true)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(0, repository.updateNamesCalls)
        assertEquals(1, repository.setVisibilityCalls)
        assertEquals(true, repository.lastVisibility)
        assertEquals(true, storage.getProfile()?.isPublic)
        assertEquals("Zapisano zmiany.", viewModel.uiState.value.successMessage)
    }

    @Test
    fun savePersistsNamesAndVisibility() = runTest(dispatcher) {
        advanceUntilIdle()

        viewModel.onFirstNameChange("Anna")
        viewModel.onLastNameChange("Nowak")
        viewModel.onVisibilityChange(true)
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Anna" to "Nowak", repository.lastNames)
        assertEquals(true, repository.lastVisibility)
        assertEquals(
            UserProfile(
                email = "user@example.com",
                firstName = "Anna",
                lastName = "Nowak",
                isPublic = true
            ),
            storage.getProfile()
        )
    }

    @Test
    fun inFlightSaveDisablesFormAndBlocksDuplicates() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.updateNamesGate = CompletableDeferred()

        viewModel.onFirstNameChange("Anna")
        viewModel.save()
        runCurrent()

        val loadingState = viewModel.uiState.value
        assertTrue(loadingState.isSaving)
        assertFalse(loadingState.canSave)
        assertEquals(1, repository.updateNamesCalls)

        viewModel.save()
        runCurrent()
        assertEquals(1, repository.updateNamesCalls)

        repository.updateNamesGate?.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Zapisano zmiany.", viewModel.uiState.value.successMessage)
    }

    @Test
    fun nameFailureShowsMessageAndKeepsStoredProfile() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.updateNamesResult =
            ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK)

        viewModel.onFirstNameChange("Anna")
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Brak połączenia z internetem. Spróbuj ponownie.",
            state.errorMessage
        )
        assertNull(state.successMessage)
        assertFalse(state.isSaving)
        assertEquals("Jan", storage.getProfile()?.firstName)
        assertEquals("Jan", profileSession.profile.value.firstName)
    }

    @Test
    fun visibilityFailureKeepsSavedNamesAndReportsPartialSuccess() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.setVisibilityResult =
            ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK)

        viewModel.onFirstNameChange("Anna")
        viewModel.onVisibilityChange(true)
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(
            "Dane zostały zapisane. Brak połączenia z internetem. Spróbuj ponownie.",
            state.errorMessage
        )
        assertEquals("Anna", storage.getProfile()?.firstName)
        assertFalse(storage.getProfile()?.isPublic ?: true)
        assertTrue(state.isPublic)
    }

    @Test
    fun remainingFailuresShowMappedMessages() = runTest(dispatcher) {
        val cases = listOf(
            ProfileUpdateError.FIRST_NAME_INVALID to "Imię musi mieć od 2 do 50 znaków.",
            ProfileUpdateError.LAST_NAME_INVALID to
                "Nazwisko musi mieć od 2 do 50 znaków.",
            ProfileUpdateError.UNAUTHORIZED to "Sesja wygasła. Zaloguj się ponownie.",
            ProfileUpdateError.NOT_FOUND to
                "Konto nie istnieje lub zostało usunięte.",
            ProfileUpdateError.RATE_LIMITED to
                "Zbyt wiele żądań. Odczekaj chwilę i spróbuj ponownie."
        )

        cases.forEach { (error, expectedMessage) ->
            createViewModel()
            repository.updateNamesResult = ProfileUpdateResult.Failure(error)
            advanceUntilIdle()

            viewModel.onFirstNameChange("Anna")
            viewModel.save()
            advanceUntilIdle()

            assertEquals(expectedMessage, viewModel.uiState.value.errorMessage)
        }
    }

    @Test
    fun unknownFailureUsesBackendMessage() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.updateNamesResult =
            ProfileUpdateResult.Failure(ProfileUpdateError.UNKNOWN, "Server error")

        viewModel.onFirstNameChange("Anna")
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Server error", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun editingClearsPreviousMessages() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.updateNamesResult =
            ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK)

        viewModel.onFirstNameChange("Anna")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(
            "Brak połączenia z internetem. Spróbuj ponownie.",
            viewModel.uiState.value.errorMessage
        )

        repository.updateNamesResult = ProfileUpdateResult.Success
        viewModel.onLastNameChange("Nowak")

        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
    }

    private fun createViewModel(
        profile: UserProfile = storedProfile,
        accessToken: String? = "access-token"
    ) {
        repository = FakeUserProfileRepository().apply { remoteProfile = profile }
        storage = FakeProfileStorage(profile)
        profileSession = ProfileSession(repository, storage)
        viewModel = ProfilePrivacySettingsViewModel(
            profileSession = profileSession,
            authSession = AuthSession(FakeSessionTokenStorage(accessToken))
        )
    }
}

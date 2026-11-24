package com.example.map.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.session.AccountSession
import com.example.map.domain.session.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeUserProfileRepository
import com.example.map.ui.screens.ProfilePrivacySettingsScreen
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilePrivacySettingsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeUserProfileRepository
    private lateinit var storage: FakeProfileStorage
    private lateinit var accountSession: AccountSession
    private lateinit var viewModel: ProfilePrivacySettingsViewModel

    @Before
    fun setUp() {
        repository = FakeUserProfileRepository()
        storage = FakeProfileStorage(
            initialProfile = UserProfile(
                firstName = "Jan",
                lastName = "Kowalski",
                isPublic = false
            ),
            initialEmail = "user@example.com"
        )
        accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = repository,
            profileStorage = storage
        )
        viewModel = ProfilePrivacySettingsViewModel(
            accountSession = accountSession,
            authSession = AuthSession(
                FakeTokenStorage(
                    AuthTokens(
                        accessToken = "access-token",
                        refreshToken = "refresh-token"
                    )
                ),
                FakeAuthRepository()
            )
        )

        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsState()
            ProfilePrivacySettingsScreen(
                uiState = uiState,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onVisibilityChange = viewModel::onVisibilityChange,
                onSaveClick = viewModel::save,
                onBackClick = {}
            )
        }
    }

    @Test
    fun showsStoredProfileData() {
        composeRule.onNodeWithText("Profil i prywatność").assertIsDisplayed()
        composeRule.onNodeWithText("user@example.com").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_first_name").assertTextContains("Jan")
        composeRule.onNodeWithTag("profile_last_name").assertTextContains("Kowalski")
        composeRule.onNodeWithTag("profile_visibility_toggle").assertIsOff()
        composeRule.onNodeWithText("Profil prywatny – widzisz go tylko Ty.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").assertIsNotEnabled()
    }

    @Test
    fun blankFirstNameShowsInlineErrorAndDisablesSave() {
        composeRule.onNodeWithTag("profile_first_name").performTextClearance()

        composeRule.onNodeWithText("Imię jest wymagane.").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").assertIsNotEnabled()
    }

    @Test
    fun tooLongFirstNameShowsInlineErrorAndDisablesSave() {
        composeRule.onNodeWithTag("profile_first_name").performTextClearance()
        composeRule.onNodeWithTag("profile_first_name")
            .performTextInput("a".repeat(51))

        composeRule.onNodeWithText("Imię może mieć maksymalnie 50 znaków.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").assertIsNotEnabled()
    }

    @Test
    fun tooShortLastNameShowsInlineErrorAndDisablesSave() {
        composeRule.onNodeWithTag("profile_last_name").performTextClearance()
        composeRule.onNodeWithTag("profile_last_name").performTextInput("N")

        composeRule.onNodeWithText("Nazwisko musi mieć co najmniej 2 znaki.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").assertIsNotEnabled()
    }

    @Test
    fun togglingVisibilityUpdatesDescriptionAndSaves() {
        composeRule.onNodeWithTag("profile_visibility_toggle")
            .assertIsOff()
            .performClick()
            .assertIsOn()

        composeRule.onNodeWithText("Profil publiczny – widzą go inni użytkownicy.")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("profile_save")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            storage.getProfile()?.isPublic == true
        }

        assertEquals(true, repository.lastVisibility)
        assertEquals(0, repository.updateNamesCalls)
        composeRule.onNodeWithText("Zapisano zmiany.")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun successfulSaveUpdatesSessionAndStorage() {
        composeRule.onNodeWithTag("profile_first_name").performTextClearance()
        composeRule.onNodeWithTag("profile_first_name").performTextInput("Anna")

        composeRule.onNodeWithTag("profile_save").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            storage.getProfile()?.firstName == "Anna"
        }

        assertEquals("Anna" to "Kowalski", repository.lastNames)
        assertEquals("Anna", accountSession.profile.value.firstName)
        composeRule.onNodeWithText("Zapisano zmiany.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("profile_save").assertIsNotEnabled()
    }

    @Test
    fun failedSaveShowsErrorMessageAndKeepsStoredData() {
        repository.updateNamesResult =
            ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK)

        composeRule.onNodeWithTag("profile_first_name").performTextClearance()
        composeRule.onNodeWithTag("profile_first_name").performTextInput("Anna")
        composeRule.onNodeWithTag("profile_save").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.errorMessage != null
        }

        composeRule.onNodeWithText("Brak połączenia z internetem. Spróbuj ponownie.")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals("Jan", storage.getProfile()?.firstName)
    }

    @Test
    fun inFlightSaveDisablesFormAndShowsSpinner() {
        repository.updateNamesGate = CompletableDeferred()

        composeRule.onNodeWithTag("profile_first_name").performTextClearance()
        composeRule.onNodeWithTag("profile_first_name").performTextInput("Anna")
        composeRule.onNodeWithTag("profile_save").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.isSaving
        }

        composeRule.onNodeWithTag("profile_first_name").assertIsNotEnabled()
        composeRule.onNodeWithTag("profile_last_name").assertIsNotEnabled()
        composeRule.onNodeWithTag("profile_visibility_toggle").assertIsNotEnabled()
        composeRule.onNodeWithTag("profile_save").assertIsNotEnabled()
        assertFalse(storage.getProfile()?.firstName == "Anna")

        composeRule.runOnIdle {
            repository.updateNamesGate?.complete(Unit)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.successMessage != null
        }

        assertTrue(storage.getProfile()?.firstName == "Anna")
    }
}
